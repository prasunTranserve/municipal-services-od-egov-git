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
public class Assessment {

	private String ulb;
	
	private String propertyId;
	
	private String assessmentNo;
	
	//@NotEmpty(message = "Assessment financial year cannot be blank/empty")
	//@Pattern(regexp = "^\\d{4}-\\d{2}$", message = "Invalid assessment financial year ")
	private String finYear;
	
	private String status;
	
	//@NotEmpty(message = "Assessment date cannot be blank/empty")
	private String assessmentDate;
	
	private String createdDate;
	
	private String additionalDetails;
}
