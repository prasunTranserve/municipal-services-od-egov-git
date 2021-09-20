package org.egov.noc.web.model;

import java.util.List;

import org.egov.noc.thirdparty.nma.model.Document;
import org.springframework.validation.annotation.Validated;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Validated
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class NmaApplicationStatus {
	
	@JsonProperty("department")
	private String department;
	
	@JsonProperty("applicationUniqueNumebr")
	private String applicationUniqueNumebr;
	
	@JsonProperty("proximityStatus")
	private String proximityStatus;
	
	@JsonProperty("status")
	private String status;
	
	@JsonProperty("responseTime")
	private String responseTime;
	
	@JsonProperty("remarks")
	private String remarks;
	
	@JsonProperty("nocFileUrl")
	private String nocFileUrl;
	
	@JsonProperty("uniqueId")
	private String uniqueId;
	
	@JsonProperty("nocNumber")
	private String nocNumber;
	
	@JsonProperty("fileName")
	private String fileName;
	
	@JsonProperty("CARemarks")
	private String CARemarks;

}
