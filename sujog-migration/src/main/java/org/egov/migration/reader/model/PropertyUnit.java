package org.egov.migration.reader.model;

import javax.validation.constraints.Digits;
import javax.validation.constraints.NotEmpty;

import com.fasterxml.jackson.annotation.JsonProperty;

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
public class PropertyUnit {
	
	private String id;

	private String ulb;

	private String floorNo;

	@JsonProperty("unitType")
	private String unitType;

	@JsonProperty("usageCategory")
//	@NotEmpty(message = "Unit usage Category should not be empty/blank")
	private String usageCategory;

	@JsonProperty("occupancyType")
	private String occupancyType;

	@JsonProperty("active")
	private String status;

	@JsonProperty("occupancyDate")
	private Long occupancyDate;
	
	@JsonProperty("carpetArea")
	private String carpetArea;

//	@Digits(integer = 8, fraction = 2)
	@JsonProperty("builtUpArea")
	private String builtUpArea;

//	@Digits(integer = 8, fraction = 2)
	@JsonProperty("plinthArea")
	private String plinthArea;

//	@Digits(integer = 8, fraction = 2)
	@JsonProperty("superBuiltUpArea")
	private String superBuiltUpArea;

//	@Digits(integer = 8, fraction = 2)
	@JsonProperty("arv")
	private String arv;

}
