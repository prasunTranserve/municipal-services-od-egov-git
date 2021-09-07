package org.egov.mrcalculator.web.models;

import org.egov.mrcalculator.web.models.demand.BillResponse;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class BillAndCalculations {

    @JsonProperty("billResponse")
    private BillResponse billResponse;

    @JsonProperty("billingSlabIds")
    private BillingSlabIds billingSlabIds;
}
