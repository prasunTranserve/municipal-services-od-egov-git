package org.egov.mrcalculator.web.models;

import java.util.List;

import org.egov.mr.web.models.calculation.TaxHeadEstimate;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class EstimatesAndSlabs {

    @JsonProperty("estimates")
    private List<TaxHeadEstimate> estimates;

    @JsonProperty("feeAndBillingSlabIds")
    private FeeAndBillingSlabIds feeAndBillingSlabIds;




}
