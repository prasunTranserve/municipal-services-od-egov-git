package org.egov.migration.mapper;

import java.util.Map;

import org.apache.poi.ss.usermodel.Row;
import org.egov.migration.reader.model.WnsConnection;
import org.egov.migration.util.MigrationConst;
import org.egov.migration.util.MigrationUtility;
import org.springframework.stereotype.Component;

@Component
public class ConnectionRowMapper {
	
	public WnsConnection mapRow(String ulb, Row row, Map<String, Integer> columnMap) {
		WnsConnection connection = new WnsConnection();
		
		connection.setUlb(ulb);
		connection.setConnectionApplicationNo(columnMap.get(MigrationConst.COL_CONNECTION_APPLICATION_NO)==null ? null : MigrationUtility.readCellValue(row.getCell(columnMap.get(MigrationConst.COL_CONNECTION_APPLICATION_NO)), false));
		connection.setConnectionFacility(columnMap.get(MigrationConst.COL_CONNECTION_FACILTY)==null ? null : MigrationUtility.readCellValue(row.getCell(columnMap.get(MigrationConst.COL_CONNECTION_FACILTY)), false));
		connection.setApplicationStatus(columnMap.get(MigrationConst.COL_CONNECTION_APPLICATION_STATUS)==null ? null : MigrationUtility.readCellValue(row.getCell(columnMap.get(MigrationConst.COL_CONNECTION_APPLICATION_STATUS)), false));
		connection.setConnectionNo(columnMap.get(MigrationConst.COL_CONNECTION_NO)==null ? null : MigrationUtility.readCellValue(row.getCell(columnMap.get(MigrationConst.COL_CONNECTION_NO)), false));
		connection.setApplicationType(columnMap.get(MigrationConst.COL_APPLICATION_TYPE)==null ? null : MigrationUtility.readCellValue(row.getCell(columnMap.get(MigrationConst.COL_APPLICATION_TYPE)), false));
		connection.setWard(columnMap.get(MigrationConst.COL_WARD)==null ? null : MigrationUtility.readCellValue(row.getCell(columnMap.get(MigrationConst.COL_WARD)), false));
		connection.setStatus(columnMap.get(MigrationConst.COL_CONNECTION_STATUS)==null ? null : MigrationUtility.readCellValue(row.getCell(columnMap.get(MigrationConst.COL_CONNECTION_STATUS)), false));
		
		return connection;
	}
}
