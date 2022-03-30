package org.egov.migration.config;

import java.util.HashMap;
import java.util.List;

import org.egov.migration.common.model.FinancialYear;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

@Component
@Getter
@Setter
@ConfigurationProperties(prefix = "masterdata")
public class SystemProperties {
	
	private HashMap<String, String> tenants;
	
	private HashMap<String, String> taxhead;
	
	private HashMap<String, String> localitycode;
	
	private HashMap<String, Long> floorNo;
	
	private List<FinancialYear> financialyear;
	
}
