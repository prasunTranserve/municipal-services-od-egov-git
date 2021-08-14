package org.egov.migration.mapper;

import java.util.Map;

import org.apache.poi.ss.usermodel.Row;
import org.egov.migration.reader.model.WnsConnectionHolder;
import org.egov.migration.util.MigrationConst;
import org.egov.migration.util.MigrationUtility;
import org.springframework.stereotype.Component;

@Component
public class ConnectionHolderRowMapper {
	
	public WnsConnectionHolder mapRow(String ulb, Row row, Map<String, Integer> columnMap) {
		WnsConnectionHolder connectionHolder = new WnsConnectionHolder();
		
		connectionHolder.setUlb(ulb);
		connectionHolder.setConnectionNo(columnMap.get(MigrationConst.COL_CONNECTION_NO)==null ? null : MigrationUtility.readCellValue(row.getCell(columnMap.get(MigrationConst.COL_CONNECTION_NO)), false));
		connectionHolder.setConnectionFacility(columnMap.get(MigrationConst.COL_CONNECTION_FACILTY)==null ? null : MigrationUtility.readCellValue(row.getCell(columnMap.get(MigrationConst.COL_CONNECTION_FACILTY)), false));
		connectionHolder.setConnectionStatus(columnMap.get(MigrationConst.COL_CONNECTION_STATUS)==null ? null : MigrationUtility.readCellValue(row.getCell(columnMap.get(MigrationConst.COL_CONNECTION_STATUS)), false));
		connectionHolder.setSalutation(columnMap.get(MigrationConst.COL_SALUTATION)==null ? null : MigrationUtility.readCellValue(row.getCell(columnMap.get(MigrationConst.COL_SALUTATION)), false));
		connectionHolder.setHolderName(columnMap.get(MigrationConst.COL_HOLDER_NAME)==null ? null : MigrationUtility.readCellValue(row.getCell(columnMap.get(MigrationConst.COL_HOLDER_NAME)), false));
		connectionHolder.setMobile(columnMap.get(MigrationConst.COL_MOBILE_NUMBER)==null ? null : MigrationUtility.readCellValue(row.getCell(columnMap.get(MigrationConst.COL_MOBILE_NUMBER)), false));
		connectionHolder.setGender(columnMap.get(MigrationConst.COL_GENDER)==null ? null : MigrationUtility.readCellValue(row.getCell(columnMap.get(MigrationConst.COL_GENDER)), false));
		connectionHolder.setGuardian(columnMap.get(MigrationConst.COL_GUARDIAN_NAME)==null ? null : MigrationUtility.readCellValue(row.getCell(columnMap.get(MigrationConst.COL_GUARDIAN_NAME)), false));
		connectionHolder.setGuardianRelation(columnMap.get(MigrationConst.COL_GUARDIAN_RELATION)==null ? null : MigrationUtility.readCellValue(row.getCell(columnMap.get(MigrationConst.COL_GUARDIAN_RELATION)), false));
		connectionHolder.setConsumerCategory(columnMap.get(MigrationConst.COL_CONSUMER_CATEGORY)==null ? null : MigrationUtility.readCellValue(row.getCell(columnMap.get(MigrationConst.COL_CONSUMER_CATEGORY)), false));
		connectionHolder.setConnectionHolderType(columnMap.get(MigrationConst.COL_CONNECTION_HOLDER_TYPE)==null ? null : MigrationUtility.readCellValue(row.getCell(columnMap.get(MigrationConst.COL_CONNECTION_HOLDER_TYPE)), false));
		connectionHolder.setHolderAddress(columnMap.get(MigrationConst.COL_HOLDER_ADDRESS)==null ? null : MigrationUtility.readCellValue(row.getCell(columnMap.get(MigrationConst.COL_HOLDER_ADDRESS)), false));

		return connectionHolder;
	}

}
