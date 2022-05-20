package org.egov.wscalculation.web.models;

import java.math.BigDecimal;
import java.util.List;

import javax.validation.Valid;

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
public class BillDetail {

	@JsonProperty("id")
	private String id;

	@JsonProperty("tenantId")
	private String tenantId;

	@JsonProperty("demandId")
	private String demandId;

	@JsonProperty("billId")
	private String billId;

	@JsonProperty("expiryDate")
	private Long expiryDate;

	@JsonProperty("amount")
	private BigDecimal amount;

	@JsonProperty("fromPeriod")
	private Long fromPeriod;

	@JsonProperty("toPeriod")
	private Long toPeriod;

	@JsonProperty("billAccountDetails")
	private List<BillAccountDetail> billAccountDetails;

}
