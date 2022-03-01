package org.egov.migration.processor;

import java.io.IOException;
import java.util.List;

import org.egov.migration.business.model.ConnectionDTO;
import org.egov.migration.common.model.RecordStatistic;
import org.egov.migration.reader.model.WSConnection;
import org.egov.migration.service.WnsService;
import org.egov.migration.util.MigrationUtility;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class WnsFetchbillItemWriter implements ItemWriter<WSConnection> {
	
	@Autowired
	private WnsService wnsService;
	
	@Autowired
	private RecordStatistic recordStatistic;

	@Override
	public void write(List<? extends WSConnection> items) throws Exception {
		
		items.forEach(conn -> {
			try {
				wnsService.callFetchbill(conn);
				recordStatistic.getFetchbillSuccessRecords().add(conn.getConnectionNo());
			} catch (Exception e) {
				log.error("Exception in demand migration: " + e.getMessage());
				MigrationUtility.addError(conn.getConnectionNo(), e.getMessage());
			}
		});
		
		generateWnsReport();
	}

	private void generateWnsReport() throws IOException {
		log.info("Generating Reports");
		wnsService.writeError();
		recordStatistic.getErrorRecords().clear();
		
		wnsService.writeFetchbillSuccess();
		recordStatistic.getFetchbillSuccessRecords().clear();
		log.info("Reports updated");
		
	}
}
