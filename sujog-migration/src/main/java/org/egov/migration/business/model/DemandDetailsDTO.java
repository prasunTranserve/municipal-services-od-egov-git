package org.egov.migration.business.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DemandDetailsDTO {
	
	private String taxHeadMasterCode;

	private Double paidAmount;
	
	//private DemandDTO demand;
	private String demandId;
	
	private String demandDetailsId;
	
	private String requestInfo;
	
	private String businessService;
	
	private String tenantId;
	
	@Builder.Default
	private Double collectionAmount = Double.valueOf(0d);
}
