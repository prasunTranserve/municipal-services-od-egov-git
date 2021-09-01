package org.egov.noc.thirdparty.nma.model;

import java.util.List;

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
public class OwnerDetails {
	@JsonProperty("ApplicantIsOtherThanOwner")
	public String applicantIsOtherThanOwner;
	@JsonProperty("NameOfTheOwner")
	public String nameOfTheOwner;
	@JsonProperty("PresentAddress")
	public String presentAddress;
	@JsonProperty("PermanentAddress")
	public String permanentAddress;
	@JsonProperty("WhetherThePropertyIsOwnedBy")
	public String whetherThePropertyIsOwnedBy;
	@JsonProperty("WhetherThePropertyIsOwnedBySecond")
	public String whetherThePropertyIsOwnedBySecond;
	@JsonProperty("Other")
	public String other;
	@JsonProperty("PropertyDocument")
	public List<Document> propertyDocument;
}
