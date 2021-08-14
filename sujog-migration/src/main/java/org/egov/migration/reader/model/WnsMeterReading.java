package org.egov.migration.reader.model;

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
	private String connectionFacility;
	private String billingPeriod;
	private String meterSerialNo;
	private String previousReading;
	private String previousReadingDate;
	private String currentReading;
	private String currentReadingDate;
}
