package org.egov.migration.processor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
import org.egov.migration.common.model.FinancialYear;
import org.egov.migration.config.SystemProperties;
import org.egov.migration.reader.model.Address;
import org.egov.migration.reader.model.Assessment;
import org.egov.migration.reader.model.DemandDetail;
import org.egov.migration.reader.model.Owner;
import org.egov.migration.reader.model.Property;
import org.egov.migration.service.PropertyService;
import org.egov.migration.service.ValidationService;
import org.egov.migration.util.MigrationConst;
import org.egov.migration.util.MigrationUtility;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Autowired;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PropertyAssessmentTransformProcessor  implements ItemProcessor<Property, PropertyDetailDTO> {

	public static final String finYearRegex = "\\d{4}-\\d{2}";

	public static final String dateFormat = "dd-MM-yy";

	private String tenant;
	
	private String localityCode;

	@Autowired
	private SystemProperties properties;
	
	@Autowired
	private ValidationService validationService;
	
	@Autowired
	private PropertyService propertyService;

	@Override
	public PropertyDetailDTO process(Property property) throws Exception {
//		if(validationService.isValidArea(property)) {
			try {
				enrichProperty(property);
				return transformProperty(property);
			} catch (Exception e) {
				MigrationUtility.addError(property.getPropertyId(), e.getMessage());
			}
//		}
		return null;
	}


	private void transformUnit(PropertyDTO propertyDTO, Property property) {
		if(property.getUnit() == null) {
			return;
		}
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
		addressDTO.setDoorNo(MigrationUtility.getDoorNo(address.getDoorNo()));
		addressDTO.setPlotNo(MigrationUtility.getDefaultOther(address.getPlotNo()));
		addressDTO.setLandmark(MigrationUtility.getDefaultOther(address.getLandMark()));
		addressDTO.setCity(MigrationUtility.getDefaultOther(address.getCity()));
		addressDTO.setDistrict(MigrationUtility.getDefaultOther(address.getDistrict()));
		addressDTO.setRegion(MigrationUtility.getDefaultOther(address.getRegion()));
		addressDTO.setState(MigrationUtility.getDefaultOther(address.getState()));
		addressDTO.setCountry(MigrationUtility.getDefaultOther(address.getCountry()));
		addressDTO.setPincode(MigrationUtility.getPIN(address.getPin()));
		addressDTO.setBuildingName(MigrationUtility.getDefaultOther(address.getBuildingName()));
		addressDTO.setLocality(MigrationUtility.getLocality(this.localityCode));
		addressDTO.setWard(MigrationUtility.getWard(address.getWard()));
		addressDTO.setStreet(MigrationUtility.getStreet(address));
		propertyDTO.setAddress(addressDTO);
	}

	private void transformOwner(PropertyDTO propertyDTO, Property property) {
		propertyDTO.setOwners(property.getOwners().stream().map(owner -> {
			OwnerInfoDTO ownerInfoDTO = new OwnerInfoDTO();
			ownerInfoDTO.setSalutation(MigrationUtility.getSalutation(owner.getSalutation()));
			ownerInfoDTO.setName(MigrationUtility.prepareName(property.getPropertyId(),  owner.getOwnerName()));
			ownerInfoDTO.setMobileNumber(MigrationUtility.processMobile(owner.getMobileNumber()));
			ownerInfoDTO.setEmailId(null);
			ownerInfoDTO.setAltContactNumber(null);
			ownerInfoDTO.setGender(prepareGender(owner));
			ownerInfoDTO.setFatherOrHusbandName(MigrationUtility.prepareName("Other",  owner.getGurdianName()));
			ownerInfoDTO.setCorrespondenceAddress(MigrationUtility.getCorrespondanceAddress(property.getAddress()));
//			ownerInfoDTO.setOwnerShipPercentage(
//					owner.getOwnerPercentage() == null ? null : Double.parseDouble(owner.getOwnerPercentage()));
			ownerInfoDTO.setOwnerType(MigrationConst.DEFAULT_OWNER_TYPE);
			ownerInfoDTO.setRelationship(MigrationUtility.getRelationship(owner.getRelationship()));

			return ownerInfoDTO;
		}).collect(Collectors.toList()));
	}


	private List<DemandDetailDTO> transformDemandDetail(List<DemandDetail> demandDetails, BigDecimal collectedAmt) {
		Map<String, DemandDetailDTO> taxHeadCodeMap = new HashMap<>();
		List<DemandDetailDTO> demandDetailDTOs = new ArrayList<>();
		DemandDetailDTO demandDetailDTO = null;
		String taxheadcode = null;
		BigDecimal amtCollectedForTaxHead = BigDecimal.ZERO;
		BigDecimal amtTaxAmountForTaxHead = BigDecimal.ZERO;
		for(DemandDetail demandDetail:demandDetails) {
			taxheadcode = demandDetail.getTaxHead().trim().toLowerCase().replace(" ", "_");
			if(Objects.isNull(taxHeadCodeMap.get(taxheadcode))) {
				demandDetailDTO = new DemandDetailDTO();
				demandDetailDTO.setTaxHeadMasterCode(
						this.properties.getTaxhead().get(taxheadcode));
				demandDetailDTO.setTaxAmount(MigrationUtility.getAmount(demandDetail.getTaxAmt()));
				amtCollectedForTaxHead = demandDetailDTO.getTaxAmount().compareTo(collectedAmt) >=0 ? collectedAmt : demandDetailDTO.getTaxAmount();
				demandDetailDTO.setCollectionAmount(BigDecimal.ZERO);
				
				collectedAmt = collectedAmt.subtract(amtCollectedForTaxHead);
				demandDetailDTOs.add(demandDetailDTO);
				taxHeadCodeMap.put(taxheadcode, demandDetailDTO);
			}else {
				amtTaxAmountForTaxHead = MigrationUtility.getAmount(demandDetail.getTaxAmt());
				demandDetailDTO = taxHeadCodeMap.get(taxheadcode);
				demandDetailDTO.setTaxAmount( demandDetailDTO.getTaxAmount().add(amtTaxAmountForTaxHead) );
				amtCollectedForTaxHead = amtTaxAmountForTaxHead.compareTo(collectedAmt) >=0 ? collectedAmt : amtTaxAmountForTaxHead;
				
				demandDetailDTO.setCollectionAmount(BigDecimal.ZERO);
				
				collectedAmt = collectedAmt.subtract(amtCollectedForTaxHead);
				
			}
			
		}
		
		return demandDetailDTOs;
	}
	
	private static String prepareGender(Owner holder) {
		String gender = "Male";
		if(holder.getGender()==null) {
			if(holder.getSalutation() != null && 
					(holder.getSalutation().equalsIgnoreCase("M/S")
							|| holder.getSalutation().equalsIgnoreCase("Miss")
							|| holder.getSalutation().equalsIgnoreCase("Mrs"))) {
				gender="Female";
			}
		} else {
			gender = MigrationUtility.getGender(holder.getGender());
		}
		return gender;
	}
	
	private PropertyDetailDTO transformProperty(Property property) {
		try {
			PropertyDTO propertyDTO = new PropertyDTO();
			transformProperty(propertyDTO, property);
			transformAddress(propertyDTO, property.getAddress());
			transformOwner(propertyDTO, property);
			transformUnit(propertyDTO, property);

			AssessmentDTO assessmentDTO = new AssessmentDTO();
			transformAssessment(assessmentDTO, property);

			List<DemandDTO> demandDTOs = transformDemand(property);

			return PropertyDetailDTO.builder().property(propertyDTO).assessment(assessmentDTO).demands(demandDTOs).build();
		} catch (Exception e) {
			log.error(String.format("Some exception generated while reading property %s, Message: ", property.getPropertyId(), e.getMessage()));
			MigrationUtility.addError(property.getPropertyId(), e.getMessage());
			return null;
		}
		
	}
	
	private List<DemandDTO> transformDemand(Property property) {
		Map<String, Map<String, BigDecimal>> finYearConsolidateAmount = new HashMap<>();
		
		property.getDemands().stream().forEach(demand -> {
			demand.setTaxPeriodFrom(demand.getTaxPeriodFrom().split("/")[0]);
			if(finYearConsolidateAmount.get(demand.getTaxPeriodFrom()) == null) {
				finYearConsolidateAmount.put(demand.getTaxPeriodFrom(), new HashMap<>());
			}
			
			if(demand.getPaymentComplete()==null || demand.getPaymentComplete().equalsIgnoreCase("N")) {
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
		
		long maxTaxPeriodFrom = finYearConsolidateAmount.keySet().stream()
				.mapToLong(finYear -> MigrationUtility.getTaxPeriodFrom(finYear)).max().getAsLong();
		BigDecimal totalDemandDtlAmt = BigDecimal.valueOf(property.getDemandDetails().stream().mapToDouble(dtl -> Double.parseDouble(dtl.getTaxAmt())).sum());
		
		FinancialYear currentFinancialYear = properties.getFinancialyear().stream()
				.filter(finyear -> MigrationConst.PROPERTY_NEW_FINYEAR.equals(finyear.getCode())
						&& MigrationConst.MODULE.PT.equals(finyear.getModule()))
				.findAny().orElse(null);
		
		return finYearConsolidateAmount.keySet().stream().filter(finYear -> MigrationUtility.getTaxPeriodFrom(finYear) == maxTaxPeriodFrom).map(finYear -> {
			BigDecimal collectedAmt = finYearConsolidateAmount.get(finYear).get(MigrationConst.AMT_COLLECTED)==null?BigDecimal.ZERO:finYearConsolidateAmount.get(finYear).get(MigrationConst.AMT_COLLECTED);
			BigDecimal dueAmt = finYearConsolidateAmount.get(finYear).get(MigrationConst.AMT_DUE)==null?BigDecimal.ZERO:finYearConsolidateAmount.get(finYear).get(MigrationConst.AMT_DUE);
			
			DemandDTO demandDTO = new DemandDTO();
			demandDTO.setTaxPeriodFrom(currentFinancialYear.getStartingDate());
			demandDTO.setTaxPeriodTo(currentFinancialYear.getEndingDate());
			
			if(collectedAmt.add(dueAmt).compareTo(totalDemandDtlAmt) != 0) {
				
				/**
				 * If total demand amount not equal to demand details then get 
				 * the nearest divisible by 4 amount for the demand details
				 */
				List<DemandDetail> demandDetailsCopy = new ArrayList<>();
				demandDetailsCopy.addAll(property.getDemandDetails());
				
				for(DemandDetail demandDetail : demandDetailsCopy) {
					demandDetail.setTaxAmt(MigrationUtility.getNearest(demandDetail.getTaxAmt(), "4"));
				}
				BigDecimal totalDemandDetailAmt = BigDecimal.valueOf(demandDetailsCopy.stream().mapToDouble(dtl -> Double.parseDouble(dtl.getTaxAmt())).sum());
				
				if(collectedAmt.add(dueAmt).compareTo(totalDemandDetailAmt) == 0) {
					demandDTO.setDemandDetails(transformDemandDetail(demandDetailsCopy, collectedAmt));
				}else {
					demandDTO.setDemandDetails(Arrays.asList(DemandDetailDTO.builder().taxHeadMasterCode(MigrationConst.TAXHEAD_HOLDING_TAX)
							.taxAmount(collectedAmt.add(dueAmt)).collectionAmount(BigDecimal.ZERO).build()));
				}
				
			} else {
				demandDTO.setDemandDetails(transformDemandDetail(property.getDemandDetails(), collectedAmt));
			}
			
			return demandDTO;
		}).collect(Collectors.toList());
		
	}
	
	private void transformAssessment(AssessmentDTO assessmentDTO, Property property) {

		assessmentDTO.setTenantId(this.tenant);
		assessmentDTO.setFinancialYear(MigrationConst.PROPERTY_NEW_FINYEAR);
		assessmentDTO.setPropertyId(property.getPropertyId());
		assessmentDTO.setAssessmentDate((new Date()).getTime());
		assessmentDTO.setSource(MigrationConst.SOURCE_MUNICIPAL_RECORDS);
		assessmentDTO.setChannel(MigrationConst.CHANNEL_CFC_COUNTER);
	}
	
	private void enrichProperty(Property property) {
		MigrationUtility.correctOwner(property);
		propertyService.enrichAssessmentV1(property);
		propertyService.enrichDemandDetailsV1(property);
		
	}
}
