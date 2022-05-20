package org.egov.tl.web.models.excel;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LegacyRow {
	
	@CellAnnotation(index = 0)
	private String applicationDate;
	@CellAnnotation(index = 1)
	private String applicationStatus;
	@CellAnnotation(index = 2)
	private String applicationNo;
	@CellAnnotation(index = 3)
	private String tenantId;
	@CellAnnotation(index = 4)
	private String tradeType;
	@CellAnnotation(index = 5)
	private String tradeSubType;
	@CellAnnotation(index = 6)
	private String tradeName;
	@CellAnnotation(index = 7)
	private String tradeUnitMeasurementName;
	@CellAnnotation(index = 8)
	private String tradeUnitOfMeasurementValue;
	@CellAnnotation(index = 9)
	private String licenseType;
	@CellAnnotation(index = 10)
	private String licenseNumber;
	@CellAnnotation(index = 11)
	private String commencementDate;
	@CellAnnotation(index = 12)
	private String issuedDate;
	@CellAnnotation(index = 13)
	private String finacialYear;
	@CellAnnotation(index = 14)
	private String validFromDate;
	@CellAnnotation(index = 15)
	private String validToDate;
	@CellAnnotation(index = 16)
	private String traderAddress;
	@CellAnnotation(index = 17)
	private String tradeWard;
	@CellAnnotation(index = 18)
	private String tradeVillage;
	@CellAnnotation(index = 19)
	private String tradeCity;
	@CellAnnotation(index = 20)
	private String pincode;
	@CellAnnotation(index = 21)
	private String tradecategory;
	@CellAnnotation(index = 22)
	private String tradePrimaryOwnerName;
	@CellAnnotation(index = 23)
	private String tradeSecondaryOwnerName;
	@CellAnnotation(index = 24)
	private String authorizedPersonName;
	@CellAnnotation(index = 25)
	private String tradeInstitutionOfficialCorrespondanceAddress;
	@CellAnnotation(index = 26)
	private String ownerMobileNumber;
	@CellAnnotation(index = 27)
	private String tradeInstitutionPhoneNumber;

	

}
