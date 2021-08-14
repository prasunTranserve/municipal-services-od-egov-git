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
public class WnsService {
	
	private String ulb;
	private String connectionNo;
	private String connectionFacility;
	private String connectionCategory;
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
