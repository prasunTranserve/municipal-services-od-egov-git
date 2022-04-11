package org.egov.migration.mapper;

import java.util.Map;

import org.apache.poi.ss.usermodel.Row;
import org.egov.migration.reader.model.DemandDetailPaymentMapper;
import org.egov.migration.util.MigrationConst;
import org.egov.migration.util.MigrationUtility;
import org.springframework.stereotype.Component;

@Component
public class PropertyDemandDetailsRowMapper {

	
	public DemandDetailPaymentMapper mapRow(String ulb, Row row, Map<String, Integer> columnMap) {
		DemandDetailPaymentMapper demand = new DemandDetailPaymentMapper();
		
		//demand.setBillId(columnMap.get(MigrationConst.COL_BILL_ID)==null ? null : MigrationUtility.readCellValue(row.getCell(columnMap.get(MigrationConst.COL_BILL_ID)), false));
		demand.setDemandid(columnMap.get(MigrationConst.COL_DEMAND_ID)==null ? null : MigrationUtility.readCellValue(row.getCell(columnMap.get(MigrationConst.COL_DEMAND_ID)), false));
		demand.setAmountpaid(columnMap.get(MigrationConst.COL_AMOUNT_PAID)==null ? null : MigrationUtility.readCellValue(row.getCell(columnMap.get(MigrationConst.COL_AMOUNT_PAID)), false));
		demand.setCollectionamount(columnMap.get(MigrationConst.COL_COLLECTED_AMOUNT)==null ? null : MigrationUtility.readCellValue(row.getCell(columnMap.get(MigrationConst.COL_COLLECTED_AMOUNT)), false));
		
		return demand;
	}
}
