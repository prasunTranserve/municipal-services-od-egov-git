package org.egov.wscalculation.web.models;

import java.util.List;
import java.util.Objects;

import javax.validation.Valid;

import org.egov.common.contract.request.RequestInfo;
import org.springframework.validation.annotation.Validated;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Contract class to receive request. Array of Installment items are used in case
 * of create and update
 */
@ApiModel(description = "Contract class to receive request. Array of Installment items  are used in case of create and update")
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.v3.generators.java.SpringCodegen", date = "2022-10-24T10:29:25.253+05:30[Asia/Kolkata]")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Setter
@Builder
public class InstallmentsRequest {

	@JsonProperty("RequestInfo")
	private RequestInfo requestInfo = null;
	
	@JsonProperty("Installments")
	private List<Installments> installments = null;

	public InstallmentsRequest requestInfo(RequestInfo requestInfo) {
		this.requestInfo = requestInfo;
		return this;
	}
	
	/**
	 * Get requestInfo
	 * 
	 * @return requestInfo
	 **/
	@ApiModelProperty(value = "")

	@Valid
	public RequestInfo getRequestInfo() {
		return requestInfo;
	}
	
	public void setRequestInfo(RequestInfo requestInfo) {
		this.requestInfo = requestInfo;
	}
	
	public InstallmentsRequest installments(List<Installments> installments) {
		this.installments = installments;
		return this;
	}
	
	/**
	 * Get installments
	 * 
	 * @return installments
	 **/
	@ApiModelProperty(value = "")

	@Valid
	public List<Installments> getInstallments() {
		return installments;
	}

	public void setInstallments(List<Installments> installments) {
		this.installments = installments;
	}
	
	@Override
	public boolean equals(java.lang.Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		InstallmentsRequest installmentsRequest = (InstallmentsRequest) o;
		return Objects.equals(this.requestInfo, installmentsRequest.requestInfo)
				&& Objects.equals(this.installments, installmentsRequest.installments);
	}

	@Override
	public int hashCode() {
		return Objects.hash(requestInfo, installments);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("class InstallmentsRequest {\n");

		sb.append("    requestInfo: ").append(toIndentedString(requestInfo)).append("\n");
		sb.append("    installments: ").append(toIndentedString(installments)).append("\n");
		sb.append("}");
		return sb.toString();
	}

	/**
	 * Convert the given object to string with each line indented by 4 spaces
	 * (except the first line).
	 */
	private String toIndentedString(java.lang.Object o) {
		if (o == null) {
			return "null";
		}
		return o.toString().replace("\n", "\n    ");
	}
}
