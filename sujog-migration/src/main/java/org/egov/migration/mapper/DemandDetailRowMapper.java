package org.egov.migration.mapper;

import java.util.Map;

import org.apache.poi.ss.usermodel.Row;
import org.egov.migration.reader.model.DemandDetail;
import org.egov.migration.util.MigrationConst;
import org.egov.migration.util.MigrationUtility;
import org.springframework.stereotype.Component;

@Component
public class DemandDetailRowMapper {

	public DemandDetail mapRow(String ulb, Row row, Map<String, Integer> columnMap) {
		DemandDetail demandDetail = new DemandDetail();
		
		demandDetail.setUlb(ulb);
		demandDetail.setPropertyId(columnMap.get(MigrationConst.COL_PROPERTY_ID)==null ? null : MigrationUtility.readCellValue(row.getCell(columnMap.get(MigrationConst.COL_PROPERTY_ID)), false));
		demandDetail.setTaxHead(columnMap.get(MigrationConst.COL_TAX_HEAD)==null ? null : MigrationUtility.readCellValue(row.getCell(columnMap.get(MigrationConst.COL_TAX_HEAD)), false));
		demandDetail.setTaxAmt(columnMap.get(MigrationConst.COL_TAX_AMOUNT)==null ? null : MigrationUtility.readCellValue(row.getCell(columnMap.get(MigrationConst.COL_TAX_AMOUNT)), true));
		demandDetail.setCollectedAmt(columnMap.get(MigrationConst.COL_COLLECTION_AMOUNT)==null ? null : MigrationUtility.readCellValue(row.getCell(columnMap.get(MigrationConst.COL_COLLECTION_AMOUNT)), true));
		demandDetail.setCreatedDate(columnMap.get(MigrationConst.COL_CREATED_DATE)==null ? null : MigrationUtility.readCellValue(row.getCell(columnMap.get(MigrationConst.COL_CREATED_DATE)), false));
		demandDetail.setAdditionalDetails(columnMap.get(MigrationConst.COL_ADDITIONAL_DETAILS)==null ? null : MigrationUtility.readCellValue(row.getCell(columnMap.get(MigrationConst.COL_ADDITIONAL_DETAILS)), false));
		
		return demandDetail;
	}

}
