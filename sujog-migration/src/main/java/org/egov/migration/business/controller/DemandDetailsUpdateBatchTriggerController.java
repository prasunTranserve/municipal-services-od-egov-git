package org.egov.migration.business.controller;

import java.io.File;
import java.io.IOException;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.egov.migration.business.model.DemandDetailSearchRequest;
import org.egov.migration.common.model.RecordStatistic;
import org.egov.migration.config.PropertiesData;
import org.egov.migration.processor.DemandUpdateJobExecutionListner;
import org.egov.migration.service.DemandService;
import org.egov.migration.util.MigrationUtility;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;

@RestController
@Slf4j
public class DemandDetailsUpdateBatchTriggerController {

	@Autowired
    public JobBuilderFactory jobBuilderFactory;
	
	@Autowired
    JobLauncher jobLauncher;
	
	@Autowired
	@Qualifier("stepDemandCollectionAmount")
	Step stepDemandCollectionAmount;
	
	@Autowired
	PropertiesData properties;
	
	@Autowired
	RecordStatistic recordStatistic;
	
	@Autowired
	DemandUpdateJobExecutionListner demandUpdateJobExecutionListner;
	
	@Autowired
	DemandService demandService;
	
	
	@Autowired
    public StepBuilderFactory stepBuilderFactory;
	
	@PostMapping("/updatedemand/_property")
	//public String updatePropertyDemand(@RequestBody @Valid MigrationRequest request) throws InvalidFormatException, IOException {
	public String updatePropertyDemand(@RequestBody DemandDetailSearchRequest request) throws InvalidFormatException, IOException {
		log.info("Update Property Demand started");
		String retVal = "";
		
		properties.setAuthToken(request.getRequestInfo().getAuthToken());//"61f25d29-e536-4b9f-828c-a66b4d9eb83d");//
		File scanFolder = new File(properties.getPropertyDataFileDirectory());
		for (File fileToProceed : scanFolder.listFiles()) {
			if(fileToProceed.isFile() && fileToProceed.getName().endsWith(".xlsx")) {
				// Scanning of folder
				if(MigrationUtility.getSystemProperties().getTenants().containsKey(fileToProceed.getName().split("\\.")[0].toLowerCase())) {
					String fileName =  fileToProceed.getName().toLowerCase();
					String file = fileToProceed.getPath();
			        try { 
			        	recordStatistic.getErrorRecords().clear();
			        	recordStatistic.getSuccessRecords().clear();
			        	
			        /*	stepDemandCollectionAmount = stepBuilderFactory
			        			.get("stepDemandCollectionAmount").chunk(100).reader(request);
			        			List<DemandDetailSearchRequest>.add(
			        			*/
			        	/*ItemWriter<DemandDetailSearchRequest> itemWriter;
			        	itemWriter.write(request);
			        	
			        	stepBuilderFactory.get("stepDemandCollectionAmount")
			            .chunk(100)
			            .writer(itemWriter)
			            .build();*/
			        	
			        	//stepDemandCollectionAmount.
			        	Job job = jobBuilderFactory.get("firstBatchJob")
			        			.incrementer(new RunIdIncrementer())
			        			.listener(demandUpdateJobExecutionListner)
			        			.flow(stepDemandCollectionAmount).end().build();
			        	
			        	JobParameters jobParameters = new JobParametersBuilder()
			        			.addLong("time", System.currentTimeMillis())
			        			.addString("filePath", file)
			        			.addString("fileName", fileName)
			        			.addString("businessService", request.getBusinessService())
			        			.addString("tenantId", request.getTenantId())
			        			.addString("demandDetailSearchRequest", String.valueOf(request))
			        			.toJobParameters();
			        	
						jobLauncher.run(job, jobParameters);
						try {
							demandService.writeExecutionTime(request); 
						} catch (InvalidFormatException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
							retVal="Failed to Execute";
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
							retVal="Failed to Execute";
						}
						retVal="Execution Successful";
					} catch (JobExecutionAlreadyRunningException | JobRestartException | JobInstanceAlreadyCompleteException
							| JobParametersInvalidException e) {
						e.printStackTrace();
						retVal="Failed to Execute";
					}
				} else {
 					log.error("File name %s is not matching with in tenants list", fileToProceed.getName());
					demandService.writeFileError(fileToProceed.getName());
					retVal="Failed to Execute";
				}
				
			}
		}
		return retVal;
	}
}

