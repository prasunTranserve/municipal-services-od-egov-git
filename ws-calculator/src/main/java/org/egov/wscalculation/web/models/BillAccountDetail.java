package org.egov.wscalculation.web.models;

import java.math.BigDecimal;
import java.util.List;

import org.egov.wscalculation.web.models.Bill.BillStatus;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@ToString
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BillAccountDetail {

	@JsonProperty("id")
	private String id;

	@JsonProperty("tenantId")
	private String tenantId;

	@JsonProperty("billDetailId")
	private String billDetailId;

	@JsonProperty("demandDetailId")
	private String demandDetailId;

	@JsonProperty("order")
	private Integer order;

	@JsonProperty("amount")
	private BigDecimal amount;

	@JsonProperty("adjustedAmount")
	private BigDecimal adjustedAmount;

	@JsonProperty("taxHeadCode")
	private String taxHeadCode;

}
