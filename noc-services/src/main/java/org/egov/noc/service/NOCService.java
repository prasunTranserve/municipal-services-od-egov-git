package org.egov.noc.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import org.egov.common.contract.request.RequestInfo;
import org.egov.noc.repository.NOCRepository;
import org.egov.noc.util.NOCConstants;
import org.egov.noc.util.NOCUtil;
import org.egov.noc.validator.NOCValidator;
import org.egov.noc.web.model.NmaApplicationStatus;
import org.egov.noc.web.model.Noc;
import org.egov.noc.web.model.NocRequest;
import org.egov.noc.web.model.NocSearchCriteria;
import org.egov.noc.web.model.ThirdPartyNocRequest;
import org.egov.noc.web.model.Workflow;
import org.egov.noc.web.model.workflow.BusinessService;
import org.egov.noc.workflow.WorkflowIntegrator;
import org.egov.noc.workflow.WorkflowService;
import org.egov.tracer.model.CustomException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

@Service
public class NOCService {

	@Autowired
	private NOCValidator nocValidator;

	@Autowired
	private WorkflowIntegrator wfIntegrator;

	@Autowired
	private NOCUtil nocUtil;

	@Autowired
	private NOCRepository nocRepository;

	@Autowired
	private EnrichmentService enrichmentService;

	@Autowired
	private WorkflowService workflowService;

	@Autowired
	private FileStoreService fileStoreService;

	/**
	 * entry point from controller, takes care of next level logic from controller
	 * to create NOC application
	 * 
	 * @param nocRequest
	 * @return
	 */
	public List<Noc> create(NocRequest nocRequest) {
		String tenantId = nocRequest.getNoc().getTenantId().split("\\.")[0];
		Object mdmsData = nocUtil.mDMSCall(nocRequest.getRequestInfo(), tenantId);
		Map<String, String> additionalDetails = nocValidator.getOrValidateBussinessService(nocRequest.getNoc(),
				mdmsData);
		nocValidator.validateCreate(nocRequest, mdmsData);
		enrichmentService.enrichCreateRequest(nocRequest, mdmsData);
		if (!ObjectUtils.isEmpty(nocRequest.getNoc().getWorkflow())
				&& !StringUtils.isEmpty(nocRequest.getNoc().getWorkflow().getAction())) {
			wfIntegrator.callWorkFlow(nocRequest, additionalDetails.get(NOCConstants.WORKFLOWCODE));
		} else {
			nocRequest.getNoc().setApplicationStatus(NOCConstants.CREATED_STATUS);
		}
		nocRepository.save(nocRequest);
		return Arrays.asList(nocRequest.getNoc());
	}

	/**
	 * entry point from controller, takes care of next level logic from controller
	 * to update NOC application
	 * 
	 * @param nocRequest
	 * @return
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public List<Noc> update(NocRequest nocRequest) {
		String tenantId = nocRequest.getNoc().getTenantId().split("\\.")[0];
		Object mdmsData = nocUtil.mDMSCall(nocRequest.getRequestInfo(), tenantId);
		Map<String, String> additionalDetails;
		if (!ObjectUtils.isEmpty(nocRequest.getNoc().getAdditionalDetails())) {
			additionalDetails = (Map) nocRequest.getNoc().getAdditionalDetails();
		} else {
			additionalDetails = nocValidator.getOrValidateBussinessService(nocRequest.getNoc(), mdmsData);
		}
		Noc searchResult = getNocForUpdate(nocRequest);
		nocValidator.validateUpdate(nocRequest, searchResult, additionalDetails.get(NOCConstants.MODE), mdmsData);
		enrichmentService.enrichNocUpdateRequest(nocRequest, searchResult);

		if (!ObjectUtils.isEmpty(nocRequest.getNoc().getWorkflow())
				&& !StringUtils.isEmpty(nocRequest.getNoc().getWorkflow().getAction())) {
			BusinessService businessService = workflowService.getBusinessService(nocRequest.getNoc(),
					nocRequest.getRequestInfo(), additionalDetails.get(NOCConstants.WORKFLOWCODE));
			wfIntegrator.callWorkFlow(nocRequest, additionalDetails.get(NOCConstants.WORKFLOWCODE));
			enrichmentService.postStatusEnrichment(nocRequest, additionalDetails.get(NOCConstants.WORKFLOWCODE));
			nocRepository.update(nocRequest,
					workflowService.isStateUpdatable(nocRequest.getNoc().getApplicationStatus(), businessService));
		} else {
			nocRepository.update(nocRequest, Boolean.FALSE);
		}

		return Arrays.asList(nocRequest.getNoc());
	}

	/**
	 * entry point from controller,applies the quired fileters and encrich search
	 * criteria and return the noc application matching the search criteria
	 * 
	 * @param nocRequest
	 * @return
	 */
	public List<Noc> search(NocSearchCriteria criteria, RequestInfo requestInfo) {
		/*
		 * List<String> uuids = new ArrayList<String>();
		 * uuids.add(requestInfo.getUserInfo().getUuid()); criteria.setAccountId(uuids);
		 */
		List<Noc> nocs = nocRepository.getNocData(criteria);
		return nocs.isEmpty() ? Collections.emptyList() : nocs;
	}

