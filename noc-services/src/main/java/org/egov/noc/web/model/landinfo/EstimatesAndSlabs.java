package org.egov.noc.web.model.landinfo;

import java.util.List;

import org.egov.noc.web.model.demand.TaxHeadEstimate;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class EstimatesAndSlabs {

	 @JsonProperty("estimates")
	    private List<TaxHeadEstimate> estimates;
}
