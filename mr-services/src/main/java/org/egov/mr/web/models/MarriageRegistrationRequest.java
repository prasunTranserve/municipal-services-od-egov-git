package org.egov.mr.web.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;

import java.util.ArrayList;
import java.util.List;

import org.egov.common.contract.request.RequestInfo;
import org.springframework.validation.annotation.Validated;
import javax.validation.Valid;

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
public class MarriageRegistrationRequest   {
        @JsonProperty("RequestInfo")
        private RequestInfo requestInfo = null;

        @JsonProperty("MarriageRegistrations")
        @Valid
        private List<MarriageRegistration> MarriageRegistrations = null;


        public MarriageRegistrationRequest addMarriageRegistrationsItem(MarriageRegistration marriageRegistrationItem) {
            if (this.MarriageRegistrations == null) {
            this.MarriageRegistrations = new ArrayList<>();
            }
        this.MarriageRegistrations.add(marriageRegistrationItem);
        return this;
        }

}

