package org.egov.noc.thirdparty.nma.service;

import java.util.List;
import java.util.Map;

import org.egov.noc.config.NOCConfiguration;
import org.egov.noc.repository.ServiceRequestRepository;
import org.egov.noc.thirdparty.model.ThirdPartyNOCRequestInfoWrapper;
import org.egov.noc.thirdparty.nma.model.NmaApplicationRequest;
import org.egov.noc.thirdparty.nma.model.NmaArchitectRegistration;
import org.egov.noc.thirdparty.nma.model.NmaUser;
import org.egov.noc.thirdparty.service.ThirdPartyNocService;
import org.egov.noc.util.NOCConstants;
import org.egov.noc.web.model.UserSearchResponse;
import org.egov.tracer.model.CustomException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;

import lombok.extern.slf4j.Slf4j;
import net.minidev.json.JSONArray;

@Slf4j
@Service(NOCConstants.NMA_NOC_TYPE)
public class NmaNocService implements ThirdPartyNocService {

	@Autowired
	private NmaArchitectRegistrationService nmaService;

	@Autowired
	private NmaUtility nmaUtility;

	@Autowired
	private NOCConfiguration config;

	@Autowired
	private ServiceRequestRepository serviceRequestRepository;

	@Override
	public String process(ThirdPartyNOCRequestInfoWrapper infoWrapper) {
		String comments=null;
		UserSearchResponse user = infoWrapper.getUserResponse();
		NmaUser nmaUser = NmaUser.builder().architectEmailId(user.getEmailId())
				.architectMobileNo(user.getMobileNumber()).architectName(user.getName())
				.tenantid(infoWrapper.getNoc().getTenantId()).userid(user.getId()).build();
		NmaArchitectRegistration nmaArchitectRegistration= nmaService.validateNmaArchitectRegistration(nmaUser);
		NmaApplicationRequest nmaApplicationRequest = nmaUtility.buildNmaApplicationRequest(infoWrapper,nmaArchitectRegistration);
		String response = null;
		log.debug("Nma application create: " + nmaApplicationRequest);
		response = (String) nmaUtility.fetchResult(getNmaFormRegURL(), nmaApplicationRequest);
//		response="{\"ApplicationStatus\":[{\"Department\":\"MCD\",\"ArchitectEmailId\":\"rezakhan9494@gmail.com\",\"ApplicationUniqueNumebr\":\"ApplicatinId\r\n"
//				+ "like: 1000254785\",\"ProximityStatus\":\"-1\",\"Status\":\"Application Received\",\"ResponseTime\":\"15-07-2021\r\n"
//				+ "13:43:38:PM\",\"Remarks\":\"Submission of Coordinates pending at architect end.\",\"NodeId\":1767,\"NocFileUrl\":\"No\r\n"
//				+ "File\",\"UniqueId\":\"ApplicatinId like: 1000254785\"}]}";
		DocumentContext documentContext = JsonPath.using(Configuration.defaultConfiguration()).parse(response);
		List<String> list = (List<String>) documentContext.read("ApplicationStatus.*.Status");// Message
		String status = null;
		if (list != null && list.size() > 0) {
			status = list.get(0);
		}
		if ("Application Received".equals(status)) {
			return "Application submited to Nma department with token "+nmaArchitectRegistration.getToken();
		} else {
			throw new CustomException(NOCConstants.NMA_ERROR,
					"Error while calling nma system with msg " + documentContext.read("ApplicationStatus.*.Message"));
		}
	}

	
	private StringBuilder getNmaFormRegURL() {
		StringBuilder url = new StringBuilder(config.getNmaHost());
		url.append(config.getNmaContextPath());
		url.append(config.getNmaApplicationCreate());
		return url;
	}

}
