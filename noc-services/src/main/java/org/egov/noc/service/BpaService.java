package org.egov.noc.service;

import java.util.LinkedHashMap;

import org.egov.common.contract.request.RequestInfo;
import org.egov.noc.config.NOCConfiguration;
import org.egov.noc.repository.ServiceRequestRepository;
import org.egov.noc.util.NOCConstants;
import org.egov.noc.web.model.BPA;
import org.egov.noc.web.model.BPAResponse;
import org.egov.noc.web.model.RequestInfoWrapper;
import org.egov.tracer.model.CustomException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class BpaService {

	@Autowired
	private ServiceRequestRepository serviceRequestRepository;

	@Autowired
	private ObjectMapper mapper;

	@Autowired
	private NOCConfiguration config;

	public BPA getBuildingPlan(RequestInfo requestInfo, String tenantId, String applicationNo, String approvalNo) {
		StringBuilder url = getBPASearchURL();
		url.append("tenantId=");
		url.append(tenantId);
		if (approvalNo != null) {
			url.append("&");
			url.append("approvalNo="+approvalNo);
		} else {
			url.append("&");
			url.append("applicationNo="+applicationNo);
		}
		//url.append(approvalNo);
		LinkedHashMap responseMap = null;
		responseMap = (LinkedHashMap) serviceRequestRepository.fetchResult(url, new RequestInfoWrapper(requestInfo));

		BPAResponse bpaResponse = null;

		try {
			bpaResponse = mapper.convertValue(responseMap, BPAResponse.class);
		} catch (IllegalArgumentException e) {
			throw new CustomException(NOCConstants.PARSING_ERROR, "Error while parsing response of TradeLicense Search");
		}

		return bpaResponse.getBPA().get(0);
	}

	private StringBuilder getBPASearchURL() {
		// TODO Auto-generated method stub
		StringBuilder url = new StringBuilder(config.getBpaHost());
		url.append(config.getBpaContextPath());
		url.append(config.getBpaSearchEndpoint());
		url.append("?");
		return url;
	}
}
