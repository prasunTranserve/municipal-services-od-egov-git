package org.egov.tl.web.models;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.*;
import org.egov.tl.web.models.AuditDetails;
import org.springframework.validation.annotation.Validated;
import javax.validation.Valid;
import javax.validation.constraints.*;



@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Validated
@EqualsAndHashCode
public class DscDetails   {

        @Size(max=64)
        @JsonProperty("id")
        private String id;

        @Size(max=64)
        @JsonProperty("tenantId")
        private String tenantId = null;

        @Size(max=64)
        @JsonProperty("documentType")
        private String documentType = null;

        @Size(max=64)
        @JsonProperty("documentId")
        private String documentId = null;
        
        @Size(max=64)
        @JsonProperty("applicationNumber")
        private String applicationNumber;

        @Size(max=64)
        @JsonProperty("approvedBy")
        private String approvedBy;

        @JsonProperty("additionalDetail")
        private JsonNode additionalDetail = null;
        
        @JsonProperty("auditDetails")
        private AuditDetails auditDetails = null;


}

