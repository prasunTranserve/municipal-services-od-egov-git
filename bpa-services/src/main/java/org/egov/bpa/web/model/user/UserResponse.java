package  org.egov.bpa.web.model.user;

import java.util.List;
import java.util.Objects;

import javax.validation.Valid;

import org.egov.bpa.web.model.landInfo.OwnerInfo;
import org.egov.common.contract.response.ResponseInfo;
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
 * Contains the ResponseHeader and the created/updated property
 */
@ApiModel(description = "Contains the ResponseHeader and the created/updated property")
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.v3.generators.java.SpringCodegen", date = "2020-06-23T05:52:32.717Z[GMT]")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class UserResponse   {
  @JsonProperty("ResponseInfo")
  private ResponseInfo responseInfo;

  @JsonProperty("users")
  private List<OwnerInfo> users;

  public UserResponse responseInfo(ResponseInfo responseInfo) {
    this.responseInfo = responseInfo;
    return this;
  }

  /**
   * Get responseInfo
   * @return responseInfo
  **/
  @ApiModelProperty(value = "")
  
    @Valid
    public ResponseInfo getResponseInfo() {
    return responseInfo;
  }

  public void setResponseInfo(ResponseInfo responseInfo) {
    this.responseInfo = responseInfo;
  }

  public UserResponse User(List<OwnerInfo> users) {
    this.users = users;
    return this;
  }

  /**
   * Get BPA
   * @return BPA
  **/
  @ApiModelProperty(value = "")
  
//    @Valid
//    public List<BPA> getBPA() {
//    return BPAR;
//  }

//  public void setBPA(List<BPA> BPA) {
//    this.BPAR = BPA;
//  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    UserResponse userResponse = (UserResponse) o;
    return Objects.equals(this.responseInfo, userResponse.responseInfo) &&
        Objects.equals(this.users, userResponse.users);
  }

  @Override
  public int hashCode() {
    return Objects.hash(responseInfo, users);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class UserResponse {\n");
    
    sb.append("    responseInfo: ").append(toIndentedString(responseInfo)).append("\n");
    sb.append("    users: ").append(toIndentedString(users)).append("\n");
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
