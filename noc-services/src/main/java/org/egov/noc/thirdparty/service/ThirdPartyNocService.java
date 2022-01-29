package org.egov.noc.thirdparty.service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.egov.common.contract.request.RequestInfo;
import org.egov.noc.repository.NOCRepository;
import org.egov.noc.service.BpaService;
import org.egov.noc.service.EdcrService;
import org.egov.noc.service.NOCService;
import org.egov.noc.service.UserService;
import org.egov.noc.thirdparty.model.ThirdPartyNOCPullRequestWrapper;
import org.egov.noc.thirdparty.model.ThirdPartyNOCPushRequestWrapper;
import org.egov.noc.util.NOCConstants;
import org.egov.noc.web.model.BPA;
import org.egov.noc.web.model.Noc;
import org.egov.noc.web.model.NocRequest;
import org.egov.noc.web.model.NocSearchCriteria;
import org.egov.noc.web.model.RequestInfoWrapper;
import org.egov.noc.web.model.UserResponse;
import org.egov.noc.web.model.Workflow;
import org.egov.tracer.model.CustomException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class ThirdPartyNocService {

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

	public ThirdPartyNocService(ApplicationContext context) {
		this.context = context;
	}

	public void process(RequestInfoWrapper requestInfoWrapper) {
		pushprocess(requestInfoWrapper);
		pullprocess(requestInfoWrapper);
	}

	/*
	 * It will fetch all NOC which having workflow type is thirdParty And
	 * application_status is ACTION_INPROGRESS After successfully execution of each
	 * it update the workflow from ACTION_INPROGRESS to ACTION_SUBMIT
	 */
	private void pushprocess(RequestInfoWrapper requestInfoWrapper) {

		List<Noc> nocs = nocRepository.getNocData(getNocSearchCriteria(NOCConstants.NOC_STATUS_INPROGRESS));
		for (Noc noc : nocs) {
			Map<String, String> map = JsonPath.parse(noc.getAdditionalDetails()).json();
			if (NOCConstants.THIRD_PARTY_MODE.equals(map.get(NOCConstants.MODE))) {
				try {
					ThirdPartyNocPushService thirdPartyNocPushService = getPushBean(noc.getNocType());

					UserResponse userResponse = getUser(noc, requestInfoWrapper);
					//requestInfoWrapper.getRequestInfo().setUserInfo(userResponse.getUser().get(0));
					BPA bpa = bpaService.getBuildingPlan(requestInfoWrapper.getRequestInfo(), noc.getTenantId(),
							noc.getSourceRefId(), null);

					DocumentContext edcr = edcrService.getEDCRDetails(bpa.getTenantId(), bpa.getEdcrNumber(),
							requestInfoWrapper.getRequestInfo());
					ThirdPartyNOCPushRequestWrapper pushRequestWrapper = ThirdPartyNOCPushRequestWrapper.builder()
							.bpa(bpa).edcr(edcr).noc(noc).requestInfo(requestInfoWrapper.getRequestInfo())
							.userResponse(userResponse.getUser().get(0)).build();
					String comment = thirdPartyNocPushService.pushProcess(pushRequestWrapper);
					Workflow workflow = getWorkflowAfterPush(comment);
					updateNocWf(noc, requestInfoWrapper.getRequestInfo(), workflow);
				} catch (Exception e) {
					e.printStackTrace();
					log.error("While processing thirdParty noc push process " + noc.getApplicationNo() + " "
							+ noc.getNocType() + " with error " + e.getMessage());
				}
			}
		}

	}

	private void pullprocess(RequestInfoWrapper requestInfoWrapper) {
		List<Noc> nocs = nocRepository.getNocData(getNocSearchCriteria(NOCConstants.NOC_STATUS_SUBMITED));
		for (Noc noc : nocs) {
			Map<String, String> map = JsonPath.parse(noc.getAdditionalDetails()).json();
			if (NOCConstants.THIRD_PARTY_MODE.equals(map.get(NOCConstants.MODE))) {
				try {
					ThirdPartyNocPullService pullService = getPullBean(noc.getNocType());
					UserResponse userResponse = getUser(noc, requestInfoWrapper);
					requestInfoWrapper.getRequestInfo().setUserInfo(userResponse.getUser().get(0));

					ThirdPartyNOCPullRequestWrapper pullRequestWrapper = ThirdPartyNOCPullRequestWrapper.builder()
							.noc(noc).requestInfo(requestInfoWrapper.getRequestInfo())
							.userResponse(userResponse.getUser().get(0)).build();

					Workflow workflow = pullService.pullProcess(pullRequestWrapper);
					updateNocWf(noc, requestInfoWrapper.getRequestInfo(), workflow);
				} catch (Exception e) {
					log.error("While processing thirdParty noc pull process " + noc.getApplicationNo() + " "
							+ noc.getNocType() + " with error " + e.getMessage());
				}
			}
		}
	}

	private void updateNocWf(Noc noc, RequestInfo requestInfo, Workflow workflow) {
		noc.setWorkflow(workflow);
		NocRequest nocRequest = new NocRequest();
		nocRequest.setNoc(noc);
		nocRequest.setRequestInfo(requestInfo);
		nocService.update(nocRequest);
	}

	private Workflow getWorkflowAfterPush(String comment) {
		Workflow workflow = new Workflow();
		workflow.setAction(NOCConstants.ACTION_SUBMIT);
		workflow.setComment(comment);
		return workflow;
//		noc.setWorkflow(workflow);
//		NocRequest nocRequest = new NocRequest();
//		nocRequest.setNoc(noc);
//		nocRequest.setRequestInfo(requestInfo);
//		nocService.update(nocRequest);
	}

	private NocSearchCriteria getNocSearchCriteria(String applicationStatus) {
		NocSearchCriteria nocSearchCriteria = new NocSearchCriteria();
		nocSearchCriteria.setApplicationStatus(applicationStatus);
		return nocSearchCriteria;
	}

	private NocSearchCriteria getNocSearchCriteriaForSearchUser(Noc noc) {
		NocSearchCriteria nocSearchCriteria = new NocSearchCriteria();
		nocSearchCriteria.setOwnerIds(Arrays.asList(new String[] { noc.getAuditDetails().getCreatedBy() }));
		nocSearchCriteria.setTenantId(noc.getTenantId());
		return nocSearchCriteria;
	}

	private ThirdPartyNocPushService getPushBean(String nocType) {
		ThirdPartyNocPushService thirdPartyNocService = null;
		try {
			thirdPartyNocService = (ThirdPartyNocPushService) context.getBean(nocType);
		} catch (Exception e) {
			throw new CustomException("BEAN_EXCEPTION", e.getMessage());
		}
		return thirdPartyNocService;
	}

	private ThirdPartyNocPullService getPullBean(String nocType) {
		ThirdPartyNocPullService partyNocPullService = null;
		try {
			partyNocPullService = (ThirdPartyNocPullService) context.getBean(nocType);
		} catch (ClassCastException e) {
			throw new CustomException("BEAN_EXCEPTION",
					nocType + " either not supporting pull service or implementation is missing !!!");
		} catch (Exception e) {
			throw new CustomException("BEAN_EXCEPTION", e.getMessage());
		}
		return partyNocPullService;
	}

	private UserResponse getUser(Noc noc, RequestInfoWrapper requestInfoWrapper) {
		return userService.getUser(getNocSearchCriteriaForSearchUser(noc), requestInfoWrapper.getRequestInfo());
	}

}