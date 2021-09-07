package org.egov.mr.web.models;

import java.util.List;

import javax.validation.constraints.Size;



import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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
    
    @Size(max=64)
    @JsonProperty("middleName")
    private String middleName;
    
    @Size(max=64)
    @JsonProperty("lastName")
    private String lastName;
    
    
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
