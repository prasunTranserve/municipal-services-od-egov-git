package org.egov.noc.thirdparty.service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.egov.common.contract.request.RequestInfo;
import org.egov.noc.consumer.NOCConsumer;
import org.egov.noc.repository.NOCRepository;
import org.egov.noc.service.BpaService;
import org.egov.noc.service.EdcrService;
import org.egov.noc.service.EnrichmentService;
import org.egov.noc.service.NOCService;
import org.egov.noc.service.UserService;
import org.egov.noc.thirdparty.model.ThirdPartyNOCRequestInfoWrapper;
import org.egov.noc.util.NOCConstants;
import org.egov.noc.util.NOCUtil;
import org.egov.noc.validator.NOCValidator;
import org.egov.noc.web.model.BPA;
import org.egov.noc.web.model.Noc;
import org.egov.noc.web.model.NocRequest;
import org.egov.noc.web.model.NocSearchCriteria;
import org.egov.noc.web.model.RequestInfoWrapper;
import org.egov.noc.web.model.ThirdPartyNocRequest;
import org.egov.noc.web.model.UserResponse;
import org.egov.noc.web.model.Workflow;
import org.egov.noc.workflow.WorkflowIntegrator;
import org.egov.noc.workflow.WorkflowService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;

import lombok.extern.slf4j.Slf4j;
import springfox.documentation.spring.web.json.Json;

@Slf4j
@Service
public class ThirdPartyNocServiceProcessor {

	@Autowired
	private NOCValidator nocValidator;

	@Autowired
	private WorkflowIntegrator wfIntegrator;

	@Autowired
	private NOCRepository nocRepository;

	@Autowired
	private NOCService nocService;

	private ApplicationContext context;

	@Autowired
	private BpaService bpaService;

	@Autowired
	private UserService userService;

	@Autowired
	private EdcrService edcrService;

	public ThirdPartyNocServiceProcessor(ApplicationContext context) {
		this.context = context;
	}

	public void process(RequestInfoWrapper requestInfoWrapper) {
		List<Noc> nocs = nocRepository.getNocData(getNocSearchCriteria());
		for (Noc noc : nocs) {
			Map<String, String> map = JsonPath.parse(noc.getAdditionalDetails()).json();
			if (NOCConstants.THIRD_PARTY_MODE.equals(map.get(NOCConstants.MODE))) {
				try {
					UserResponse userResponse = getUser(noc, requestInfoWrapper);
					requestInfoWrapper.getRequestInfo().setUserInfo(userResponse.getUser().get(0));
					BPA bpa = bpaService.getBuildingPlan(requestInfoWrapper.getRequestInfo(), noc.getTenantId(),
							noc.getSourceRefId(), null);
					ThirdPartyNocService thirdPartyNocService = getBean(noc.getNocType());
					DocumentContext edcr = edcrService.getEDCRDetails(bpa.getTenantId(), bpa.getEdcrNumber(),
							requestInfoWrapper.getRequestInfo());
					ThirdPartyNOCRequestInfoWrapper thirdPartyNOCRequestInfoWrapper = ThirdPartyNOCRequestInfoWrapper
							.builder().bpa(bpa).edcr(edcr).noc(noc).requestInfo(requestInfoWrapper.getRequestInfo())
							.userResponse(userResponse.getUser().get(0)).build();
					String comment = thirdPartyNocService.process(thirdPartyNOCRequestInfoWrapper);
					updateNocWf(requestInfoWrapper.getRequestInfo(), noc, comment);
				} catch (Exception e) {
					log.error("While processing thirdparty noc " + noc.getNocType() + " with error " + e.getMessage());
				}
			}
		}
	}

	private void updateNocWf(RequestInfo requestInfo, Noc noc, String comment) {
		Workflow workflow = new Workflow();
		workflow.setAction(NOCConstants.ACTION_SUBMIT);
		workflow.setComment(comment);
		noc.setWorkflow(workflow);
		NocRequest nocRequest = new NocRequest();
		nocRequest.setNoc(noc);
		nocRequest.setRequestInfo(requestInfo);
		nocService.update(nocRequest);
	}

	private NocSearchCriteria getNocSearchCriteria() {
		NocSearchCriteria nocSearchCriteria = new NocSearchCriteria();
		nocSearchCriteria.setApplicationStatus(NOCConstants.ACTION_INPROGRESS);
		return nocSearchCriteria;
	}

	private NocSearchCriteria getNocSearchCriteriaForSearchUser(Noc noc) {
		NocSearchCriteria nocSearchCriteria = new NocSearchCriteria();
		nocSearchCriteria.setOwnerIds(Arrays.asList(new String[] { noc.getAuditDetails().getCreatedBy() }));
		nocSearchCriteria.setTenantId(noc.getTenantId());
		return nocSearchCriteria;
	}

	private ThirdPartyNocService getBean(String nocType) {
		ThirdPartyNocService thirdPartyNocService = null;
		try {
			thirdPartyNocService = (ThirdPartyNocService) context.getBean(nocType);
		} catch (Exception e) {
			log.error(e.getMessage());
		}
		if (thirdPartyNocService == null) {
			log.error(nocType + " bean not found");
		}
		return thirdPartyNocService;
	}

	private UserResponse getUser(Noc noc, RequestInfoWrapper requestInfoWrapper) {
		return userService.getUser(getNocSearchCriteriaForSearchUser(noc), requestInfoWrapper.getRequestInfo());
	}

}
