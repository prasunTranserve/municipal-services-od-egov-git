package org.egov.mr.web.controllers;





import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.annotations.*;

import org.egov.mr.service.MarriageRegistrationService;
import org.egov.mr.util.ResponseInfoFactory;
import org.egov.mr.web.models.MarriageRegistration;
import org.egov.mr.web.models.MarriageRegistrationRequest;
import org.egov.mr.web.models.MarriageRegistrationResponse;
import org.egov.mr.web.models.MarriageRegistrationSearchCriteria;
import org.egov.mr.web.models.RequestInfoWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.*;

import javax.validation.constraints.*;
import javax.validation.Valid;
import javax.servlet.http.HttpServletRequest;

@RestController
    @RequestMapping("/v1")
    public class MarriageRegistrationController {

        private final ObjectMapper objectMapper;

        private final HttpServletRequest request;

        private final MarriageRegistrationService marriageRegistrationService;

        private final ResponseInfoFactory responseInfoFactory;

    @Autowired
    public MarriageRegistrationController(ObjectMapper objectMapper, HttpServletRequest request,
    		MarriageRegistrationService marriageRegistrationService, ResponseInfoFactory responseInfoFactory) {
        this.objectMapper = objectMapper;
        this.request = request;
        this.marriageRegistrationService = marriageRegistrationService;
        this.responseInfoFactory = responseInfoFactory;
    }


    @PostMapping({"/{servicename}/_create", "/_create"})
    public ResponseEntity<MarriageRegistrationResponse> create(@Valid @RequestBody MarriageRegistrationRequest marriageRegistrationRequest,
                                                       @PathVariable(required = false) String servicename) {
        List<MarriageRegistration> marraiageRegistrations = marriageRegistrationService.create(marriageRegistrationRequest, servicename);
        MarriageRegistrationResponse response = MarriageRegistrationResponse.builder().marriageRegistrations(marraiageRegistrations).responseInfo(
                responseInfoFactory.createResponseInfoFromRequestInfo(marriageRegistrationRequest.getRequestInfo(), true))
                .build();
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
    


    @RequestMapping(value = {"/{servicename}/_search", "/_search"}, method = RequestMethod.POST)
    public ResponseEntity<MarriageRegistrationResponse> search(@Valid @RequestBody RequestInfoWrapper requestInfoWrapper,
                                                       @Valid @ModelAttribute MarriageRegistrationSearchCriteria criteria,
                                                       @PathVariable(required = false) String servicename
            , @RequestHeader HttpHeaders headers) {
        List<MarriageRegistration> marriageRegistrations = marriageRegistrationService.search(criteria, requestInfoWrapper.getRequestInfo(), servicename, headers);

        MarriageRegistrationResponse response = MarriageRegistrationResponse.builder().marriageRegistrations(marriageRegistrations).responseInfo(
                responseInfoFactory.createResponseInfoFromRequestInfo(requestInfoWrapper.getRequestInfo(), true))
                .build();
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @RequestMapping(value = {"/{servicename}/_update", "/_update"}, method = RequestMethod.POST)
    public ResponseEntity<MarriageRegistrationResponse> update(@Valid @RequestBody MarriageRegistrationRequest marriageRegistrationRequest,
                                                       @PathVariable(required = false) String servicename) {
        List<MarriageRegistration> marriageRegistrations = marriageRegistrationService.update(marriageRegistrationRequest, servicename);

        MarriageRegistrationResponse response = MarriageRegistrationResponse.builder().marriageRegistrations(marriageRegistrations).responseInfo(
                responseInfoFactory.createResponseInfoFromRequestInfo(marriageRegistrationRequest.getRequestInfo(), true))
                .build();
        return new ResponseEntity<>(response, HttpStatus.OK);
    }





}