	/**
	 * Fetch the noc based on the id to update the NOC record
	 * 
	 * @param nocRequest
	 * @return
	 */
	public Noc getNocForUpdate(NocRequest nocRequest) {
		List<String> ids = Arrays.asList(nocRequest.getNoc().getId());
		NocSearchCriteria criteria = new NocSearchCriteria();
		criteria.setIds(ids);
		List<Noc> nocList = search(criteria, nocRequest.getRequestInfo());
		if (CollectionUtils.isEmpty(nocList)) {
			StringBuilder builder = new StringBuilder();
			builder.append("Noc Application not found for: ").append(nocRequest.getNoc().getId()).append(" :ID");
			throw new CustomException("INVALID_NOC_SEARCH", builder.toString());
		} else if (nocList.size() > 1) {
			StringBuilder builder = new StringBuilder();
			builder.append("Multiple Noc Application(s) not found for: ").append(nocRequest.getNoc().getId())
					.append(" :ID");
			throw new CustomException("INVALID_NOC_SEARCH", builder.toString());
		}
		return nocList.get(0);
	}

	public List<Noc> updateThirdPartyNoc(ThirdPartyNocRequest nocRequest) {
		List<Noc> processedNoc = new ArrayList<>();
		for (NmaApplicationStatus nmaApplicationStatus : nocRequest.getNmaRequest().getApplicationStatus()) {
			NocSearchCriteria criteria = new NocSearchCriteria();
			String applicationNumber = nmaApplicationStatus.getApplicationUniqueNumebr();
			criteria.setApplicationNo(applicationNumber);
			List<Noc> nocList = search(criteria, nocRequest.getRequestInfo());

			if (CollectionUtils.isEmpty(nocList)) {
				StringBuilder builder = new StringBuilder();
				builder.append("Noc Application not found for: ").append(applicationNumber).append(" :ID");
				throw new CustomException("INVALID_NOC_SEARCH", builder.toString());
			} else {
				Noc noc = nocList.get(0);
				Workflow workflow = new Workflow();
				String action = null;
				if (nmaApplicationStatus.getStatus() != null)
					action = getNmaValidAction(nmaApplicationStatus.getStatus().trim());
				workflow.setAction(action);
				workflow.setComment(getNmaValidComment(nmaApplicationStatus));

				if (nmaApplicationStatus.getNocFileUrl() != null && !nmaApplicationStatus.getNocFileUrl().isEmpty()) {
//					String docType="NOC.NMA.CERTIFICATE";
					String docType = getNmaValidDocType(action);
					workflow.setDocuments(fileStoreService.copyDocuments(nmaApplicationStatus.getDepartment(), "NOC",
							docType, Arrays.asList(new String[] { nmaApplicationStatus.getNocFileUrl() })));
				}

				noc.setWorkflow(workflow);
				NocRequest nocRequest2 = new NocRequest();
				nocRequest2.setNoc(noc);
				nocRequest2.setRequestInfo(nocRequest.getRequestInfo());
				nocList = update(nocRequest2);
				processedNoc.addAll(nocList);
			}
		}

		return processedNoc;
	}

	private String getNmaValidAction(String status) {

		if ("Approved".equalsIgnoreCase(status)) {
			return NOCConstants.ACTION_APPROVE;
		} else if ("Rejected".equalsIgnoreCase(status)) {
			return NOCConstants.ACTION_REJECT;
		} else if ("NOC not required from NMA".equalsIgnoreCase(status)) {
			return NOCConstants.ACTION_VOID;
		} else {
			return NOCConstants.ACTION_SUBMIT;
		}
	}

	private String getNmaValidComment(NmaApplicationStatus applicationStatus) {
		StringBuilder comment = new StringBuilder();
		comment.append(applicationStatus.getRemarks())
				.append(applicationStatus.getUniqueId() != null ? ", UniqueId " + applicationStatus.getUniqueId() : " ")
				.append(applicationStatus.getNocNumber() != null ? ", NocNumber " + applicationStatus.getNocNumber() : " ");
		return comment.toString();
	}

	private String getNmaValidDocType(String action) {
		StringBuilder doctype = new StringBuilder("Document - 1");
		return doctype.toString();
	}
}
