package org.egov.pt.migration.config;

import java.util.HashMap;

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
	
}
