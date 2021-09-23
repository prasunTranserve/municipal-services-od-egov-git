package org.egov.migration.processor;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.egov.migration.common.model.RecordStatistic;
import org.egov.migration.config.PropertiesData;
import org.egov.migration.service.PropertyService;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class PropertyMigrationJobExecutionListner implements JobExecutionListener {

	@Autowired
	private PropertiesData properties;
	
	@Autowired
	RecordStatistic recordStatistic;
	
	@Autowired
	PropertyService propertyService;
	
	
	@Override
	public void beforeJob(JobExecution jobExecution) {
		String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_hh-mm-ss"));
		String errorDirectory = properties.getPropertyErrorFileDirectory();
		String successDirectory = properties.getPropertySuccessFileDirectory();
		
		String inputFile = jobExecution.getJobParameters().getString("fileName");
		String errorFile = errorDirectory.concat(File.separator).concat(inputFile.replace(".", "_error.")).replace(".", "_".concat(timestamp).concat("."));
		String successFile = successDirectory.concat(File.separator).concat(inputFile.replace(".", "_success.")).replace(".", "_".concat(timestamp).concat("."));
		
		recordStatistic.setSuccessFile(successFile);
		recordStatistic.setErrorFile(errorFile);
		
		String startTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm:ss a"));
		recordStatistic.setStartTime(startTime);
		
		//propertyService.refreshAuthToken();
	}

	@Override
	public void afterJob(JobExecution jobExecution) {
		String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm:ss a"));
		recordStatistic.setEndTime(timestamp);
	}
}
