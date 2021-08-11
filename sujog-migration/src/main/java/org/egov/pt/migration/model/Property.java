package org.egov.pt.migration.model;

import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

import org.egov.pt.migration.validation.ValidTenant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class Property {
	
	@NotEmpty(message = "Property id cannot be blank/empty")
	private String propertyId;
	
	@ValidTenant
	private String ulb;
	
	private String status;
	
	private String propertyType;
	
	@NotEmpty(message = "OwnershipCategory can not be blank/empty")
	private String ownershipCategory;
	
	@NotEmpty(message = "UsageCategory can not be blank/empty")
	private String usageCategory;
	
	private String floorNo;
	
	@NotEmpty
	@Pattern(regexp = "((\\d+)(((\\.)(\\d+)){0,1}))", message = "Land area is either not a number or not in valid format")
	private String landArea;
	
	private String landAreaUnit;
	
	@Pattern(regexp = "((\\d+)(((\\.)(\\d+)){0,1}))", message = "Super buildup area is either not a number or not in valid format")
	private String buildupArea;
	
	private String buildupAreaUnit;
	
	@NotEmpty(message = "CreatedDate cannot be blank/empty")
	private String createdDate;
	
	private String additionalDetails;
	
	@NotEmpty(message = "Owner detail is missing")
	private List<@Valid Owner> owners;
	
	@NotNull(message = "Address detail is missing")
	@Valid
	private Address address;
	
	private List<@Valid PropertyUnit> unit;
	
	@NotEmpty(message = "Assessment detail missing")
	private List<@Valid Assessment> assessments;
	
	@NotEmpty(message = "Demand is missing")
	private List<@Valid Demand> demands;
	
	@NotEmpty(message = "Demand breakup missing")
	private List<@Valid DemandDetail> demandDetails;
	
}
