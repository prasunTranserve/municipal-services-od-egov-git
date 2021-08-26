package org.egov.migration.processor;

import java.math.BigDecimal;
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

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class WnsTransformProcessor implements ItemProcessor<WnsConnection, ConnectionDTO> {
	
	public static final String dateFormat = "dd-MM-yy";

	private String tenantId;

	private String localityCode;

	@Autowired
	private SystemProperties properties;

	@Autowired
	private ValidationService validationService;

	@Override
	public ConnectionDTO process(WnsConnection connection) throws Exception {
		if (validationService.isValidConnection(connection)) {
			return transformConnection(connection);
		}
		return null;
	}

	private ConnectionDTO transformConnection(WnsConnection connection) {
		try {
			ConnectionDTO connectionDTO = new ConnectionDTO();
			transformWnSConnection(connectionDTO, connection);
			transformDemand(connectionDTO, connection);
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
				demandDetailDTO.setTaxAmount(demandDetailDTO.getTaxAmount().add(new BigDecimal(demand.getWaterCharges())));
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
	
	private Object transformAdditional(WnsConnection connection) {
		ObjectNode additionalDetails = JsonNodeFactory.instance.objectNode();
		additionalDetails.put("locality", this.localityCode);
		additionalDetails.put("ward", connection.getWard());
		return additionalDetails;
	}
	

	private void transformWater(ConnectionDTO connectionDTO, WnsConnection connection) {
		connectionDTO.setWater(true);
		WaterConnectionDTO waterConnectionDTO = new WaterConnectionDTO();
		transformWaterConnection(waterConnectionDTO, connection);
		transformWaterService(waterConnectionDTO, connection);
		waterConnectionDTO.setConnectionHolders(transformConnectionHolder(connection));
		waterConnectionDTO.setAdditionalDetails(transformAdditional(connection));
		connectionDTO.setWaterConnection(waterConnectionDTO);
		
		transformMeterReading(connectionDTO, connection);
	}

	private List<ConnectionHolderDTO> transformConnectionHolder(WnsConnection connection) {
		WnsConnectionHolder holder = connection.getConnectionHolder();
		ConnectionHolderDTO connectionHolder = new ConnectionHolderDTO();
		if(holder == null) {
			connectionHolder.setName(connection.getConnectionNo());
			connectionHolder.setOwnerType("NONE");
		} else {
			connectionHolder.setSalutation(MigrationUtility.getSalutation(holder.getSalutation()));
			connectionHolder.setName(MigrationUtility.prepareName(connection.getConnectionNo(), holder.getHolderName()));
			connectionHolder.setMobileNumber(MigrationUtility.processMobile(holder.getMobile()));
			connectionHolder.setGender(MigrationUtility.getGender(holder.getGender()));
			connectionHolder.setFatherOrHusbandName(holder.getGuardian());
			connectionHolder.setRelationship(holder.getGuardianRelation());
			connectionHolder.setCorrespondenceAddress(holder.getHolderAddress());
			connectionHolder.setOwnerType("NONE");
		}
		
		return Arrays.asList(connectionHolder);
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
		waterConnectionDTO.setConnectionExecutionDate(MigrationUtility.getLongDate(service.getConnectionExecutionDate(), dateFormat));
		waterConnectionDTO.setProposedTaps(MigrationUtility.getNoOfTaps(service.getNoOfTaps()));
		waterConnectionDTO.setUsageCategory(MigrationUtility.getConnectionUsageCategory(service.getUsageCategory()));
		waterConnectionDTO.setNoOfFlats(MigrationUtility.getDefaultZero(service.getNoOfFlats()));
		
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
		sewerageConnectionDTO.setAdditionalDetails(transformAdditional(connection));
		connectionDTO.setSewerageConnection(sewerageConnectionDTO);
	}

	private void transformSewerageService(SewerageConnectionDTO sewerageConnectionDTO, WnsConnection connection) {
		WnsConnectionService service = connection.getService();
		sewerageConnectionDTO.setConnectionCategory(MigrationUtility.getConnectionCategory(service.getConnectionCategory()));
		sewerageConnectionDTO.setConnectionType(MigrationConst.CONNECTION_NON_METERED);
		sewerageConnectionDTO.setConnectionExecutionDate(MigrationUtility.getLongDate(service.getConnectionExecutionDate(), dateFormat));
		sewerageConnectionDTO.setUsageCategory(service.getUsageCategory().trim().toUpperCase());
		sewerageConnectionDTO.setNoOfFlats(MigrationUtility.getDefaultZero(service.getNoOfFlats()));
		sewerageConnectionDTO.setProposedWaterClosets(MigrationUtility.getWaterClosets(connection));
		sewerageConnectionDTO.setProposedToilets(MigrationUtility.getToilets(connection));
		sewerageConnectionDTO.setNoOfWaterClosets(MigrationUtility.getWaterClosets(connection));
		sewerageConnectionDTO.setNoOfToilets(MigrationUtility.getToilets(connection));
		sewerageConnectionDTO.setPipeSize(connection.getService().getActualPipeSize()==null? 0 : Integer.parseInt(connection.getService().getActualPipeSize()));
		sewerageConnectionDTO.setUsageCategory(dateFormat);
		
	}

	private void transformSewerageConnection(SewerageConnectionDTO sewerageConnectionDTO, WnsConnection connection) {
		sewerageConnectionDTO.setTenantId(this.tenantId);
		sewerageConnectionDTO.setOldConnectionNo(MigrationUtility.addLeadingZeros(connection.getConnectionNo()));
	}

}
