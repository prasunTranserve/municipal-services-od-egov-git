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
public class WnsConnectionHolder {

	private String ulb;
	private String connectionNo;
	private String connectionFacility;
	private String connectionStatus;
	private String salutation;
	private String holderName;
	private String mobile;
	private String gender;
	private String guardian;
	private String guardianRelation;
	private String consumerCategory;
	private String connectionHolderType;
	private String holderAddress;
	
}
