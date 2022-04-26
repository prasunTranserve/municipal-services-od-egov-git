package org.egov.wscalculation.web.models;

import java.math.BigDecimal;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnnualPaymentDetails {
	
	@JsonProperty("tenantid")
	private String tenantid;
	
	@JsonProperty("consumerCode")
	private String consumerCode;
	
	@JsonProperty("waterCharge")
	private BigDecimal totalWaterCharge;
	
	@JsonProperty("sewerageCharge")
	private BigDecimal totalSewerageCharge;
	
	@JsonProperty("rebate")
	private BigDecimal totalRebate;
	
	@JsonProperty("netAnnualAdvancePayable")
	private BigDecimal netAnnualAdvancePayable;

}
