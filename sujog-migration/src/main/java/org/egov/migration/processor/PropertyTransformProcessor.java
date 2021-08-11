package org.egov.migration.processor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.egov.migration.business.model.AddressDTO;
import org.egov.migration.business.model.AssessmentDTO;
import org.egov.migration.business.model.ConstructionDetailDTO;
import org.egov.migration.business.model.DemandDTO;
import org.egov.migration.business.model.DemandDetailDTO;
import org.egov.migration.business.model.OwnerInfoDTO;
import org.egov.migration.business.model.PropertyDTO;
import org.egov.migration.business.model.PropertyDetailDTO;
import org.egov.migration.business.model.UnitDTO;
import org.egov.migration.config.SystemProperties;
import org.egov.migration.model.Address;
import org.egov.migration.model.Assessment;
import org.egov.migration.model.DemandDetail;
import org.egov.migration.model.Property;
import org.egov.migration.service.ValidationService;
import org.egov.migration.util.MigrationConst;
import org.egov.migration.util.MigrationUtility;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Autowired;

public class PropertyTransformProcessor implements ItemProcessor<Property, PropertyDetailDTO> {

	public static final String finYearRegex = "\\d{4}-\\d{2}";

	public static final String dateFormat = "dd-MM-yy";

	private String tenant;
	
	private String localityCode;

	@Autowired
	private SystemProperties properties;
	
	@Autowired
	private ValidationService validationService;

	@Override
	public PropertyDetailDTO process(Property property) throws Exception {
		if(validationService.isValidProperty(property)) {
			return transformProperty(property);
		}
		return null;
	}

	private PropertyDetailDTO transformProperty(Property property) {
		PropertyDTO propertyDTO = new PropertyDTO();
		transformProperty(propertyDTO, property);
		transformAddress(propertyDTO, property.getAddress());
		transformOwner(propertyDTO, property);
		transformUnit(propertyDTO, property);

		AssessmentDTO assessmentDTO = new AssessmentDTO();
		transformAssessment(assessmentDTO, property.getAssessments());

		List<DemandDTO> demandDTOs = transformDemand(property);

		return PropertyDetailDTO.builder().property(propertyDTO).assessment(assessmentDTO).demands(demandDTOs).build();
	}

	private void transformUnit(PropertyDTO propertyDTO, Property property) {
		propertyDTO.setUnits(property.getUnit().stream().map(unit -> {
			UnitDTO unitDTO = new UnitDTO();
			unitDTO.setFloorNo(MigrationUtility.getFloorNo(unit.getFloorNo()));
			unitDTO.setArv(MigrationUtility.getAnnualRentValue(unit.getArv()));
			unitDTO.setOccupancyType(MigrationUtility.getOccupancyType(unit.getOccupancyType(), unitDTO.getArv()));
			unitDTO.setTenantId(this.tenant);
			unitDTO.setUsageCategory(MigrationUtility.getUsageCategory(unit.getUsageCategory()));
			unitDTO.setConstructionDetail(ConstructionDetailDTO.builder().builtUpArea(MigrationUtility.convertAreaToYard(unit.getBuiltUpArea())).build());
			return unitDTO;
		}).collect(Collectors.toList()));
		
	}

	private void transformProperty(PropertyDTO propertyDTO, Property property) {
		String ulb = property.getUlb().trim().toLowerCase();
		this.tenant = properties.getTenants().get(ulb);
		this.localityCode = this.properties.getLocalitycode().get(ulb);

		propertyDTO.setTenantId(this.tenant);
		propertyDTO.setOldPropertyId(property.getPropertyId());
		propertyDTO.setUsageCategory(MigrationUtility.getUsageCategory(property.getUsageCategory()));
		propertyDTO.setPropertyType(MigrationUtility.getProperty(property.getPropertyType()));
		propertyDTO.setOwnershipCategory(MigrationUtility.getOwnershioCategory(property.getOwnershipCategory()));
		propertyDTO.setCreationReason(MigrationConst.MIGRATE);
		propertyDTO.setNoOfFloors(MigrationUtility.getFloorNo(property.getFloorNo()));
		propertyDTO.setLandArea(MigrationUtility.convertAreaToYard(property.getLandArea()));
		propertyDTO.setSuperBuiltUpArea(MigrationUtility.convertAreaToYard(property.getBuildupArea()));
		propertyDTO.setSource(MigrationConst.SOURCE_MUNICIPAL_RECORDS);
		propertyDTO.setChannel(MigrationConst.CHANNEL_CFC_COUNTER);
		propertyDTO.setAdditionalDetails(null);
	}

