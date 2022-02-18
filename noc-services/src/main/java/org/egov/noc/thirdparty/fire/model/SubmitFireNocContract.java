package org.egov.noc.thirdparty.fire.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubmitFireNocContract {

	private String token;
	private String name;
	private String email;
	private String mobile;
	private String isOwner;
	private String identyProofType;
	private String identyProofNo;
	private String noofBuilding;
	private String buidlingType;
	private String buildingName;
	private String proposedOccupany;
	private String noOfFloor;
	private String height;
	private String measureType;
	private String category;
	private String builtupArea;
	private String areameasureType;
	private String fireDistrict;
	private String fireStation;
	@JsonProperty("AdreesOfBuilding")
	private String adreesOfBuilding;
	private String plainApplication;
	private String plainApplicationext;
	private String buildingPlan;
	private String buildingPlanext;
	private String ownershipDoc;
	private String ownershipDocext;
	private String identyProofDoc;
	private String identyProofDocext;
	private String applicantSignature;
	private String applicantSignatureext;
	private String applicantPhoto;
	private String applicantPhotoext;
	private String suyogApplicationId;
}
