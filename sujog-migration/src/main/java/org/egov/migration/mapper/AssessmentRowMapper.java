package org.egov.migration.mapper;

import java.util.Map;

import org.apache.poi.ss.usermodel.Row;
import org.egov.migration.reader.model.Assessment;
import org.egov.migration.util.MigrationConst;
import org.egov.migration.util.MigrationUtility;
import org.springframework.stereotype.Component;

@Component
public class AssessmentRowMapper {

	public Assessment mapRow(String ulb, Row row, Map<String, Integer> columnMap) {
		Assessment assessment = new Assessment();
		
		assessment.setUlb(ulb);
		assessment.setPropertyId(columnMap.get(MigrationConst.COL_PROPERTY_ID)==null ? null : MigrationUtility.readCellValue(row.getCell(columnMap.get(MigrationConst.COL_PROPERTY_ID)), false));
		assessment.setFinYear(columnMap.get(MigrationConst.COL_FINANCIAL_YEAR)==null ? null : MigrationUtility.readCellValue(row.getCell(columnMap.get(MigrationConst.COL_FINANCIAL_YEAR)), false));
		assessment.setStatus(columnMap.get(MigrationConst.COL_STATUS)==null ? null : MigrationUtility.readCellValue(row.getCell(columnMap.get(MigrationConst.COL_STATUS)), false));
		assessment.setAssessmentDate(columnMap.get(MigrationConst.COL_ASSESSMENT_DATE)==null ? null : MigrationUtility.readCellValue(row.getCell(columnMap.get(MigrationConst.COL_ASSESSMENT_DATE)), false));
		assessment.setCreatedDate(columnMap.get(MigrationConst.COL_CREATED_DATE)==null ? null : MigrationUtility.readCellValue(row.getCell(columnMap.get(MigrationConst.COL_CREATED_DATE)), false));
		assessment.setAdditionalDetails(columnMap.get(MigrationConst.COL_ADDITIONAL_DETAILS)==null ? null : MigrationUtility.readCellValue(row.getCell(columnMap.get(MigrationConst.COL_ADDITIONAL_DETAILS)), false));
		
		return assessment;
	}
}
