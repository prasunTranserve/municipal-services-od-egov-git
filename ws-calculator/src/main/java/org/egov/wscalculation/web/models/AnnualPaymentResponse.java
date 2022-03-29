package org.egov.wscalculation.web.models;

import org.egov.common.contract.response.ResponseInfo;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnnualPaymentResponse {
	@JsonProperty("ResponseInfo")
	private ResponseInfo responseInfo;
	
	@JsonProperty("annualPaymentDetails")
	private AnnualPaymentDetails payment;
	
	@JsonProperty("annualAdvance")
	private AnnualAdvance annualAdvance;

}
