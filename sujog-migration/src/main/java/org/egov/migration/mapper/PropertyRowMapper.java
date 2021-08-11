package org.egov.migration.mapper;

import java.util.Map;

import org.apache.poi.ss.usermodel.Row;
import org.egov.migration.reader.model.Property;
import org.egov.migration.util.MigrationConst;
import org.egov.migration.util.MigrationUtility;
import org.springframework.stereotype.Component;

@Component
public class PropertyRowMapper {

	public Property mapRow(String ulb, Row row, Map<String, Integer> columnMap) {
		Property property = new Property();
		
		property.setUlb(ulb);
		property.setPropertyId(columnMap.get(MigrationConst.COL_PROPERTY_ID)==null ? null : MigrationUtility.readCellValue(row.getCell(columnMap.get(MigrationConst.COL_PROPERTY_ID)), false));
		property.setStatus(columnMap.get(MigrationConst.COL_STATUS)==null ? null : MigrationUtility.readCellValue(row.getCell(columnMap.get(MigrationConst.COL_STATUS)), false));
		property.setPropertyType(columnMap.get(MigrationConst.COL_PROPERTY_TYPE)==null ? null : MigrationUtility.readCellValue(row.getCell(columnMap.get(MigrationConst.COL_PROPERTY_TYPE)), false));
		property.setOwnershipCategory(columnMap.get(MigrationConst.COL_OWNERSHIP_CATEGORY)==null ? null : MigrationUtility.readCellValue(row.getCell(columnMap.get(MigrationConst.COL_OWNERSHIP_CATEGORY)), false));
		property.setUsageCategory(columnMap.get(MigrationConst.COL_USAGE_CATEGORY)==null ? null : MigrationUtility.readCellValue(row.getCell(columnMap.get(MigrationConst.COL_USAGE_CATEGORY)), false));
		property.setFloorNo(columnMap.get(MigrationConst.COL_FLOOR_NO)==null ? null : MigrationUtility.readCellValue(row.getCell(columnMap.get(MigrationConst.COL_FLOOR_NO)), false));
		property.setLandArea(columnMap.get(MigrationConst.COL_LAND_AREA)==null ? null : MigrationUtility.readCellValue(row.getCell(columnMap.get(MigrationConst.COL_LAND_AREA)), true));
		property.setLandAreaUnit(columnMap.get(MigrationConst.COL_PLOT_SIZE_UNIT)==null ? null : MigrationUtility.readCellValue(row.getCell(columnMap.get(MigrationConst.COL_PLOT_SIZE_UNIT)), false));
		property.setBuildupArea(columnMap.get(MigrationConst.COL_SUPER_BUILTUP_AREA)==null ? null : MigrationUtility.readCellValue(row.getCell(columnMap.get(MigrationConst.COL_SUPER_BUILTUP_AREA)), true));
		property.setBuildupAreaUnit(columnMap.get(MigrationConst.COL_BUILTUP_AREA_UNIT)==null ? null : MigrationUtility.readCellValue(row.getCell(columnMap.get(MigrationConst.COL_BUILTUP_AREA_UNIT)), false));
		property.setCreatedDate(columnMap.get(MigrationConst.COL_CREATED_DATE)==null ? null : MigrationUtility.readCellValue(row.getCell(columnMap.get(MigrationConst.COL_CREATED_DATE)), false));
		property.setAdditionalDetails(columnMap.get(MigrationConst.COL_ADDITIONAL_DETAILS)==null ? null : MigrationUtility.readCellValue(row.getCell(columnMap.get(MigrationConst.COL_ADDITIONAL_DETAILS)), false));
		
		return property;
	}

}
