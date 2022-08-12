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
public class UlbServiceResponse {

	@JsonProperty("SWSOBPAS")
	private ServiceObpasData swsObpas;
}
