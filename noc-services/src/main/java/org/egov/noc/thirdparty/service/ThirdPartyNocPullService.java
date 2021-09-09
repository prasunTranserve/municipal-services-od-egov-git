package org.egov.noc.thirdparty.service;

import org.egov.noc.thirdparty.model.ThirdPartyNOCPullRequestWrapper;
import org.egov.noc.web.model.Workflow;

public interface ThirdPartyNocPullService {
	
	public Workflow pullProcess(ThirdPartyNOCPullRequestWrapper pullRequestWrapper);

}
