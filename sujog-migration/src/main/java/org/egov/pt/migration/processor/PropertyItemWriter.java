package org.egov.pt.migration.processor;

import java.io.IOException;
import java.util.List;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.egov.pt.migration.business.model.PropertyDTO;
import org.egov.pt.migration.business.model.PropertyDetailDTO;
import org.egov.pt.migration.common.model.RecordStatistic;
import org.egov.pt.migration.service.PropertyService;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PropertyItemWriter implements ItemWriter<PropertyDetailDTO> {
	
	@Autowired
	private PropertyService propertyService;
	
	@Autowired
	private RecordStatistic recordStatistic;

	@Override
	public void write(List<? extends PropertyDetailDTO> items) throws Exception {
		
		items.forEach(propertyDetail -> {
			boolean isPropertyMigrated = propertyService.migrateItem(propertyDetail);
			if(isPropertyMigrated) {
				PropertyDTO migratedProperty = propertyDetail.getProperty();
				recordStatistic.getSuccessRecords().put(migratedProperty.getOldPropertyId(), migratedProperty.getPropertyId());
			}
		});
		
		generateReport();
		
	}

	private void generateReport() throws IOException, InvalidFormatException {
		log.info("Generating Reports");
		propertyService.writeError();
		recordStatistic.getErrorRecords().clear();
		
		propertyService.writeSuccess();
		recordStatistic.getSuccessRecords().clear();
		log.info("Reports updated");
	}

}
