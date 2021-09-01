package org.egov.noc.thirdparty.nma.model;

import java.sql.Timestamp;

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
public class NmaUser {

	@JsonProperty("ArchitectEmailId")
	private String architectEmailId;
	
	@JsonProperty("ArchitectName")
	private String architectName;
	
	@JsonProperty("Department")
	private String department;
	
	@JsonProperty("ArchitectMobileNo")
	private String architectMobileNo;
	
	@JsonProperty("TenantId")
	private String tenantid;
	
	@JsonProperty("UserId")
	private Long userid;
}
