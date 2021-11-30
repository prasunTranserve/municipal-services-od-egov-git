package org.egov.wscalculation.web.models;

import java.util.List;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DemandWard {
	
	@JsonProperty(value = "tenant")
	private String tenant;
	
	@JsonProperty(value = "wards")
	private List<String> wards;

}
