package org.egov.pt.migration.mapper;

import java.util.Map;

import org.apache.poi.ss.usermodel.Row;
import org.egov.pt.migration.model.Owner;
import org.egov.pt.migration.util.MigrationConst;
import org.egov.pt.migration.util.MigrationUtility;
import org.springframework.stereotype.Component;

@Component
public class OwnerRowMapper {

	public Owner mapRow(String ulb, Row row, Map<String, Integer> columnMap) {
		Owner owner = new Owner();
		
		owner.setUlb(ulb);
		owner.setSalutation(columnMap.get(MigrationConst.COL_SALUTATION)==null ? null : MigrationUtility.readCellValue(row.getCell(columnMap.get(MigrationConst.COL_SALUTATION)), false));
		owner.setOwnerName(columnMap.get(MigrationConst.COL_OWNER_NAME)==null ? null : MigrationUtility.readCellValue(row.getCell(columnMap.get(MigrationConst.COL_OWNER_NAME)), false));
		owner.setGender(columnMap.get(MigrationConst.COL_GENDER)==null ? null : MigrationUtility.readCellValue(row.getCell(columnMap.get(MigrationConst.COL_GENDER)), false));
		owner.setDob(columnMap.get(MigrationConst.COL_DOB)==null ? null : MigrationUtility.readCellValue(row.getCell(columnMap.get(MigrationConst.COL_DOB)), false));
		owner.setMobileNumber(columnMap.get(MigrationConst.COL_MOBILE_NUMBER)==null ? null : MigrationUtility.readCellValue(row.getCell(columnMap.get(MigrationConst.COL_MOBILE_NUMBER)), false));
		owner.setEmail(columnMap.get(MigrationConst.COL_OWNER_EMAIL)==null ? null : MigrationUtility.readCellValue(row.getCell(columnMap.get(MigrationConst.COL_OWNER_EMAIL)), false));
		owner.setCreatedDate(columnMap.get(MigrationConst.COL_CREATED_DATE)==null ? null : MigrationUtility.readCellValue(row.getCell(columnMap.get(MigrationConst.COL_CREATED_DATE)), false));
		owner.setStatus(columnMap.get(MigrationConst.COL_STATUS)==null ? null : MigrationUtility.readCellValue(row.getCell(columnMap.get(MigrationConst.COL_STATUS)), false));
		owner.setGurdianName(columnMap.get(MigrationConst.COL_GUARDIAN_NAME)==null ? null : MigrationUtility.readCellValue(row.getCell(columnMap.get(MigrationConst.COL_GUARDIAN_NAME)), false));
		owner.setRelationship(columnMap.get(MigrationConst.COL_RELATIONSHIP_WITH_THE_GUARDIAN)==null ? null : MigrationUtility.readCellValue(row.getCell(columnMap.get(MigrationConst.COL_RELATIONSHIP_WITH_THE_GUARDIAN)), false));
		owner.setPropertyId(columnMap.get(MigrationConst.COL_PROPERTY_ID)==null ? null : MigrationUtility.readCellValue(row.getCell(columnMap.get(MigrationConst.COL_PROPERTY_ID)), false));
		owner.setPrimaryOwner(columnMap.get(MigrationConst.COL_PRIMARY_OWNER)==null ? null : MigrationUtility.readCellValue(row.getCell(columnMap.get(MigrationConst.COL_PRIMARY_OWNER)), false));
		owner.setOwnerType(columnMap.get(MigrationConst.COL_OWNER_TYPE)==null ? null : MigrationUtility.readCellValue(row.getCell(columnMap.get(MigrationConst.COL_OWNER_TYPE)), false));
		owner.setOwnerPercentage(columnMap.get(MigrationConst.COL_OWNERSHIP_PERCENTAGE)==null ? null : MigrationUtility.readCellValue(row.getCell(columnMap.get(MigrationConst.COL_OWNERSHIP_PERCENTAGE)), true));
		
		return owner;
	}

}
