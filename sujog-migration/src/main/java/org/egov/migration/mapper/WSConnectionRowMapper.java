package org.egov.migration.mapper;

import java.util.Map;

import org.apache.poi.ss.usermodel.Row;
import org.egov.migration.reader.model.WSConnection;
import org.egov.migration.util.MigrationConst;
import org.egov.migration.util.MigrationUtility;
import org.springframework.stereotype.Component;

@Component
public class WSConnectionRowMapper {
	
	public WSConnection mapRow(String ulb, Row row, Map<String, Integer> columnMap) {
		WSConnection connection = new WSConnection();
		
		connection.setTenantId(columnMap.get(MigrationConst.COL_ULB)==null ? null : MigrationUtility.readCellValue(row.getCell(columnMap.get(MigrationConst.COL_ULB)), false));
		connection.setConnectionNo(columnMap.get(MigrationConst.COL_CONNECTION_NO)==null ? null : MigrationUtility.readCellValue(row.getCell(columnMap.get(MigrationConst.COL_CONNECTION_NO)), false));
		return connection;
	}
}
