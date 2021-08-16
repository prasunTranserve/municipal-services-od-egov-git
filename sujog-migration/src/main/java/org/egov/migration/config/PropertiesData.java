package org.egov.migration.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

@Component
@Getter
@Setter
public class PropertiesData {
	
	@Value("${file.path.input.directory.property}")
	private String propertyDataFileDirectory;
	
	@Value("${file.path.input.directory.wns}")
	private String wnsDataFileDirectory;
	
	@Value("${file.path.output.property.error}")
	private String propertyErrorFileDirectory;
	
	@Value("${file.path.output.property.success}")
	private String propertySuccessFileDirectory;
	
	@Value("${file.path.output.wns.error}")
	private String wnsErrorFileDirectory;
	
	@Value("${file.path.output.wns.success}")
	private String wnsSuccessFileDirectory;
	
	@Value("${remomte.api.pt.host}")
	private String ptServiceHost;
	
	@Value("${remomte.api.pt.migrate}")
	private String migratePropertyEndpoint;
	
	@Value("${remomte.api.pt.search}")
	private String ptSearchEndpoint;
	
	@Value("${remomte.api.pt.asmt.migrate}")
	private String migrateAssessmentEndPoint;
	
	@Value("${remomte.api.pt.asmt.search}")
	private String asmtSearchEndPoint;
	
	//@Value("${remomte.api.auth.token}")
	private String authToken;

}
