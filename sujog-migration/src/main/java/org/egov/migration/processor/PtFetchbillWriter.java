package org.egov.migration.processor;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.egov.migration.business.model.PropertyDTO;
import org.egov.migration.business.model.PropertyDetailDTO;
import org.egov.migration.common.model.RecordStatistic;
import org.egov.migration.service.PropertyService;
import org.egov.migration.util.MigrationUtility;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PtFetchbillWriter implements ItemWriter<PropertyDetailDTO> {

	@Autowired
	private PropertyService propertyService;
	
	@Autowired
	private RecordStatistic recordStatistic;
	
	@Override
	public void write(List<? extends PropertyDetailDTO> items) throws Exception {
		items.forEach(propertyDetail -> {
			PropertyDTO propertyDTO = null;
			try {
				propertyDTO = propertyService.searchPropertyByPropertyId(propertyDetail);
				if(!Objects.isNull(propertyDTO)) {
					propertyService.callFetchBill(propertyDetail);
					MigrationUtility.addSuccessForPropertyFetchBill(propertyDetail.getProperty());
				} else {
					MigrationUtility.addError(propertyDetail.getProperty().getOldPropertyId(), "Property Not found");
				}
			} catch (Exception e) {
				log.error(String.format("PropertyId: %s, error message: %s", propertyDetail.getProperty().getOldPropertyId(), e.getMessage()));
				MigrationUtility.addError(propertyDetail.getProperty().getOldPropertyId(),String.format("Fetchbill error: %s",  e.getMessage()));
			}
			
		});
		
		try {
			generateReport();
		} catch (InvalidFormatException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	

	private void generateReport() throws IOException, InvalidFormatException {
		log.info("Generating Reports");
		propertyService.writeError();
		recordStatistic.getErrorRecords().clear();
		
		propertyService.writeFetchbillSuccess();
		recordStatistic.getSuccessRecords().clear();
		log.info("Reports updated");
	}
	
}