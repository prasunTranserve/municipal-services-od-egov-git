package org.egov.migration.config;

import java.io.IOException;

import org.apache.poi.EncryptedDocumentException;
import org.egov.migration.business.model.ConnectionDTO;
import org.egov.migration.business.model.DemandDetailsDTO;
import org.egov.migration.business.model.PropertyDetailDTO;
import org.egov.migration.common.model.RecordStatistic;
import org.egov.migration.processor.DemandSearchTransformProcessor;
import org.egov.migration.processor.PropertyAssessmentItemWriter;
import org.egov.migration.processor.PropertyAssessmentTransformProcessor;
import org.egov.migration.processor.PropertyDemandDetailsUpdateReader;
import org.egov.migration.processor.PropertyDemandDetailsUpdateWriter;
import org.egov.migration.processor.PropertyFetchbillReader;
import org.egov.migration.processor.PropertyFetchbillWriter;
import org.egov.migration.processor.PropertyItemWriter;
import org.egov.migration.processor.PropertyItemWriterForUser;
import org.egov.migration.processor.PropertyReader;
import org.egov.migration.processor.PropertyReaderForUser;
import org.egov.migration.processor.PropertySearchTransformProcessor;
import org.egov.migration.processor.PropertyTransformProcessor;
import org.egov.migration.processor.PropertyTransformProcessorForUser;
import org.egov.migration.processor.PtFetchbillItemReader;
import org.egov.migration.processor.PtFetchbillWriter;
import org.egov.migration.processor.PtSearchTransformProcessor;
import org.egov.migration.processor.WnsItemReader;
import org.egov.migration.processor.WnsItemWriter;
import org.egov.migration.processor.WnsTransformProcessor;
import org.egov.migration.reader.model.DemandDetailPaymentMapper;
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
        return stepBuilderFactory.get("stepPropertyMigrate").<Property, PropertyDetailDTO> chunk(100)
          .reader(getPropertyReader())
          .processor(getPropertyProcessor())
          .writer(getPropertyWriter()).build();
    }
    
    @Bean(name = "stepWnsMigrate")
    protected Step stepWnsMigrate() throws EncryptedDocumentException, IOException, Exception {
        return stepBuilderFactory.get("stepWnsMigrate").<WnsConnection, ConnectionDTO> chunk(100)
          .reader(getWnsReader())
          .processor(getWnsProcessor())
          .writer(getWnsWriter()).build();
    }
    
    @Bean(name = "stepPropertyFetchBill")
    protected Step stepPropertyFetchBill() throws EncryptedDocumentException, IOException, Exception {
        return stepBuilderFactory.get("stepPropertyMigrate").<Property, PropertyDetailDTO> chunk(100)
          .reader(getPropertyFetchbillReader())
          .processor(getProprtyFetchbillProcessor())
          .writer(getProprtyFetchbillWriter()).build();
    }
    
    @Bean(name = "stepPropertyMigrateUser")
    protected Step stepPropertyMigrateUser() throws EncryptedDocumentException, IOException, Exception {
        return stepBuilderFactory.get("stepPropertyMigrateUser").<Property, PropertyDetailDTO> chunk(100)
          .reader(getPropertyReaderUser())
          .processor(getPropertyProcessorUser())
          .writer(getPropertyWriterUser()).build();
    }
	
	@Bean
    @StepScope
    public ItemReader<Property> getPropertyFetchbillReader() throws EncryptedDocumentException, IOException, Exception {
    	PropertyFetchbillReader propertyFetchbillReader = new PropertyFetchbillReader();
    	propertyFetchbillReader.setSkipRecord(1);
    	return propertyFetchbillReader;
    }
    
    @Bean
    public ItemProcessor<Property, PropertyDetailDTO> getProprtyFetchbillProcessor() {
		return new PropertySearchTransformProcessor();
	}
    
    @Bean
    public ItemWriter<PropertyDetailDTO> getProprtyFetchbillWriter() {
		return new PropertyFetchbillWriter();
	}
    
    @Bean
    @StepScope
    public ItemReader<Property> getPropertyReaderUser() throws EncryptedDocumentException, IOException, Exception {
    	PropertyReaderForUser propertyReader = new PropertyReaderForUser();
    	propertyReader.setSkipRecord(1);
    	return propertyReader;
    }
    
    @Bean
    public ItemProcessor<Property, PropertyDetailDTO> getPropertyProcessorUser() {
		return new PropertyTransformProcessorForUser();
	}
    
    @Bean
    public ItemWriter<PropertyDetailDTO> getPropertyWriterUser() {
		return new PropertyItemWriterForUser();
	}

    @Bean(name = "stepPtFetchbill")
    protected Step stepPtFetchbill() throws EncryptedDocumentException, IOException, Exception {
        return stepBuilderFactory.get("stepPtFetchbill").<Property, PropertyDetailDTO> chunk(100)
          .reader(getPTFetchBillReader())
          .processor(getPTFetchbillProcessor())
          .writer(getPTFetchbillWriter()).build();
    }
    
    @Bean
    public ItemProcessor<Property, PropertyDetailDTO> getPTFetchbillProcessor() {
		return new PtSearchTransformProcessor();
	}
    
    @Bean
    @StepScope
	public ItemReader<? extends Property> getPTFetchBillReader() throws EncryptedDocumentException, IOException, Exception {
    	PtFetchbillItemReader ptItemReader = new PtFetchbillItemReader();
		ptItemReader.setSkipRecord(1);
    	return ptItemReader;
	}
    
    @Bean
    public ItemWriter<PropertyDetailDTO> getPTFetchbillWriter() {
		return new PtFetchbillWriter();
	}
    
    @Bean
    public ItemProcessor<Property, PropertyDetailDTO> getPropertyAssessmentProcessor() {
		return new PropertyAssessmentTransformProcessor();
	}
    
    @Bean
    public ItemWriter<PropertyDetailDTO> getPropertyAssessmentWriter() {
		return new PropertyAssessmentItemWriter();
	}
    
    @Bean(name = "stepPropertyAssessmentMigrate")
    protected Step stepPropertyAssessmentMigrate() throws EncryptedDocumentException, IOException, Exception {
        return stepBuilderFactory.get("stepPropertyMigrate").<Property, PropertyDetailDTO> chunk(100)
          .reader(getPropertyReader())
          .processor(getPropertyAssessmentProcessor())
          .writer(getPropertyAssessmentWriter()).build();
    }

