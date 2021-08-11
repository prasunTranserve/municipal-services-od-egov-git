package org.egov.pt.migration.processor;

import org.egov.pt.migration.common.model.RecordStatistic;
import org.egov.pt.migration.config.PropertiesData;
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
		String errorDirectory = properties.getErrorFileDirectory();
		String successDirectory = properties.getSuccessFileDirectory();
		
		String inputFile = jobExecution.getJobParameters().getString("fileName");
		String errorFile = errorDirectory.concat("\\").concat(inputFile.replace(".", "_Error."));
		String successFile = successDirectory.concat("\\").concat(inputFile.replace(".", "_success."));
		
		recordStatistic.setSuccessFile(successFile);
		recordStatistic.setErrorFile(errorFile);
		
	}

	@Override
	public void afterJob(JobExecution jobExecution) {

	}
	
}
