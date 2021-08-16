package org.egov.migration.business.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@Data
@NoArgsConstructor
public class AssessmentResponse {
	
	@JsonProperty("Assessments")
    List<AssessmentDTO> assessments;

}
