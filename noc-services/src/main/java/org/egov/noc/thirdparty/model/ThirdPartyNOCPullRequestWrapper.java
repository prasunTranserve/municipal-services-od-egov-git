package org.egov.noc.thirdparty.model;

import org.egov.common.contract.request.RequestInfo;
import org.egov.noc.thirdparty.nma.model.NmaArchitectRegistration;
import org.egov.noc.web.model.BPA;
import org.egov.noc.web.model.Noc;
import org.egov.noc.web.model.UserSearchResponse;
import org.springframework.validation.annotation.Validated;

import com.jayway.jsonpath.DocumentContext;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Validated
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class ThirdPartyNOCPullRequestWrapper {
	
	private RequestInfo requestInfo;
	
	private Noc noc;
	
	private UserSearchResponse userResponse;
	
}