// For Demand Details Update
    @Bean(name = "stepDemandCollectionAmount")
    protected Step stepDemandCollectionAmount() throws EncryptedDocumentException, IOException, Exception {
        return stepBuilderFactory.get("stepDemandCollectionAmount").<DemandDetailPaymentMapper, DemandDetailsDTO> chunk(100)
          .reader(getDemandCollectionReader())
          .processor(getDemandCollectionProcessor())
          .writer(getDemandCollectionWriter()).build();
    }

    @Bean
    @StepScope
    public ItemReader<DemandDetailPaymentMapper> getDemandCollectionReader() throws EncryptedDocumentException, IOException, Exception {
    	PropertyDemandDetailsUpdateReader propertyDemandDetailsUpdateReader = new PropertyDemandDetailsUpdateReader();
    	propertyDemandDetailsUpdateReader.setSkipRecord(1);
    	return propertyDemandDetailsUpdateReader;
    }
    
    @Bean
    public ItemProcessor<DemandDetailPaymentMapper, DemandDetailsDTO> getDemandCollectionProcessor() {
		return new DemandSearchTransformProcessor(); 
	}
    
    @Bean
    public ItemWriter<DemandDetailsDTO> getDemandCollectionWriter() {//DemandDetailSearchRequest request
		return new PropertyDemandDetailsUpdateWriter();
	}
}
