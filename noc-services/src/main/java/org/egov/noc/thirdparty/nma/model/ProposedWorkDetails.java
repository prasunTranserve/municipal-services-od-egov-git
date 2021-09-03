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
public class ProposedWorkDetails {
	
	 @JsonProperty("NatureOfWorkProposed") 
	    public String natureOfWorkProposed;
	    @JsonProperty("DetailsOfRepairAndRenovation") 
	    public String detailsOfRepairAndRenovation;
	    @JsonProperty("NumberOfStoreys") 
	    public String numberOfStoreys;
	    @JsonProperty("FloorAreaInSquareMetresStoreyWise") 
	    public String floorAreaInSquareMetresStoreyWise;
	    @JsonProperty("HeightInMetresExcludingMumtyParapetWaterStorageTankEtc") 
	    public String heightInMetresExcludingMumtyParapetWaterStorageTankEtc;
	    @JsonProperty("HeightInMetresIncludingMumtyParapetWaterStorageTankEtc") 
	    public String heightInMetresIncludingMumtyParapetWaterStorageTankEtc;
	    @JsonProperty("BasementIfAnyProposedWithDetails") 
	    public String basementIfAnyProposedWithDetails;
	    @JsonProperty("NatureOfWorkProposedOther") 
	    public String natureOfWorkProposedOther;
	    @JsonProperty("PurposeOfProposedWork") 
	    public String purposeOfProposedWork;
	    @JsonProperty("PurposeOfProposedWorkOther") 
	    public String purposeOfProposedWorkOther;
	    @JsonProperty("ApproximateDateOfCommencementOfWorks") 
	    public String approximateDateOfCommencementOfWorks;
	    @JsonProperty("ApproximateDurationOfCommencementOfWorks") 
	    public String approximateDurationOfCommencementOfWorks;
	    @JsonProperty("ElevationDocument") 
	    public List<Document> elevationDocument;
	    @JsonProperty("SectionDocument") 
	    public List<Document> sectionDocument;
	    @JsonProperty("TypicalFloorPlan") 
	    public List<Document> typicalFloorPlan;
	    @JsonProperty("OtherDocument") 
	    public List<Document> otherDocument;
	    @JsonProperty("MaximumHeightOfExistingModernBuildingInCloseVicinityOf") 
	    public MaximumHeightOfExistingModernBuildingInCloseVicinityOf maximumHeightOfExistingModernBuildingInCloseVicinityOf;
	

}
