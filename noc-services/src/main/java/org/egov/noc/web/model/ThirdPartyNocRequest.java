package org.egov.noc.web.model;

import javax.validation.Valid;

import org.egov.common.contract.request.RequestInfo;
import org.springframework.validation.annotation.Validated;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * A object to bind the metadata contract and main application contract
 */
@ApiModel(description = "A object to bind the metadata contract and main application contract")
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.v3.generators.java.SpringCodegen", date = "2020-07-30T05:26:25.138Z[GMT]")
public class ThirdPartyNocRequest   {
	  @JsonProperty("RequestInfo")
	  private RequestInfo requestInfo = null;

	  @JsonProperty("NMA")
	  private NmaRequest nmaRequest = null;

	  public ThirdPartyNocRequest requestInfo(RequestInfo requestInfo) {
	    this.requestInfo = requestInfo;
	    return this;
	  }

	  /**
	   * Get requestInfo
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

	  public ThirdPartyNocRequest nmNocRequest(NmaRequest nmaRequest) {
	    this.nmaRequest = nmaRequest;
	    return this;
	  }

	  /**
	   * Get nmaRequest
	   * @return nmaRequest
	  **/
	  @ApiModelProperty(value = "")
	  
	    @Valid
	    public NmaRequest getNmaRequest() {
	    return nmaRequest;
	  }

	  public void setNmaRequest(NmaRequest nmaRequest) {
	    this.nmaRequest = nmaRequest;
	  }

	  @Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ThirdPartyNocRequest other = (ThirdPartyNocRequest) obj;
		if (nmaRequest == null) {
			if (other.nmaRequest != null)
				return false;
		} else if (!nmaRequest.equals(other.nmaRequest))
			return false;
		if (requestInfo == null) {
			if (other.requestInfo != null)
				return false;
		} else if (!requestInfo.equals(other.requestInfo))
			return false;
		return true;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((nmaRequest == null) ? 0 : nmaRequest.hashCode());
		result = prime * result + ((requestInfo == null) ? 0 : requestInfo.hashCode());
		return result;
	}


	  @Override
	public String toString() {
		return "ThirdPartyNocRequest [requestInfo=" + requestInfo + ", nmaRequest=" + nmaRequest + "]";
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
