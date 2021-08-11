package org.egov.migration.business.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AssessmentDTO {

	@JsonProperty("tenantId")
	private String tenantId ;

	@JsonProperty("financialYear")
	private String financialYear ;

	@JsonProperty("propertyId")
	private String propertyId;

	@JsonProperty("assessmentDate")
	private Long assessmentDate ;

	@JsonProperty("source")
	private String source ;

	@JsonProperty("channel")
	private String channel ;

}

