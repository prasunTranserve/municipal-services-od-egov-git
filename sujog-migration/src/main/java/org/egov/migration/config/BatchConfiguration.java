package org.egov.migration.config;

import java.io.IOException;

import org.apache.poi.EncryptedDocumentException;
import org.egov.migration.business.model.ConnectionDTO;
import org.egov.migration.business.model.PropertyDetailDTO;
import org.egov.migration.common.model.RecordStatistic;
import org.egov.migration.processor.PropertyItemWriter;
import org.egov.migration.processor.PropertyReader;
import org.egov.migration.processor.PropertyTransformProcessor;
import org.egov.migration.processor.WnsItemReader;
import org.egov.migration.processor.WnsItemWriter;
import org.egov.migration.processor.WnsTransformProcessor;
import org.egov.migration.reader.model.Property;
import org.egov.migration.reader.model.WnsConnection;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
@EnableBatchProcessing
public class BatchConfiguration {
	
	@Bean
	public RecordStatistic getRecordStatistic() {
		return new RecordStatistic();
	}
	
	@Bean
	public RestTemplate restTemplate() {
		return new RestTemplate();
	}
	
	@Autowired
    public JobBuilderFactory jobBuilderFactory;

    @Autowired
    public StepBuilderFactory stepBuilderFactory;
    
    @Bean
    @StepScope
    public ItemReader<Property> getPropertyReader() throws EncryptedDocumentException, IOException, Exception {
    	PropertyReader propertyReader = new PropertyReader();
    	propertyReader.setSkipRecord(1);
    	return propertyReader;
    }
    
    @Bean
    public ItemProcessor<Property, PropertyDetailDTO> getPropertyProcessor() {
		return new PropertyTransformProcessor();
	}
    
    @Bean
    public ItemWriter<PropertyDetailDTO> getPropertyWriter() {
		return new PropertyItemWriter();
	}
    
    @Bean
    @StepScope
    public ItemReader<WnsConnection> getWnsReader() throws EncryptedDocumentException, IOException, Exception {
    	WnsItemReader wnsItemReader = new WnsItemReader();
    	wnsItemReader.setSkipRecord(1);
    	return wnsItemReader;
    }
    
    @Bean
    public ItemProcessor<WnsConnection, ConnectionDTO> getWnsProcessor() {
		return new WnsTransformProcessor();
	}
    
    @Bean
    public ItemWriter<ConnectionDTO> getWnsWriter() {
		return new WnsItemWriter();
	}
    
    @Bean(name = "stepPropertyMigrate")
    protected Step stepPropertyMigrate() throws EncryptedDocumentException, IOException, Exception {
        return stepBuilderFactory.get("stepPropertyMigrate").<Property, PropertyDetailDTO> chunk(20)
          .reader(getPropertyReader())
          .processor(getPropertyProcessor())
          .writer(getPropertyWriter()).build();
    }
    
    @Bean(name = "stepWnsMigrate")
    protected Step stepWnsMigrate() throws EncryptedDocumentException, IOException, Exception {
        return stepBuilderFactory.get("stepWnsMigrate").<WnsConnection, ConnectionDTO> chunk(30)
          .reader(getWnsReader())
          .processor(getWnsProcessor())
          .writer(getWnsWriter()).build();
    }

}