	private void transformAddress(PropertyDTO propertyDTO, Address address) {
		AddressDTO addressDTO = new AddressDTO();
		addressDTO.setDoorNo(address.getDoorNo());
		addressDTO.setPlotNo(address.getPlotNo());
		addressDTO.setLandmark(address.getLandMark());
		addressDTO.setCity(address.getCity());
		addressDTO.setDistrict(address.getDistrict());
		addressDTO.setRegion(address.getRegion());
		addressDTO.setState(address.getState());
		addressDTO.setCountry(address.getCountry());
		addressDTO.setPincode(address.getPin());
		addressDTO.setBuildingName(address.getBuildingName());
		addressDTO.setLocality(MigrationUtility.getLocality(this.localityCode));
		addressDTO.setWard(address.getWard());
		addressDTO.setWard(null);

		propertyDTO.setAddress(addressDTO);
	}

	private void transformOwner(PropertyDTO propertyDTO, Property property) {
		propertyDTO.setOwners(property.getOwners().stream().map(owner -> {
			OwnerInfoDTO ownerInfoDTO = new OwnerInfoDTO();
			ownerInfoDTO.setSalutation(owner.getSalutation());
			ownerInfoDTO.setName(owner.getOwnerName().trim());
			ownerInfoDTO.setMobileNumber(MigrationUtility.processMobile(owner.getMobileNumber()));
			ownerInfoDTO.setEmailId(null);
			ownerInfoDTO.setAltContactNumber(null);
			ownerInfoDTO.setGender(MigrationUtility.getGender(owner.getGender()));
			ownerInfoDTO.setFatherOrHusbandName(owner.getGurdianName());
			ownerInfoDTO.setCorrespondenceAddress(MigrationUtility.getCorrespondanceAddress(property.getAddress()));
			ownerInfoDTO.setOwnerShipPercentage(
					owner.getOwnerPercentage() == null ? null : Double.parseDouble(owner.getOwnerPercentage()));
			ownerInfoDTO.setOwnerType(MigrationConst.DEFAULT_OWNER_TYPE);
			ownerInfoDTO
					.setRelationship(owner.getRelationship() == null ? null : owner.getRelationship().toUpperCase());

			return ownerInfoDTO;
		}).collect(Collectors.toList()));
	}

	private void transformAssessment(AssessmentDTO assessmentDTO, List<Assessment> assessments) {
		 int latestAssessedYear = assessments.stream().filter(asmt -> asmt.getFinYear().matches(finYearRegex))
			.mapToInt(asmt -> Integer.parseInt(asmt.getFinYear().split("-")[0]))
			.max().getAsInt();
		
		Assessment assessment = assessments.stream().filter(asmt -> asmt.getFinYear().matches(finYearRegex))
				.filter(asmt -> Integer.parseInt(asmt.getFinYear().substring(0, 4))==latestAssessedYear).findFirst().orElse(null);
				//.sorted(Comparator.comparing(Assessment::getFinYear).reversed()).findFirst().orElse(null);

		assessmentDTO.setTenantId(this.tenant);
		assessmentDTO.setFinancialYear(assessment.getFinYear());
		assessmentDTO.setPropertyId(assessment.getPropertyId());
		assessmentDTO.setAssessmentDate(MigrationUtility.getLongDate(assessment.getAssessmentDate(), dateFormat));
		assessmentDTO.setSource(MigrationConst.SOURCE_MUNICIPAL_RECORDS);
		assessmentDTO.setChannel(MigrationConst.CHANNEL_CFC_COUNTER);
	}

