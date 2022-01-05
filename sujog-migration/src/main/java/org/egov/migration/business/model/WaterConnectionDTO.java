package org.egov.migration.business.model;

import java.util.ArrayList;
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
public class WaterConnectionDTO {
	
	@JsonProperty("waterSource")
	private String waterSource = null;

	@JsonProperty("meterId")
	private String meterId = null;

	@JsonProperty("meterInstallationDate")
	private Long meterInstallationDate = null;

	@JsonProperty("proposedPipeSize")
	private Integer proposedPipeSize = null;

	@JsonProperty("proposedTaps")
	private Integer proposedTaps = null;

	@JsonProperty("pipeSize")
	private Integer pipeSize = null;

	@JsonProperty("noOfTaps")
	private Integer noOfTaps = null;

	@JsonProperty("noOfFlats")
	private Integer noOfFlats = null;

	@JsonProperty("usageCategory")
	private String usageCategory = null;
	
// ------------------
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

	@JsonProperty("applicationType")
	private String applicationType = null;

	@JsonProperty("dateEffectiveFrom")
	private Long dateEffectiveFrom = null;
	
	@JsonProperty("connectionHolders")
	@Valid
	private List<ConnectionHolderDTO> connectionHolders;

	@JsonProperty("oldApplication")
	private Boolean oldApplication = false;
	
	@JsonProperty("proposedWaterClosets")
	private Integer proposedWaterClosets = null;

	@JsonProperty("proposedToilets")
	private Integer proposedToilets = null;

	@JsonProperty("noOfWaterClosets")
	private Integer noOfWaterClosets = null;

	@JsonProperty("noOfToilets")
	private Integer noOfToilets = null;
	
	@JsonProperty("connectionFacility")
	private String connectionFacility = null;
	
	@JsonProperty("processInstance")
	private ProcessInstance processInstance = null;
	
	public void addConnectionHolders(ConnectionHolderDTO connectionHolder) {
		if(this.connectionHolders == null) {
			this.connectionHolders = new ArrayList<>();
		}
		
		this.connectionHolders.add(connectionHolder);
	}

}
