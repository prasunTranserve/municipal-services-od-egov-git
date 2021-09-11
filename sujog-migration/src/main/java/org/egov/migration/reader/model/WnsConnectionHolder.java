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
	
//	@Pattern(regexp = MigrationConst.OWNER_NAME_PATTERN, message = "OwnerName not a valid name")
//	@Size(max = 50, message = "Owner name can not be greater than 50 character")
//	@NotEmpty(message = "Owner name can not be blank/empty")
	private String holderName;
	
	private String mobile;
	private String gender;
	private String guardian;
	private String guardianRelation;
	private String consumerCategory;
	private String connectionHolderType;
	private String holderAddress;
	
}
