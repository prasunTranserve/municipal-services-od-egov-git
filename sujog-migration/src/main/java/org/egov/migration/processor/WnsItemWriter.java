package org.egov.migration.processor;

import java.io.IOException;
import java.util.List;

import org.egov.migration.business.model.ConnectionDTO;
import org.egov.migration.common.model.RecordStatistic;
import org.egov.migration.service.WnsService;
import org.egov.migration.util.MigrationUtility;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class WnsItemWriter implements ItemWriter<ConnectionDTO> {
	
	@Autowired
	private WnsService wnsService;
	
	@Autowired
	private RecordStatistic recordStatistic;

	@Override
	public void write(List<? extends ConnectionDTO> items) throws Exception {
		
		items.forEach(conn -> {
			try {
				wnsService.migrateConnection(conn);
			} catch (Exception e) {
				log.error("Exception in demand migration: " + e.getMessage());
				MigrationUtility.addError(conn.getWaterConnection().getOldConnectionNo(), e.getMessage());
			}
		});
		
		try {
			Thread.sleep(3000);
		} catch (Exception e) {
			// TODO: handle exception
		}
		
		items.forEach(conn -> {
			try {
				boolean isMeterReadingMigrated = false;
				if(conn.getWaterConnection() != null && conn.getWaterConnection().getConnectionNo() != null)
					isMeterReadingMigrated = wnsService.migrateMeterReading(conn);
				if(isMeterReadingMigrated) {
					MigrationUtility.addSuccessForMeterReading(conn);
				}
			} catch (Exception e) {
				log.error("Exception in meterreading migration: " + e.getMessage());
				MigrationUtility.addError(conn.getWaterConnection().getOldConnectionNo(), e.getMessage());
			}
			
			try {
				wnsService.migrateDemands(conn);
			} catch (Exception e) {
				log.error("Exception in demand migration: " + e.getMessage());
				if(conn.isWater()) 
					MigrationUtility.addError(conn.getWaterConnection().getOldConnectionNo(), e.getMessage());
				if(conn.isSewerage())
					MigrationUtility.addError(conn.getSewerageConnection().getOldConnectionNo(), e.getMessage());
			}
		});
		
		generateWnsReport();
	}

	private void generateWnsReport() throws IOException {
		log.info("Generating Reports");
		wnsService.writeError();
		recordStatistic.getErrorRecords().clear();
		
		wnsService.writeSuccess();
		recordStatistic.getSuccessRecords().clear();
		log.info("Reports updated");
		
	}
}
