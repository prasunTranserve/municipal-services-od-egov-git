package org.egov.mr.web.models;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import org.springframework.validation.annotation.Validated;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Validated
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class GuardianDetails {
	
    @Size(max=64)
    @JsonProperty("id")
    private String id;

    @Size(max=64)
    @JsonProperty("tenantId")
    private String tenantId = null;

    @Size(max=256)
    @JsonProperty("addressLine1")
    private String addressLine1 = null;


    @JsonProperty("addressLine2")
    private String addressLine2 = null;

    @Size(max=256)
    @JsonProperty("addressLine3")
    private String addressLine3 = null;

    @Size(max=64)
    @JsonProperty("country")
    private String country = null;

    @Size(max=64)
    @JsonProperty("state")
    private String state = null;
    
    @Size(max=64)
    @JsonProperty("district")
    private String district = null;

    @Size(max=64)
    @JsonProperty("pinCode")
    private String pinCode = null;

    

    @Size(max=64)
    @JsonProperty("locality")
    private String locality = null;

    @JsonProperty("auditDetails")
    private AuditDetails auditDetails = null;

	

    @JsonProperty("groomSideGuardian")
	private boolean groomSideGuardian ;
	
	@Size(max=64)
    @JsonProperty("relationship")
	private String relationship ;
	
	@Size(max=64)
    @JsonProperty("name")
	private String name ;
	
	
    @Pattern(regexp = "^[6-9][0-9]{9}$", message = "Invalid mobile number")
    @JsonProperty("contact")
    private String contact;

    @Size(max=128)
    @JsonProperty("emailAddress")
    @Pattern(regexp = "^$|^(?=^.{1,64}$)((([^<>()\\[\\]\\\\.,;:\\s$*@'\"]+(\\.[^<>()\\[\\]\\\\.,;:\\s@'\"]+)*)|(\".+\"))@((\\[[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\])|(([a-zA-Z\\-0-9]+\\.)+[a-zA-Z]{2,})))$", message = "Invalid emailId")
    private String emailAddress;

}
