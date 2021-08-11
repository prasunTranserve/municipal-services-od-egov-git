package org.egov.pt.migration.mapper;

import java.util.Map;

import org.apache.poi.ss.usermodel.Row;
import org.egov.pt.migration.model.Demand;
import org.egov.pt.migration.util.MigrationConst;
import org.egov.pt.migration.util.MigrationUtility;
import org.springframework.stereotype.Component;

@Component
public class DemandRowMapper {

	public Demand mapRow(String ulb, Row row, Map<String, Integer> columnMap) {
		Demand demand = new Demand();
		
		demand.setUlb(ulb);
		demand.setPropertyId(columnMap.get(MigrationConst.COL_PROPERTY_ID)==null ? null : MigrationUtility.readCellValue(row.getCell(columnMap.get(MigrationConst.COL_PROPERTY_ID)), false));
		demand.setPayerName(columnMap.get(MigrationConst.COL_PAYER_NAME)==null ? null : MigrationUtility.readCellValue(row.getCell(columnMap.get(MigrationConst.COL_PAYER_NAME)), false));
		demand.setTaxPeriodFrom(columnMap.get(MigrationConst.COL_TAX_PERIOD_FROM)==null ? null : MigrationUtility.readCellValue(row.getCell(columnMap.get(MigrationConst.COL_TAX_PERIOD_FROM)), false));
		demand.setTaxPeriodTo(columnMap.get(MigrationConst.COL_TAX_PERIOD_TO)==null ? null : MigrationUtility.readCellValue(row.getCell(columnMap.get(MigrationConst.COL_TAX_PERIOD_TO)), false));
		demand.setCreatedDate(columnMap.get(MigrationConst.COL_CREATED_DATE)==null ? null : MigrationUtility.readCellValue(row.getCell(columnMap.get(MigrationConst.COL_CREATED_DATE)), false));
		demand.setMinPayableAmt(columnMap.get(MigrationConst.COL_MINIMUM_AMOUNT_PAYBLE)==null ? null : MigrationUtility.readCellValue(row.getCell(columnMap.get(MigrationConst.COL_MINIMUM_AMOUNT_PAYBLE)), true));
		demand.setStatus(columnMap.get(MigrationConst.COL_STATUS)==null ? null : MigrationUtility.readCellValue(row.getCell(columnMap.get(MigrationConst.COL_STATUS)), false));
		demand.setAdditionalDetails(columnMap.get(MigrationConst.COL_ADDITIONAL_DETAILS)==null ? null : MigrationUtility.readCellValue(row.getCell(columnMap.get(MigrationConst.COL_ADDITIONAL_DETAILS)), false));
		demand.setPaymentComplete(columnMap.get(MigrationConst.COL_IS_PAYMENT_COMPLETED)==null ? null : MigrationUtility.readCellValue(row.getCell(columnMap.get(MigrationConst.COL_IS_PAYMENT_COMPLETED)), false));
		
		return demand;
	}

}
