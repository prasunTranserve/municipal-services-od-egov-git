package org.egov.wscalculation.web.models;

import java.util.ArrayList;
import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BillSchedulerCriteria {

	@JsonProperty(value = "tenants")
	private List<String> tenants;
	
	@JsonProperty(value = "skipTenants")
	private List<String> skipTenants;
	
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
	
	@JsonProperty(value = "specificWards")
	private List<DemandWard> wards; 	
	
	@JsonProperty(value = "connectionNos")
	private List<String> connectionNos = new ArrayList<>();
	
}
