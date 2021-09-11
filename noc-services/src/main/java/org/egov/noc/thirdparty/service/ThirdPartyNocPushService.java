package org.egov.noc.thirdparty.service;

import org.egov.noc.thirdparty.model.ThirdPartyNOCPushRequestWrapper;

public interface ThirdPartyNocPushService {
	
	public String pushProcess(ThirdPartyNOCPushRequestWrapper pushRequestWrapper);

}
