package org.egov.mr.web.models;

import java.util.List;

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
public class Couple {
	
	@Size(max=64)
    @JsonProperty("id")
    private String id;

    @JsonProperty("isDivyang")
    private Boolean isDivyang;

    @Size(max=64)
    @JsonProperty("tenantId")
    private String tenantId = null;
    
    @JsonProperty("isGroom")
    private Boolean isGroom;
    
    @Size(max=64)
    @JsonProperty("title")
    private String title;
    
    @Size(max=64)
    @JsonProperty("firstName")
    private String firstName;
       
    @JsonProperty("dateOfBirth")
    private Long dateOfBirth;
    
    
    @Size(max=64)
    @JsonProperty("fatherName")
    private String fatherName;
    @Size(max=64)

    
    @Size(max=64)
    @JsonProperty("motherName")
    private String motherName;

    
    @JsonProperty("auditDetails")
    private AuditDetails auditDetails = null;
    
    @JsonProperty("coupleAddress")
    private CoupleAddress coupleAddress = null ;
    
    @JsonProperty("guardianDetails")
    private GuardianDetails guardianDetails = null ;

}
