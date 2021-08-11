package org.egov.migration.mapper;

import java.util.Map;

import org.apache.poi.ss.usermodel.Row;
import org.egov.migration.model.Address;
import org.egov.migration.util.MigrationConst;
import org.egov.migration.util.MigrationUtility;
import org.springframework.stereotype.Component;

@Component
public class AddressRowMapper {

	public Address mapRow(String ulb, Row row, Map<String, Integer> columnMap) {
		Address address = new Address();
		
		address.setUlb(ulb);
		address.setPropertyId(columnMap.get(MigrationConst.COL_PROPERTY_ID)==null ? null : MigrationUtility.readCellValue(row.getCell(columnMap.get(MigrationConst.COL_PROPERTY_ID)), false));
		address.setDoorNo(columnMap.get(MigrationConst.COL_DOOR_NO)==null ? null : MigrationUtility.readCellValue(row.getCell(columnMap.get(MigrationConst.COL_DOOR_NO)), false));
		address.setPlotNo(columnMap.get(MigrationConst.COL_PLOT_NO)==null ? null : MigrationUtility.readCellValue(row.getCell(columnMap.get(MigrationConst.COL_PLOT_NO)), false));
		address.setBuildingName(columnMap.get(MigrationConst.COL_BUILDING_NAME)==null ? null : MigrationUtility.readCellValue(row.getCell(columnMap.get(MigrationConst.COL_BUILDING_NAME)), false));
		address.setAddressLine1(columnMap.get(MigrationConst.COL_ADDRESS_LINE1)==null ? null : MigrationUtility.readCellValue(row.getCell(columnMap.get(MigrationConst.COL_ADDRESS_LINE1)), false));
		address.setAddressLine2(columnMap.get(MigrationConst.COL_ADDRESS_LINE2)==null ? null : MigrationUtility.readCellValue(row.getCell(columnMap.get(MigrationConst.COL_ADDRESS_LINE2)), false));
		address.setLandMark(columnMap.get(MigrationConst.COL_LANDMARK)==null ? null : MigrationUtility.readCellValue(row.getCell(columnMap.get(MigrationConst.COL_LANDMARK)), false));
		address.setCity(columnMap.get(MigrationConst.COL_CITY)==null ? null : MigrationUtility.readCellValue(row.getCell(columnMap.get(MigrationConst.COL_CITY)), false));
		address.setPin(columnMap.get(MigrationConst.COL_PIN_CODE)==null ? null : MigrationUtility.readCellValue(row.getCell(columnMap.get(MigrationConst.COL_PIN_CODE)), false));
		address.setLocality(columnMap.get(MigrationConst.COL_LOCALITY)==null ? null : MigrationUtility.readCellValue(row.getCell(columnMap.get(MigrationConst.COL_LOCALITY)), false));
		address.setDistrict(columnMap.get(MigrationConst.COL_DISTRICT_NAME)==null ? null : MigrationUtility.readCellValue(row.getCell(columnMap.get(MigrationConst.COL_DISTRICT_NAME)), false));
		address.setRegion(columnMap.get(MigrationConst.COL_REGION)==null ? null : MigrationUtility.readCellValue(row.getCell(columnMap.get(MigrationConst.COL_REGION)), false));
		address.setState(columnMap.get(MigrationConst.COL_STATE)==null ? null : MigrationUtility.readCellValue(row.getCell(columnMap.get(MigrationConst.COL_STATE)), false));
		address.setCountry(columnMap.get(MigrationConst.COL_COUNTRY)==null ? null : MigrationUtility.readCellValue(row.getCell(columnMap.get(MigrationConst.COL_COUNTRY)), false));
		address.setWard(columnMap.get(MigrationConst.COL_WARD)==null ? null : MigrationUtility.readCellValue(row.getCell(columnMap.get(MigrationConst.COL_WARD)), false));
		address.setCreatedDate(columnMap.get(MigrationConst.COL_CREATED_DATE)==null ? null : MigrationUtility.readCellValue(row.getCell(columnMap.get(MigrationConst.COL_CREATED_DATE)), false));
		address.setAdditionalDetails(columnMap.get(MigrationConst.COL_ADDITIONAL_DETAILS)==null ? null : MigrationUtility.readCellValue(row.getCell(columnMap.get(MigrationConst.COL_ADDITIONAL_DETAILS)), false));
		
		return address;
	}

}
