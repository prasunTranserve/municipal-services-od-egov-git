package org.egov.bpa.web.controller;

import javax.validation.Valid;

import org.egov.bpa.service.UserService;
import org.egov.bpa.util.ResponseInfoFactory;
import org.egov.bpa.web.model.RequestInfoWrapper;
import org.egov.bpa.web.model.user.UserDetailResponse;
import org.egov.bpa.web.model.user.UserResponse;
import org.egov.bpa.web.model.user.UserSearchCriteria;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/citizen")
public class UserController {

	@Autowired
	private UserService userService;
	
	@Autowired
	private ResponseInfoFactory responseInfoFactory;
	
	@PostMapping(value = "/_search")
	public ResponseEntity<UserResponse> search(@Valid @RequestBody RequestInfoWrapper requestInfoWrapper,
			@Valid @ModelAttribute UserSearchCriteria criteria) {
		UserDetailResponse userDetailResponse = userService.search(criteria, requestInfoWrapper.getRequestInfo());

		UserResponse response = UserResponse.builder().users(userDetailResponse.getUser()).responseInfo(
				responseInfoFactory.createResponseInfoFromRequestInfo(requestInfoWrapper.getRequestInfo(), true))
				.build();
		return new ResponseEntity<>(response, HttpStatus.OK);
	}
}