	private List<DemandDTO> transformDemand(Property property) {
		
		Map<String, Map<String, BigDecimal>> finYearConsolidateAmount = new HashMap<>();
		
		property.getDemands().stream().forEach(demand -> {
			demand.setTaxPeriodFrom(demand.getTaxPeriodFrom().split("/")[0]);
			if(finYearConsolidateAmount.get(demand.getTaxPeriodFrom()) == null) {
				finYearConsolidateAmount.put(demand.getTaxPeriodFrom(), new HashMap<>());
			}
			
			if(demand.getPaymentComplete().equalsIgnoreCase("N")) {
				BigDecimal dueAmount = finYearConsolidateAmount.get(demand.getTaxPeriodFrom()).get(MigrationConst.AMT_DUE);
				if (dueAmount == null) {
					dueAmount = new BigDecimal(demand.getMinPayableAmt());
				} else {
					dueAmount = dueAmount.add(new BigDecimal(demand.getMinPayableAmt()));
				}
				finYearConsolidateAmount.get(demand.getTaxPeriodFrom()).put(MigrationConst.AMT_DUE, dueAmount);
			} else if(demand.getPaymentComplete().equalsIgnoreCase("Y")) {
				BigDecimal collectedAmt = finYearConsolidateAmount.get(demand.getTaxPeriodFrom()).get(MigrationConst.AMT_COLLECTED);
				if (collectedAmt == null) {
					collectedAmt = new BigDecimal(demand.getMinPayableAmt());
				} else {
					collectedAmt = collectedAmt.add(new BigDecimal(demand.getMinPayableAmt()));
				}
				finYearConsolidateAmount.get(demand.getTaxPeriodFrom()).put(MigrationConst.AMT_COLLECTED, collectedAmt);
			}
		});
		
		
		return finYearConsolidateAmount.keySet().stream().map(finYear -> {
			BigDecimal collectedAmt = finYearConsolidateAmount.get(finYear).get(MigrationConst.AMT_COLLECTED)==null?BigDecimal.ZERO:finYearConsolidateAmount.get(finYear).get(MigrationConst.AMT_COLLECTED);
			DemandDTO demandDTO = new DemandDTO();
			demandDTO.setTaxPeriodFrom(MigrationUtility.getTaxPeriodFrom(finYear));
			demandDTO.setTaxPeriodTo(MigrationUtility.getTaxPeriodTo(finYear));
			demandDTO.setDemandDetails(transformDemandDetail(property.getDemandDetails(), collectedAmt));
			
			return demandDTO;
		}).collect(Collectors.toList());
		
	}

	private List<DemandDetailDTO> transformDemandDetail(List<DemandDetail> demandDetails, BigDecimal collectedAmt) {
		List<DemandDetailDTO> demandDetailDTOs = new ArrayList<>();
		for(DemandDetail demandDetail:demandDetails) {
			DemandDetailDTO demandDetailDTO = new DemandDetailDTO();
			demandDetailDTO.setTaxHeadMasterCode(
					this.properties.getTaxhead().get(demandDetail.getTaxHead().trim().toLowerCase().replace(" ", "_")));
			demandDetailDTO.setTaxAmount(MigrationUtility.getAmount(demandDetail.getTaxAmt()));
			BigDecimal amtCollectedForTaxHead = demandDetailDTO.getTaxAmount().compareTo(collectedAmt) >=0 ? collectedAmt : demandDetailDTO.getTaxAmount();
			demandDetailDTO.setCollectionAmount(amtCollectedForTaxHead);
			
			collectedAmt = collectedAmt.subtract(amtCollectedForTaxHead);
			demandDetailDTOs.add(demandDetailDTO);
		}
		
		return demandDetailDTOs;
	}

}
