package org.egov.noc.thirdparty.aai.model;

import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApplicationData {

	@Size(max = 50)
	@JsonProperty("AUTHORITY")
	private final String authority = "SUJOG";

	@Size(max = 10)
	@JsonProperty("UNIQUEID")
	private String uniqueid;

	@Size(max = 10)
	@JsonProperty("APPLICATIONDATE")
	private String applicationdate;

	@Size(max = 50)
	@JsonProperty("APPLICANTNAME")
	private String applicantname;

	@Size(max = 200)
	@JsonProperty("APPLICANTADDRESS")
	private String applicantaddress;

	@Size(max = 10)
	@JsonProperty("APPLICANTNO")
	private String applicantno;

	@Size(max = 100)
	@JsonProperty("APPLICANTEMAIL")
	private String applicantemail;

	@Size(max = 20)
	@JsonProperty("APPLICATIONNO")
	private String applicationno;

	@Size(max = 100)
	@JsonProperty("OWNERNAME")
	private String ownername;

	@Size(max = 200)
	@JsonProperty("OWNERADDRESS")
	private String owneraddress;

	@Size(max = 50)
	@JsonProperty("STRUCTURETYPE")
	private String structuretype;

	// TODO: validate 4 types provided
	@Size(max = 50)
	@JsonProperty("STRUCTUREPURPOSE")
	private String structurepurpose;

	@Size(max = 250)
	@JsonProperty("SITEADDRESS")
	private String siteaddress;

	@Size(max = 100)
	@JsonProperty("SITECITY")
	private String sitecity;

	@Size(max = 100)
	@JsonProperty("SITESTATE")
	private String sitestate;

	// TODO:validate length 9 (max to 2 decimal places)
	@JsonProperty("PLOTSIZE")
	private Double plotsize;

	@Size(max = 3)
	@JsonProperty("ISINAIRPORTPREMISES")
	private String isinairportpremises;

	@Size(max = 3)
	@JsonProperty("PERMISSIONTAKEN")
	private String permissiontaken;
}
