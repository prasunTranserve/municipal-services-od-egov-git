package org.egov.migration.reader.model;

import javax.validation.constraints.NotNull;

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
public class WnsConnectionService {
	
	private String ulb;
	private String connectionNo;
	private String connectionFacility;
	
	@NotNull(message = "Connection Category is missing")
	private String connectionCategory;
	
	@NotNull(message = "Connection type is missing")
	private String connectionType;
	private String waterSource;
	private String meterSerialNo;
	private String meterInstallationDate;
	private String actualPipeSize;
	private String noOfTaps;
	private String connectionExecutionDate;
	private String proposedPipeSize;
	private String propesedTaps;
	private String lastMeterReading;
	private String usageCategory;
	private String noOfFlats;
	private String noOfClosets;
	private String noOfToilets;
	private String proposedWaterClosets;
	private String proposedToilets;
}
