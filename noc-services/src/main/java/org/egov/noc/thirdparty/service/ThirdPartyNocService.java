package org.egov.noc.thirdparty.service;

import org.egov.noc.thirdparty.model.ThirdPartyNOCRequestInfoWrapper;
import org.egov.noc.web.model.BPA;
import org.egov.noc.web.model.Noc;
import org.egov.noc.web.model.UserSearchResponse;

public interface ThirdPartyNocService {
	
	public String process(ThirdPartyNOCRequestInfoWrapper nocRequestInfoWrapper);

}
