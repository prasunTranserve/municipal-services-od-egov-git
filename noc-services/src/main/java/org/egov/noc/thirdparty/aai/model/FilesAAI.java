package org.egov.noc.thirdparty.aai.model;

import java.util.List;

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
public class FilesAAI {

	@JsonProperty("UNDERTAKING1A")
	private String undertaking1a;
	@JsonProperty("SITEELEVATION")
	private String siteelevation;
	@JsonProperty("SITECORDINATES")
	private String sitecordinates;
	@JsonProperty("AUTHORIZATION")
	private String authorization;
	@JsonProperty("PERMISSION")
	private String permission;
}
