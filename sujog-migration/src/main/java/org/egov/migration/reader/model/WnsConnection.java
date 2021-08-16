package org.egov.migration.reader.model;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class WnsConnection {
	
	private String ulb;
	private String connectionApplicationNo;
	private String connectionFacility;
	private String applicationStatus;
	private String connectionNo;
	private String applicationType;
	private String ward;
	
	private WnsService service;
	
	private WnsConnectionHolder connectionHolder;
	
	private WnsMeterReading meterReading;
	
	private List<WnsDemand> demands;
	
}
