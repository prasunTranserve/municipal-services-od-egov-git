package org.egov.pt.migration.business.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor

public class OwnerInfoDTO {


	@JsonProperty("salutation")
    private String salutation;
	
	@JsonProperty("name")
    private String name;
	
	@JsonProperty("mobileNumber")
    private String mobileNumber;
	
	@JsonProperty("emailId")
    private String emailId;
	
	@JsonProperty("altContactNumber")
    private String altContactNumber;
	
	@JsonProperty("gender")
	private String gender;

	@JsonProperty("fatherOrHusbandName")
	private String fatherOrHusbandName;

	@JsonProperty("correspondenceAddress")
	private String correspondenceAddress;

	@JsonProperty("isPrimaryOwner")
	private Boolean isPrimaryOwner;

	@JsonProperty("ownerShipPercentage")
	private Double ownerShipPercentage;

	@JsonProperty("ownerType")
	private String ownerType;

	@JsonProperty("relationship")
	private String relationship;

	@Builder()
	public OwnerInfoDTO(Long id, String uuid, String userName, String password, String salutation, String name,
					 String gender, String mobileNumber, String emailId, String altContactNumber, String pan,
					 String aadhaarNumber, String permanentAddress, String permanentCity, String permanentPincode,
					 String correspondenceCity, String correspondencePincode, String correspondenceAddress, Boolean active,
					 Long dob, Long pwdExpiryDate, String locale, String type, String signature, Boolean accountLocked,
					 String fatherOrHusbandName, String bloodGroup, String identificationMark, String photo,
					 String createdBy, Long createdDate, String lastModifiedBy, Long lastModifiedDate, String tenantId,
					 String ownerInfoUuid, String mobileNumber2, String gender2, String fatherOrHusbandName2,
					 String correspondenceAddress2, Boolean isPrimaryOwner, Double ownerShipPercentage, String ownerType,
					 String institutionId, String relationship) {
		mobileNumber = mobileNumber2;
		gender = gender2;
		fatherOrHusbandName = fatherOrHusbandName2;
		correspondenceAddress = correspondenceAddress2;
		this.isPrimaryOwner = isPrimaryOwner;
		this.ownerShipPercentage = ownerShipPercentage;
		this.ownerType = ownerType;
		this.relationship = relationship;
	}


	public OwnerInfoDTO(OwnerInfoDTO ownerInfo) {

		this.fatherOrHusbandName = ownerInfo.getFatherOrHusbandName();
		this.correspondenceAddress = ownerInfo.getCorrespondenceAddress();
		this.isPrimaryOwner = ownerInfo.getIsPrimaryOwner();
		this.ownerShipPercentage = ownerInfo.getOwnerShipPercentage();
		this.ownerType = ownerInfo.getOwnerType();
		this.relationship = ownerInfo.getRelationship();
	}

	public boolean mutationEquals(OwnerInfoDTO otherOwner) {


		if (this == otherOwner)
			return true;

		if (fatherOrHusbandName == null) {
			if (otherOwner.fatherOrHusbandName != null)
				return false;
		} else if (!fatherOrHusbandName.equals(otherOwner.fatherOrHusbandName))
			return false;

		if (gender == null) {
			if (otherOwner.gender != null)
				return false;
		} else if (!gender.equals(otherOwner.gender))
			return false;

		if (isPrimaryOwner == null) {
			if (otherOwner.isPrimaryOwner != null)
				return false;
		} else if (!isPrimaryOwner.equals(otherOwner.isPrimaryOwner))
			return false;

		if (ownerShipPercentage == null) {
			if (otherOwner.ownerShipPercentage != null)
				return false;
		} else if (!ownerShipPercentage.equals(otherOwner.ownerShipPercentage))
			return false;

		if (ownerType == null) {
			if (otherOwner.ownerType != null)
				return false;
		} else if (!ownerType.equals(otherOwner.ownerType))
			return false;

		if (relationship != otherOwner.relationship)
			return false;

		if (this.getName() == null) {
			if (otherOwner.getName() != null)
				return false;
		} else if (!this.getName().equals(otherOwner.getName()))
			return false;

		if (this.getMobileNumber() == null) {
			if (otherOwner.getMobileNumber() != null)
				return false;
		} else if (!this.getMobileNumber().equals(otherOwner.getMobileNumber()))
			return false;


		return true;
	}

}