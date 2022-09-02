package org.egov.bpa.web.model.accreditedperson;

import org.egov.common.contract.request.RequestInfo;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AccreditedPersonSearchCriteriaWrapper {

	@JsonProperty("RequestInfo")
	private RequestInfo requestInfo;

	@JsonProperty("accreditedPersonSearchCriteria")
	private AccreditedPersonSearchCriteria accreditedPersonSearchCriteria;
}