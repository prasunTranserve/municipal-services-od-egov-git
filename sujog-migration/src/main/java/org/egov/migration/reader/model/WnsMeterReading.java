package org.egov.migration.reader.model;

import javax.validation.constraints.NotEmpty;
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
public class WnsMeterReading {
	
	private String ulb;
	private String connectionNo;
	
//	@NotEmpty(message = "Connection Facility cannot be empty/blank")
	private String connectionFacility;
	
//	@NotEmpty(message = "Billing period cannot be empty/blank")
	private String billingPeriod;
	
	private String meterStatus;
	private String previousReading;
	private String previousReadingDate;
	
//	@NotEmpty(message = "Current Reading cannot be empty/blank")
//	@Pattern(regexp = "\\d+", message = "Current Reading not a numeric")
	private String currentReading;
	
//	@NotEmpty(message = "Current Reading Date cannot be empty/blank")
	private String currentReadingDate;
	
	private String createdDate;
}
