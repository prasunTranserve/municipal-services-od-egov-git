package org.egov.migration.processor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.egov.migration.business.model.ConnectionDTO;
import org.egov.migration.business.model.ConnectionHolderDTO;
import org.egov.migration.business.model.DemandDTO;
import org.egov.migration.business.model.DemandDetailDTO;
import org.egov.migration.business.model.MeterReadingDTO;
import org.egov.migration.business.model.SewerageConnectionDTO;
import org.egov.migration.business.model.WaterConnectionDTO;
import org.egov.migration.config.SystemProperties;
import org.egov.migration.reader.model.WnsConnection;
import org.egov.migration.reader.model.WnsConnectionHolder;
import org.egov.migration.reader.model.WnsConnectionService;
import org.egov.migration.reader.model.WnsDemand;
import org.egov.migration.reader.model.WnsMeterReading;
import org.egov.migration.service.ValidationService;
import org.egov.migration.util.MigrationConst;
import org.egov.migration.util.MigrationUtility;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class WnsTransformProcessor implements ItemProcessor<WnsConnection, ConnectionDTO> {
	
	public static final String dateFormat = "dd-MM-yy";
	
	public static final BigDecimal appplyRebate = new BigDecimal("0.98");
	
	public static final BigDecimal rebatePercentage = new BigDecimal("0.02");

	private String tenantId;

	private String localityCode;

	@Autowired
	private SystemProperties properties;

	@Autowired
	private ValidationService validationService;

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
			
			return transformConnection(connection);
			
		}
		return null;
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

	private void enrichConnectionType(WnsConnection connection) {
		WnsConnectionService connectionService = connection.getService();
		if(connectionService.getUsageCategory()==null) 
			return;

		if(MigrationConst.METERED_VOLUMETRIC_CONNECTION.contains(connectionService.getUsageCategory().toLowerCase())) {
			connection.getService().setConnectionType(MigrationConst.CONNECTION_METERED);
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
			transformDemandV4(connectionDTO, connection);
			return connectionDTO;
		} catch (Exception e) {
			MigrationUtility.addError(connection.getConnectionNo(), e.getLocalizedMessage());
			return null;
		}
		
	}

	private void transformDemand(ConnectionDTO connectionDTO, WnsConnection connection) {
		List<WnsDemand> demands = connection.getDemands();
		if(demands == null)
			return;
		
		if(connectionDTO.isSewerage()) {
			List<DemandDTO> demandDTOs = new ArrayList<DemandDTO>();
			DemandDTO demandDTO = new DemandDTO();
			demandDTO.setTenantId(connectionDTO.getSewerageConnection().getTenantId());
			demandDTO.setTaxPeriodFrom(0L);
			demandDTO.setTaxPeriodTo(0L);
			
			DemandDetailDTO demandDetailDTO = new DemandDetailDTO();
			demandDetailDTO.setTaxHeadMasterCode("SW_CHARGE");
			demandDetailDTO.setTaxAmount(BigDecimal.ZERO);
			demandDetailDTO.setCollectionAmount(BigDecimal.ZERO);
			demands.forEach(demand -> {
				demandDetailDTO.setTaxAmount(demandDetailDTO.getTaxAmount().add(new BigDecimal(demand.getSewerageFee())));
				if(connectionDTO.isWater()) {
					if(new BigDecimal(demand.getCollectedAmount()).compareTo(demandDetailDTO.getTaxAmount()) >= 0) {
						demandDetailDTO.setCollectionAmount(demandDetailDTO.getCollectionAmount().add(demandDetailDTO.getTaxAmount()));
						demand.setCollectedAmount(new BigDecimal(demand.getCollectedAmount()).subtract(demandDetailDTO.getTaxAmount()).toString());
					} else {
						demandDetailDTO.setCollectionAmount(demandDetailDTO.getCollectionAmount().add(new BigDecimal(demand.getCollectedAmount())));
						demand.setCollectedAmount("0");
					}
				} else {
					demandDetailDTO.setTaxAmount(demandDetailDTO.getTaxAmount().add(new BigDecimal(demand.getWaterCharges())));
					demandDetailDTO.setCollectionAmount(demandDetailDTO.getCollectionAmount().add(new BigDecimal(demand.getCollectedAmount())));
				}
				long taxPeriodFrom = MigrationUtility.getLongDate(demand.getBillingPeriodFrom(), dateFormat);
				long taxPeriodTo = MigrationUtility.getLongDate(demands.get(0).getBillingPeriodTo(), dateFormat);
				if(demandDTO.getTaxPeriodFrom() < taxPeriodFrom )
					demandDTO.setTaxPeriodFrom(taxPeriodFrom);
				if(demandDTO.getTaxPeriodTo() < taxPeriodTo)
					demandDTO.setTaxPeriodTo(taxPeriodTo);
			});
			
			demandDTO.setDemandDetails(Arrays.asList(demandDetailDTO));
			demandDTOs.add(demandDTO);
			connectionDTO.setSewerageDemands(demandDTOs);
		}
		
		if(connectionDTO.isWater()) {
			List<DemandDTO> demandDTOs = new ArrayList<DemandDTO>();
			DemandDTO demandDTO = new DemandDTO();
			demandDTO.setTenantId(connectionDTO.getWaterConnection().getTenantId());
			demandDTO.setTaxPeriodFrom(0L);
			demandDTO.setTaxPeriodTo(0L);
			
			DemandDetailDTO demandDetailDTO = new DemandDetailDTO();
			demandDetailDTO.setTaxHeadMasterCode("WS_CHARGE");
			demandDetailDTO.setTaxAmount(BigDecimal.ZERO);
			demandDetailDTO.setCollectionAmount(BigDecimal.ZERO);
			demands.forEach(demand -> {
				if(demand.getCollectedAmount() == null)
					demand.setCollectedAmount("0");
				if(demand.getWaterCharges() == null)
					demand.setWaterCharges("0");
				if(demand.getSewerageFee() == null)
					demand.setSewerageFee("0");
				
				demandDetailDTO.setTaxAmount(demandDetailDTO.getTaxAmount().add(new BigDecimal(demand.getWaterCharges())));
				if(!connectionDTO.isSewerage()) {
					demandDetailDTO.setTaxAmount(demandDetailDTO.getTaxAmount().add(new BigDecimal(demand.getSewerageFee())));
				}
				demandDetailDTO.setCollectionAmount(demandDetailDTO.getCollectionAmount().add(new BigDecimal(demand.getCollectedAmount())));
				
				long taxPeriodFrom = MigrationUtility.getLongDate(demand.getBillingPeriodFrom(), dateFormat);
				long taxPeriodTo = MigrationUtility.getLongDate(demands.get(0).getBillingPeriodTo(), dateFormat);
				if(demandDTO.getTaxPeriodFrom() < taxPeriodFrom )
					demandDTO.setTaxPeriodFrom(taxPeriodFrom);
				if(demandDTO.getTaxPeriodTo() < taxPeriodTo)
					demandDTO.setTaxPeriodTo(taxPeriodTo);
			});
			
			demandDTO.setDemandDetails(Arrays.asList(demandDetailDTO));
			demandDTOs.add(demandDTO);
			connectionDTO.setWaterDemands(demandDTOs);
		}

	}

	private void transformDemandV2(ConnectionDTO connectionDTO, WnsConnection connection) {
		List<WnsDemand> demands = connection.getDemands();
		if(demands==null || demands.isEmpty())
			return;
		
		WnsDemand demand = demands.get(0);
		boolean isDemandRequired = true;
		boolean isArrearDemandRequired = false;
		
		BigDecimal totalOutStanding = BigDecimal.ZERO;
		BigDecimal waterCharge = MigrationUtility.convertToBigDecimal(demand.getWaterCharges());
		BigDecimal sewerageFee = MigrationUtility.convertToBigDecimal(demand.getSewerageFee());
		BigDecimal arrearAmount =  MigrationUtility.convertToBigDecimal(demand.getArrear());
		BigDecimal collectedAmount = MigrationUtility.convertToBigDecimal(demand.getCollectedAmount());
		
		BigDecimal withRebatePayable = arrearAmount.add(waterCharge.add(sewerageFee).multiply(appplyRebate).setScale(2, RoundingMode.HALF_UP));
		BigDecimal withoutRebatePayable = arrearAmount.add(waterCharge).add(sewerageFee);
		if(collectedAmount.compareTo(withRebatePayable)>=0) {
			totalOutStanding = withRebatePayable.subtract(collectedAmount);
		} else {
			totalOutStanding = withoutRebatePayable.subtract(collectedAmount);
		}
		
		if(totalOutStanding.compareTo(waterCharge.add(sewerageFee))>0) {
			isArrearDemandRequired = true;
		} else if(totalOutStanding.compareTo(BigDecimal.ZERO)==0) {
			isDemandRequired = false;
		}
		
		if(isDemandRequired) {
			if(connectionDTO.isSewerage()) {
				List<DemandDTO> demandDTOs = new ArrayList<DemandDTO>();
				if(isArrearDemandRequired && !connectionDTO.isWater()) {
					DemandDTO arrearDemandDTO = new DemandDTO();
					arrearDemandDTO.setTenantId(connectionDTO.getSewerageConnection().getTenantId());
					arrearDemandDTO.setTaxPeriodFrom(MigrationUtility.getPreviousMonthLongDate(demand.getBillingPeriodFrom(), dateFormat));
					arrearDemandDTO.setTaxPeriodTo(MigrationUtility.getPreviousMonthLongDate(demand.getBillingPeriodTo(), dateFormat));
					
					DemandDetailDTO arrearDemandDetailDTO = new DemandDetailDTO();
					arrearDemandDetailDTO.setTaxHeadMasterCode("SW_CHARGE");
					arrearDemandDetailDTO.setTaxAmount(arrearAmount);
					arrearDemandDetailDTO.setCollectionAmount(collectedAmount);
					
					arrearDemandDTO.setDemandDetails(Arrays.asList(arrearDemandDetailDTO));
					demandDTOs.add(arrearDemandDTO);
				}
//				BigDecimal sewerageOutStanding = totalOutStanding;
//				if(totalOutStanding.compareTo(waterCharge)>0) {
//					sewerageOutStanding = totalOutStanding.subtract(waterCharge);
//					totalOutStanding = waterCharge;
				BigDecimal sewerageOutStanding = totalOutStanding;
				if(connectionDTO.isWater()) {
					if(totalOutStanding.compareTo(BigDecimal.ZERO)>0 && waterCharge.add(sewerageFee).compareTo(totalOutStanding)>=0) {
						sewerageOutStanding = waterCharge.add(sewerageFee).subtract(totalOutStanding);
						totalOutStanding = totalOutStanding.subtract(sewerageOutStanding);
					}
				}
				
				
				List<DemandDetailDTO> currentDemandDetails = new ArrayList<>();
				if(totalOutStanding.compareTo(BigDecimal.ZERO)<0 && !connectionDTO.isWater()) {
					DemandDetailDTO demandDetailDTO = new DemandDetailDTO();
					demandDetailDTO.setTaxHeadMasterCode("SW_CHARGE");
					demandDetailDTO.setTaxAmount(sewerageFee);
					demandDetailDTO.setCollectionAmount(sewerageFee);
					currentDemandDetails.add(demandDetailDTO);
					
					DemandDetailDTO AdDemandDetailDTO = new DemandDetailDTO();
					AdDemandDetailDTO.setTaxHeadMasterCode("SW_ADVANCE_CARRYFORWARD");
					AdDemandDetailDTO.setTaxAmount(totalOutStanding);
					AdDemandDetailDTO.setCollectionAmount(BigDecimal.ZERO);
					currentDemandDetails.add(AdDemandDetailDTO);
				} else if(totalOutStanding.compareTo(BigDecimal.ZERO)>0 && !isArrearDemandRequired) {
					DemandDetailDTO demandDetailDTO = new DemandDetailDTO();
					demandDetailDTO.setTaxHeadMasterCode("SW_CHARGE");
					demandDetailDTO.setTaxAmount(sewerageFee);
					demandDetailDTO.setCollectionAmount(sewerageFee.subtract(sewerageOutStanding));
					currentDemandDetails.add(demandDetailDTO);
				} else if(totalOutStanding.compareTo(BigDecimal.ZERO)>0 && !connectionDTO.isWater()) {
					DemandDetailDTO demandDetailDTO = new DemandDetailDTO();
					demandDetailDTO.setTaxHeadMasterCode("SW_CHARGE");
					demandDetailDTO.setTaxAmount(sewerageFee);
					demandDetailDTO.setCollectionAmount(BigDecimal.ZERO);
					currentDemandDetails.add(demandDetailDTO);
				} else if(totalOutStanding.compareTo(BigDecimal.ZERO)>0 && totalOutStanding.compareTo(waterCharge.add(sewerageFee))>0 && connectionDTO.isWater()) {
					DemandDetailDTO demandDetailDTO = new DemandDetailDTO();
					demandDetailDTO.setTaxHeadMasterCode("SW_CHARGE");
					demandDetailDTO.setTaxAmount(sewerageFee);
					demandDetailDTO.setCollectionAmount(BigDecimal.ZERO);
					currentDemandDetails.add(demandDetailDTO);
				}
				
				if(!currentDemandDetails.isEmpty()) {
					DemandDTO demandDTO = new DemandDTO();
					demandDTO.setTenantId(connectionDTO.getSewerageConnection().getTenantId());
					demandDTO.setTaxPeriodFrom(MigrationUtility.getLongDate(demand.getBillingPeriodFrom(), dateFormat));
					demandDTO.setTaxPeriodTo(MigrationUtility.getLongDate(demand.getBillingPeriodTo(), dateFormat));
					demandDTO.setDemandDetails(currentDemandDetails);
					demandDTOs.add(demandDTO);
					
					connectionDTO.setSewerageDemands(demandDTOs);
				}
				
			}
			
			//Water
			if(connectionDTO.isWater()) {
				List<DemandDTO> demandDTOs = new ArrayList<DemandDTO>();
				if(isArrearDemandRequired) {
					DemandDTO arrearDemandDTO = new DemandDTO();
					arrearDemandDTO.setTenantId(connectionDTO.getWaterConnection().getTenantId());
					arrearDemandDTO.setTaxPeriodFrom(MigrationUtility.getPreviousMonthLongDate(demand.getBillingPeriodFrom(), dateFormat));
					arrearDemandDTO.setTaxPeriodTo(MigrationUtility.getPreviousMonthLongDate(demand.getBillingPeriodTo(), dateFormat));
					
					DemandDetailDTO arrearDemandDetailDTO = new DemandDetailDTO();
					arrearDemandDetailDTO.setTaxHeadMasterCode("WS_CHARGE");
					arrearDemandDetailDTO.setTaxAmount(arrearAmount);
					arrearDemandDetailDTO.setCollectionAmount(collectedAmount);
					
					arrearDemandDTO.setDemandDetails(Arrays.asList(arrearDemandDetailDTO));
					demandDTOs.add(arrearDemandDTO);
					
				}
				
				DemandDTO demandDTO = new DemandDTO();
				demandDTO.setTenantId(connectionDTO.getWaterConnection().getTenantId());
				demandDTO.setTaxPeriodFrom(MigrationUtility.getLongDate(demand.getBillingPeriodFrom(), dateFormat));
				demandDTO.setTaxPeriodTo(MigrationUtility.getLongDate(demand.getBillingPeriodTo(), dateFormat));
				List<DemandDetailDTO> currentDemandDetails = new ArrayList<>();
				if(totalOutStanding.compareTo(BigDecimal.ZERO)<0) {
					DemandDetailDTO demandDetailDTO = new DemandDetailDTO();
					demandDetailDTO.setTaxHeadMasterCode("WS_CHARGE");
					demandDetailDTO.setTaxAmount(waterCharge);
					demandDetailDTO.setCollectionAmount(waterCharge);
					currentDemandDetails.add(demandDetailDTO);
					
					DemandDetailDTO AdDemandDetailDTO = new DemandDetailDTO();
					AdDemandDetailDTO.setTaxHeadMasterCode("WS_ADVANCE_CARRYFORWARD");
					AdDemandDetailDTO.setTaxAmount(totalOutStanding);
					AdDemandDetailDTO.setCollectionAmount(BigDecimal.ZERO);
					currentDemandDetails.add(AdDemandDetailDTO);
				} else if(totalOutStanding.compareTo(BigDecimal.ZERO)>0 && !isArrearDemandRequired) {
					DemandDetailDTO demandDetailDTO = new DemandDetailDTO();
					demandDetailDTO.setTaxHeadMasterCode("WS_CHARGE");
					demandDetailDTO.setTaxAmount(waterCharge);
					demandDetailDTO.setCollectionAmount(waterCharge.subtract(totalOutStanding));
					currentDemandDetails.add(demandDetailDTO);
				} else if(totalOutStanding.compareTo(BigDecimal.ZERO)>0) {
					DemandDetailDTO demandDetailDTO = new DemandDetailDTO();
					demandDetailDTO.setTaxHeadMasterCode("WS_CHARGE");
					demandDetailDTO.setTaxAmount(waterCharge);
					demandDetailDTO.setCollectionAmount(BigDecimal.ZERO);
					currentDemandDetails.add(demandDetailDTO);
				}
				demandDTO.setDemandDetails(currentDemandDetails);
				demandDTOs.add(demandDTO);
				
				connectionDTO.setWaterDemands(demandDTOs);
			}
			
		}
	}

	private void transformDemandV3(ConnectionDTO connectionDTO, WnsConnection connection) {
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
		
		boolean isDemandRequired = true;
		boolean isArrearDemandRequired = false;
		
		BigDecimal totalOutStanding = BigDecimal.ZERO;
		BigDecimal waterCharge = MigrationUtility.convertToBigDecimal(demand.getWaterCharges());
		BigDecimal sewerageFee = MigrationUtility.convertToBigDecimal(demand.getSewerageFee());
		BigDecimal arrearAmount =  MigrationUtility.convertToBigDecimal(demand.getArrear());
		BigDecimal collectedAmount = MigrationUtility.convertToBigDecimal(demand.getCollectedAmount());
		
		BigDecimal withRebatePayable = arrearAmount.add(waterCharge.add(sewerageFee).multiply(appplyRebate).setScale(0, RoundingMode.HALF_UP));
		totalOutStanding = withRebatePayable.subtract(collectedAmount);
		
		if(totalOutStanding.compareTo(waterCharge.add(sewerageFee))>0) {
			isArrearDemandRequired = true;
		} else if(totalOutStanding.compareTo(BigDecimal.ZERO)==0) {
			isDemandRequired = false;
		} else if(connectionDTO.isWater() && connectionDTO.isSewerage() && totalOutStanding.compareTo(waterCharge)>0) {
			isArrearDemandRequired = true;
		}
		
		if(isDemandRequired) {
			if(connectionDTO.isSewerage()) {
				List<DemandDTO> demandDTOs = new ArrayList<DemandDTO>();
				if(isArrearDemandRequired && !connectionDTO.isWater()) {
					DemandDTO arrearDemandDTO = new DemandDTO();
					arrearDemandDTO.setTenantId(connectionDTO.getSewerageConnection().getTenantId());
					arrearDemandDTO.setTaxPeriodFrom(MigrationUtility.getPreviousMonthLongDate(demand.getBillingPeriodFrom(), dateFormat));
					arrearDemandDTO.setTaxPeriodTo(MigrationUtility.getPreviousMonthLongDate(demand.getBillingPeriodTo(), dateFormat));
					
					// Arrear for only sewerage
					DemandDetailDTO arrearDemandDetailDTO = new DemandDetailDTO();
					arrearDemandDetailDTO.setTaxHeadMasterCode("SW_CHARGE");
					arrearDemandDetailDTO.setTaxAmount(arrearAmount);
					arrearDemandDetailDTO.setCollectionAmount(collectedAmount.add(sewerageFee.add(arrearAmount).subtract(withRebatePayable)));
					
					arrearDemandDTO.setDemandDetails(Arrays.asList(arrearDemandDetailDTO));
					demandDTOs.add(arrearDemandDTO);
				}
				
				List<DemandDetailDTO> currentDemandDetails = new ArrayList<>();
				if(totalOutStanding.compareTo(BigDecimal.ZERO)<0 && !connectionDTO.isWater()) {
					DemandDetailDTO demandDetailDTO = new DemandDetailDTO();
					demandDetailDTO.setTaxHeadMasterCode("SW_CHARGE");
					demandDetailDTO.setTaxAmount(sewerageFee);
					demandDetailDTO.setCollectionAmount(sewerageFee);
					currentDemandDetails.add(demandDetailDTO);
					
					DemandDetailDTO AdDemandDetailDTO = new DemandDetailDTO();
					AdDemandDetailDTO.setTaxHeadMasterCode("SW_ADVANCE_CARRYFORWARD");
					AdDemandDetailDTO.setTaxAmount(totalOutStanding);
					AdDemandDetailDTO.setCollectionAmount(BigDecimal.ZERO);
					currentDemandDetails.add(AdDemandDetailDTO);
				} else if(totalOutStanding.compareTo(BigDecimal.ZERO)>0 && !isArrearDemandRequired && !connectionDTO.isWater()) {
					DemandDetailDTO demandDetailDTO = new DemandDetailDTO();
					demandDetailDTO.setTaxHeadMasterCode("SW_CHARGE");
					demandDetailDTO.setTaxAmount(sewerageFee);
					demandDetailDTO.setCollectionAmount(sewerageFee.subtract(totalOutStanding));
					currentDemandDetails.add(demandDetailDTO);
				} else if(totalOutStanding.compareTo(BigDecimal.ZERO)>0 && !connectionDTO.isWater()) {
					DemandDetailDTO demandDetailDTO = new DemandDetailDTO();
					demandDetailDTO.setTaxHeadMasterCode("SW_CHARGE");
					demandDetailDTO.setTaxAmount(sewerageFee);
					demandDetailDTO.setCollectionAmount(BigDecimal.ZERO);
					currentDemandDetails.add(demandDetailDTO);
				}
//				else if(totalOutStanding.compareTo(BigDecimal.ZERO)>0 && totalOutStanding.compareTo(waterCharge.add(sewerageFee))>0 && connectionDTO.isWater()) {
//					DemandDetailDTO demandDetailDTO = new DemandDetailDTO();
//					demandDetailDTO.setTaxHeadMasterCode("SW_CHARGE");
//					demandDetailDTO.setTaxAmount(sewerageFee);
//					demandDetailDTO.setCollectionAmount(BigDecimal.ZERO);
//					currentDemandDetails.add(demandDetailDTO);
//				}
				
				if(!currentDemandDetails.isEmpty()) {
					DemandDTO demandDTO = new DemandDTO();
					demandDTO.setTenantId(connectionDTO.getSewerageConnection().getTenantId());
					demandDTO.setTaxPeriodFrom(MigrationUtility.getLongDate(demand.getBillingPeriodFrom(), dateFormat));
					demandDTO.setTaxPeriodTo(MigrationUtility.getLongDate(demand.getBillingPeriodTo(), dateFormat));
					demandDTO.setDemandDetails(currentDemandDetails);
					demandDTOs.add(demandDTO);
					
					connectionDTO.setSewerageDemands(demandDTOs);
				}
				
			}
			
			//Water
			if(connectionDTO.isWater()) {
				List<DemandDTO> demandDTOs = new ArrayList<DemandDTO>();
				if(isArrearDemandRequired) {
					DemandDTO arrearDemandDTO = new DemandDTO();
					arrearDemandDTO.setTenantId(connectionDTO.getWaterConnection().getTenantId());
					arrearDemandDTO.setTaxPeriodFrom(MigrationUtility.getPreviousMonthLongDate(demand.getBillingPeriodFrom(), dateFormat));
					arrearDemandDTO.setTaxPeriodTo(MigrationUtility.getPreviousMonthLongDate(demand.getBillingPeriodTo(), dateFormat));
					
					DemandDetailDTO arrearDemandDetailDTO = new DemandDetailDTO();
					arrearDemandDetailDTO.setTaxHeadMasterCode("WS_CHARGE");
					arrearDemandDetailDTO.setTaxAmount(arrearAmount.add(sewerageFee));
					arrearDemandDetailDTO.setCollectionAmount(collectedAmount.add(waterCharge.add(sewerageFee).add(arrearAmount).subtract(withRebatePayable)));
					
					arrearDemandDTO.setDemandDetails(Arrays.asList(arrearDemandDetailDTO));
					demandDTOs.add(arrearDemandDTO);
					
				}
				
				DemandDTO demandDTO = new DemandDTO();
				demandDTO.setTenantId(connectionDTO.getWaterConnection().getTenantId());
				demandDTO.setTaxPeriodFrom(MigrationUtility.getLongDate(demand.getBillingPeriodFrom(), dateFormat));
				demandDTO.setTaxPeriodTo(MigrationUtility.getLongDate(demand.getBillingPeriodTo(), dateFormat));
				List<DemandDetailDTO> currentDemandDetails = new ArrayList<>();
				if(totalOutStanding.compareTo(BigDecimal.ZERO)<0) {
					DemandDetailDTO demandDetailDTO = new DemandDetailDTO();
					demandDetailDTO.setTaxHeadMasterCode("WS_CHARGE");
					demandDetailDTO.setTaxAmount(waterCharge);
					demandDetailDTO.setCollectionAmount(waterCharge);
					currentDemandDetails.add(demandDetailDTO);
					
					DemandDetailDTO AdDemandDetailDTO = new DemandDetailDTO();
					AdDemandDetailDTO.setTaxHeadMasterCode("WS_ADVANCE_CARRYFORWARD");
					AdDemandDetailDTO.setTaxAmount(totalOutStanding);
					AdDemandDetailDTO.setCollectionAmount(BigDecimal.ZERO);
					currentDemandDetails.add(AdDemandDetailDTO);
				} else if(totalOutStanding.compareTo(BigDecimal.ZERO)>0 && !isArrearDemandRequired) {
					DemandDetailDTO demandDetailDTO = new DemandDetailDTO();
					demandDetailDTO.setTaxHeadMasterCode("WS_CHARGE");
					demandDetailDTO.setTaxAmount(waterCharge);
					demandDetailDTO.setCollectionAmount(waterCharge.subtract(totalOutStanding));
					currentDemandDetails.add(demandDetailDTO);
				} else if(totalOutStanding.compareTo(BigDecimal.ZERO)>0) {
					DemandDetailDTO demandDetailDTO = new DemandDetailDTO();
					demandDetailDTO.setTaxHeadMasterCode("WS_CHARGE");
					demandDetailDTO.setTaxAmount(waterCharge);
					demandDetailDTO.setCollectionAmount(BigDecimal.ZERO);
					currentDemandDetails.add(demandDetailDTO);
				}
				demandDTO.setDemandDetails(currentDemandDetails);
				demandDTOs.add(demandDTO);
				
				connectionDTO.setWaterDemands(demandDTOs);
			}
			
		}
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
		
		boolean isDemandRequired = true;
		boolean isArrearDemandRequired = false;
		
		BigDecimal totalOutStanding = BigDecimal.ZERO;
		BigDecimal waterCharge = MigrationUtility.convertToBigDecimal(demand.getWaterCharges());
		//BigDecimal sewerageFee = MigrationUtility.convertToBigDecimal(demand.getSewerageFee());
		BigDecimal arrearAmount =  MigrationUtility.convertToBigDecimal(demand.getArrear());
		BigDecimal collectedAmount = MigrationUtility.convertToBigDecimal(demand.getCollectedAmount());
		BigDecimal rebateAmount = waterCharge.multiply(rebatePercentage).setScale(0, RoundingMode.HALF_UP);
		
//		BigDecimal withRebatePayable = arrearAmount.add(waterCharge.multiply(appplyRebate).setScale(0, RoundingMode.HALF_UP));
//		totalOutStanding = withRebatePayable.subtract(collectedAmount);
		
		BigDecimal payableAmount = arrearAmount.add(waterCharge);
		totalOutStanding = payableAmount.subtract(collectedAmount);
		
		if(totalOutStanding.compareTo(waterCharge)>0) {
			isArrearDemandRequired = true;
		} else if(totalOutStanding.compareTo(BigDecimal.ZERO)==0) {
			isDemandRequired = false;
//		} else if(connectionDTO.isWater() && connectionDTO.isSewerage() && totalOutStanding.compareTo(waterCharge)>0) {
//			isArrearDemandRequired = true;
		}
		
		if(isDemandRequired) {
//			if(connectionDTO.isSewerage()) {
//				List<DemandDTO> demandDTOs = new ArrayList<DemandDTO>();
//				if(isArrearDemandRequired && !connectionDTO.isWater()) {
//					DemandDTO arrearDemandDTO = new DemandDTO();
//					arrearDemandDTO.setTenantId(connectionDTO.getSewerageConnection().getTenantId());
//					arrearDemandDTO.setTaxPeriodFrom(MigrationUtility.getPreviousMonthLongDate(demand.getBillingPeriodFrom(), dateFormat));
//					arrearDemandDTO.setTaxPeriodTo(MigrationUtility.getPreviousMonthLongDate(demand.getBillingPeriodTo(), dateFormat));
//					
//					// Arrear for only sewerage
//					DemandDetailDTO arrearDemandDetailDTO = new DemandDetailDTO();
//					arrearDemandDetailDTO.setTaxHeadMasterCode("SW_CHARGE");
//					arrearDemandDetailDTO.setTaxAmount(arrearAmount);
//					arrearDemandDetailDTO.setCollectionAmount(collectedAmount.add(sewerageFee.add(arrearAmount).subtract(withRebatePayable)));
//					
//					arrearDemandDTO.setDemandDetails(Arrays.asList(arrearDemandDetailDTO));
//					demandDTOs.add(arrearDemandDTO);
//				}
//				
//				List<DemandDetailDTO> currentDemandDetails = new ArrayList<>();
//				if(totalOutStanding.compareTo(BigDecimal.ZERO)<0 && !connectionDTO.isWater()) {
//					DemandDetailDTO demandDetailDTO = new DemandDetailDTO();
//					demandDetailDTO.setTaxHeadMasterCode("SW_CHARGE");
//					demandDetailDTO.setTaxAmount(sewerageFee);
//					demandDetailDTO.setCollectionAmount(sewerageFee);
//					currentDemandDetails.add(demandDetailDTO);
//					
//					DemandDetailDTO AdDemandDetailDTO = new DemandDetailDTO();
//					AdDemandDetailDTO.setTaxHeadMasterCode("SW_ADVANCE_CARRYFORWARD");
//					AdDemandDetailDTO.setTaxAmount(totalOutStanding);
//					AdDemandDetailDTO.setCollectionAmount(BigDecimal.ZERO);
//					currentDemandDetails.add(AdDemandDetailDTO);
//				} else if(totalOutStanding.compareTo(BigDecimal.ZERO)>0 && !isArrearDemandRequired && !connectionDTO.isWater()) {
//					DemandDetailDTO demandDetailDTO = new DemandDetailDTO();
//					demandDetailDTO.setTaxHeadMasterCode("SW_CHARGE");
//					demandDetailDTO.setTaxAmount(sewerageFee);
//					demandDetailDTO.setCollectionAmount(sewerageFee.subtract(totalOutStanding));
//					currentDemandDetails.add(demandDetailDTO);
//				} else if(totalOutStanding.compareTo(BigDecimal.ZERO)>0 && !connectionDTO.isWater()) {
//					DemandDetailDTO demandDetailDTO = new DemandDetailDTO();
//					demandDetailDTO.setTaxHeadMasterCode("SW_CHARGE");
//					demandDetailDTO.setTaxAmount(sewerageFee);
//					demandDetailDTO.setCollectionAmount(BigDecimal.ZERO);
//					currentDemandDetails.add(demandDetailDTO);
//				}
//				
//				if(!currentDemandDetails.isEmpty()) {
//					DemandDTO demandDTO = new DemandDTO();
//					demandDTO.setTenantId(connectionDTO.getSewerageConnection().getTenantId());
//					demandDTO.setTaxPeriodFrom(MigrationUtility.getLongDate(demand.getBillingPeriodFrom(), dateFormat));
//					demandDTO.setTaxPeriodTo(MigrationUtility.getLongDate(demand.getBillingPeriodTo(), dateFormat));
//					demandDTO.setDemandDetails(currentDemandDetails);
//					demandDTOs.add(demandDTO);
//					
//					connectionDTO.setSewerageDemands(demandDTOs);
//				}
//				
//			}
			
			//Water
			if(connectionDTO.isWater()) {
				List<DemandDTO> demandDTOs = new ArrayList<DemandDTO>();
				if(isArrearDemandRequired) {
					DemandDTO arrearDemandDTO = new DemandDTO();
					arrearDemandDTO.setTenantId(connectionDTO.getWaterConnection().getTenantId());
					arrearDemandDTO.setTaxPeriodFrom(MigrationUtility.getPreviousMonthLongDate(demand.getBillingPeriodFrom(), dateFormat));
					arrearDemandDTO.setTaxPeriodTo(MigrationUtility.getPreviousMonthLongDate(demand.getBillingPeriodTo(), dateFormat));
					
					DemandDetailDTO arrearDemandDetailDTO = new DemandDetailDTO();
					arrearDemandDetailDTO.setTaxHeadMasterCode("WS_CHARGE");
					arrearDemandDetailDTO.setTaxAmount(arrearAmount);
					arrearDemandDetailDTO.setCollectionAmount(collectedAmount);
					
					arrearDemandDTO.setDemandDetails(Arrays.asList(arrearDemandDetailDTO));
					demandDTOs.add(arrearDemandDTO);
					
				}
				
				DemandDTO demandDTO = new DemandDTO();
				demandDTO.setTenantId(connectionDTO.getWaterConnection().getTenantId());
				demandDTO.setTaxPeriodFrom(MigrationUtility.getLongDate(demand.getBillingPeriodFrom(), dateFormat));
				demandDTO.setTaxPeriodTo(MigrationUtility.getLongDate(demand.getBillingPeriodTo(), dateFormat));
				List<DemandDetailDTO> currentDemandDetails = new ArrayList<>();
				if(totalOutStanding.compareTo(BigDecimal.ZERO)<0) {
					DemandDetailDTO demandDetailDTO = new DemandDetailDTO();
					demandDetailDTO.setTaxHeadMasterCode("WS_CHARGE");
					demandDetailDTO.setTaxAmount(waterCharge);
					demandDetailDTO.setCollectionAmount(waterCharge);
					currentDemandDetails.add(demandDetailDTO);
					
					// Special Rebate for all demand
					DemandDetailDTO specialRebateDetailDTO = new DemandDetailDTO();
					specialRebateDetailDTO.setTaxHeadMasterCode("WS_SPECIAL_REBATE");
					specialRebateDetailDTO.setTaxAmount(rebateAmount.negate());
					specialRebateDetailDTO.setCollectionAmount(rebateAmount.negate());
					currentDemandDetails.add(specialRebateDetailDTO);
					
					DemandDetailDTO AdDemandDetailDTO = new DemandDetailDTO();
					AdDemandDetailDTO.setTaxHeadMasterCode("WS_ADVANCE_CARRYFORWARD");
					AdDemandDetailDTO.setTaxAmount(totalOutStanding.add(rebateAmount.negate()));
					AdDemandDetailDTO.setCollectionAmount(BigDecimal.ZERO);
					currentDemandDetails.add(AdDemandDetailDTO);
				} else if(totalOutStanding.compareTo(BigDecimal.ZERO)>0 && !isArrearDemandRequired) {
					DemandDetailDTO demandDetailDTO = new DemandDetailDTO();
					demandDetailDTO.setTaxHeadMasterCode("WS_CHARGE");
					demandDetailDTO.setTaxAmount(waterCharge);
					demandDetailDTO.setCollectionAmount(waterCharge.subtract(totalOutStanding));
					currentDemandDetails.add(demandDetailDTO);
					
					// Special Rebate for all demand
					DemandDetailDTO specialRebateDetailDTO = new DemandDetailDTO();
					specialRebateDetailDTO.setTaxHeadMasterCode("WS_SPECIAL_REBATE");
					specialRebateDetailDTO.setTaxAmount(rebateAmount.negate());
					specialRebateDetailDTO.setCollectionAmount(BigDecimal.ZERO);
					currentDemandDetails.add(specialRebateDetailDTO);
				} else if(totalOutStanding.compareTo(BigDecimal.ZERO)>0) {
					DemandDetailDTO demandDetailDTO = new DemandDetailDTO();
					demandDetailDTO.setTaxHeadMasterCode("WS_CHARGE");
					demandDetailDTO.setTaxAmount(waterCharge);
					demandDetailDTO.setCollectionAmount(BigDecimal.ZERO);
					currentDemandDetails.add(demandDetailDTO);
					
					// Special Rebate for all demand
					DemandDetailDTO specialRebateDetailDTO = new DemandDetailDTO();
					specialRebateDetailDTO.setTaxHeadMasterCode("WS_SPECIAL_REBATE");
					specialRebateDetailDTO.setTaxAmount(rebateAmount.negate());
					specialRebateDetailDTO.setCollectionAmount(BigDecimal.ZERO);
					currentDemandDetails.add(specialRebateDetailDTO);
				}
				
				demandDTO.setDemandDetails(currentDemandDetails);
				demandDTOs.add(demandDTO);
				
				connectionDTO.setWaterDemands(demandDTOs);
			}
			
		}
	}
	
	private void transformWnSConnection(ConnectionDTO connectionDTO, WnsConnection connection) {
		String ulb = connection.getUlb().trim().toLowerCase();
		this.tenantId = properties.getTenants().get(ulb);
		this.localityCode = this.properties.getLocalitycode().get(ulb);
		
		if(MigrationConst.CONNECTION_SEWERAGE.equalsIgnoreCase(connection.getConnectionFacility())) {
			transformSewerage(connectionDTO, connection);
		}
		
		if(MigrationConst.CONNECTION_WATER.equalsIgnoreCase(connection.getConnectionFacility())) {
			transformWater(connectionDTO, connection);
		}
		
		if(MigrationConst.CONNECTION_WATER_SEWERAGE.equalsIgnoreCase(connection.getConnectionFacility())) {
			transformWater(connectionDTO, connection);
			transformSewerage(connectionDTO, connection);
		}
	}
	
	private Object transformAdditional(WnsConnection connection, String source) {
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
		if(MigrationConst.CONNECTION_SEWERAGE.equals(source)) {
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
		connectionDTO.setWater(true);
		WaterConnectionDTO waterConnectionDTO = new WaterConnectionDTO();
		transformWaterConnection(waterConnectionDTO, connection);
		transformWaterService(waterConnectionDTO, connection);
		waterConnectionDTO.setConnectionHolders(transformConnectionHolder(connection));
		waterConnectionDTO.setAdditionalDetails(transformAdditional(connection, MigrationConst.CONNECTION_WATER));
		connectionDTO.setWaterConnection(waterConnectionDTO);
		
		transformMeterReading(connectionDTO, connection);
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

	private void transformMeterReading(ConnectionDTO connectionDTO, WnsConnection connection) {
		List<WnsMeterReading> meterReading = connection.getMeterReading();
		
		if(MigrationConst.CONNECTION_METERED.equals(connectionDTO.getWaterConnection().getConnectionType())) {
			if(meterReading == null) {
				connectionDTO.setMeterReading(createZeroMeterReading());
				return;
			}
			WnsMeterReading lastMeterReading = meterReading.stream().sorted((mr1, mr2) -> MigrationUtility.toDate(mr2.getBillingPeriod()).compareTo(MigrationUtility.toDate(mr1.getBillingPeriod())))
					.findFirst().orElse(null);
			if(lastMeterReading !=null && !lastMeterReading.getCurrentReading().equalsIgnoreCase("0")) {
				if(lastMeterReading.getCurrentReadingDate() == null && lastMeterReading.getCreatedDate() == null) {
					connectionDTO.setMeterReading(createZeroMeterReading());
					connectionDTO.getMeterReading().setCurrentReading(MigrationUtility.getMeterCurrentReading(lastMeterReading));
				} else {
					MeterReadingDTO meterReadingDTO = new MeterReadingDTO();
					meterReadingDTO.setTenantId(this.tenantId);
					meterReadingDTO.setBillingPeriod(MigrationUtility.getConnectionBillingPeriod(lastMeterReading.getBillingPeriod()));
					meterReadingDTO.setMeterStatus(MigrationUtility.getMeterStatus(lastMeterReading.getMeterStatus()));
					meterReadingDTO.setLastReading(MigrationUtility.getMeterLastReading(lastMeterReading));
					meterReadingDTO.setLastReadingDate(MigrationUtility.getMeterLastReadingDate(lastMeterReading));
					meterReadingDTO.setCurrentReading(MigrationUtility.getMeterCurrentReading(lastMeterReading));
					meterReadingDTO.setCurrentReadingDate(MigrationUtility.getMeterCurrentReadingDate(lastMeterReading));
					meterReadingDTO.setConsumption(meterReadingDTO.getCurrentReading() - meterReadingDTO.getLastReading());
					
					connectionDTO.setMeterReading(meterReadingDTO);
				}
			} else {
				connectionDTO.setMeterReading(createZeroMeterReading());
			}
		}
		
	}

	private MeterReadingDTO createZeroMeterReading() {
		LocalDate previousMothDate = LocalDate.now().minusMonths(1);
		MeterReadingDTO meterReadingDTO = new MeterReadingDTO();
		meterReadingDTO.setTenantId(this.tenantId);
		meterReadingDTO.setBillingPeriod(MigrationUtility.getDummyConnectionBillingPeriod());
		meterReadingDTO.setMeterStatus(MigrationUtility.getMeterStatus("NW"));
		meterReadingDTO.setLastReading(0D);
		meterReadingDTO.setLastReadingDate(previousMothDate.withDayOfMonth(1).atTime(11, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
		meterReadingDTO.setCurrentReading(0D);
		meterReadingDTO.setCurrentReadingDate(previousMothDate.withDayOfMonth(previousMothDate.lengthOfMonth()).atTime(11, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
		meterReadingDTO.setConsumption(0D);
		
		return meterReadingDTO;
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
		waterConnectionDTO.setConnectionFacility(MigrationConst.SERVICE_WATER);
		
	}
	
	private void transformWaterConnection(WaterConnectionDTO waterConnectionDTO, WnsConnection connection) {
		waterConnectionDTO.setTenantId(this.tenantId);
		waterConnectionDTO.setOldConnectionNo(MigrationUtility.addLeadingZeros(connection.getConnectionNo()));
	}

	private void transformSewerage(ConnectionDTO connectionDTO, WnsConnection connection) {
		connectionDTO.setSewerage(true);
		SewerageConnectionDTO sewerageConnectionDTO = new SewerageConnectionDTO();
		transformSewerageConnection(sewerageConnectionDTO, connection);
		transformSewerageService(sewerageConnectionDTO, connection);
		sewerageConnectionDTO.setConnectionHolders(transformConnectionHolder(connection));
		sewerageConnectionDTO.setAdditionalDetails(transformAdditional(connection, MigrationConst.CONNECTION_SEWERAGE));
		connectionDTO.setSewerageConnection(sewerageConnectionDTO);
	}

	private void transformSewerageService(SewerageConnectionDTO sewerageConnectionDTO, WnsConnection connection) {
		WnsConnectionService service = connection.getService();
		sewerageConnectionDTO.setConnectionCategory(MigrationUtility.getConnectionCategory(service.getConnectionCategory()));
		sewerageConnectionDTO.setConnectionType(MigrationConst.CONNECTION_NON_METERED);
		sewerageConnectionDTO.setConnectionExecutionDate(MigrationUtility.getExecutionDate(service.getConnectionExecutionDate(), dateFormat));
		sewerageConnectionDTO.setUsageCategory(MigrationUtility.getConnectionUsageCategory(service.getUsageCategory()));
		sewerageConnectionDTO.setNoOfFlats(MigrationUtility.getDefaultZero(service.getNoOfFlats()));
		sewerageConnectionDTO.setProposedWaterClosets(MigrationUtility.getWaterClosets(connection));
		sewerageConnectionDTO.setProposedToilets(MigrationUtility.getToilets(connection));
		sewerageConnectionDTO.setNoOfWaterClosets(MigrationUtility.getWaterClosets(connection));
		sewerageConnectionDTO.setNoOfToilets(MigrationUtility.getToilets(connection));
		sewerageConnectionDTO.setPipeSize(MigrationUtility.getPipeSize(connection.getService().getActualPipeSize()));
	}

	private void transformSewerageConnection(SewerageConnectionDTO sewerageConnectionDTO, WnsConnection connection) {
		sewerageConnectionDTO.setTenantId(this.tenantId);
		sewerageConnectionDTO.setOldConnectionNo(MigrationUtility.addLeadingZeros(connection.getConnectionNo()));
	}
}
