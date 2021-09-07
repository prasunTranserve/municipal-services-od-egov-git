package org.egov.mr.web.models;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.ArrayList;
import java.util.List;

import org.egov.common.contract.response.ResponseInfo;
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
public class MarriageRegistrationResponse   {
        @JsonProperty("ResponseInfo")
        private ResponseInfo responseInfo = null;

        @JsonProperty("MarriageRegistrations")
        @Valid
        private List<MarriageRegistration> marriageRegistrations = null;


        public MarriageRegistrationResponse addMarriageRegistrationsItem(MarriageRegistration marriageRegistrationItem) {
            if (this.marriageRegistrations == null) {
            this.marriageRegistrations = new ArrayList<>();
            }
        this.marriageRegistrations.add(marriageRegistrationItem);
        return this;
        }

}

