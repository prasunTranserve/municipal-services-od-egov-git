package org.egov.pgr.web.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import org.egov.common.contract.response.ResponseInfo;
import org.springframework.validation.annotation.Validated;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.Builder;

/**
 * Response to the service request
 */
@ApiModel(description = "Response to the service request")
@Validated
@javax.annotation.Generated(value = "org.egov.codegen.SpringBootCodegen", date = "2020-07-15T11:35:33.568+05:30")

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ServiceResponse   {
        @JsonProperty("responseInfo")
        private ResponseInfo responseInfo = null;

        @JsonProperty("services")
        private Service services = null;

        @JsonProperty("workflow")
        private Workflow workflow = null;


}
