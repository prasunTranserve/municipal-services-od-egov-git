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
public class Address {

	private String ulb;
	
	private String propertyId;
	
	private String doorNo;
	
	private String plotNo;
	
	private String buildingName;
	
	private String addressLine1;
	
	private String addressLine2;
	
	private String landMark;
	
	private String city;
	
	private String pin;
	
	private String locality;
	
	private String district;
	
	private String region;
	
	private String state;
	
	private String country;
	
	private String ward;
	
	private String createdDate;
	
	private String additionalDetails;
}
