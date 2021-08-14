package org.egov.migration.business.controller;

import java.io.File;

import javax.validation.Valid;

import org.egov.migration.common.model.MigrationRequest;
import org.egov.migration.common.model.RecordStatistic;
import org.egov.migration.config.PropertiesData;
import org.egov.migration.processor.PropertyMigrationJobExecutionListner;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
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
public class PropertyBatchTriggerController {
	
	@Autowired
    public JobBuilderFactory jobBuilderFactory;
	
	@Autowired
    JobLauncher jobLauncher;
	
	@Autowired
	@Qualifier("stepPropertyMigrate")
	Step stepPropertyMigrate;
	
	@Autowired
	PropertiesData properties;
	
	@Autowired
	RecordStatistic recordStatistic;
	
	@Autowired
	PropertyMigrationJobExecutionListner propertyMigrationJobExecutionListner;
	
	@PostMapping("/property-migrate/run")
	public void runPropertyMigration(@RequestBody @Valid MigrationRequest request) {
		properties.setAuthToken(request.getAuthToken());
		File scanFolder = new File(properties.getPropertyDataFileDirectory());
		for (File fileToProceed : scanFolder.listFiles()) {
			if(fileToProceed.isFile() && fileToProceed.getName().endsWith(".xlsx")) {
				// Scanning of folder
				String fileName = fileToProceed.getName().toLowerCase();
				String file = fileToProceed.getPath();
				log.info(String.format("Processing %s", fileName));
		        try {
		        	recordStatistic.getErrorRecords().clear();
		        	recordStatistic.getSuccessRecords().clear();
		        	
		        	Job job = jobBuilderFactory.get("firstBatchJob")
		        			.incrementer(new RunIdIncrementer())
		        			.listener(propertyMigrationJobExecutionListner)
		        			.flow(stepPropertyMigrate).end().build();
		        	
		        	JobParameters jobParameters = new JobParametersBuilder()
		        			.addLong("time", System.currentTimeMillis())
		        			.addString("filePath", file)
		        			.addString("fileName", fileName)
		        			.toJobParameters();
		        	
					jobLauncher.run(job, jobParameters);
				} catch (JobExecutionAlreadyRunningException | JobRestartException | JobInstanceAlreadyCompleteException
						| JobParametersInvalidException e) {
					e.printStackTrace();
				}
			}
		}
	}
}
