package org.egov.migration.business.controller;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;

import javax.validation.Valid;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.egov.migration.common.model.MigrationRequest;
import org.egov.migration.common.model.RecordStatistic;
import org.egov.migration.config.PropertiesData;
import org.egov.migration.processor.PropertyFetchbillJobExecutionListner;
import org.egov.migration.processor.PropertyMigrationJobExecutionListner;
import org.egov.migration.service.PropertyService;
import org.egov.migration.util.MigrationUtility;
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
	@Qualifier("stepPropertyFetchBill")
	Step stepPropertyFetchBill;
	
	@Autowired
	@Qualifier("stepPropertyMigrateUser")
	Step stepPropertyMigrateUser;
	
	@Autowired
	PropertiesData properties;
	
	@Autowired
	RecordStatistic recordStatistic;
	
	@Autowired
	PropertyMigrationJobExecutionListner propertyMigrationJobExecutionListner;
	
	@Autowired
	PropertyFetchbillJobExecutionListner propertyFetchbillJobExecutionListner;
	
	@Autowired
	PropertyService propertyService;
	
	@PostMapping("/property-migrate/run")
	public void runPropertyMigration(@RequestBody @Valid MigrationRequest request) {
		properties.setAuthToken(request.getAuthToken());
		File scanFolder = new File(properties.getPropertyDataFileDirectory());
		for (File fileToProceed : scanFolder.listFiles()) {
			if(fileToProceed.isFile() && fileToProceed.getName().endsWith(".xlsx")) {
				// Scanning of folder
				if(MigrationUtility.getSystemProperties().getTenants().containsKey(fileToProceed.getName().split("\\.")[0].toLowerCase())) {
					String fileName = fileToProceed.getName().toLowerCase();
					String file = fileToProceed.getPath();
					log.info(String.format("Processing %s, timestaamp: %s", fileName, LocalDateTime.now().toString()));
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
						log.info(String.format("Processing end %s, timestaamp: %s", fileName, LocalDateTime.now().toString()));
						propertyService.writeExecutionTime();
					} catch (JobExecutionAlreadyRunningException | JobRestartException | JobInstanceAlreadyCompleteException
							| JobParametersInvalidException e) {
						e.printStackTrace();
					} catch (InvalidFormatException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				} else {
					log.error(String.format("File name %s is not matching with in tenants list", fileToProceed.getName()));
//					recordStatistic.getFileNotProcessed().put(fileToProceed.getName(), "Tenant not match with digit listed tenants. File name should match with one of the tenants.");
					propertyService.writeFileError(fileToProceed.getName());
				}
			}
		}
	}

	@PostMapping("/property-migrate/_fetchbill")
	public void fetchBill(@RequestBody @Valid MigrationRequest request) {
		properties.setAuthToken(request.getAuthToken());
		File scanFolder = new File(properties.getPropertyDataFileDirectory());
		for (File fileToProceed : scanFolder.listFiles()) {
			if(fileToProceed.isFile() && fileToProceed.getName().endsWith(".xlsx")) {
				// Scanning of folder
				if(MigrationUtility.getSystemProperties().getTenants().containsKey(fileToProceed.getName().split("\\.")[0].toLowerCase())) {
					String fileName = fileToProceed.getName().toLowerCase();
					String file = fileToProceed.getPath();
					log.info(String.format("Processing %s, timestaamp: %s", fileName, LocalDateTime.now().toString()));
			        try {
			        	recordStatistic.getErrorRecords().clear();
			        	recordStatistic.getSuccessRecords().clear();
			        	
			        	Job job = jobBuilderFactory.get("firstBatchJob")
			        			.incrementer(new RunIdIncrementer())
			        			.listener(propertyFetchbillJobExecutionListner)
			        			.flow(stepPropertyFetchBill).end().build();
			        	
			        	JobParameters jobParameters = new JobParametersBuilder()
			        			.addLong("time", System.currentTimeMillis())
			        			.addString("filePath", file)
			        			.addString("fileName", fileName)
			        			.toJobParameters();
			        	
						jobLauncher.run(job, jobParameters);
						log.info(String.format("Processing end %s, timestaamp: %s", fileName, LocalDateTime.now().toString()));
						propertyService.writeExecutionTime();
					} catch (JobExecutionAlreadyRunningException | JobRestartException | JobInstanceAlreadyCompleteException
							| JobParametersInvalidException e) {
						e.printStackTrace();
					} catch (InvalidFormatException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				} else {
					log.error(String.format("File name %s is not matching with in tenants list", fileToProceed.getName()));
//					recordStatistic.getFileNotProcessed().put(fileToProceed.getName(), "Tenant not match with digit listed tenants. File name should match with one of the tenants.");
					propertyService.writeFileError(fileToProceed.getName());
				}
			}
		}
	}
	
	@PostMapping("/property-user-migrate/run")
	public void runPropertyUserMigration(@RequestBody @Valid MigrationRequest request) {
		properties.setAuthToken(request.getAuthToken());
		File scanFolder = new File(properties.getPropertyDataFileDirectory());
		for (File fileToProceed : scanFolder.listFiles()) {
			if(fileToProceed.isFile() && fileToProceed.getName().endsWith(".xlsx")) {
				// Scanning of folder
				if(MigrationUtility.getSystemProperties().getTenants().containsKey(fileToProceed.getName().split("\\.")[0].toLowerCase())) {
					String fileName = fileToProceed.getName().toLowerCase();
					String file = fileToProceed.getPath();
					log.info(String.format("Processing %s, timestaamp: %s", fileName, LocalDateTime.now().toString()));
			        try {
			        	recordStatistic.getErrorRecords().clear();
			        	recordStatistic.getSuccessRecords().clear();
			        	
			        	Job job = jobBuilderFactory.get("firstBatchJob")
			        			.incrementer(new RunIdIncrementer())
			        			.listener(propertyMigrationJobExecutionListner)
			        			.flow(stepPropertyMigrateUser).end().build();
			        	
			        	JobParameters jobParameters = new JobParametersBuilder()
			        			.addLong("time", System.currentTimeMillis())
			        			.addString("filePath", file)
			        			.addString("fileName", fileName)
			        			.toJobParameters();
			        	
						jobLauncher.run(job, jobParameters);
						log.info(String.format("Processing end %s, timestaamp: %s", fileName, LocalDateTime.now().toString()));
						propertyService.writeExecutionTime();
					} catch (JobExecutionAlreadyRunningException | JobRestartException | JobInstanceAlreadyCompleteException
							| JobParametersInvalidException e) {
						e.printStackTrace();
					} catch (InvalidFormatException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				} else {
					log.error(String.format("File name %s is not matching with in tenants list", fileToProceed.getName()));
//					recordStatistic.getFileNotProcessed().put(fileToProceed.getName(), "Tenant not match with digit listed tenants. File name should match with one of the tenants.");
					propertyService.writeFileError(fileToProceed.getName());
				}
			}
		}
	}
}
