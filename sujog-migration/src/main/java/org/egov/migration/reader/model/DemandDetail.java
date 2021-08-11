package org.egov.migration.reader.model;

import javax.validation.constraints.NotEmpty;

import org.egov.migration.validation.ValidTaxHead;

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
public class DemandDetail {
	
	@NotEmpty(message = "Cannot be blank/empty")
	private String propertyId;
	
	private String ulb;
	
	@ValidTaxHead
	private String taxHead;
	
	@NotEmpty(message = "Cannot be blank/empty")
	private String taxAmt;
	
	private String collectedAmt;
	
	private String createdDate;
	
	private String additionalDetails;
}
