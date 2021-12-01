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
	
	@Value("${remomte.api.pt.migrateuser}")
	private String migratePropertyUserEndpoint;
	
	@Value("${remomte.api.pt.search}")
	private String ptSearchEndpoint;
	
	@Value("${remomte.api.pt.asmt.migrate}")
	private String migrateAssessmentEndPoint;
	
	@Value("${remomte.api.pt.asmt.search}")
	private String asmtSearchEndPoint;
	
	private String authToken;
	
	@Value("${remomte.api.auth.url}")
	private String authTokenUrl;
	
	@Value("${remomte.api.auth.username}")
	private String username;
	
	@Value("${remomte.api.auth.password}")
	private String password;
	
	@Value("${remomte.api.auth.tenant}")
	private String tenantId;
	
	@Value("${remomte.api.ws.host}")
	private String wsServiceHost;
	
	@Value("${remomte.api.ws.search}")
	private String wsSearchEndpoint;
	
	@Value("${remomte.api.ws.connectionmigrate}")
	private String wsMigrateEndpoint;
	
	@Value("${remomte.api.ws.calculator.host}")
	private String wsCalculatorHost; //meterreadingsearch
	
	@Value("${remomte.api.ws.calculator.meterreadingsearch}")
	private String wsSearchMeterReadingEndpoint;
	
	@Value("${remomte.api.ws.calculator.meterreadingmigeate}")
	private String wsMigrateMeterReadingEndpoint;
	
	@Value("${remomte.api.ws.calculator.demandmigrate}")
	private String wsMigrateDemandEndpoint;

	@Value("${remomte.api.sw.host}")
	private String swServiceHost;
	
	@Value("${remomte.api.sw.search}")
	private String swSearchEndpoint;
	
	@Value("${remomte.api.sw.connectionmigrate}")
	private String swMigrateConnectionEndpoint;
	
	@Value("${remomte.api.sw.calculator.host}")
	private String swCalculatorHost;
	
	@Value("${remomte.api.sw.calculator.demandmigrate}")
	private String swMigrateDemandEndpoint;
	
	@Value("${remomte.api.demand.host}")
	private String billingServiceHost;
	
	@Value("${remomte.api.demand.search}")
	private String demandSearchEndpoint;
	
	@Value("${remomte.api.demand.fetchbill}")
	private String fetchBillEndpoint;
	
}
