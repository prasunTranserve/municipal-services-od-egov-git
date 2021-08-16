package org.egov.migration.processor;

import org.egov.migration.business.model.ConnectionDTO;
import org.egov.migration.business.model.ConnectionHolderDTO;
import org.egov.migration.business.model.MeterReadingDTO;
import org.egov.migration.business.model.SewerageConnectionDTO;
import org.egov.migration.business.model.WaterConnectionDTO;
import org.egov.migration.config.SystemProperties;
import org.egov.migration.reader.model.WnsConnection;
import org.egov.migration.reader.model.WnsConnectionHolder;
import org.egov.migration.reader.model.WnsMeterReading;
import org.egov.migration.reader.model.WnsService;
import org.egov.migration.service.ValidationService;
import org.egov.migration.util.MigrationConst;
import org.egov.migration.util.MigrationUtility;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Autowired;

public class WnsTransformProcessor implements ItemProcessor<WnsConnection, ConnectionDTO> {
	
	public static final String dateFormat = "dd-MM-yy";

	private String tenant;

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
		transformConnectionHolder(connectionDTO, connection);

		transformDemand(connectionDTO, connection);

		return connectionDTO;
	}

	private void transformDemand(ConnectionDTO connectionDTO, WnsConnection connection) {
		// TODO Auto-generated method stub

	}

	private void transformConnectionHolder(ConnectionDTO connectionDTO, WnsConnection connection) {
		// TODO Auto-generated method stub

	}

	private void transformWnSConnection(ConnectionDTO connectionDTO, WnsConnection connection) {
		String ulb = connection.getUlb().trim().toLowerCase();
		this.tenant = properties.getTenants().get(ulb);
		this.localityCode = this.properties.getLocalitycode().get(ulb);
		
		if(MigrationConst.CONNECTION_SEWERAGE.equalsIgnoreCase(connection.getConnectionFacility())) {
			transformSewerage(connectionDTO, connection);
		} else if(MigrationConst.CONNECTION_WATER.equalsIgnoreCase(connection.getConnectionFacility())) {
			transformWater(connectionDTO, connection);
		}
		
		transformConnectionHolder(connection);
	}

	private void transformWater(ConnectionDTO connectionDTO, WnsConnection connection) {
		WaterConnectionDTO waterConnectionDTO = new WaterConnectionDTO();
		
		transformWaterConnection(waterConnectionDTO, connection);
		transformWaterService(waterConnectionDTO, connection);
		transformConnectionHolder(waterConnectionDTO, connection);
		transformMeterConnection(waterConnectionDTO, connection);
		
		
		connectionDTO.setWater(waterConnectionDTO);
	}

	private void transformConnectionHolder(WaterConnectionDTO waterConnectionDTO, WnsConnection connection) {
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
		
		waterConnectionDTO.addConnectionHolders(connectionHolder);
	}

	private void transformMeterConnection(WaterConnectionDTO waterConnectionDTO, WnsConnection connection) {
		WnsMeterReading meterReading = connection.getMeterReading();
		if(MigrationConst.CONNECTION_METERED.equals(waterConnectionDTO.getConnectionType())) {
			MeterReadingDTO meterReadingDTO = new MeterReadingDTO();
			meterReadingDTO.setBillingPeriod(MigrationUtility.getConnectionBillingPeriod(meterReading.getBillingPeriod()));
			meterReadingDTO.setMeterStatus(MigrationConst.METER_WORKING);
			
		}
		
	}

	private void transformWaterService(WaterConnectionDTO waterConnectionDTO, WnsConnection connection) {
		WnsService service = connection.getService();
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

	private void transformSewerage(ConnectionDTO connectionDTO, WnsConnection connection) {
		connectionDTO.setSewerage(transformSewerageConnection(connection));
	}
	
	private void transformConnectionHolder(WnsConnection connection) {
		
		
	}

	private void transformWaterConnection(WaterConnectionDTO waterConnectionDTO, WnsConnection connection) {
		waterConnectionDTO.setTenantId(this.tenant);
		waterConnectionDTO.setStatus("Active");
		waterConnectionDTO.setApplicationStatus("CONNECTION_ACTIVATED");
		waterConnectionDTO.setApplicationType(null);
		waterConnectionDTO.setOldConnectionNo(MigrationUtility.addLeadingZeros(connection.getConnectionNo()));
		waterConnectionDTO.setOldApplication(false);
	}

	private SewerageConnectionDTO transformSewerageConnection(WnsConnection connection) {
		// TODO Auto-generated method stub
		return null;
	}

}
