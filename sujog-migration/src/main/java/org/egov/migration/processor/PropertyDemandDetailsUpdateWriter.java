package org.egov.migration.processor;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.egov.migration.business.model.DemandDetailsDTO;
import org.egov.migration.common.model.RecordStatistic;
import org.egov.migration.service.DemandService;
import org.egov.migration.util.MigrationUtility;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PropertyDemandDetailsUpdateWriter implements ItemWriter<DemandDetailsDTO> {

	@Autowired
	private DemandService demandService;

	@Autowired
	private RecordStatistic recordStatistic;

	String businessService = "";
	String tenantId = "";

	@Override
	public void write(List<? extends DemandDetailsDTO> items) throws Exception {
		items.forEach(demandDetail -> {
			try {
				log.info("Starting Process for Demand ..:"+demandDetail.getDemandId());
				Double diffAmount = demandDetail.getPaidAmount() - demandDetail.getCollectionAmount();
				String amountToBeAdjusted = String.valueOf(diffAmount);
				businessService = demandDetail.getBusinessService();
				tenantId = demandDetail.getTenantId();
				String demandId = demandDetail.getDemandId();
				boolean isProcessSuccessful = false;
				Map<String, String> status = new HashMap<>();

				// Call update API Here
				isProcessSuccessful = demandService.callDemandDetailsUpdate(demandDetail.getRequestInfo(), demandId,
						diffAmount, businessService, tenantId);
				status.put(demandId, String.valueOf(isProcessSuccessful));

				if (isProcessSuccessful) {
					MigrationUtility.addSuccessForExternalDemandUpdate(amountToBeAdjusted, demandId);
				} else {
					MigrationUtility.addError(demandId, String.format("Demand Could not be Updated"));
				}
			} catch (Exception e) {
				log.error(
						String.format("Demand Id: %s, error message: %s", demandDetail.getDemandId(), e.getMessage()));
				MigrationUtility.addError(demandDetail.getDemandId(),
						String.format("Demand Update error: %s", e.getMessage()));
			}
		});
		try {
			generateReport();
		} catch (InvalidFormatException | IOException e) {
			e.printStackTrace();
		}
	}

	private void generateReport() throws IOException, InvalidFormatException {
		log.info("Generating Reports");
		demandService.writeError();
		recordStatistic.getErrorRecords().clear();

		demandService.writeSuccess();
		recordStatistic.getSuccessRecords().clear();
		log.info("Reports updated");
	}
}
