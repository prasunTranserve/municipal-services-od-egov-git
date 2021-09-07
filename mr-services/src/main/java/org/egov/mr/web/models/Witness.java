package org.egov.mr.web.models;

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
public class Witness {

	
	@Size(max=64)
    @JsonProperty("id")
    private String id;


    @Size(max=64)
    @JsonProperty("tenantId")
    private String tenantId = null;
    
    @Size(max=64)
    @JsonProperty("title")
    private String title;
    
    @Size(max=256)
    @JsonProperty("address")
    private String address;
	
    @Size(max=64)
    @JsonProperty("firstName")
    private String firstName;
    
    @Size(max=64)
    @JsonProperty("middleName")
    private String middleName;
    
    @Size(max=64)
    @JsonProperty("lastName")
    private String lastName;
    
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
    
    @Pattern(regexp = "^[6-9][0-9]{9}$", message = "Invalid mobile number")
    @JsonProperty("contact")
    private String contact;
    
    @JsonProperty("auditDetails")
    private AuditDetails auditDetails = null;
}


