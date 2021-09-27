package org.egov.migration.reader.model;

import javax.validation.constraints.NotEmpty;

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
public class WnsDemand {
	
	private String ulb;
	
//	@NotEmpty(message = "Connection Number cannot be empty/blank")
	private String connectionNo;
	
//	@NotEmpty(message = "Connection Facility cannot be empty/blank")
	private String connectionFacility;
	private String payerName;
	
//	@NotEmpty(message = "Billing period from cannot be empty/blank")
	private String billingPeriodFrom;
	
//	@NotEmpty(message = "Billing period to cannot be empty/blank")
	private String billingPeriodTo;
	private String minAmountPayable;
	
//	@NotEmpty(message = "Status cannot be empty/blank")
	private String status;
	
//	@NotEmpty(message = "Is Payment completed cannot be empty/blank. It shpuld be either Y or N")
	private String isPaymentCompleted;
	
//	@NotEmpty(message = "Collected amount cannot be empty/blank")
	private String collectedAmount;
	
//	@NotEmpty(message = "Water charges cannot be empty/blank")
	private String waterCharges;
	
//	@NotEmpty(message = "Sewerage fee cannot be empty/blank")
	private String sewerageFee;

	private String arrear;

}
