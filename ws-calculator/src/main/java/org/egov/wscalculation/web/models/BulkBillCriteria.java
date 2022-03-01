package org.egov.wscalculation.web.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.List;

@Validated

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class BulkBillCriteria {

	@JsonProperty("tenantIds")
	private List<String> tenantIds;

	@JsonProperty("offset")
	private Long offset;

	@JsonProperty("limit")
	private Long limit;
	
	@JsonProperty(value = "skipTenantIds")
	private List<String> skipTenantIds;
	
	@JsonProperty(value = "specificMonth")
	private boolean specificMonth;
	
	@JsonProperty(value = "demandMonth")
	private int demandMonth;
	
	@JsonProperty(value = "demandYear")
	private int demandYear;
	
	@JsonProperty(value = "specialRebateMonths")
	private List<Integer> specialRebateMonths;
	
	@JsonProperty(value = "specialRebateYear")
	private int specialRebateYear;
	
	@JsonProperty(value = "connectionNos")
	private List<String> connectionNos = new ArrayList<>();
}
