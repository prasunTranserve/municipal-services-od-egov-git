package org.egov.migration.reader.model;

import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

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
	
//	@NotEmpty(message = "Connection facility cannot be empty/blank")
	private String connectionFacility;
	
	@NotEmpty(message = "Application Status cannot be empty/blank")
	private String applicationStatus;
	
	@NotEmpty(message = "Status cannot be empty/blank")
	private String status;
	
	private String connectionNo;
	
	private String applicationType;
	
//	@Pattern(regexp = "\\d+", message = "Word can not be non numeric")
	private String ward;
	
	@NotNull(message = "Connection service is missing")
	private @Valid WnsConnectionService service;
	
//	@NotNull(message = "Connection owner/holder is missing")
	private WnsConnectionHolder connectionHolder;
	
	private List<WnsMeterReading> meterReading;
	
	private List<WnsDemand> demands;
	
}
