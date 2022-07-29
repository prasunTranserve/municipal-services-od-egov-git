package org.egov.bpa.service;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import org.egov.bpa.config.BPAConfiguration;
import org.egov.bpa.repository.RevisionRepository;
import org.egov.bpa.util.BPAErrorConstants;
import org.egov.bpa.util.BPAUtil;
import org.egov.bpa.web.model.Revision;
import org.egov.bpa.web.model.RevisionRequest;
import org.egov.bpa.web.model.RevisionSearchCriteria;
import org.egov.common.contract.request.RequestInfo;
import org.egov.tracer.model.CustomException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class RevisionService {

	@Autowired
	private RevisionRepository repository;

	@Autowired
	private BPAUtil util;

	@Autowired
	private BPAConfiguration config;
	
	@Autowired
	EnrichmentService enrichmentService;

	/**
	 * does all the validations required to create BPA Record in the system
	 * 
	 * @param bpaRequest
	 * @return
	 */
	public Revision create(RevisionRequest revisionRequest) {
		RequestInfo requestInfo = revisionRequest.getRequestInfo();
		validateRevisionAlreadyExists(revisionRequest);
		enrichmentService.enrichRevisionCreateRequest(revisionRequest);
		//Object mdmsData = util.mDMSCall(requestInfo, tenantId);
		// TODO validations
		repository.save(revisionRequest);
		return revisionRequest.getRevision();
	}
	
	private void validateRevisionAlreadyExists(RevisionRequest revisionRequest) {
		// validate if revision request already exists for given applicationNo-
		RevisionSearchCriteria revisionSearchCriteriaForApplicationNo = RevisionSearchCriteria.builder()
				.bpaApplicationNo(revisionRequest.getRevision().getBpaApplicationNo()).build();
		List<Revision> revisionByApplicationNo = repository.getRevisionData(revisionSearchCriteriaForApplicationNo);
		if (Objects.nonNull(revisionByApplicationNo) && revisionByApplicationNo.size() > 0) {
			throw new CustomException(
					"Found already existing revision data for given bpaApplicationNo:"
							+ revisionRequest.getRevision().getBpaApplicationNo(),
					"Found already existing revision data for given bpaApplicationNo:"
							+ revisionRequest.getRevision().getBpaApplicationNo());
		}
		// validate if revision request already exists for given refApplicationNo-
		RevisionSearchCriteria revisionSearchCriteriaForRefApplicationNo = RevisionSearchCriteria.builder()
				.refBpaApplicationNo(revisionRequest.getRevision().getRefBpaApplicationNo()).build();
		List<Revision> revisionByRefApplicationNo = repository
				.getRevisionData(revisionSearchCriteriaForRefApplicationNo);
		if (Objects.nonNull(revisionByRefApplicationNo) && revisionByRefApplicationNo.size() > 0) {
			throw new CustomException(
					"Found already existing revision data for given RefBpaApplicationNo:"
							+ revisionRequest.getRevision().getRefBpaApplicationNo(),
					"Found already existing revision data for given RefBpaApplicationNo:"
							+ revisionRequest.getRevision().getRefBpaApplicationNo());
		}
		
		// validate if revision request already exists for given refPermitNo if isSujogExistingApplication=false -
		if (!revisionRequest.getRevision().isSujogExistingApplication()) {
			RevisionSearchCriteria revisionSearchCriteriaForRefPermitNo = RevisionSearchCriteria.builder()
					.refPermitNo(revisionRequest.getRevision().getRefPermitNo()).build();
			List<Revision> revisionByRefPermitNo = repository.getRevisionData(revisionSearchCriteriaForRefPermitNo);
			if (Objects.nonNull(revisionByRefPermitNo) && revisionByRefPermitNo.size() > 0) {
				throw new CustomException(
						"Found already existing revision data for given RefPermitNo:"
								+ revisionRequest.getRevision().getRefPermitNo(),
						"Found already existing revision data for given RefPermitNo:"
								+ revisionRequest.getRevision().getRefPermitNo());
			}
		}
	}

	/**
	 * Returns the revision with enriched owners from user service
	 * 
	 * @param criteria    The object containing the parameters on which to search
	 * @param requestInfo The search request's requestInfo
	 * @return List of revision for the given criteria
	 */
	public List<Revision> getRevisionFromCriteria(RevisionSearchCriteria criteria) {
		List<Revision> revisions = repository.getRevisionData(criteria);
		if (revisions.isEmpty())
			return Collections.emptyList();
		return revisions;
	}
	
	/**
	 * Updates the revision
	 * 
	 * @param revisionRequest The update Request
	 * @return Updated Revision
	 */
	@SuppressWarnings("unchecked")
	public Revision update(RevisionRequest revisionRequest) {
		RequestInfo requestInfo = revisionRequest.getRequestInfo();
		String tenantId = revisionRequest.getRevision().getTenantId().split("\\.")[0];
		// Object mdmsData = util.mDMSCall(requestInfo, tenantId);
		// TODO : validations if any
		Revision revision = revisionRequest.getRevision();

		if (revision.getId() == null) {
			throw new CustomException(BPAErrorConstants.UPDATE_ERROR,
					"Revision not found in the System" + revision);
		}

		List<Revision> searchResult = getRevisionsWithId(revisionRequest);
		if (CollectionUtils.isEmpty(searchResult) || searchResult.size() > 1) {
			throw new CustomException(BPAErrorConstants.UPDATE_ERROR,
					"Failed to Update the Application, Found None or multiple revision application!");
		}
		//validate revision should be updated only by the creator--
		String createdBy = searchResult.get(0).getAuditDetails().getCreatedBy();
		String modifiedBy = requestInfo.getUserInfo().getUuid();
		if (!createdBy.equals(modifiedBy)) {
			log.error("only creator could update the revision. createdBy user is:" + createdBy);
			throw new CustomException(BPAErrorConstants.UPDATE_ERROR,
					"Failed to Update the Application, Only creator could update the revision");
		}
		revision.setAuditDetails(searchResult.get(0).getAuditDetails());
		enrichmentService.enrichRevisionUpdateRequest(revisionRequest);
		repository.update(revisionRequest);
		return revisionRequest.getRevision();
	}
	
	/**
	 * Returns Revision from db for the update request
	 * 
	 * @param request The update request
	 * @return List of Revision
	 */
	public List<Revision> getRevisionsWithId(RevisionRequest request) {
		RevisionSearchCriteria criteria = new RevisionSearchCriteria();
		List<String> ids = new LinkedList<>();
		ids.add(request.getRevision().getId());
		criteria.setTenantId(request.getRevision().getTenantId());
		criteria.setIds(ids);
		List<Revision> revisions = repository.getRevisionData(criteria);
		return revisions;
	}

}
