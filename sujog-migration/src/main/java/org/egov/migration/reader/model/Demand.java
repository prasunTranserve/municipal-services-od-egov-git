package org.egov.migration.reader.model;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Pattern;

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
public class Demand {

	private String ulb;
	
//	@NotEmpty(message = "Cannot be blank/empty")
	private String propertyId;
	
	private String demandId;
	
	private String payerName;
	
//	@NotEmpty(message = "Tax period cannot be blank/empty")
//	@Pattern(regexp = "\\d{4}-\\d{2}/Q\\d{1}", message = "Tax period should be in format YYYY-YY/QN")
	private String taxPeriodFrom;
	
//	@NotEmpty(message = "Tax period cannot be blank/empty")
//	@Pattern(regexp = "\\d{4}-\\d{2}/Q\\d{1}", message = "Tax period should be in format YYYY-YY/QN")
	private String taxPeriodTo;
	
	private String createdDate;
	
//	@NotEmpty(message = "minPayableAmt cannot be empty")
	private String minPayableAmt;
	
	private String status;
	
	private String additionalDetails;
	
//	@NotEmpty(message = "paymentComplete cannot be empty")
	private String paymentComplete;
}
