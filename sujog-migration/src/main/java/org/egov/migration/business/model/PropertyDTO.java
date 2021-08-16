package org.egov.migration.business.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Property
 */

@ToString
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PropertyDTO {

	@JsonProperty("tenantId")
	private String tenantId;
	
	@JsonProperty("acknowldgementNumber")
	private String acknowldgementNumber;
	
	@JsonProperty("propertyId")
	private String propertyId;
	
	@JsonProperty("oldPropertyId")
	private String oldPropertyId;

	@JsonProperty("propertyType")
	private String propertyType;

	@JsonProperty("ownershipCategory")
	private String ownershipCategory;

	@JsonProperty("owners")
	private List<OwnerInfoDTO> owners;

	@JsonProperty("creationReason")
	private String creationReason;
	
	@JsonProperty("usageCategory")
	private String usageCategory;

	@JsonProperty("noOfFloors")
	private Long noOfFloors;

	@JsonProperty("landArea")
	private BigDecimal landArea;

	@JsonProperty("superBuiltUpArea")
	private BigDecimal superBuiltUpArea;

	@JsonProperty("source")
	private String source;

	@JsonProperty("channel")
	private String channel;
	
	@JsonProperty("address")
	private AddressDTO address;
	
	@JsonProperty("units")
	private List<UnitDTO> units;

	@JsonProperty("additionalDetails")
	private JsonNode additionalDetails;

	public PropertyDTO addOwnersItem(OwnerInfoDTO ownersItem) {
		if (this.owners == null) {
			this.owners = new ArrayList<>();
		}

		if (null != ownersItem)
			this.owners.add(ownersItem);
		return this;
	}

}
