package org.egov.migration.mapper;

import java.util.Map;

import org.apache.poi.ss.usermodel.Row;
import org.egov.migration.reader.model.WnsDemand;
import org.egov.migration.util.MigrationConst;
import org.egov.migration.util.MigrationUtility;
import org.springframework.stereotype.Component;

@Component
public class WnsDemandRowMapper {
	
	public WnsDemand mapRow(String ulb, Row row, Map<String, Integer> columnMap) {
		WnsDemand demand = new WnsDemand();
		
		demand.setUlb(ulb);
		demand.setConnectionNo(columnMap.get(MigrationConst.COL_CONNECTION_NO)==null ? null : MigrationUtility.readCellValue(row.getCell(columnMap.get(MigrationConst.COL_CONNECTION_NO)), false));
//		demand.setConnectionFacility(columnMap.get(MigrationConst.COL_CONNECTION_FACILTY)==null ? null : MigrationUtility.readCellValue(row.getCell(columnMap.get(MigrationConst.COL_CONNECTION_FACILTY)), false));
//		demand.setPayerName(columnMap.get(MigrationConst.COL_PAYER_NAME)==null ? null : MigrationUtility.readCellValue(row.getCell(columnMap.get(MigrationConst.COL_PAYER_NAME)), false));
		demand.setBillingPeriodFrom(columnMap.get(MigrationConst.COL_LATEST_BILLING_PERIOD_FROM)==null ? null : MigrationUtility.readCellValue(row.getCell(columnMap.get(MigrationConst.COL_LATEST_BILLING_PERIOD_FROM)), false));
		demand.setBillingPeriodTo(columnMap.get(MigrationConst.COL_LATEST_BILLING_PERIOD_TO)==null ? null : MigrationUtility.readCellValue(row.getCell(columnMap.get(MigrationConst.COL_LATEST_BILLING_PERIOD_TO)), false));
//		demand.setMinAmountPayable(columnMap.get(MigrationConst.COL_MINIMUM_AMOUNT_PAYBLE)==null ? null : MigrationUtility.readCellValue(row.getCell(columnMap.get(MigrationConst.COL_MINIMUM_AMOUNT_PAYBLE)), false));
//		demand.setStatus(columnMap.get(MigrationConst.COL_STATUS)==null ? null : MigrationUtility.readCellValue(row.getCell(columnMap.get(MigrationConst.COL_STATUS)), false));
//		demand.setIsPaymentCompleted(columnMap.get(MigrationConst.COL_IS_PAYMENT_COMPLETED)==null ? null : MigrationUtility.readCellValue(row.getCell(columnMap.get(MigrationConst.COL_IS_PAYMENT_COMPLETED)), false));
		demand.setCollectedAmount(columnMap.get(MigrationConst.COL_COLLECTION_AMOUNT_AGAINST_LATEST_BILL)==null ? null : MigrationUtility.readCellValue(row.getCell(columnMap.get(MigrationConst.COL_COLLECTION_AMOUNT_AGAINST_LATEST_BILL)), true));
		demand.setWaterCharges(columnMap.get(MigrationConst.COL_LATEST_BILLING_WATER_CHARGES)==null ? null : MigrationUtility.readCellValue(row.getCell(columnMap.get(MigrationConst.COL_LATEST_BILLING_WATER_CHARGES)), true));
		demand.setSewerageFee(columnMap.get(MigrationConst.COL_LATEST_BILLING_SEWERAGE_FEE)==null ? null : MigrationUtility.readCellValue(row.getCell(columnMap.get(MigrationConst.COL_LATEST_BILLING_SEWERAGE_FEE)), true));
		demand.setArrear(columnMap.get(MigrationConst.COL_ARREAR)==null ? null : MigrationUtility.readCellValue(row.getCell(columnMap.get(MigrationConst.COL_ARREAR)), true));
		
		return demand;
	}
}
