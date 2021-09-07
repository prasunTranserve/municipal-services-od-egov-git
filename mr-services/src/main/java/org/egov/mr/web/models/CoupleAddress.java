package org.egov.mr.web.models;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.springframework.validation.annotation.Validated;
import javax.validation.Valid;
import javax.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.Builder;


@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CoupleAddress   {

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

}

