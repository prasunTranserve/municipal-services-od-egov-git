package org.egov.migration.processor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.egov.migration.business.model.ConnectionDTO;
import org.egov.migration.business.model.ConnectionHolderDTO;
import org.egov.migration.business.model.DemandDTO;
import org.egov.migration.business.model.DemandDetailDTO;
import org.egov.migration.business.model.ProcessInstance;
import org.egov.migration.business.model.WaterConnectionDTO;
import org.egov.migration.config.SystemProperties;
import org.egov.migration.reader.model.WnsConnection;
import org.egov.migration.reader.model.WnsConnectionHolder;
import org.egov.migration.reader.model.WnsConnectionService;
import org.egov.migration.reader.model.WnsDemand;
import org.egov.migration.reader.model.WnsMeterReading;
import org.egov.migration.service.ValidationService;
import org.egov.migration.service.WnsService;
import org.egov.migration.util.MigrationConst;
import org.egov.migration.util.MigrationUtility;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class SwTransformProcessor implements ItemProcessor<WnsConnection, ConnectionDTO> {
	
	public static final String dateFormat = "dd-MM-yy";
	
	public static final BigDecimal appplyRebate = new BigDecimal("0.98");
	
	public static final BigDecimal rebatePercentage = new BigDecimal("0.02");

	private String tenantId;

	private String localityCode;

	@Autowired
	private SystemProperties properties;

	@Autowired
	private ValidationService validationService;
	
	@Autowired
	private WnsService wnsService;

	@Override
	public ConnectionDTO process(WnsConnection connection) throws Exception {
		if (validationService.isValidConnection(connection)) {
			try {
				enrichConnection(connection);
			} catch (Exception e) {
				MigrationUtility.addError(connection.getConnectionNo(), e.getLocalizedMessage());
				return null;
			}
			if(connection.getConnectionFacility()==null) {
//				MigrationUtility.addError(connection.getConnectionNo(), "Not able to determine connection Facility");
				connection.setConnectionFacility(MigrationConst.CONNECTION_WATER);
			}

			ConnectionDTO connectionDTO = transformConnection(connection);
			if(MigrationConst.SERVICE_SEWERAGE.equalsIgnoreCase(connectionDTO.getWaterConnection().getConnectionFacility())) {
				// Enrich connection type to Non metered for pure sewerage connection
				connectionDTO.getWaterConnection().setConnectionType(MigrationConst.CONNECTION_NON_METERED);
				connectionDTO.getWaterConnection().setWaterSource(null);
				return connectionDTO;
			} else if(MigrationConst.SERVICE_WATER_SEWERAGE.equalsIgnoreCase(connectionDTO.getWaterConnection().getConnectionFacility())) {
				return updateJointConnection(connectionDTO);
			}
		}
		return null;
	}

	private ConnectionDTO updateJointConnection(ConnectionDTO connectionDTO) {
		try {
			WaterConnectionDTO waterConnectionDTO = wnsService.getWaterConnection(connectionDTO);
			if(waterConnectionDTO != null) {
				waterConnectionDTO.setConnectionFacility(connectionDTO.getWaterConnection().getConnectionFacility());
				waterConnectionDTO.setProposedPipeSize(connectionDTO.getWaterConnection().getPipeSize());
				waterConnectionDTO.setProposedWaterClosets(connectionDTO.getWaterConnection().getNoOfWaterClosets());
				waterConnectionDTO.setProposedToilets(connectionDTO.getWaterConnection().getNoOfToilets());
				waterConnectionDTO.setPipeSize(connectionDTO.getWaterConnection().getPipeSize());
				waterConnectionDTO.setNoOfWaterClosets(connectionDTO.getWaterConnection().getNoOfWaterClosets());
				waterConnectionDTO.setNoOfToilets(connectionDTO.getWaterConnection().getNoOfToilets());
				
				HashMap<String, Object> additionalDtl = (HashMap<String, Object>) waterConnectionDTO.getAdditionalDetails();
				JsonNode connAddJsonNode = (JsonNode) connectionDTO.getWaterConnection().getAdditionalDetails();
				additionalDtl.put("migratedSewerageFee", connAddJsonNode.get("migratedSewerageFee"));
				if(connAddJsonNode.has("diameter")) {
					additionalDtl.put("diameter", connAddJsonNode.get("diameter"));
				}
				waterConnectionDTO.setAdditionalDetails(additionalDtl);
				
				//
				ProcessInstance processInstance = new ProcessInstance();
				processInstance.setAction("ACTIVATE_CONNECTION");
				
				waterConnectionDTO.setProcessInstance(processInstance);
				//
				
				connectionDTO.setWaterConnection(waterConnectionDTO);
			}
			return connectionDTO;
		} catch (Exception e) {
			MigrationUtility.addError(connectionDTO.getWaterConnection().getOldConnectionNo(), e.getLocalizedMessage());
			return null;
		}
	}

	private void enrichConnection(WnsConnection connection) {
		enrichConnectionFacility(connection);
//		enrichConnectionType(connection);
		enrichConnectionUsageCategory(connection);
	}

	private void enrichConnectionUsageCategory(WnsConnection connection) {

		if(MigrationConst.CONNECTION_METERED.equalsIgnoreCase(connection.getService().getConnectionType())
				&& MigrationConst.USAGE_CATEGORY_OTHERS.equalsIgnoreCase(connection.getService().getUsageCategory())
				&& !MigrationConst.CONNECTION_SEWERAGE.equalsIgnoreCase(connection.getConnectionFacility())) {
			if(connection.getDemands() != null) {
				// get the latest demand
				WnsDemand demand = connection.getDemands().stream()
						.sorted((d1,d2) -> MigrationUtility.getLongDate(d2.getBillingPeriodTo(), dateFormat).compareTo(MigrationUtility.getLongDate(d1.getBillingPeriodTo(), dateFormat)))
						.findFirst().orElse(null);
				
				if(demand != null) {
					BigDecimal waterCharge = MigrationUtility.convertToBigDecimal(demand.getWaterCharges());
					if(waterCharge.compareTo(BigDecimal.valueOf(56)) < 1) {
						if(MigrationUtility.convertToInt(connection.getService().getNoOfTaps()) == null) {
							connection.getService().setUsageCategory("BPL");
							connection.getService().setNoOfTaps("1");
						} else if(MigrationUtility.convertToInt(connection.getService().getNoOfTaps()).equals(1)) {
							connection.getService().setUsageCategory("BPL");
						} else {
							connection.getService().setUsageCategory("DOMESTIC");
						}
					} else if(waterCharge.compareTo(BigDecimal.valueOf(360)) == 0) {
						connection.getService().setUsageCategory("ROADSIDEEATERS");
					}
				}
			}
		}
		
		if(MigrationConst.USAGE_CATEGORY_OTHERS.equalsIgnoreCase(connection.getService().getUsageCategory())
				&& MigrationConst.CONNECTION_SEWERAGE.equalsIgnoreCase(connection.getConnectionFacility())) {
			connection.getService().setUsageCategory("DOMESTIC");
			if(!StringUtils.hasText(connection.getService().getNoOfFlats())) {
				connection.getService().setNoOfFlats("24");
			}
		}
	}

	private void enrichConnectionFacility(WnsConnection connection) {
		if(connection.getDemands() == null)
			return;
		if(!connection.getDemands().isEmpty()) {
			String connectionFacility = connection.getConnectionFacility();
			WnsDemand demand = connection.getDemands().get(0);
			BigDecimal waterCharge = MigrationUtility.convertToBigDecimal(demand.getWaterCharges());
			BigDecimal sewerageFee = MigrationUtility.convertToBigDecimal(demand.getSewerageFee());
			if(connectionFacility == null) {
				if(waterCharge.compareTo(BigDecimal.ZERO) > 0 && sewerageFee.compareTo(BigDecimal.ZERO) > 0) {
					connectionFacility = MigrationConst.CONNECTION_WATER_SEWERAGE;
				} else if(sewerageFee.compareTo(BigDecimal.ZERO) > 0) {
					connectionFacility = MigrationConst.CONNECTION_SEWERAGE;
				} else if(waterCharge.compareTo(BigDecimal.ZERO) > 0) {
					connectionFacility = MigrationConst.CONNECTION_WATER;
				}
			} else if(connectionFacility != null && MigrationConst.CONNECTION_WATER.equalsIgnoreCase(connectionFacility)) {
				if(sewerageFee.compareTo(BigDecimal.ZERO) > 0) {
					connectionFacility = MigrationConst.CONNECTION_WATER_SEWERAGE;
				}
			} else if(connectionFacility != null && MigrationConst.CONNECTION_SEWERAGE.equalsIgnoreCase(connectionFacility)) {
				if(waterCharge.compareTo(BigDecimal.ZERO) > 0) {
					connectionFacility = MigrationConst.CONNECTION_WATER_SEWERAGE;
				}
			}
			connection.setConnectionFacility(connectionFacility);
		}
	}

	private ConnectionDTO transformConnection(WnsConnection connection) {
		try {
			ConnectionDTO connectionDTO = new ConnectionDTO();
			transformWnSConnection(connectionDTO, connection);
			if(MigrationConst.SERVICE_SEWERAGE.equalsIgnoreCase(connectionDTO.getWaterConnection().getConnectionFacility())) {
				transformDemandV4(connectionDTO, connection);
			}
			return connectionDTO;
		} catch (Exception e) {
			MigrationUtility.addError(connection.getConnectionNo(), e.getLocalizedMessage());
			return null;
		}
		
	}

	private void transformWnSConnection(ConnectionDTO connectionDTO, WnsConnection connection) {
		String ulb = connection.getUlb().trim().toLowerCase();
		this.tenantId = properties.getTenants().get(ulb);
		this.localityCode = this.properties.getLocalitycode().get(ulb);
		
		transformWater(connectionDTO, connection);
	}
	
	private Object transformAdditional(WnsConnection connection) {
		String source = connection.getConnectionFacility();
		ObjectNode additionalDetails = JsonNodeFactory.instance.objectNode();
		additionalDetails.put("locality", this.localityCode);
		additionalDetails.put("ward", MigrationUtility.getWard(connection.getWard()));
		if((MigrationConst.CONNECTION_WATER.equalsIgnoreCase(connection.getConnectionFacility())
				|| MigrationConst.CONNECTION_WATER_SEWERAGE.equalsIgnoreCase(connection.getConnectionFacility()))
				&& MigrationConst.CONNECTION_METERED.equalsIgnoreCase(connection.getService().getConnectionType())) {
			String meterRatio = "1:1";
			String meterMake = "Other";
			if(connection.getMeterReading() != null && !connection.getMeterReading().isEmpty()) {
				WnsMeterReading wmr = connection.getMeterReading().stream().filter(mr -> StringUtils.hasText(mr.getMeterReadingRatio()))
											.findFirst().orElse(null);
				if(wmr != null) {
					meterRatio = wmr.getMeterReadingRatio().replaceAll(" ", "");
				}
				meterMake = connection.getMeterReading().get(0).getMeterMake();
			}
			additionalDetails.put("meterReadingRatio", meterRatio);
			additionalDetails.put("meterMake", meterMake);
		}
		
		if(MigrationConst.CONNECTION_SEWERAGE.equalsIgnoreCase(connection.getConnectionFacility())
				|| MigrationConst.CONNECTION_WATER_SEWERAGE.equalsIgnoreCase(connection.getConnectionFacility())) {
			if(MigrationConst.CONNECTION_CATEGORY_PERMANENT.equalsIgnoreCase(connection.getService().getConnectionCategory())
					&& ((StringUtils.isEmpty(connection.getService().getUsageCategory()) && MigrationUtility.getNoOfFlat(connection.getService().getUsageCategory(), connection.getService().getNoOfFlats())>0) 
							|| (StringUtils.hasText(connection.getService().getUsageCategory())
								&& (connection.getService().getUsageCategory().equalsIgnoreCase("Industrial")
								|| connection.getService().getUsageCategory().equalsIgnoreCase("Commertial")
								|| connection.getService().getUsageCategory().equalsIgnoreCase("Apartment"))))) {
				BigDecimal sewerageAmt = BigDecimal.ZERO;
				if(connection.getDemands() != null && !connection.getDemands().isEmpty()) {
					WnsDemand demand = connection.getDemands().stream()
							.sorted((d1,d2) -> MigrationUtility.getLongDate(d2.getBillingPeriodTo(), dateFormat).compareTo(MigrationUtility.getLongDate(d1.getBillingPeriodTo(), dateFormat)))
							.findFirst().orElse(null);
					if(demand != null) {
						sewerageAmt = MigrationUtility.convertToBigDecimal(demand.getSewerageFee());
					}
				}
				
				String dia = "4";
				if(sewerageAmt.compareTo(BigDecimal.valueOf(500)) < 0) {
					dia = "4";
				} else if(sewerageAmt.compareTo(BigDecimal.valueOf(500)) >= 0 && sewerageAmt.compareTo(BigDecimal.valueOf(800)) < 0) {
					dia = "6";
				} else if(sewerageAmt.compareTo(BigDecimal.valueOf(800)) >= 0) {
					dia = "8";
				}
				additionalDetails.put("diameter", dia);
			}
		}
		
		// Water non-metered marked as volumetric
		if(MigrationConst.CONNECTION_WATER.equals(source) && MigrationConst.CONNECTION_NON_METERED.equalsIgnoreCase(MigrationUtility.getConnectionType(connection.getService().getConnectionType()))) {
			additionalDetails.put("isVolumetricConnection", "Y");
			BigDecimal volumetricWaterCharge = BigDecimal.ZERO;
			if(connection.getDemands() != null) {
				WnsDemand demand = connection.getDemands().stream()
						.sorted((d1,d2) -> MigrationUtility.getLongDate(d2.getBillingPeriodTo(), dateFormat).compareTo(MigrationUtility.getLongDate(d1.getBillingPeriodTo(), dateFormat)))
						.findFirst().orElse(null);
				
				if(demand != null) {
					volumetricWaterCharge = MigrationUtility.convertToBigDecimal(demand.getWaterCharges());
				}
			}
			
			additionalDetails.put("volumetricWaterCharge", volumetricWaterCharge);
		}
		
		// Latest Sewerage Fee 
		if(MigrationConst.CONNECTION_SEWERAGE.equals(source)
				|| MigrationConst.CONNECTION_WATER_SEWERAGE.equals(source)) {
			BigDecimal sewerageFee = BigDecimal.ZERO;
			if(connection.getDemands() != null) {
				WnsDemand demand = connection.getDemands().stream()
						.sorted((d1,d2) -> MigrationUtility.getLongDate(d2.getBillingPeriodTo(), dateFormat).compareTo(MigrationUtility.getLongDate(d1.getBillingPeriodTo(), dateFormat)))
						.findFirst().orElse(null);
				
				if(demand != null) {
					sewerageFee = MigrationUtility.convertToBigDecimal(demand.getSewerageFee());
				}
			}
			additionalDetails.put("migratedSewerageFee", sewerageFee);
		}
		
		return additionalDetails;
	}
	

	private void transformWater(ConnectionDTO connectionDTO, WnsConnection connection) {
		WaterConnectionDTO waterConnectionDTO = new WaterConnectionDTO();
		transformWaterConnection(waterConnectionDTO, connection);
		transformWaterService(waterConnectionDTO, connection);
		waterConnectionDTO.setConnectionHolders(transformConnectionHolder(connection));
		waterConnectionDTO.setAdditionalDetails(transformAdditional(connection));
		connectionDTO.setWaterConnection(waterConnectionDTO);
	}

	private List<ConnectionHolderDTO> transformConnectionHolder(WnsConnection connection) {
		WnsConnectionHolder holder = connection.getConnectionHolder();
		ConnectionHolderDTO connectionHolder = new ConnectionHolderDTO();
		if(holder == null) {
			connectionHolder.setName(connection.getConnectionNo());
			connectionHolder.setGender("MALE");
			connectionHolder.setFatherOrHusbandName("Other");
			connectionHolder.setRelationship("FATHER");
			connectionHolder.setOwnerType("NONE");
			connectionHolder.setCorrespondenceAddress("Other");
		} else {
			connectionHolder.setSalutation(MigrationUtility.getSalutation(holder.getSalutation()));
			connectionHolder.setName(MigrationUtility.prepareName(connection.getConnectionNo(), holder.getHolderName()));
			connectionHolder.setMobileNumber(MigrationUtility.processMobile(holder.getMobile()));
			connectionHolder.setGender(prepareGender(holder));
			connectionHolder.setFatherOrHusbandName(MigrationUtility.getGuardian(holder.getGuardian()));
			connectionHolder.setRelationship(MigrationUtility.getRelationship(holder.getGuardianRelation()));
			connectionHolder.setCorrespondenceAddress(MigrationUtility.getAddress(holder.getHolderAddress()));
			connectionHolder.setOwnerType(MigrationUtility.getOwnerType(holder.getConnectionHolderType()));
		}
		
		return Arrays.asList(connectionHolder);
	}
	
	private static String prepareGender(WnsConnectionHolder holder) {
		String gender = "Male";
		if(holder.getGender()==null) {
			if(holder.getSalutation() != null && 
					(holder.getSalutation().equalsIgnoreCase("M/S")
							|| holder.getSalutation().equalsIgnoreCase("Miss")
							|| holder.getSalutation().equalsIgnoreCase("Mrs"))) {
				gender="Female";
			}
		} else {
			gender = MigrationUtility.getGender(holder.getGender());
		}
		return gender;
	}

	private void transformWaterService(WaterConnectionDTO waterConnectionDTO, WnsConnection connection) {
		WnsConnectionService service = connection.getService();
		waterConnectionDTO.setConnectionCategory(MigrationUtility.getConnectionCategory(service.getConnectionCategory()));
		waterConnectionDTO.setConnectionType(MigrationUtility.getConnectionType(service.getConnectionType()));
		waterConnectionDTO.setWaterSource(MigrationUtility.getWaterSource(service.getWaterSource()));
		waterConnectionDTO.setMeterId(service.getMeterSerialNo());
		waterConnectionDTO.setMeterInstallationDate(MigrationUtility.getMeterInstallationDate(service.getMeterInstallationDate(),waterConnectionDTO.getConnectionType()));
		waterConnectionDTO.setNoOfTaps(MigrationUtility.getNoOfTaps(service.getNoOfTaps()));
		waterConnectionDTO.setConnectionExecutionDate(MigrationUtility.getExecutionDate(service.getConnectionExecutionDate(), dateFormat));
		waterConnectionDTO.setProposedTaps(MigrationUtility.getNoOfTaps(service.getNoOfTaps()));
		waterConnectionDTO.setUsageCategory(MigrationUtility.getConnectionUsageCategory(service.getUsageCategory()));
		waterConnectionDTO.setNoOfFlats(MigrationUtility.getDefaultZero(service.getNoOfFlats()));
		
		waterConnectionDTO.setProposedPipeSize(MigrationUtility.getPipeSize(connection.getService().getActualPipeSize()));
		waterConnectionDTO.setProposedToilets(MigrationUtility.getToilets(connection));
		waterConnectionDTO.setProposedWaterClosets(MigrationUtility.getWaterClosets(connection));
		waterConnectionDTO.setPipeSize(MigrationUtility.getPipeSize(connection.getService().getActualPipeSize()));
		waterConnectionDTO.setNoOfToilets(MigrationUtility.getToilets(connection));
		waterConnectionDTO.setNoOfWaterClosets(MigrationUtility.getWaterClosets(connection));
		
		waterConnectionDTO.setConnectionFacility(MigrationUtility.getConnectionFacility(connection));
		
	}
	
	private void transformWaterConnection(WaterConnectionDTO waterConnectionDTO, WnsConnection connection) {
		waterConnectionDTO.setTenantId(this.tenantId);
		waterConnectionDTO.setOldConnectionNo(MigrationUtility.addLeadingZeros(connection.getConnectionNo()));
	}
	
	private void transformDemandV4(ConnectionDTO connectionDTO, WnsConnection connection) {
		List<WnsDemand> demands = connection.getDemands();
		if(demands==null || demands.isEmpty())
			return;
		
		// get the latest demand
		WnsDemand demand = demands.stream()
				.sorted((d1,d2) -> MigrationUtility.getLongDate(d2.getBillingPeriodTo(), dateFormat).compareTo(MigrationUtility.getLongDate(d1.getBillingPeriodTo(), dateFormat)))
				.findFirst().orElse(null);
		
		if(demand == null) {
			return;
		}
		
		boolean isArrearDemandRequired = false;
		
		BigDecimal totalArrearOutStanding = BigDecimal.ZERO;
		BigDecimal arrearAmount =  MigrationUtility.convertToBigDecimal(demand.getArrear());
		BigDecimal collectedAmount = MigrationUtility.convertToBigDecimal(demand.getCollectedAmount());

		totalArrearOutStanding = arrearAmount.subtract(collectedAmount);
		
		if(totalArrearOutStanding.compareTo(BigDecimal.ZERO) != 0) {
			isArrearDemandRequired = true;
		}
		
		if(isArrearDemandRequired) {
			List<DemandDTO> demandDTOs = new ArrayList<DemandDTO>();
			if(totalArrearOutStanding.compareTo(BigDecimal.ZERO) > 0) {
				// Have due
				DemandDTO arrearDemandDTO = new DemandDTO();
				arrearDemandDTO.setTenantId(connectionDTO.getWaterConnection().getTenantId());
				arrearDemandDTO.setTaxPeriodFrom(MigrationUtility.getPreviousMonthLongDate(demand.getBillingPeriodFrom(), dateFormat));
				arrearDemandDTO.setTaxPeriodTo(MigrationUtility.getPreviousMonthLongDate(demand.getBillingPeriodTo(), dateFormat));
				
				DemandDetailDTO arrearDemandDetailDTO = new DemandDetailDTO();
				arrearDemandDetailDTO.setTaxHeadMasterCode("SW_CHARGE");
				arrearDemandDetailDTO.setTaxAmount(arrearAmount);
				arrearDemandDetailDTO.setCollectionAmount(collectedAmount);
				
				arrearDemandDTO.setDemandDetails(Arrays.asList(arrearDemandDetailDTO));
				demandDTOs.add(arrearDemandDTO);
				connectionDTO.setWaterDemands(demandDTOs);
			} else if(totalArrearOutStanding.compareTo(BigDecimal.ZERO) < 0) {
				// have advance
				DemandDTO arrearDemandDTO = new DemandDTO();
				arrearDemandDTO.setTenantId(connectionDTO.getWaterConnection().getTenantId());
				arrearDemandDTO.setTaxPeriodFrom(MigrationUtility.getPreviousMonthLongDate(demand.getBillingPeriodFrom(), dateFormat));
				arrearDemandDTO.setTaxPeriodTo(MigrationUtility.getPreviousMonthLongDate(demand.getBillingPeriodTo(), dateFormat));
				
				List<DemandDetailDTO> demanddetails = new ArrayList<>();
				DemandDetailDTO arrearDemandDetailDTO = new DemandDetailDTO();
				arrearDemandDetailDTO.setTaxHeadMasterCode("SW_CHARGE");
				arrearDemandDetailDTO.setTaxAmount(arrearAmount);
				arrearDemandDetailDTO.setCollectionAmount(arrearAmount);
				demanddetails.add(arrearDemandDetailDTO);
				
				DemandDetailDTO advanceDemandDetailDTO = new DemandDetailDTO();
				advanceDemandDetailDTO.setTaxHeadMasterCode("SW_ADVANCE_CARRYFORWARD");
				advanceDemandDetailDTO.setTaxAmount(totalArrearOutStanding);
				advanceDemandDetailDTO.setCollectionAmount(BigDecimal.ZERO);
				demanddetails.add(advanceDemandDetailDTO);
				arrearDemandDTO.setDemandDetails(demanddetails);
				demandDTOs.add(arrearDemandDTO);
				connectionDTO.setWaterDemands(demandDTOs);
			}
		}
	}

}
