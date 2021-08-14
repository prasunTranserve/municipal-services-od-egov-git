package org.egov.migration.reader.model;

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
	private String connectionNo;
	private String connectionFacility;
	private String payerName;
	private String billingPeriodFrom;
	private String billingPeriodTo;
	private String minAmountPayable;
	private String status;
	private String isPaymentCompleted;
	private String collectedAmount;
	private String waterCharges;
	private String sewerageFee;

}
