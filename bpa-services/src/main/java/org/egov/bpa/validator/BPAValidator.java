package org.egov.bpa.validator;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.egov.bpa.config.BPAConfiguration;
import org.egov.bpa.service.EDCRService;
import org.egov.bpa.util.BPAConstants;
import org.egov.bpa.web.model.BPA;
import org.egov.bpa.web.model.BPARequest;
import org.egov.bpa.web.model.BPASearchCriteria;
import org.egov.bpa.web.model.Document;
import org.egov.common.contract.request.RequestInfo;
import org.egov.tracer.model.CustomException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class BPAValidator {

	@Autowired
	private MDMSValidator mdmsValidator;

	@Autowired
	private BPAConfiguration config;
	
	@Autowired
	private EDCRService edcrService;

	public void validateCreate(BPARequest bpaRequest, Object mdmsData, Map<String, String> values) {
		mdmsValidator.validateMdmsData(bpaRequest, mdmsData);
		validateApplicationDocuments(bpaRequest, mdmsData, null, values);
	}


	private void validateApplicationDocuments(BPARequest request, Object mdmsData, String currentState, Map<String, String> values) {
		Map<String, List<String>> masterData = mdmsValidator.getAttributeValues(mdmsData);
		BPA bpa = request.getBPA();

		if (!bpa.getWorkflow().getAction().equalsIgnoreCase(BPAConstants.ACTION_REJECT)
				&& !bpa.getWorkflow().getAction().equalsIgnoreCase(BPAConstants.ACTION_ADHOC)
				&& !bpa.getWorkflow().getAction().equalsIgnoreCase(BPAConstants.ACTION_PAY)) {

			String applicationType = values.get(BPAConstants.APPLICATIONTYPE);
			String serviceType = values.get(BPAConstants.SERVICETYPE);
			
			String filterExp = "$.[?(@.applicationType=='" + applicationType + "' && @.ServiceType=='"
					+ serviceType + "' && @.RiskType=='" + bpa.getRiskType() + "' && @.WFState=='"
					+ currentState + "')].docTypes";
			
			List<Object> docTypeMappings = JsonPath.read(masterData.get(BPAConstants.DOCUMENT_TYPE_MAPPING), filterExp);

			List<Document> allDocuments = new ArrayList<Document>();
			if (bpa.getDocuments() != null) {
				allDocuments.addAll(bpa.getDocuments());
			}

			if (CollectionUtils.isEmpty(docTypeMappings)) {
				return;
			}

			filterExp = "$.[?(@.required==true)].code";
			List<String> requiredDocTypes = JsonPath.read(docTypeMappings.get(0), filterExp);

			List<String> validDocumentTypes = masterData.get(BPAConstants.DOCUMENT_TYPE);

			if (!CollectionUtils.isEmpty(allDocuments)) {

				allDocuments.forEach(document -> {

					if (!validDocumentTypes.contains(document.getDocumentType())) {
						throw new CustomException("BPA_UNKNOWN_DOCUMENTTYPE",
								document.getDocumentType() + " is Unkown");
					}
				});

				if (requiredDocTypes.size() > 0 && allDocuments.size() < requiredDocTypes.size()) {

					throw new CustomException("BPA_MDNADATORY_DOCUMENTPYE_MISSING",
							requiredDocTypes.size() + " Documents are requied ");
				} else if (requiredDocTypes.size() > 0) {

					List<String> addedDocTypes = new ArrayList<String>();
					allDocuments.forEach(document -> {

						String docType = document.getDocumentType();
						int lastIndex = docType.lastIndexOf(".");
						String documentNs = "";
						if (lastIndex > 1) {
							documentNs = docType.substring(0, lastIndex);
						} else if (lastIndex == 1) {
							throw new CustomException("BPA_INVALID_DOCUMENTTYPE",
									document.getDocumentType() + " is Invalid");
						} else {
							documentNs = docType;
						}

						addedDocTypes.add(documentNs);
					});
					requiredDocTypes.forEach(docType -> {
						String docType1 = docType.toString();
						if (!addedDocTypes.contains(docType1)) {
							throw new CustomException("BPA_MDNADATORY_DOCUMENTPYE_MISSING",
									"Document Type " + docType1 + " is Missing");
						}
					});
				}
			} else if (requiredDocTypes.size() > 0) {
				throw new CustomException("BPA_MDNADATORY_DOCUMENTPYE_MISSING",
						"Atleast " + requiredDocTypes.size() + " Documents are requied ");
			}
			bpa.setDocuments(allDocuments);
		}

	}

	private void validateDuplicateDocuments(BPARequest request) {
		if (request.getBPA().getDocuments() != null) {
			List<String> documentFileStoreIds = new LinkedList<String>();
			request.getBPA().getDocuments().forEach(document -> {
				if (documentFileStoreIds.contains(document.getFileStoreId()))
					throw new CustomException("BPA_DUPLICATE_DOCUMENT", "Same document cannot be used multiple times");
				else
					documentFileStoreIds.add(document.getFileStoreId());
			});
		}
	}

	/**
	 * Validates if the search parameters are valid
	 * 
	 * @param requestInfo
	 *            The requestInfo of the incoming request
	 * @param criteria
	 *            The BPASearch Criteria
	 */
//TODO need to make the changes in the data
	public void validateSearch(RequestInfo requestInfo, BPASearchCriteria criteria) {
		if (!requestInfo.getUserInfo().getType().equalsIgnoreCase(BPAConstants.CITIZEN) && criteria.isEmpty())
			throw new CustomException(BPAConstants.INVALID_SEARCH, "Search without any paramters is not allowed");

		if (!requestInfo.getUserInfo().getType().equalsIgnoreCase(BPAConstants.CITIZEN) && !criteria.tenantIdOnly()
				&& criteria.getTenantId() == null)
			throw new CustomException(BPAConstants.INVALID_SEARCH, "TenantId is mandatory in search");

		if (requestInfo.getUserInfo().getType().equalsIgnoreCase(BPAConstants.CITIZEN) && !criteria.isEmpty()
				&& !criteria.tenantIdOnly() && criteria.getTenantId() == null)
			throw new CustomException(BPAConstants.INVALID_SEARCH, "TenantId is mandatory in search");

		String allowedParamStr = null;

		if (requestInfo.getUserInfo().getType().equalsIgnoreCase(BPAConstants.CITIZEN))
			allowedParamStr = config.getAllowedCitizenSearchParameters();
		else if (requestInfo.getUserInfo().getType().equalsIgnoreCase(BPAConstants.EMPLOYEE))
			allowedParamStr = config.getAllowedEmployeeSearchParameters();
		else
			throw new CustomException(BPAConstants.INVALID_SEARCH,
					"The userType: " + requestInfo.getUserInfo().getType() + " does not have any search config");

		if (StringUtils.isEmpty(allowedParamStr) && !criteria.isEmpty())
			throw new CustomException(BPAConstants.INVALID_SEARCH, "No search parameters are expected");
		else {
			List<String> allowedParams = Arrays.asList(allowedParamStr.split(","));
			validateSearchParams(criteria, allowedParams);
		}
	}

	/**
	 * Validates if the paramters coming in search are allowed
	 * 
	 * @param criteria
	 *            BPA search criteria
	 * @param allowedParams
	 *            Allowed Params for search
	 */
	private void validateSearchParams(BPASearchCriteria criteria, List<String> allowedParams) {

		if (criteria.getApplicationNo() != null && !allowedParams.contains("applicationNo"))
			throw new CustomException(BPAConstants.INVALID_SEARCH, "Search on applicationNo is not allowed");

		if (criteria.getEdcrNumber() != null && !allowedParams.contains("edcrNumber"))
			throw new CustomException(BPAConstants.INVALID_SEARCH, "Search on edcrNumber is not allowed");

		if (criteria.getStatus() != null && !allowedParams.contains("status"))
			throw new CustomException(BPAConstants.INVALID_SEARCH, "Search on Status is not allowed");

		if (criteria.getIds() != null && !allowedParams.contains("ids"))
			throw new CustomException(BPAConstants.INVALID_SEARCH, "Search on ids is not allowed");

		if (criteria.getMobileNumber() != null && !allowedParams.contains("mobileNumber"))
			throw new CustomException(BPAConstants.INVALID_SEARCH, "Search on mobileNumber is not allowed");

		if (criteria.getOffset() != null && !allowedParams.contains("offset"))
			throw new CustomException(BPAConstants.INVALID_SEARCH, "Search on offset is not allowed");

		if (criteria.getLimit() != null && !allowedParams.contains("limit"))
			throw new CustomException(BPAConstants.INVALID_SEARCH, "Search on limit is not allowed");
		
		if (criteria.getApprovalDate() != null && (criteria.getApprovalDate() > new Date().getTime()))
			throw new CustomException(BPAConstants.INVALID_SEARCH, "Permit Order Genarated date cannot be a future date");
		
		if (criteria.getFromDate() != null && (criteria.getFromDate() > new Date().getTime()))
			throw new CustomException(BPAConstants.INVALID_SEARCH, "From date cannot be a future date");

		if (criteria.getToDate() != null && criteria.getFromDate() != null
				&& (criteria.getFromDate() > criteria.getToDate()))
			throw new CustomException(BPAConstants.INVALID_SEARCH, "To date cannot be prior to from date");
	}

	public void validateUpdate(BPARequest bpaRequest, List<BPA> searchResult, Object mdmsData, String currentState, Map<String, String> edcrResponse) {

		BPA bpa = bpaRequest.getBPA();
		validateApplicationDocuments(bpaRequest, mdmsData, currentState, edcrResponse);
		validateAllIds(searchResult, bpa);
		mdmsValidator.validateMdmsData(bpaRequest, mdmsData);
		validateDuplicateDocuments(bpaRequest);
		setFieldsFromSearch(bpaRequest, searchResult, mdmsData);

	}

	private void setFieldsFromSearch(BPARequest bpaRequest, List<BPA> searchResult, Object mdmsData) {
		Map<String, BPA> idToBPAFromSearch = new HashMap<>();

		searchResult.forEach(bpa -> {
			idToBPAFromSearch.put(bpa.getId(), bpa);
		});

		bpaRequest.getBPA().getAuditDetails()
				.setCreatedBy(idToBPAFromSearch.get(bpaRequest.getBPA().getId()).getAuditDetails().getCreatedBy());
		bpaRequest.getBPA().getAuditDetails()
				.setCreatedTime(idToBPAFromSearch.get(bpaRequest.getBPA().getId()).getAuditDetails().getCreatedTime());
		bpaRequest.getBPA().setStatus(idToBPAFromSearch.get(bpaRequest.getBPA().getId()).getStatus());
	}



	private void validateAllIds(List<BPA> searchResult, BPA bpa) {

		Map<String, BPA> idToBPAFromSearch = new HashMap<>();
		searchResult.forEach(bpas -> {
			idToBPAFromSearch.put(bpas.getId(), bpas);
		});

		Map<String, String> errorMap = new HashMap<>();
		BPA searchedBpa = idToBPAFromSearch.get(bpa.getId());

		if (!searchedBpa.getApplicationNo().equalsIgnoreCase(bpa.getApplicationNo()))
			errorMap.put("INVALID UPDATE", "The application number from search: " + searchedBpa.getApplicationNo()
					+ " and from update: " + bpa.getApplicationNo() + " does not match");

		if (!searchedBpa.getId().equalsIgnoreCase(bpa.getId()))
			errorMap.put("INVALID UPDATE", "The id " + bpa.getId() + " does not exist");




		if (!CollectionUtils.isEmpty(errorMap))
			throw new CustomException(errorMap);
	}




	public void validateCheckList(Object mdmsData, BPARequest bpaRequest, String wfState) {
		BPA bpa = bpaRequest.getBPA();
		Map<String, String> edcrResponse = edcrService.getEDCRDetails(bpaRequest.getRequestInfo(), bpaRequest.getBPA());
		log.info("applicationType is " + edcrResponse.get(BPAConstants.APPLICATIONTYPE));
        log.info("serviceType is " + edcrResponse.get(BPAConstants.SERVICETYPE));
        
		validateQuestions(mdmsData, bpa, wfState, edcrResponse);
		validateDocTypes(mdmsData, bpa, wfState, edcrResponse);
	}

	@SuppressWarnings(value = { "unchecked", "rawtypes" })
	private void validateQuestions(Object mdmsData, BPA bpa, String wfState, Map<String, String> edcrResponse) {
		List<String> mdmsQns = null;

		log.info("Fetching MDMS result for the state " + wfState);

		try {
			String questionsPath = BPAConstants.QUESTIONS_MAP.replace("{1}", wfState)
					.replace("{2}", bpa.getRiskType().toString()).replace("{3}", edcrResponse.get(BPAConstants.SERVICETYPE))
					.replace("{4}", edcrResponse.get(BPAConstants.APPLICATIONTYPE));;

			List<Object> mdmsQuestionsArray = (List<Object>) JsonPath.read(mdmsData, questionsPath);

			if (!CollectionUtils.isEmpty(mdmsQuestionsArray))
				mdmsQns = JsonPath.read(mdmsQuestionsArray.get(0), BPAConstants.QUESTIONS_PATH);

			log.info("MDMS questions " + mdmsQns);
			if (!CollectionUtils.isEmpty(mdmsQns)) {
				if (bpa.getAdditionalDetails() != null) {
					List checkListFromReq = (List) ((Map) bpa.getAdditionalDetails()).get(wfState.toLowerCase());
					if (!CollectionUtils.isEmpty(checkListFromReq)) {
						for (int i = 0; i < checkListFromReq.size(); i++) {
							List<Map> requestCheckList = new ArrayList<Map>();
							List<String> requestQns = new ArrayList<String>();
							validateDateTime((Map)checkListFromReq.get(i));
							List<Map> questions = ((Map) checkListFromReq.get(i))
									.get(BPAConstants.QUESTIONS_TYPE) != null
											? (List<Map>) ((Map) checkListFromReq.get(i))
													.get(BPAConstants.QUESTIONS_TYPE)
											: null;
							if (questions != null)
								requestCheckList.addAll(questions);
							if (!CollectionUtils.isEmpty(requestCheckList)) {
								for (Map reqQn : requestCheckList) {
									requestQns.add((String) reqQn.get(BPAConstants.QUESTION_TYPE));
								}
							}

							log.info("Request questions " + requestQns);

							if (!CollectionUtils.isEmpty(requestQns)) {
								if (requestQns.size() < mdmsQns.size())
									throw new CustomException("BPA_UNKNOWN_QUESTIONS",
											"Please answer all the questions " + StringUtils.join(mdmsQns, ","));
								else {
									List<String> pendingQns = new ArrayList<String>();
									for (String qn : mdmsQns) {
										if (!requestQns.contains(qn)) {
											pendingQns.add(qn);
										}
									}
									if (pendingQns.size() > 0) {
										throw new CustomException("BPA_UNKNOWN_QUESTIONS",
												"Please answer " + StringUtils.join(pendingQns, ","));
									}
								}
							} else {
								throw new CustomException("BPA_UNKNOWN_QUESTIONS",
										"Please answer the required questions");
							}
						}
					} else {
						throw new CustomException("BPA_UNKNOWN_QUESTIONS", "Please answer the required questions");
					}
				} else {
					throw new CustomException("BPA_UNKNOWN_QUESTIONS", "Please answer the required questions");
				}
			}
		} catch (PathNotFoundException ex) {
			log.error("Exception occured while validating the Checklist Questions" + ex.getMessage());
		}
	}

	@SuppressWarnings(value = { "unchecked", "rawtypes" })
	private void validateDocTypes(Object mdmsData, BPA bpa, String wfState, Map<String, String> edcrResponse) {
		List<String> mdmsDocs = null;

		log.info("Fetching MDMS result for the state " + wfState);

		try {
			String docTypesPath = BPAConstants.DOCTYPES_MAP.replace("{1}", wfState)
					.replace("{2}", bpa.getRiskType().toString()).replace("{3}", edcrResponse.get(BPAConstants.SERVICETYPE))
					.replace("{4}", edcrResponse.get(BPAConstants.APPLICATIONTYPE));;

			List<Object> docTypesArray = (List<Object>) JsonPath.read(mdmsData, docTypesPath);

			if (!CollectionUtils.isEmpty(docTypesArray))
				mdmsDocs = JsonPath.read(docTypesArray.get(0), BPAConstants.DOCTYPESS_PATH);

			log.info("MDMS DocTypes " + mdmsDocs);
			if (!CollectionUtils.isEmpty(mdmsDocs)) {
				if (bpa.getAdditionalDetails() != null) {
					List checkListFromReq = (List) ((Map) bpa.getAdditionalDetails()).get(wfState.toLowerCase());
					if (!CollectionUtils.isEmpty(checkListFromReq)) {
						for (int i = 0; i < checkListFromReq.size(); i++) {
							List<Map> requestCheckList = new ArrayList<Map>();
							List<String> requestDocs = new ArrayList<String>();
							List<Map> docs = ((Map) checkListFromReq.get(i)).get(BPAConstants.DOCS) != null
									? (List<Map>) ((Map) checkListFromReq.get(i)).get(BPAConstants.DOCS) : null;
							if (docs != null)
								requestCheckList.addAll(docs);
							
							if (!CollectionUtils.isEmpty(requestCheckList)) {
								for (Map reqDoc : requestCheckList) {
									String fileStoreId = ((String) reqDoc.get(BPAConstants.FILESTOREID));
									if (!StringUtils.isEmpty(fileStoreId)) {
										String docType = (String) reqDoc.get(BPAConstants.CODE);
										int lastIndex = docType.lastIndexOf(".");
										String documentNs = "";
										if (lastIndex > 1) {
											documentNs = docType.substring(0, lastIndex);
										} else if (lastIndex == 1) {
											throw new CustomException("BPA_INVALID_DOCUMENTTYPE",
													(String) reqDoc.get(BPAConstants.CODE) + " is Invalid");
										} else {
											documentNs = docType;
										}
										requestDocs.add(documentNs);
									} else {
										throw new CustomException("BPA_UNKNOWN_DOCS",
												"fileStoreId is not exists for the documents");
									}
								}
							}

							log.info("Request Docs " + requestDocs);

							if (!CollectionUtils.isEmpty(requestDocs)) {
								if (requestDocs.size() < mdmsDocs.size())
									throw new CustomException("BPA_UNKNOWN_DOCS",
											"Please upload all the required docs " + StringUtils.join(mdmsDocs, ","));
								else {
									List<String> pendingDocs = new ArrayList<String>();
									for (String doc : mdmsDocs) {
										if (!requestDocs.contains(doc)) {
											pendingDocs.add(doc);
										}
									}
									if (pendingDocs.size() > 0) {
										throw new CustomException("BPA_UNKNOWN_DOCS",
												"Please upload " + StringUtils.join(pendingDocs, ","));
									}
								}
							} else {
								throw new CustomException("BPA_UNKNOWN_DOCS", "Please upload required Documents");
							}
						}
					} else {
						throw new CustomException("BPA_UNKNOWN_DOCS", "Please upload required Documents");
					}
				} else {
					throw new CustomException("BPA_UNKNOWN_DOCS", "Please upload required Documents");
				}
			}
		} catch (PathNotFoundException ex) {
			log.error("Exception occured while validating the Checklist Documents" + ex.getMessage());
		}
	}
	
	private void validateDateTime(@SuppressWarnings("rawtypes") Map checkListFromRequest) {

		if (checkListFromRequest.get(BPAConstants.INSPECTION_DATE) == null
				|| StringUtils.isEmpty(checkListFromRequest.get(BPAConstants.INSPECTION_DATE).toString())) {
			throw new CustomException("BPA_UNKNOWN_DATE", "Please mention the inspection date");
		} else {
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
			Date dt;
			try {
				dt = sdf.parse(checkListFromRequest.get(BPAConstants.INSPECTION_DATE).toString());
				long inspectionEpoch = dt.getTime();
				if (inspectionEpoch > new Date().getTime()) {
					throw new CustomException("BPA_UNKNOWN_DATE", "Inspection date cannot be a future date");
				} else if (inspectionEpoch < 0) {
					throw new CustomException("BPA_UNKNOWN_DATE", "Provide the date in specified format 'yyyy-MM-dd'");
				}
			} catch (ParseException e) {
				throw new CustomException("BPA_UNKNOWN_DATE", "Unable to parase the inspection date");
			}
		}
		if (checkListFromRequest.get(BPAConstants.INSPECTION_TIME) == null
				|| StringUtils.isEmpty(checkListFromRequest.get(BPAConstants.INSPECTION_TIME).toString())) {
			throw new CustomException("BPA_UNKNOWN_TIME", "Please mention the inspection time");
		}
	}


	


	
}
