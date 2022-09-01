package org.egov.bpa.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.validation.Valid;

import org.egov.bpa.config.BPAConfiguration;
import org.egov.bpa.repository.BPARepository;
import org.egov.bpa.repository.IdGenRepository;
import org.egov.bpa.util.BPAConstants;
import org.egov.bpa.util.BPAErrorConstants;
import org.egov.bpa.util.BPAUtil;
import org.egov.bpa.web.model.AuditDetails;
import org.egov.bpa.web.model.BPA;
import org.egov.bpa.web.model.BPARequest;
import org.egov.bpa.web.model.DscDetails;
import org.egov.bpa.web.model.NoticeRequest;
import org.egov.bpa.web.model.PreapprovedPlan;
import org.egov.bpa.web.model.PreapprovedPlanRequest;
import org.egov.bpa.web.model.RevisionRequest;
import org.egov.bpa.web.model.Workflow;
import org.egov.bpa.web.model.accreditedperson.AccreditedPersonRequest;
import org.egov.bpa.web.model.idgen.IdResponse;
import org.egov.bpa.web.model.workflow.BusinessService;
import org.egov.bpa.workflow.WorkflowIntegrator;
import org.egov.bpa.workflow.WorkflowService;
import org.egov.common.contract.request.RequestInfo;
import org.egov.common.contract.request.User;
import org.egov.tracer.model.CustomException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.TypeRef;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class EnrichmentService {

	@Autowired
	private BPAConfiguration config;

	@Autowired
	private BPAUtil bpaUtil;

	@Autowired
	private IdGenRepository idGenRepository;

	@Autowired
	private WorkflowService workflowService;

	@Autowired
	private EDCRService edcrService;

	@Autowired
	private WorkflowIntegrator wfIntegrator;

	@Autowired
	private NocService nocService;

	@Autowired
	private BPAUtil util;

	@Autowired
	private UserService userService;
	
	@Autowired
	private BPARepository bpaRepository;

	/**
	 * encrich create BPA Reqeust by adding audidetails and uuids
	 * 
	 * @param bpaRequest
	 * @param mdmsData
	 * @param values
	 */
	public void enrichBPACreateRequest(BPARequest bpaRequest, Object mdmsData, Map<String, String> values) {
		RequestInfo requestInfo = bpaRequest.getRequestInfo();
		AuditDetails auditDetails = bpaUtil.getAuditDetails(requestInfo.getUserInfo().getUuid(), true);
		bpaRequest.getBPA().setAuditDetails(auditDetails);
		bpaRequest.getBPA().setId(UUID.randomUUID().toString());

		bpaRequest.getBPA().setAccountId(bpaRequest.getBPA().getAuditDetails().getCreatedBy());
		String applicationType = values.get(BPAConstants.APPLICATIONTYPE);
		if (applicationType.equalsIgnoreCase(BPAConstants.BUILDING_PLAN)) {
			if (!bpaRequest.getBPA().getRiskType().equalsIgnoreCase(BPAConstants.LOW_RISKTYPE)) {
				bpaRequest.getBPA().setBusinessService(BPAConstants.BPA_MODULE_CODE);
			} else {
				bpaRequest.getBPA().setBusinessService(BPAConstants.BPA_LOW_MODULE_CODE);
			}
		} else {
			bpaRequest.getBPA().setBusinessService(BPAConstants.BPA_OC_MODULE_CODE);
			bpaRequest.getBPA().setLandId(values.get("landId"));
		}
		if (bpaRequest.getBPA().getLandInfo() != null) {
			bpaRequest.getBPA().setLandId(bpaRequest.getBPA().getLandInfo().getId());
		}
		// BPA Documents
		if (!CollectionUtils.isEmpty(bpaRequest.getBPA().getDocuments()))
			bpaRequest.getBPA().getDocuments().forEach(document -> {
				if (document.getId() == null) {
					document.setId(UUID.randomUUID().toString());
				}
			});
		setIdgenIds(bpaRequest);
	}

	/**
	 * Sets the ApplicationNumber for given bpaRequest
	 *
	 * @param request bpaRequest which is to be created
	 */
	private void setIdgenIds(BPARequest request) {
		RequestInfo requestInfo = request.getRequestInfo();
		String tenantId = request.getBPA().getTenantId();
		//String tenantId ="od.cuttack";
		BPA bpa = request.getBPA();
		List<String> applicationNumbers = new ArrayList<>();
        if(BPAConstants.BPA_AC_MODULE_CODE.equalsIgnoreCase(request.getBPA().getBusinessService())) {
        	 applicationNumbers = getIdList(requestInfo, tenantId, config.getApplicationNoIdgenNameforBPA5(),
    				config.getApplicationNoIdgenFormatforBPA5(), 1);
        	 //System.out.println("idgen:"+applicationNumbers);
        }else if(BPAConstants.BPA_PAP_MODULE_CODE.equalsIgnoreCase(request.getBPA().getBusinessService())) {
        	 applicationNumbers = getIdList(requestInfo, tenantId, config.getApplicationNoIdgenNameforBPA6(),
     				config.getApplicationNoIdgenFormatforBPA6(), 1);
        } else {
		 applicationNumbers = getIdList(requestInfo, tenantId, config.getApplicationNoIdgenName(),
				config.getApplicationNoIdgenFormat(), 1);
		 //System.out.println("idgen1:"+applicationNumbers);
        }
		ListIterator<String> itr = applicationNumbers.listIterator();

		Map<String, String> errorMap = new HashMap<>();

		if (!errorMap.isEmpty())
			throw new CustomException(errorMap);

		bpa.setApplicationNo(itr.next());
	}

	/**
	 * Returns a list of numbers generated from idgen
	 *
	 * @param requestInfo RequestInfo from the request
	 * @param tenantId    tenantId of the city
	 * @param idKey       code of the field defined in application properties for
	 *                    which ids are generated for
	 * @param idformat    format in which ids are to be generated
	 * @param count       Number of ids to be generated
	 * @return List of ids generated using idGen service
	 */
	private List<String> getIdList(RequestInfo requestInfo, String tenantId, String idKey, String idformat, int count) {
		List<IdResponse> idResponses = idGenRepository.getId(requestInfo, tenantId, idKey, idformat, count)
				.getIdResponses();

		if (CollectionUtils.isEmpty(idResponses))
			throw new CustomException(BPAErrorConstants.IDGEN_ERROR, "No ids returned from idgen Service");

		return idResponses.stream().map(IdResponse::getId).collect(Collectors.toList());
	}

	/**
	 * enchrich the updateRequest
	 * 
	 * @param bpaRequest
	 * @param businessService
	 */
	public void enrichBPAUpdateRequest(BPARequest bpaRequest, BusinessService businessService) {

		RequestInfo requestInfo = bpaRequest.getRequestInfo();
		AuditDetails auditDetails = bpaUtil.getAuditDetails(requestInfo.getUserInfo().getUuid(), false);
		auditDetails.setCreatedBy(bpaRequest.getBPA().getAuditDetails().getCreatedBy());
		auditDetails.setCreatedTime(bpaRequest.getBPA().getAuditDetails().getCreatedTime());
		bpaRequest.getBPA().getAuditDetails().setLastModifiedTime(auditDetails.getLastModifiedTime());
		enrichAssignes(bpaRequest.getBPA());
		// BPA Documents
		if (!CollectionUtils.isEmpty(bpaRequest.getBPA().getDocuments()))
			bpaRequest.getBPA().getDocuments().forEach(document -> {
				if (document.getId() == null) {
					document.setId(UUID.randomUUID().toString());
				}
			});
		// BPA WfDocuments
		if (!CollectionUtils.isEmpty(bpaRequest.getBPA().getWorkflow().getVarificationDocuments())) {
			bpaRequest.getBPA().getWorkflow().getVarificationDocuments().forEach(document -> {
				if (document.getId() == null) {
					document.setId(UUID.randomUUID().toString());
				}
			});
		}
		// dsc integration after approval-
		List<String> roles = bpaRequest.getRequestInfo().getUserInfo().getRoles().stream().map(role -> role.getCode())
				.collect(Collectors.toList());
		if ((bpaRequest.getBPA().getStatus() != null) && (roles.contains("EMPLOYEE") || roles.contains("BPA_ARC_APPROVER"))
				&& bpaRequest.getBPA().getWorkflow().getAction().equalsIgnoreCase("APPROVE")) {
			List<DscDetails> dscDetailss = new ArrayList<>();
			DscDetails dscDetails = new DscDetails();
			dscDetails.setTenantId(bpaRequest.getBPA().getTenantId());
			dscDetails.setId(UUID.randomUUID().toString());
			dscDetails.setApprovedBy(requestInfo.getUserInfo().getUuid());
			dscDetailss.add(dscDetails);
			bpaRequest.getBPA().setDscDetails(dscDetailss);
		} else {
			bpaRequest.getBPA().setDscDetails(null);
		}

	}

	/**
	 * postStatus encrichment to update the status of the workflow to the
	 * application and generating permit and oc number when applicable
	 * 
	 * @param bpaRequest
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void postStatusEnrichment(BPARequest bpaRequest) {
		BPA bpa = bpaRequest.getBPA();
		String tenantId = bpaRequest.getBPA().getTenantId().split("\\.")[0];
		Object mdmsData = util.mDMSCall(bpaRequest.getRequestInfo(), tenantId);

		BusinessService businessService = workflowService.getBusinessService(bpa, bpaRequest.getRequestInfo(),
				bpa.getApplicationNo());
		log.info("Application status is : " + bpa.getStatus());
		String state = workflowService.getCurrentState(bpa.getStatus(), businessService);

		if (state.equalsIgnoreCase(BPAConstants.DOCVERIFICATION_STATE)) {
			bpa.setApplicationDate(Calendar.getInstance().getTimeInMillis());
		}

		if (StringUtils.isEmpty(bpa.getRiskType())) {
			if (bpa.getBusinessService().equals(BPAConstants.BPA_LOW_MODULE_CODE)) {
				bpa.setRiskType(BPAConstants.LOW_RISKTYPE);
			} else {
				bpa.setRiskType(BPAConstants.OTHER_RISKTYPE);
			}
		}

		log.info("Application state is : " + state);
		this.generateApprovalNo(bpaRequest, state);
		nocService.initiateNocWorkflow(bpaRequest, mdmsData);

	}

	/**
	 * generate the permit and oc number on approval status of the BPA and BPAOC
	 * respectively
	 * 
	 * @param bpaRequest
	 * @param state
	 */
	private void generateApprovalNo(BPARequest bpaRequest, String state) {
		BPA bpa = bpaRequest.getBPA();
		if ((bpa.getBusinessService().equalsIgnoreCase(BPAConstants.BPA_OC_MODULE_CODE)
				&& bpa.getStatus().equalsIgnoreCase(BPAConstants.APPROVED_STATE))
				|| (!bpa.getBusinessService().equalsIgnoreCase(BPAConstants.BPA_OC_MODULE_CODE)
						&& state.equalsIgnoreCase(BPAConstants.APPROVED_STATE))) {
			int vailidityInMonths = config.getValidityInMonths();
			//commenting out below line as approvaldate to be set while digitally signing permit letter-
			//Calendar calendar = Calendar.getInstance();
			//bpa.setApprovalDate(Calendar.getInstance().getTimeInMillis());

			// Adding 3years (36 months) to Current Date
			//calendar.add(Calendar.MONTH, vailidityInMonths);
			Map<String, Object> additionalDetail = null;
			if (bpa.getAdditionalDetails() != null) {
				additionalDetail = (Map) bpa.getAdditionalDetails();
			} else {
				additionalDetail = new HashMap<String, Object>();
				bpa.setAdditionalDetails(additionalDetail);
			}

			//additionalDetail.put("validityDate", calendar.getTimeInMillis());
			List<IdResponse> idResponses = idGenRepository.getId(bpaRequest.getRequestInfo(), bpa.getTenantId(),
					config.getPermitNoIdgenName(), config.getPermitNoIdgenFormat(), 1).getIdResponses();
			bpa.setApprovalNo(idResponses.get(0).getId());
			// if (state.equalsIgnoreCase(BPAConstants.DOCVERIFICATION_STATE)
			// && bpa.getRiskType().toString().equalsIgnoreCase(BPAConstants.LOW_RISKTYPE))
			// {

			Object mdmsData = bpaUtil.mDMSCall(bpaRequest.getRequestInfo(), bpaRequest.getBPA().getTenantId());
			Map<String, String> edcrResponse = new HashMap<>();
			String businessService = bpaRequest.getBPA().getBusinessService();
			if(!(businessService.isEmpty()) && businessService.equalsIgnoreCase(BPAConstants.BPA_PAP_MODULE_CODE)) {
				edcrResponse = edcrService.getEdcrDetailsForPreapprovedPlan(edcrResponse, bpaRequest);
			}else {
			 edcrResponse = edcrService.getEDCRDetails(bpaRequest.getRequestInfo(),
					bpaRequest.getBPA());
			}
			log.debug("applicationType is " + edcrResponse.get(BPAConstants.APPLICATIONTYPE));
			log.debug("serviceType is " + edcrResponse.get(BPAConstants.SERVICETYPE));

			String condeitionsPath = BPAConstants.CONDITIONS_MAP.replace("{1}", BPAConstants.PENDING_APPROVAL_STATE)
					.replace("{2}", bpa.getRiskType().toString())
					.replace("{3}", edcrResponse.get(BPAConstants.SERVICETYPE))
					.replace("{4}", edcrResponse.get(BPAConstants.APPLICATIONTYPE));
			log.debug(condeitionsPath);

			try {
				List<String> conditions = (List<String>) JsonPath.read(mdmsData, condeitionsPath);
				log.debug(conditions.toString());
				if (bpa.getAdditionalDetails() == null) {
					bpa.setAdditionalDetails(new HashMap());
				}
				Map additionalDetails = (Map) bpa.getAdditionalDetails();
				additionalDetails.put(BPAConstants.PENDING_APPROVAL_STATE.toLowerCase(), conditions.get(0));

			} catch (Exception e) {
				log.warn("No approval conditions found for the application " + bpa.getApplicationNo());
			}
			// }
		}
	}

	/**
	 * handles the skippayment of the BPA when demand is zero
	 * 
	 * @param bpaRequest
	 */
	public void skipPayment(BPARequest bpaRequest) {
		BPA bpa = bpaRequest.getBPA();
		BigDecimal demandAmount = bpaUtil.getDemandAmount(bpaRequest);
		if (!(demandAmount.compareTo(BigDecimal.ZERO) > 0)) {
			Workflow workflow = Workflow.builder().action(BPAConstants.ACTION_SKIP_PAY).build();
			bpa.setWorkflow(workflow);
			wfIntegrator.callWorkFlow(bpaRequest);
		}
	}

	/**
	 * encrich create BPA Reqeust by adding audidetails and uuids
	 * 
	 * @param bpaRequest
	 * @param mdmsData
	 * @param values
	 */
	public void enrichBPACreateRequestV2(BPARequest bpaRequest, Object mdmsData, Map<String, String> values,
			LinkedHashMap<String, Object> edcr) {
		log.info(" Inside enrichBPACreateRequestV2 ");
		RequestInfo requestInfo = bpaRequest.getRequestInfo();
		AuditDetails auditDetails = bpaUtil.getAuditDetails(requestInfo.getUserInfo().getUuid(), true);
		bpaRequest.getBPA().setAuditDetails(auditDetails);
		bpaRequest.getBPA().setId(UUID.randomUUID().toString());

		bpaRequest.getBPA().setAccountId(bpaRequest.getBPA().getAuditDetails().getCreatedBy());
		String applicationType = values.get(BPAConstants.APPLICATIONTYPE);
		log.info("applicationType "+applicationType);
		if (applicationType.equalsIgnoreCase(BPAConstants.BUILDING_PLAN)) {
			populateBusinessService(bpaRequest, edcr);

		} else {
			populateBusinessService(bpaRequest, edcr);
			//bpaRequest.getBPA().setBusinessService(BPAConstants.BPA_OC_MODULE_CODE);
			bpaRequest.getBPA().setLandId(values.get("landId"));
		}

		if (bpaRequest.getBPA().getLandInfo() != null) {
			bpaRequest.getBPA().setLandId(bpaRequest.getBPA().getLandInfo().getId());
		}
		// BPA Documents
		if (!CollectionUtils.isEmpty(bpaRequest.getBPA().getDocuments()))
			bpaRequest.getBPA().getDocuments().forEach(document -> {
				if (document.getId() == null) {
					document.setId(UUID.randomUUID().toString());
				}
			});
		setIdgenIds(bpaRequest);
	}
	
	/**
	 * enrich create Preapprovedplan Request by adding auditdetails and uuids
	 * 
	 * @param bpaRequest
	 * @param mdmsData
	 */
	public void enrichPreapprovedPlanCreateRequestV2(PreapprovedPlanRequest request) {
		log.info(" Inside enrichPreapprovedPlanCreateRequestV2 ");
		RequestInfo requestInfo = request.getRequestInfo();
		AuditDetails auditDetails = bpaUtil.getAuditDetails(requestInfo.getUserInfo().getUuid(), true);
		request.getPreapprovedPlan().setAuditDetails(auditDetails);
		request.getPreapprovedPlan().setId(UUID.randomUUID().toString());

		// Documents-
		if (!CollectionUtils.isEmpty(request.getPreapprovedPlan().getDocuments()))
			request.getPreapprovedPlan().getDocuments().forEach(document -> {
				if (document.getId() == null) {
					document.setId(UUID.randomUUID().toString());
				}
			});
		setIdgenIdsForPreapprovedPlan(request);
	}
	
	/**
	 * enrich create Revision Request by adding auditdetails and uuids
	 * 
	 * @param request
	 * @param mdmsData
	 */
	public void enrichRevisionCreateRequest(RevisionRequest request) {
		log.info(" Inside enrichRevisionCreateRequest ");
		RequestInfo requestInfo = request.getRequestInfo();
		AuditDetails auditDetails = bpaUtil.getAuditDetails(requestInfo.getUserInfo().getUuid(), true);
		request.getRevision().setAuditDetails(auditDetails);
		request.getRevision().setId(UUID.randomUUID().toString());

		// Documents-
		if (!CollectionUtils.isEmpty(request.getRevision().getDocuments()))
			request.getRevision().getDocuments().forEach(document -> {
				if (document.getId() == null) {
					document.setId(UUID.randomUUID().toString());
				}
			});
	}
	
	/**
	 * enrich create Accredited Person Request by adding auditdetails and uuids
	 * 
	 * @param request
	 * @param mdmsData
	 */
	public void enrichAccreditedPersonCreateRequest(AccreditedPersonRequest request) {
		log.info(" Inside enrichAccreditedPersonCreateRequest ");
		RequestInfo requestInfo = request.getRequestInfo();
		AuditDetails auditDetails = bpaUtil.getAuditDetails(requestInfo.getUserInfo().getUuid(), true);
		request.getAccreditedPerson().setAuditDetails(auditDetails);
		request.getAccreditedPerson().setId(UUID.randomUUID().toString());
	}

	/**
	 * enchrich the updateRequest
	 * 
	 * @param preapprovedPlanRequest
	 */
	public void enrichPreapprovedPlanUpdateRequest(PreapprovedPlanRequest preapprovedPlanRequest) {

		RequestInfo requestInfo = preapprovedPlanRequest.getRequestInfo();
		AuditDetails auditDetails = bpaUtil.getAuditDetails(requestInfo.getUserInfo().getUuid(), false);
		auditDetails.setCreatedBy(preapprovedPlanRequest.getPreapprovedPlan().getAuditDetails().getCreatedBy());
		auditDetails.setCreatedTime(preapprovedPlanRequest.getPreapprovedPlan().getAuditDetails().getCreatedTime());
		preapprovedPlanRequest.getPreapprovedPlan().getAuditDetails()
				.setLastModifiedTime(auditDetails.getLastModifiedTime());
	}
	
	/**
	 * enchrich the updateRequest
	 * 
	 * @param revisionRequest
	 */
	public void enrichRevisionUpdateRequest(RevisionRequest revisionRequest) {

		RequestInfo requestInfo = revisionRequest.getRequestInfo();
		AuditDetails auditDetails = bpaUtil.getAuditDetails(requestInfo.getUserInfo().getUuid(), false);
		auditDetails.setCreatedBy(revisionRequest.getRevision().getAuditDetails().getCreatedBy());
		auditDetails.setCreatedTime(revisionRequest.getRevision().getAuditDetails().getCreatedTime());
		revisionRequest.getRevision().getAuditDetails().setLastModifiedTime(auditDetails.getLastModifiedTime());
	}

	/**
	 * Sets the ApplicationNumber for given preapprovedPlanRequest
	 *
	 * @param request preapprovedPlanRequest which is to be created
	 */
	private void setIdgenIdsForPreapprovedPlan(PreapprovedPlanRequest preapprovedPlanRequest) {
		RequestInfo requestInfo = preapprovedPlanRequest.getRequestInfo();
		String tenantId = preapprovedPlanRequest.getPreapprovedPlan().getTenantId();

		List<String> applicationNumbers = getIdList(requestInfo, tenantId, config.getDrawingNoIdGenName(),
				config.getDrawingNoIdGenFormat(), 1);
		ListIterator<String> itr = applicationNumbers.listIterator();

		Map<String, String> errorMap = new HashMap<>();

		if (!errorMap.isEmpty())
			throw new CustomException(errorMap);

		preapprovedPlanRequest.getPreapprovedPlan().setDrawingNo(itr.next());
	}

	/**
	 * @param bpaRequest
	 * @param edcr
	 */
	private void populateBusinessService(BPARequest bpaRequest, LinkedHashMap<String, Object> edcr) {
		log.info("populateBusinessService ");
		DocumentContext context = generateERCRContext(edcr);

		// Double plotArea = extractPlotArea(context);

		// Double buildingHeight = extractBuildingHeight(context);

		// boolean isSpecialBuilding = isSpecialBuilding(context);
		
		String businessServiceFromReq = bpaRequest.getBPA().getBusinessService();
		
		String businessServiceFromEdcr = extractBusinessService(context);
		List<String> edcrSuggestedList = Arrays.asList(businessServiceFromEdcr.split("\\|"));
				
		if(StringUtils.hasText(businessServiceFromReq)
				&& !edcrSuggestedList.contains(businessServiceFromReq)) {
			throw new CustomException(BPAErrorConstants.BPA_BUSINESS_SERVICE_ISSUE,
					"Business service is not found in EDCR suggested list.");
		}
		
		if(!StringUtils.hasText(businessServiceFromReq)) {
			bpaRequest.getBPA().setBusinessService(edcrSuggestedList.get(0));
		}
		log.info("businessService "+bpaRequest.getBPA().getBusinessService());
		
		// setBusinessService(bpaRequest, buildingHeight, plotArea, isSpecialBuilding);

	}

	private String extractBusinessService(DocumentContext context) {
		if (null != context) {
			String businessService = null;
			LinkedList<String> businessServiceJSONArray = context.read(BPAConstants.BUSINESS_SERVICE_PATH);
			if (!CollectionUtils.isEmpty(businessServiceJSONArray)) {
				if (null != businessServiceJSONArray.get(0)) {
					businessService = businessServiceJSONArray.get(0).toString();
				}

			}
			return businessService;

		}
		return null;
	}

	/**
	 * @param context
	 * @return
	 */
	private boolean isSpecialBuilding(DocumentContext context) {

		/*
		 * Set<String> specialBuildings = getSpecialBuildings(); String subOccupancyType
		 * = extractSubOccupancyType(context); if (null != subOccupancyType &&
		 * specialBuildings.contains(subOccupancyType)) { return true; }
		 */
		if (null != context) {
			String specialBuilding = null;
			LinkedList<String> specialBuildingJSONArray = context.read(BPAConstants.SPECIAL_BUILDING_PATH);
			if (!CollectionUtils.isEmpty(specialBuildingJSONArray)) {
				if (null != specialBuildingJSONArray.get(0)) {
					specialBuilding = specialBuildingJSONArray.get(0).toString();
					if (null != specialBuilding && specialBuilding.equalsIgnoreCase(BPAConstants.YES)) {
						return true;
					}
				}

			}

		}

		return false;
	}

	/**
	 * @param edcr
	 * @return
	 */
	private DocumentContext generateERCRContext(LinkedHashMap<String, Object> edcr) {
		if (null != edcr) {
			String jsonString = new JSONObject(edcr).toString();
			DocumentContext context = JsonPath.using(Configuration.defaultConfiguration()).parse(jsonString);
			return context;

		}
		return null;
	}

	/**
	 * @param context
	 * @return
	 */
	private Double extractPlotArea(DocumentContext context) {
		TypeRef<List<Double>> typeRef = new TypeRef<List<Double>>() {
		};
		if (null != context) {
			Double plotArea = null;
			List<Double> plotAreas = context.read(BPAConstants.PLOT_AREA_PATH, typeRef);
			if (!CollectionUtils.isEmpty(plotAreas)) {
				if (null != plotAreas.get(0)) {
					String plotAreaString = plotAreas.get(0).toString();
					plotArea = Double.parseDouble(plotAreaString);
				}
			}
			return plotArea;

		}
		return null;
	}

	/**
	 * @param context
	 * @return
	 */
	private String extractSubOccupancyType(DocumentContext context) {
		if (null != context) {
			String subOccupancyType = null;
			LinkedList<String> subOccupancyTypeJSONArray = context.read(BPAConstants.SUB_OCCUPANCY_TYPE_PATH);
			if (!CollectionUtils.isEmpty(subOccupancyTypeJSONArray)) {
				if (null != subOccupancyTypeJSONArray.get(0)) {
					subOccupancyType = subOccupancyTypeJSONArray.get(0).toString();
				}

			}
			return subOccupancyType;

		}
		return null;
	}

	/**
	 * @param context
	 * @return
	 */
	private Double extractBuildingHeight(DocumentContext context) {
		TypeRef<List<Double>> typeRef = new TypeRef<List<Double>>() {
		};
		if (null != context) {
			Double buildingHeight = null;
			List<Double> buildingHeights = context.read(BPAConstants.BUILDING_HEIGHT_PATH, typeRef);
			if (!CollectionUtils.isEmpty(buildingHeights)) {
				if (null != buildingHeights.get(0)) {
					String buildingHeightString = buildingHeights.get(0).toString();
					buildingHeight = Double.parseDouble(buildingHeightString);
				}
			}
			return buildingHeight;

		}
		return null;
	}

	/**
	 * @param bpaRequest
	 * @param buildingHeight
	 * @param plotArea
	 * @param isSpecialBuilding
	 */
	private void setBusinessService(BPARequest bpaRequest, Double buildingHeight, Double plotArea,
			boolean isSpecialBuilding) {
		if (null != buildingHeight && null != plotArea) {
			if (!isSpecialBuilding) {
				if ((buildingHeight <= 10) || (plotArea <= 500)) {
					bpaRequest.getBPA().setBusinessService(BPAConstants.BPA_PA_MODULE_CODE);
				}
				if ((buildingHeight > 10 && buildingHeight <= 15) || (plotArea > 500 && plotArea <= 4047)) {
					bpaRequest.getBPA().setBusinessService(BPAConstants.BPA_PO_MODULE_CODE);
				}
				if ((buildingHeight > 15 && buildingHeight <= 30) || (plotArea > 4047 && plotArea <= 10000)) {
					bpaRequest.getBPA().setBusinessService(BPAConstants.BPA_PM_MODULE_CODE);
				}
				if ((buildingHeight > 30) || (plotArea > 10000)) {
					bpaRequest.getBPA().setBusinessService(BPAConstants.BPA_DP_BP_MODULE_CODE);
				}

			} else {
				if (buildingHeight <= 15) {
					bpaRequest.getBPA().setBusinessService(BPAConstants.BPA_PO_MODULE_CODE);
				} else if (buildingHeight > 15 && buildingHeight <= 30) {
					bpaRequest.getBPA().setBusinessService(BPAConstants.BPA_PM_MODULE_CODE);
				} else if (buildingHeight > 30) {
					bpaRequest.getBPA().setBusinessService(BPAConstants.BPA_DP_BP_MODULE_CODE);
				}

			}

		}
	}

	/**
	 * @return
	 */
	private Set<String> getSpecialBuildings() {
		Set<String> specialBuildings = new HashSet<>();
		specialBuildings.add(BPAConstants.E_IB);
		specialBuildings.add(BPAConstants.E_NPI);
		specialBuildings.add(BPAConstants.E_ITB);
		specialBuildings.add(BPAConstants.E_L);
		specialBuildings.add(BPAConstants.E_FF);
		specialBuildings.add(BPAConstants.E_SF);

		specialBuildings.add(BPAConstants.B_H);
		specialBuildings.add(BPAConstants.B_5S);
		specialBuildings.add(BPAConstants.B_WSt1);
		specialBuildings.add(BPAConstants.B_WSt2);
		specialBuildings.add(BPAConstants.B_WM);

		return specialBuildings;
	}	
/**
	 * In case of SENDBACKTOCITIZEN enrich the assignee with the owners and creator
	 * of BPA
	 * 
	 * @param bpa BPA to be enriched
	 */
	public void enrichAssignes(BPA bpa) {
		Workflow wf = bpa.getWorkflow();
		Set<String> assignes = new HashSet<>();
		// Add assignes from request
		if(bpa.getWorkflow() != null && bpa.getWorkflow().getAssignes() != null) {
			assignes.addAll(bpa.getWorkflow().getAssignes());
		}
		
		if (wf != null && wf.getAction().equalsIgnoreCase(BPAConstants.ACTION_SENDBACKTOCITIZEN)) {

			// Adding owners to assignes list
			bpa.getLandInfo().getOwners().forEach(ownerInfo -> {
				assignes.add(ownerInfo.getUuid());
			});
			// architect should also be able to submit if sent back to citizen
			if (bpa.getAccountId() != null)
				assignes.add(bpa.getAccountId());

			Set<String> registeredUUIDS = userService.getUUidFromUserName(bpa);

			if (!CollectionUtils.isEmpty(registeredUUIDS))
				assignes.addAll(registeredUUIDS);

		} else if ( wf.getAction().equalsIgnoreCase(BPAConstants.ACTION_SEND_TO_CITIZEN)) {
			// Adding owners to assignes list
			bpa.getLandInfo().getOwners().forEach(ownerInfo -> {
				assignes.add(ownerInfo.getUuid());
			});
			Set<String> registeredUUIDS = userService.getUUidFromUserName(bpa);
			if (!CollectionUtils.isEmpty(registeredUUIDS))
				assignes.addAll(registeredUUIDS);
		}
		else if (wf != null && (wf.getAction().equalsIgnoreCase(BPAConstants.ACTION_SEND_TO_ARCHITECT)
				|| (bpa.getStatus().equalsIgnoreCase(BPAConstants.STATUS_CITIZEN_APPROVAL_INPROCESS)
						&& wf.getAction().equalsIgnoreCase(BPAConstants.ACTION_APPROVE)))) {
			// Adding creator of BPA(Licensee)
			if (bpa.getAccountId() != null)
				assignes.add(bpa.getAccountId());
		} 
		/*
		 * commenting out as show cause notice to be replied by any of citizen,architect,accredited person
		else if(wf != null && wf.getAction().equalsIgnoreCase(BPAConstants.ACTION_SHOW_CAUSE)) {
			// get Approver
			List<String> approvers = bpaRepository.getApprover(bpa.getTenantId(), bpa.getApplicationNo());
			if(!CollectionUtils.isEmpty(approvers)) {
				assignes.addAll(approvers);
			}
		}*/
		if(bpa.getWorkflow() == null) {
			Workflow wfNew = new Workflow();
			wfNew.setAssignes(new LinkedList<>(assignes));
			bpa.setWorkflow(wfNew);
		} else {
			bpa.getWorkflow().setAssignes(new LinkedList<>(assignes));
		}
	}

public void enrichScnCreateRequestV2(@Valid NoticeRequest request) {
	
	RequestInfo requestInfo = request.getRequestInfo();
	//requestInfo.setUserInfo(User.builder().uuid("b111659f-7b4c-4cb8-acab-0187407d9d47").build());
	AuditDetails auditDetails = bpaUtil.getAuditDetails(requestInfo.getUserInfo().getUuid(), true);
	Map<String, String> errorMap = new HashMap<>();

	
	if(request.getnotice().getLetterNo()!=null && request.getnotice().getBusinessid()!=null && request.getnotice().getLetterType()!=null
		&&	request.getnotice().getFilestoreid()!=null && request.getnotice().getTenantid()!=null	) {
	request.getnotice().setAuditDetails(auditDetails);
	request.getnotice().setId(UUID.randomUUID().toString());
	}else {
		errorMap.put("NoticeCreateError","please provide valid details to create a  notice.");
	}
	if (!errorMap.isEmpty())
		throw new CustomException(errorMap);
}
}
