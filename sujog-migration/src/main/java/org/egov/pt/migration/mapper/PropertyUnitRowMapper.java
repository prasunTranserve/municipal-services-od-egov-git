package org.egov.pt.migration.mapper;

import java.util.Map;

import org.apache.poi.ss.usermodel.Row;
import org.egov.pt.migration.model.PropertyUnit;
import org.egov.pt.migration.util.MigrationConst;
import org.egov.pt.migration.util.MigrationUtility;
import org.springframework.stereotype.Component;

@Component
public class PropertyUnitRowMapper {
	
	public PropertyUnit mapRow(String ulb, Row row, Map<String, Integer> columnMap) {
		
		PropertyUnit propertyUnit = new PropertyUnit();
		
		propertyUnit.setUlb(ulb);
		propertyUnit.setFloorNo(columnMap.get(MigrationConst.COL_FLOOR_NO)==null ? null : MigrationUtility.readCellValue(row.getCell(columnMap.get(MigrationConst.COL_FLOOR_NO)), false));
		propertyUnit.setUnitType(columnMap.get(MigrationConst.COL_UNIT_TYPE)==null ? null : MigrationUtility.readCellValue(row.getCell(columnMap.get(MigrationConst.COL_UNIT_TYPE)), false));
		propertyUnit.setUsageCategory(columnMap.get(MigrationConst.COL_USAGE_CATEGORY)==null ? null : MigrationUtility.readCellValue(row.getCell(columnMap.get(MigrationConst.COL_USAGE_CATEGORY)), false));
		propertyUnit.setOccupancyType(columnMap.get(MigrationConst.COL_OCCUPANCY_TYPE)==null ? null : MigrationUtility.readCellValue(row.getCell(columnMap.get(MigrationConst.COL_OCCUPANCY_TYPE)), false));
		propertyUnit.setBuiltUpArea(columnMap.get(MigrationConst.COL_BUILTUP_AREA)==null ? null : MigrationUtility.readCellValue(row.getCell(columnMap.get(MigrationConst.COL_BUILTUP_AREA)), true));
		propertyUnit.setArv(columnMap.get(MigrationConst.COL_RENT)==null ? null : MigrationUtility.readCellValue(row.getCell(columnMap.get(MigrationConst.COL_RENT)), true));
		
		return propertyUnit;
	}

}
