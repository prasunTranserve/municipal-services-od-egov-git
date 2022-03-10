package org.egov.waterconnection.web.models;

import java.math.BigDecimal;

import javax.validation.Valid;

import org.springframework.validation.annotation.Validated;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Installments
 */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.v3.generators.java.SpringCodegen", date = "2022-02-11T12:10:14.583+05:30[Asia/Kolkata]")
public class Installments {
	
	@JsonProperty("id")
	private String id;

	@JsonProperty("applicationNo")
	private String applicationNo;
	
	@JsonProperty("tenantId")
	private String tenantId;
	
	@JsonProperty("consumerNo")
	private String consumerNo;
	
	@JsonProperty("feeType")
	private String feeType;
	
	@JsonProperty("installmentNo")
	private int installmentNo;
	
	@JsonProperty("amount")
	private BigDecimal amount;
	
	@JsonProperty("demandId")
	private String demandId;
	
	@JsonProperty("additionalDetails")
	private Object additionalDetails = null;
	
	@JsonProperty("auditDetails")
	private AuditDetails auditDetails = null;

	public Installments id(String id) {
		this.id = id;
		return this;
	}
	
	/**
	 * Get id
	 * 
	 * @return id
	 **/
	@ApiModelProperty(value = "")
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}
	
	public Installments applicationNo(String applicationNo) {
		this.applicationNo = applicationNo;
		return this;
	}
	
	/**
	 * Get applicationNo
	 * 
	 * @return applicationNo
	 **/
	@ApiModelProperty(value = "")
	public String getApplicationNo() {
		return applicationNo;
	}

	public void setApplicationNo(String applicationNo) {
		this.applicationNo = applicationNo;
	}
	
	public Installments tenantId(String tenantId) {
		this.tenantId = tenantId;
		return this;
	}

	/**
	 * Get tenantId
	 * 
	 * @return tenantId
	 **/
	@ApiModelProperty(value = "")
	public String getTenantId() {
		return tenantId;
	}

	public void setTenantId(String tenantId) {
		this.tenantId = tenantId;
	}

	public Installments consumerNo(String consumerNo) {
		this.consumerNo = consumerNo;
		return this;
	}
	
	/**
	 * Get consumerNo
	 * 
	 * @return consumerNo
	 **/
	@ApiModelProperty(value = "")
	public String getConsumerNo() {
		return consumerNo;
	}

	public void setConsumerNo(String consumerNo) {
		this.consumerNo = consumerNo;
	}

	public Installments feeType(String feeType) {
		this.feeType = feeType;
		return this;
	}
	
	/**
	 * Get feeType
	 * 
	 * @return feeType
	 **/
	@ApiModelProperty(value = "")
	public String getFeeType() {
		return feeType;
	}

	public void setFeeType(String feeType) {
		this.feeType = feeType;
	}
	
	public Installments installmentNo(int installmentNo) {
		this.installmentNo = installmentNo;
		return this;
	}

	/**
	 * Get installmentNo
	 * 
	 * @return installmentNo
	 **/
	@ApiModelProperty(value = "")
	public int getInstallmentNo() {
		return installmentNo;
	}

	public void setInstallmentNo(int installmentNo) {
		this.installmentNo = installmentNo;
	}
	
	public Installments amount(BigDecimal amount) {
		this.amount = amount;
		return this;
	}

	/**
	 * Get amount
	 * 
	 * @return amount
	 **/
	@ApiModelProperty(value = "")
	public BigDecimal getAmount() {
		return amount;
	}

	public void setAmount(BigDecimal amount) {
		this.amount = amount;
	}
	
	public Installments demandId(String demandId) {
		this.demandId = demandId;
		return this;
	}

	/**
	 * Get demandId
	 * 
	 * @return demandId
	 **/
	@ApiModelProperty(value = "")
	public String getDemandId() {
		return demandId;
	}

	public void setDemandId(String demandId) {
		this.demandId = demandId;
	}

	public Installments additionalDetails(Object additionalDetails) {
		this.additionalDetails = additionalDetails;
		return this;
	}
	
	/**
	 * Get additionalDetails
	 * 
	 * @return additionalDetails
	 **/
	@ApiModelProperty(value = "")
	public Object getAdditionalDetails() {
		return additionalDetails;
	}

	public void setAdditionalDetails(Object additionalDetails) {
		this.additionalDetails = additionalDetails;
	}
	
	public Installments auditDetails(AuditDetails auditDetails) {
		this.auditDetails = auditDetails;
		return this;
	}

	/**
	 * Get auditDetails
	 * 
	 * @return auditDetails
	 **/
	@ApiModelProperty(value = "")

	@Valid
	public AuditDetails getAuditDetails() {
		return auditDetails;
	}

	public void setAuditDetails(AuditDetails auditDetails) {
		this.auditDetails = auditDetails;
	}
	
}
