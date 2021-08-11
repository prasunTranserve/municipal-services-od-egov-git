package org.egov.migration.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

@Component
@Getter
@Setter
public class PropertiesData {
	
	@Value("${file.path.input.directory}")
	private String dataFileDirectory;
	
	@Value("${file.path.output.error}")
	private String errorFileDirectory;
	
	@Value("${file.path.output.success}")
	private String successFileDirectory;
	
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
