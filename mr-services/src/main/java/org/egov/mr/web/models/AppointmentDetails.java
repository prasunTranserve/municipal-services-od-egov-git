package org.egov.mr.web.models;

import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;



@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@EqualsAndHashCode
public class AppointmentDetails   {

        @Size(max=64)
        @JsonProperty("id")
        private String id;

        @JsonProperty("active")
        private Boolean active;

        @Size(max=64)
        @JsonProperty("tenantId")
        private String tenantId = null;

        @JsonProperty("startTime")
        private Long startTime = null;

        @JsonProperty("endTime")
        private Long endTime = null;
        
        
        @Size(max=256)
        @JsonProperty("description")
        private String description = null ;
        
        @JsonProperty("additionalDetail")
        private JsonNode additionalDetail = null;

        @JsonProperty("auditDetails")
        private AuditDetails auditDetails = null;


}

