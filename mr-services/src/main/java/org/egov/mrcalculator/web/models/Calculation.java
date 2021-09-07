package org.egov.mrcalculator.web.models;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import org.egov.mr.web.models.MarriageRegistration;
import org.springframework.validation.annotation.Validated;
import javax.validation.Valid;
import javax.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.Builder;

/**
 * Calculation
 */
@Validated


@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Calculation {

	@JsonProperty("applicationNumber")
	private String applicationNumber = null;

	@JsonProperty("marriageRegistration")
	private MarriageRegistration marriageRegistration = null;

	@NotNull
	@JsonProperty("tenantId")
	@Size(min = 2, max = 256)
	private String tenantId = null;

	@JsonProperty("taxHeadEstimates")
	List<org.egov.mr.web.models.calculation.TaxHeadEstimate> taxHeadEstimates;

	@JsonProperty("billingIds")
	FeeAndBillingSlabIds billingIds;


}
