package org.egov.pt.migration.business.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Representation of a address. Individual APIs may choose to extend from this
 * using allOf if more details needed to be added in their case.
 */

@ToString
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddressDTO {

	@JsonProperty("tenantId")
	private String tenantId;

	@JsonProperty("doorNo")
	private String doorNo;

	@JsonProperty("plotNo")
	private String plotNo;

	@JsonProperty("landmark")
	private String landmark;

	@JsonProperty("city")
	private String city;

	@JsonProperty("district")
	private String district;

	@JsonProperty("region")
	private String region;

	@JsonProperty("state")
	private String state;

	@JsonProperty("country")
	private String country;

	@JsonProperty("pincode")
	private String pincode;

	@JsonProperty("buildingName")
	private String buildingName;

	@JsonProperty("street")
	private String street;

	@JsonProperty("locality")
	private LocalityDTO locality;

	@JsonProperty("geoLocation")
	private GeoLocationDTO geoLocation;
	
	@JsonProperty("additionalDetails")
	private Object additionalDetails;
	
	@JsonProperty("ward")
	private String ward;
}
