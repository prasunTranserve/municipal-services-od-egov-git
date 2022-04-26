package org.egov.migration.reader.model;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class BulkBillCriteria {

	private List<String> tenantIds;
	
	private final Boolean specificMonth = Boolean.TRUE;
	
	private int demandMonth;
	
	private int demandYear;
	
	private List<Integer> specialRebateMonths;
	
	private int specialRebateYear;
	
	private List<String> connectionNos;
	
}
