package org.egov.migration.processor;

import java.io.File;

import org.egov.migration.common.model.RecordStatistic;
import org.egov.migration.config.PropertiesData;
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
	
	
	@Override
	public void beforeJob(JobExecution jobExecution) {
		String errorDirectory = properties.getPropertyErrorFileDirectory();
		String successDirectory = properties.getPropertySuccessFileDirectory();
		
		String inputFile = jobExecution.getJobParameters().getString("fileName");
		String errorFile = errorDirectory.concat(File.separator).concat(inputFile.replace(".", "_Error."));
		String successFile = successDirectory.concat(File.separator).concat(inputFile.replace(".", "_success."));
		
		recordStatistic.setSuccessFile(successFile);
		recordStatistic.setErrorFile(errorFile);
		
	}

	@Override
	public void afterJob(JobExecution jobExecution) {

	}
	
}
