package org.egov.noc.thirdparty.nma.model;

import org.springframework.validation.annotation.Validated;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Validated
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class NmaApplicationRequest {
	@JsonProperty("Department")
	public String department;
	@JsonProperty("ArchitectEmailId")
	public String architectEmailId;
	@JsonProperty("ApplicationUniqueNumebr")
	public String applicationUniqueNumebr;
	@JsonProperty("NOCRequestScreen")
	public String nOCRequestScreen;
	@JsonProperty("ApplicantDetails")
	public ApplicantDetails applicantDetails;
	@JsonProperty("OwnerDetails")
	public OwnerDetails ownerDetails;
	@JsonProperty("ProposedWorkLocalityDtails")
	public ProposedWorkLocalityDtails proposedWorkLocalityDtails;
	@JsonProperty("ProposedWorkDetails")
	public ProposedWorkDetails proposedWorkDetails;
	@JsonProperty("Documents")
	public Documents documents;
}
