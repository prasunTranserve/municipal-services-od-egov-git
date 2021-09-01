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
public class MaximumHeightOfExistingModernBuildingInCloseVicinityOf {

	@JsonProperty("NearTheMonument") 
    public double nearTheMonument;
    @JsonProperty("NearTheSiteConstructionRelatedActivity") 
    public double nearTheSiteConstructionRelatedActivity;
    @JsonProperty("WhetherMonumentIsLocatedWithinLimitOf") 
    public String whetherMonumentIsLocatedWithinLimitOf;
    @JsonProperty("DoesMasterPlanApprovedByConcernedAuthoritiesExistsForTheCityTownVillage") 
    public String doesMasterPlanApprovedByConcernedAuthoritiesExistsForTheCityTownVillage;
    @JsonProperty("StatusOfModernConstructions") 
    public String statusOfModernConstructions;
    @JsonProperty("OpenSpaceOrParkOrGreenAreaCloseToProtectedMonumentOrProtectedArea") 
    public String openSpaceOrParkOrGreenAreaCloseToProtectedMonumentOrProtectedArea;
    @JsonProperty("WhetherAnyRoadExistsBetweenTheMonumentAndTheSiteOfConstruction") 
    public String whetherAnyRoadExistsBetweenTheMonumentAndTheSiteOfConstruction;
    @JsonProperty("Remarks") 
    public String remarks;
    @JsonProperty("Signature") 
    public List<Document> signature;
    @JsonProperty("InCaseOfRepairsOrRenovationReportFromDulyAuthorisedOrLicencedArchitectSubmittedByApplicant") 
    public List<Document> inCaseOfRepairsOrRenovationReportFromDulyAuthorisedOrLicencedArchitectSubmittedByApplicant;
}
