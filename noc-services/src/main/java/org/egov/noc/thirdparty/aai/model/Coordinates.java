package org.egov.noc.thirdparty.aai.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Coordinates {

	@JsonProperty("LATITUDE")
	private String latitude;
	@JsonProperty("LONGITUDE")
	private String longitude;
	@JsonProperty("SITEELEVATION")
	private Double siteelevation;
	@JsonProperty("BUILDINGHEIGHT")
	private Double buildingheight;
	@JsonProperty("STRUCTURENO")
	private Integer structureno;
}
