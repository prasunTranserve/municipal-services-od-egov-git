package org.egov.noc.thirdparty.fire.service;

import org.egov.noc.thirdparty.model.ThirdPartyNOCRequestInfoWrapper;
import org.egov.noc.thirdparty.service.ThirdPartyNocService;
import org.egov.noc.util.NOCConstants;
import org.egov.noc.web.model.BPA;
import org.egov.noc.web.model.Noc;
import org.egov.noc.web.model.UserSearchResponse;
import org.springframework.stereotype.Service;

@Service(NOCConstants.FIRE_NOC_TYPE)
public class FireNocService implements ThirdPartyNocService{

	@Override
	public String process(ThirdPartyNOCRequestInfoWrapper infoWrapper) {
		String comment=null;
		System.out.println(NOCConstants.FIRE_NOC_TYPE);
		
		return comment;
	}

}
