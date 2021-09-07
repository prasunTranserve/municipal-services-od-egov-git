package org.egov.mrcalculator.web.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class BillingSlabIds {

    @JsonProperty("consumerCode")
    private String consumerCode;

    @JsonProperty("billingSlabIds")
    private List<String> billingSlabIds;


}
