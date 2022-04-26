package org.egov.migration.processor;

import java.util.Arrays;

import org.egov.migration.config.PropertiesData;
import org.egov.migration.config.SystemProperties;
import org.egov.migration.reader.model.BulkBillCriteria;
import org.egov.migration.reader.model.WSConnection;
import org.egov.migration.util.MigrationUtility;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Autowired;

public class WnsSchedulerTransformProcessor implements ItemProcessor<WSConnection, WSConnection> {
	
	@Autowired
	private PropertiesData properties;
	
	@Override
	public WSConnection process(WSConnection connection) throws Exception {
		String tenant = MigrationUtility.getSystemProperties().getTenants().get(connection.getTenantId().toLowerCase());
		BulkBillCriteria billCriteria = BulkBillCriteria.builder()
										.tenantIds(Arrays.asList(tenant))
										.connectionNos(Arrays.asList(connection.getConnectionNo()))
										.demandMonth(properties.getBulkBillCriteria().getDemandMonth())
										.demandYear(properties.getBulkBillCriteria().getDemandYear())
										.specialRebateMonths(properties.getBulkBillCriteria().getSpecialRebateMonths())
										.specialRebateYear(properties.getBulkBillCriteria().getSpecialRebateYear())
										.build();
		
		connection.setBulkBillCriteria(billCriteria);
		return connection;
	}
}
