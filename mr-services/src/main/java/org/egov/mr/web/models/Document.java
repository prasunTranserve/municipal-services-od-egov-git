package org.egov.mr.web.models;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.*;

import org.egov.mr.web.models.AuditDetails;
import org.springframework.validation.annotation.Validated;
import javax.validation.Valid;
import javax.validation.constraints.*;



@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@EqualsAndHashCode
public class Document   {

        @Size(max=64)
        @JsonProperty("id")
        private String id;

        @JsonProperty("active")
        private Boolean active;

        @Size(max=64)
        @JsonProperty("tenantId")
        private String tenantId = null;

        @Size(max=64)
        @JsonProperty("documentType")
        private String documentType = null;

        @Size(max=64)
        @JsonProperty("fileStoreId")
        private String fileStoreId = null;

        @Size(max=64)
        @JsonProperty("documentUid")
        private String documentUid;

        @JsonProperty("auditDetails")
        private AuditDetails auditDetails = null;


}

