package org.egov.migration.mapper;

import java.util.Map;

import org.apache.poi.ss.usermodel.Row;
import org.egov.migration.reader.model.WnsConnectionService;
import org.egov.migration.util.MigrationConst;
import org.egov.migration.util.MigrationUtility;
import org.springframework.stereotype.Component;

@Component
public class ConnectionServiceRowMapper {

	public WnsConnectionService mapRow(String ulb, Row row, Map<String, Integer> columnMap) {
		WnsConnectionService service = new WnsConnectionService();
		
		service.setUlb(ulb);
		service.setConnectionNo(columnMap.get(MigrationConst.COL_CONNECTION_NO)==null ? null : MigrationUtility.readCellValue(row.getCell(columnMap.get(MigrationConst.COL_CONNECTION_NO)), false));
		service.setConnectionFacility(columnMap.get(MigrationConst.COL_CONNECTION_FACILTY)==null ? null : MigrationUtility.readCellValue(row.getCell(columnMap.get(MigrationConst.COL_CONNECTION_FACILTY)), false));
		service.setConnectionType(columnMap.get(MigrationConst.COL_CONNECTION_TYPE)==null ? null : MigrationUtility.readCellValue(row.getCell(columnMap.get(MigrationConst.COL_CONNECTION_TYPE)), false));
		service.setWaterSource(columnMap.get(MigrationConst.COL_WATER_SOURCE)==null ? null : MigrationUtility.readCellValue(row.getCell(columnMap.get(MigrationConst.COL_WATER_SOURCE)), false));
		service.setMeterSerialNo(columnMap.get(MigrationConst.COL_METER_SERIAL_NO)==null ? null : MigrationUtility.readCellValue(row.getCell(columnMap.get(MigrationConst.COL_METER_SERIAL_NO)), false));
		service.setMeterInstallationDate(columnMap.get(MigrationConst.COL_METER_INSTALLATION_DATE)==null ? null : MigrationUtility.readCellValue(row.getCell(columnMap.get(MigrationConst.COL_METER_INSTALLATION_DATE)), false));
		service.setActualPipeSize(columnMap.get(MigrationConst.COL_ACTUAL_PIPE_SIZE)==null ? null : MigrationUtility.readCellValue(row.getCell(columnMap.get(MigrationConst.COL_ACTUAL_PIPE_SIZE)), false));
		service.setNoOfTaps(columnMap.get(MigrationConst.COL_NO_OF_TAPS)==null ? null : MigrationUtility.readCellValue(row.getCell(columnMap.get(MigrationConst.COL_NO_OF_TAPS)), false));
		service.setConnectionExecutionDate(columnMap.get(MigrationConst.COL_CONNECTION_EXECUTION_DATE)==null ? null : MigrationUtility.readCellValue(row.getCell(columnMap.get(MigrationConst.COL_CONNECTION_EXECUTION_DATE)), false));
		service.setProposedPipeSize(columnMap.get(MigrationConst.COL_PROPOSED_PIPE_SIZE)==null ? null : MigrationUtility.readCellValue(row.getCell(columnMap.get(MigrationConst.COL_PROPOSED_PIPE_SIZE)), false));
		service.setPropesedTaps(columnMap.get(MigrationConst.COL_PROPOSED_TAPS)==null ? null : MigrationUtility.readCellValue(row.getCell(columnMap.get(MigrationConst.COL_PROPOSED_TAPS)), false));
		service.setLastMeterReading(columnMap.get(MigrationConst.COL_LAST_METER_READING)==null ? null : MigrationUtility.readCellValue(row.getCell(columnMap.get(MigrationConst.COL_LAST_METER_READING)), false));
		service.setUsageCategory(columnMap.get(MigrationConst.COL_USAGE_CATEGORY)==null ? null : MigrationUtility.readCellValue(row.getCell(columnMap.get(MigrationConst.COL_USAGE_CATEGORY)), false));
		service.setNoOfFlats(columnMap.get(MigrationConst.COL_NO_OF_FLATS)==null ? null : MigrationUtility.readCellValue(row.getCell(columnMap.get(MigrationConst.COL_NO_OF_FLATS)), false));
		service.setNoOfClosets(columnMap.get(MigrationConst.COL_NO_OF_CLOSETS)==null ? null : MigrationUtility.readCellValue(row.getCell(columnMap.get(MigrationConst.COL_NO_OF_CLOSETS)), false));
		service.setNoOfToilets(columnMap.get(MigrationConst.COL_NO_OF_TOILETS)==null ? null : MigrationUtility.readCellValue(row.getCell(columnMap.get(MigrationConst.COL_NO_OF_TOILETS)), false));
		service.setProposedWaterClosets(columnMap.get(MigrationConst.COL_PROPOSED_WATER_CLOSETS)==null ? null : MigrationUtility.readCellValue(row.getCell(columnMap.get(MigrationConst.COL_PROPOSED_WATER_CLOSETS)), false));
		service.setProposedToilets(columnMap.get(MigrationConst.COL_PROPOSED_TOILETS)==null ? null : MigrationUtility.readCellValue(row.getCell(columnMap.get(MigrationConst.COL_PROPOSED_TOILETS)), false));
		service.setConnectionCategory(columnMap.get(MigrationConst.COL_CONNECTION_CATEGORY)==null ? null : MigrationUtility.readCellValue(row.getCell(columnMap.get(MigrationConst.COL_CONNECTION_CATEGORY)), false));
		service.setCreatedDate(columnMap.get(MigrationConst.COL_CREATED_DATE)==null ? null : MigrationUtility.readCellValue(row.getCell(columnMap.get(MigrationConst.COL_CREATED_DATE)), false));
		
		return service;
	}
}
