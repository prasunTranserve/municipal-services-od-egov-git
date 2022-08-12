package org.egov.noc.web.controller;

import javax.validation.Valid;

import org.egov.noc.config.ResponseInfoFactory;
import org.egov.noc.thirdparty.aai.model.UlbServiceResponse;
import org.egov.noc.thirdparty.aai.service.AAINocService;
import org.egov.noc.web.model.RequestInfoWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;

@RestController
@RequestMapping("v1/aainoc")
public class AAINOCController {

	@Autowired
	private ResponseInfoFactory responseInfoFactory;

	@Autowired
	private AAINocService aaiNocService;

	@PostMapping(value = "/_pull")
	public ResponseEntity<UlbServiceResponse> pull(@Valid @RequestBody RequestInfoWrapper requestInfoWrapper)
			throws JsonProcessingException {
		UlbServiceResponse ulbServiceResponse = aaiNocService.fetchAAINocsInProgress(requestInfoWrapper);
		return new ResponseEntity<>(ulbServiceResponse, HttpStatus.OK);
	}
}
