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
public class ProposedWorkLocalityDtails {
	@JsonProperty("LocalityOfTheProposedConstruction")
	public LocalityOfTheProposedConstruction localityOfTheProposedConstruction;
	@JsonProperty("NameOfTheNearestMonumentOrSite")
	public NameOfTheNearestMonumentOrSite nameOfTheNearestMonumentOrSite;
	@JsonProperty("DistanceOfTheSiteOfTheConstructionFromProtectedBoundaryOfMonument")
	public DistanceOfTheSiteOfTheConstructionFromProtectedBoundaryOfMonument distanceOfTheSiteOfTheConstructionFromProtectedBoundaryOfMonument;

}
