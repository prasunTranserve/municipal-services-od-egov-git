package org.egov.migration.business.model;

import java.util.List;

import javax.validation.Valid;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@ToString
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SewerageConnectionDTO {

	@JsonProperty("proposedWaterClosets")
	private Integer proposedWaterClosets = null;

	@JsonProperty("proposedToilets")
	private Integer proposedToilets = null;

	@JsonProperty("noOfWaterClosets")
	private Integer noOfWaterClosets = null;

	@JsonProperty("noOfToilets")
	private Integer noOfToilets = null;

	@JsonProperty("noOfFlats")
	private Integer noOfFlats = null;

	@JsonProperty("pipeSize")
	private Integer pipeSize = null;

	@JsonProperty("usageCategory")
	private String usageCategory = null;

//	---------------------
	@JsonProperty("id")
	private String id = null;

	@JsonProperty("tenantId")
	private String tenantId = null;

	@JsonProperty("propertyId")
	private String propertyId = null;

	@JsonProperty("applicationNo")
	private String applicationNo = null;

	@JsonProperty("applicationStatus")
	private String applicationStatus = null;
	
	@JsonProperty("status")
	private String status = null;

	@JsonProperty("connectionNo")
	private String connectionNo = null;

	@JsonProperty("oldConnectionNo")
	private String oldConnectionNo = null;


	@JsonProperty("connectionExecutionDate")
	private Long connectionExecutionDate = null;

	@JsonProperty("connectionCategory")
	private String connectionCategory = null;

	@JsonProperty("connectionType")
	private String connectionType = null;

	@JsonProperty("additionalDetails")
	private Object additionalDetails = null;

	@JsonProperty("connectionHolders")
	@Valid
	private List<ConnectionHolderDTO> connectionHolders;
	
	@JsonProperty("applicationType")
	private String applicationType = null;

	@JsonProperty("dateEffectiveFrom")
	private Long dateEffectiveFrom = null;

	@JsonProperty("oldApplication")
	private Boolean oldApplication = false;
	
}
