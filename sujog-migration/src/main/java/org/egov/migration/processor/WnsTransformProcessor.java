package org.egov.migration.processor;

import java.math.BigDecimal;
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
import org.egov.migration.reader.model.WnsDemand;
import org.egov.migration.reader.model.WnsMeterReading;
import org.egov.migration.reader.model.WnsConnectionService;
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
		ConnectionDTO connectionDTO = new ConnectionDTO();
		transformWnSConnection(connectionDTO, connection);
		transformDemand(connectionDTO, connection);
		return connectionDTO;
	}

	private void transformDemand(ConnectionDTO connectionDTO, WnsConnection connection) {
		List<WnsDemand> demands = connection.getDemands();
		
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
		
		if(connectionDTO.isSewerage()) {
			List<DemandDTO> demandDTOs = new ArrayList<DemandDTO>();
			DemandDTO demandDTO = new DemandDTO();
			demandDTO.setTenantId(connectionDTO.getSewerageConnection().getTenantId());
			demandDTO.setTaxPeriodFrom(MigrationUtility.getLongDate(demands.get(0).getBillingPeriodFrom(), dateFormat));
			demandDTO.setTaxPeriodTo(MigrationUtility.getLongDate(demands.get(0).getBillingPeriodTo(), dateFormat));
			
			DemandDetailDTO demandDetailDTO = new DemandDetailDTO();
			demandDetailDTO.setTaxHeadMasterCode("SW_CHARGE");
			demandDetailDTO.setTaxAmount(BigDecimal.ZERO);
			demandDetailDTO.setCollectionAmount(BigDecimal.ZERO);
			demands.forEach(demand -> {
				demandDetailDTO.setTaxAmount(demandDetailDTO.getTaxAmount().add(new BigDecimal(demand.getSewerageFee())));
				demandDetailDTO.setCollectionAmount(demandDetailDTO.getCollectionAmount().add(new BigDecimal(demand.getCollectedAmount())));
			});
			
			demandDTO.setDemandDetails(Arrays.asList(demandDetailDTO));
			demandDTOs.add(demandDTO);
			connectionDTO.setSewerageDemands(demandDTOs);
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
		
		transformMeterConnection(connectionDTO, connection);
	}

	private List<ConnectionHolderDTO> transformConnectionHolder(WnsConnection connection) {
		WnsConnectionHolder holder = connection.getConnectionHolder();
		ConnectionHolderDTO connectionHolder = new ConnectionHolderDTO();
		connectionHolder.setSalutation(holder.getSalutation());
		connectionHolder.setName(holder.getHolderName().trim());
		connectionHolder.setMobileNumber(MigrationUtility.processMobile(holder.getMobile()));
		connectionHolder.setGender(MigrationUtility.getGender(holder.getGender()));
		connectionHolder.setFatherOrHusbandName(holder.getGuardian());
		connectionHolder.setRelationship(holder.getGuardianRelation());
		connectionHolder.setCorrespondenceAddress(holder.getHolderAddress());
		connectionHolder.setOwnerType("NONE");
		
		return Arrays.asList(connectionHolder);
	}

	private void transformMeterConnection(ConnectionDTO connectionDTO, WnsConnection connection) {
		WnsMeterReading meterReading = connection.getMeterReading();
		if(MigrationConst.CONNECTION_METERED.equals(connectionDTO.getWaterConnection().getConnectionType())) {
			MeterReadingDTO meterReadingDTO = new MeterReadingDTO();
			meterReadingDTO.setTenantId(this.tenantId);
			meterReadingDTO.setBillingPeriod(MigrationUtility.getConnectionBillingPeriod(meterReading.getBillingPeriod()));
			meterReadingDTO.setMeterStatus(MigrationUtility.getMeterStatus(connection));
			meterReadingDTO.setLastReading(MigrationUtility.getMeterLastReading(connection));
			meterReadingDTO.setLastReadingDate(MigrationUtility.getMeterLastReadingDate(connection));
			meterReadingDTO.setCurrentReading(MigrationUtility.getMeterCurrentReading(connection));
			meterReadingDTO.setCurrentReadingDate(MigrationUtility.getMeterCurrentReadingDate(connection));
			meterReadingDTO.setConsumption(meterReadingDTO.getCurrentReading() - meterReadingDTO.getLastReading());
			meterReadingDTO.setConsumption(MigrationUtility.getConsumption(meterReadingDTO));
			
			connectionDTO.setMeterReading(meterReadingDTO);
		}
		
	}

	private void transformWaterService(WaterConnectionDTO waterConnectionDTO, WnsConnection connection) {
		WnsConnectionService service = connection.getService();
		waterConnectionDTO.setConnectionCategory(MigrationUtility.getConnectionCategory(service.getConnectionCategory()));
		waterConnectionDTO.setConnectionType(MigrationUtility.getConnectionType(service.getConnectionType()));
		waterConnectionDTO.setWaterSource(MigrationUtility.getWaterSource(service.getWaterSource()));
		waterConnectionDTO.setMeterId(service.getMeterSerialNo());
		waterConnectionDTO.setMeterInstallationDate(MigrationUtility.getMeterInstallationDate(service.getMeterInstallationDate(),waterConnectionDTO.getConnectionType()));
		waterConnectionDTO.setNoOfTaps(Integer.parseInt(service.getNoOfTaps().trim()));
		waterConnectionDTO.setConnectionExecutionDate(MigrationUtility.getLongDate(service.getConnectionExecutionDate(), dateFormat));
		waterConnectionDTO.setProposedTaps(Integer.parseInt(service.getNoOfTaps().trim()));
		waterConnectionDTO.setUsageCategory(service.getUsageCategory().trim().toUpperCase());
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
		sewerageConnectionDTO.setConnectionType(MigrationUtility.getConnectionType(service.getConnectionType()));
		sewerageConnectionDTO.setConnectionExecutionDate(MigrationUtility.getLongDate(service.getConnectionExecutionDate(), dateFormat));
		sewerageConnectionDTO.setUsageCategory(service.getUsageCategory().trim().toUpperCase());
		sewerageConnectionDTO.setNoOfFlats(MigrationUtility.getDefaultZero(service.getNoOfFlats()));
		sewerageConnectionDTO.setProposedWaterClosets(MigrationUtility.getWaterClosets(connection));
		sewerageConnectionDTO.setProposedToilets(MigrationUtility.getToilets(connection));
		sewerageConnectionDTO.setNoOfWaterClosets(MigrationUtility.getWaterClosets(connection));
		sewerageConnectionDTO.setNoOfToilets(MigrationUtility.getToilets(connection));
		sewerageConnectionDTO.setPipeSize(connection.getService().getActualPipeSize()==null? 0 : Integer.parseInt(connection.getService().getActualPipeSize()));
		
	}

	private void transformSewerageConnection(SewerageConnectionDTO sewerageConnectionDTO, WnsConnection connection) {
		sewerageConnectionDTO.setTenantId(this.tenantId);
		sewerageConnectionDTO.setOldConnectionNo(MigrationUtility.addLeadingZeros(connection.getConnectionNo()));
	}

}
