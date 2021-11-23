package org.egov.tlcalculator.web.models;

import java.util.List;

import javax.validation.Valid;

import org.egov.common.contract.response.ResponseInfo;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;


@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class ModifyBillingSlabRes {
	@JsonProperty("ResponseInfo")
	@Valid
	private ResponseInfo responseInfo = null;

	@JsonProperty("deleteBillingSlabs")
	@Valid
	private List<BillingSlab> deleteBillingSlabs = null;
	
	@JsonProperty("createBillingSlabs")
	@Valid
	private List<BillingSlab> createBillingSlabs = null;

}
