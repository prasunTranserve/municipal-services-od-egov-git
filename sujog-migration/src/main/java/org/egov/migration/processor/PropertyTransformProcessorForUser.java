package org.egov.migration.processor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
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
public class PropertyTransformProcessorForUser implements ItemProcessor<Property, PropertyDetailDTO> {

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

	private PropertyDetailDTO transformProperty(Property property) {
		try {
			PropertyDTO propertyDTO = new PropertyDTO();
			transformProperty(propertyDTO, property);
			transformOwner(propertyDTO, property);

			return PropertyDetailDTO.builder().property(propertyDTO).build();
		} catch (Exception e) {
			log.error(String.format("Some exception generated while reading property %s, Message: ", property.getPropertyId(), e.getMessage()));
			MigrationUtility.addError(property.getPropertyId(), e.getMessage());
			return null;
		}
		
	}

	private void enrichProperty(Property property) {
		MigrationUtility.correctOwner(property);
	}

	private void transformProperty(PropertyDTO propertyDTO, Property property) {
		String ulb = property.getUlb().trim().toLowerCase();
		this.tenant = properties.getTenants().get(ulb);
		this.localityCode = this.properties.getLocalitycode().get(ulb);

		propertyDTO.setTenantId(this.tenant);
		propertyDTO.setOldPropertyId(property.getPropertyId());
		propertyDTO.setOwnershipCategory(MigrationUtility.getOwnershioCategory(property.getOwnershipCategory()));
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
			ownerInfoDTO.setOwnerType(MigrationConst.DEFAULT_OWNER_TYPE);
			ownerInfoDTO.setRelationship(MigrationUtility.getRelationship(owner.getRelationship()));

			return ownerInfoDTO;
		}).collect(Collectors.toList()));
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

}
