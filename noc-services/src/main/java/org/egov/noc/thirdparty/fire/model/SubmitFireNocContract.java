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

	public String token;
	public String name;
	public String email;
	public String mobile;
	public String isOwner;
	public String identyProofType;
	public String identyProofNo;
	public String noofBuilding;
	public String buidlingType;
	public String buildingName;
	public String proposedOccupany;
	public String noOfFloor;
	public String height;
	public String measureType;
	public String category;
	public String builtupArea;
	public String areameasureType;
	public String fireDistrict;
	public String fireStation;
	@JsonProperty("AdreesOfBuilding")
	public String adreesOfBuilding;
	public String plainApplication;
	public String plainApplicationext;
	public String buildingPlan;
	public String buildingPlanext;
	public String ownershipDoc;
	public String ownershipDocext;
	public String identyProofDoc;
	public String identyProofDocext;
	public String applicantSignature;
	public String applicantSignatureext;
	public String applicantPhoto;
	public String applicantPhotoext;
}
