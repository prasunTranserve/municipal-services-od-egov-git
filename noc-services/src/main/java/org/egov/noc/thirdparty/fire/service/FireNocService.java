package org.egov.noc.thirdparty.fire.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.egov.noc.config.NOCConfiguration;
import org.egov.noc.repository.NOCRepository;
import org.egov.noc.repository.ServiceRequestRepository;
import org.egov.noc.service.FileStoreService;
import org.egov.noc.thirdparty.fire.model.FetchApplicationIdsContract;
import org.egov.noc.thirdparty.fire.model.FetchRecommendationStatusContract;
import org.egov.noc.thirdparty.fire.model.SubmitFireNocContract;
import org.egov.noc.thirdparty.model.ThirdPartyNOCPullRequestWrapper;
import org.egov.noc.thirdparty.model.ThirdPartyNOCPushRequestWrapper;
import org.egov.noc.thirdparty.service.ThirdPartyNocPullService;
import org.egov.noc.thirdparty.service.ThirdPartyNocPushService;
import org.egov.noc.util.NOCConstants;
import org.egov.noc.util.NOCUtil;
import org.egov.noc.web.model.Document;
import org.egov.noc.web.model.NocRequest;
import org.egov.noc.web.model.Workflow;
import org.egov.tracer.model.CustomException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.jayway.jsonpath.DocumentContext;

import lombok.extern.slf4j.Slf4j;
import net.minidev.json.JSONArray;

@Slf4j
@Service(NOCConstants.FIRE_NOC_TYPE)
public class FireNocService implements ThirdPartyNocPushService, ThirdPartyNocPullService {

	@Autowired
	private ObjectMapper mapper;

	@Autowired
	private NOCConfiguration config;

	@Autowired
	ServiceRequestRepository serviceRequestRepository;

	@Autowired
	FileStoreService fileStoreService;
	
	@Autowired
	NOCRepository nocRepository;
	
	@Autowired
	NOCUtil nocUtil;

	private static final String GET_DISTRICTS_ENDPOINT = "/fire_safety/webservices/getFiredistricts";
	private static final String GET_FIRESTATIONS_ENDPOINT = "/fire_safety/webservices/getFirestations";
	private static final String SUBMIT_FIRE_NOC_APPL_ENDPOINT = "/fire_safety/webservices/recommendationApi";
	private static final String FETCH_FIRE_NOC_STATUS_ENDPOINT = "/fire_safety/webservices/recommendationStatus";
	private static final String FETCH_APPLICATION_IDS_ENDPOINT = "/fire_safety/webservices/recommendationID";

	@Override
	public String pushProcess(ThirdPartyNOCPushRequestWrapper infoWrapper) {
		try {
			// TODO--
			// add in config of noc-services the new property fire.host,token

			// submit fire noc application -
			StringBuilder submitFireNocUrl = new StringBuilder(config.getFireNocHost());
			submitFireNocUrl.append(SUBMIT_FIRE_NOC_APPL_ENDPOINT);
			DocumentContext edcrDetail = infoWrapper.getEdcr();
			Map<String, String> paramMap = getParamsFromEdcr(edcrDetail);

			String sitePlanDoc = getBinaryEncodedDocFromDocuments(infoWrapper, "BPA", "APPL.REVSITEPLAN.REVSITEPLAN");
			String plainApplicationDoc = getBinaryEncodedDocFromDocuments(infoWrapper, "NOC",
					"NOC.FIRE.PlainApplication");
			String applicantSignature = getBinaryEncodedDocFromDocuments(infoWrapper, "NOC",
					"NOC.FIRE.ApplicantSignature");
			String applicantPhoto = getBinaryEncodedDocFromDocuments(infoWrapper, "NOC", "NOC.FIRE.ApplicantPhoto");
			String identityProofDoc = getBinaryEncodedDocFromDocuments(infoWrapper, "BPA", "APPL.IDENTITYPROOF");
			String ownershipDoc = getBinaryEncodedDocFromDocuments(infoWrapper, "BPA", "APPL.OWNERIDPROOF");

			Map<String, String> additionalDetails = (Map<String, String>) infoWrapper.getNoc().getAdditionalDetails();
			SubmitFireNocContract sfnc = SubmitFireNocContract.builder()
					.name(infoWrapper.getUserResponse().getUserName()).email(infoWrapper.getUserResponse().getEmailId())
					.mobile(infoWrapper.getUserResponse().getMobileNumber()).isOwner("NO")
					.noofBuilding(paramMap.get("noOfBlocks")).buidlingType(nocUtil.getValue(additionalDetails, "thirdPartyNOC.buildingType.id"))
					.buildingName(infoWrapper.getBpa().getLandInfo().getAddress().getBuildingName())
					.proposedOccupany(paramMap.get("occupancyTypeEdcr")).noOfFloor(paramMap.get("noOfStorey"))
					.height(paramMap.get("buildingHeight")).measureType("Mtr")
					.category(paramMap.get("proposedOccupancy")).builtupArea(paramMap.get("builtupAreaEdcr"))
					.areameasureType("Mtr").fireDistrict(nocUtil.getValue(additionalDetails, "thirdPartyNOC.fireDistrict.id"))
					.fireStation(nocUtil.getValue(additionalDetails, "thirdPartyNOC.fireStation.id"))
					.adreesOfBuilding(mapper.writeValueAsString(infoWrapper.getBpa().getLandInfo().getAddress()))
					.buildingPlan(sitePlanDoc).buildingPlanext("pdf") // to resolve later
					.plainApplication(plainApplicationDoc).plainApplicationext("pdf")
					.applicantSignature(applicantSignature).applicantSignatureext("jpg").applicantPhoto(applicantPhoto)
					.applicantPhotoext("jpg").identyProofDoc(identityProofDoc).identyProofDocext("pdf")
					.ownershipDoc(ownershipDoc).ownershipDocext("pdf").token(config.getFireNocToken()).build();

			Object submitFireNocResponse = serviceRequestRepository.fetchResult(submitFireNocUrl, sfnc);
			if (submitFireNocResponse instanceof Map) {
				Map<String, Object> fireNocResponse = (Map<String, Object>) submitFireNocResponse;
				String responseMessage = String.valueOf(fireNocResponse.get("message"));
				if (!"Recommendation saved successfully".equalsIgnoreCase(responseMessage))
					throw new CustomException("fire noc submission failed", "fire noc submission failed");
			}

			String comment = "Recommendation saved successfully";

			return comment;
		} catch (JsonProcessingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new CustomException("error while parsing json", "error while parsing json");
		}
	}

	private Map<String, String> getParamsFromEdcr(DocumentContext edcrDetail) {
		Map<String, String> paramMap = new HashMap<>();
		List<Object> blocks = edcrDetail.read("edcrDetail.*.planDetail.blocks");
		String noOfBlocks = String.valueOf(blocks.size());
		blocks = null;
		JSONArray occupancyTypeJSONArray = edcrDetail.read("edcrDetail.*.planDetail.planInformation.occupancy");
		String occupancyTypeEdcr = !CollectionUtils.isEmpty(occupancyTypeJSONArray)
				&& (null != occupancyTypeJSONArray.get(0)) ? occupancyTypeJSONArray.get(0).toString() : "";
		String proposedOccupancy = "Residential".equalsIgnoreCase(occupancyTypeEdcr) ? "1" : "2";
		JSONArray noOfStoreyArray = edcrDetail.read("edcrDetail.*.planDetail.planInformation.numberOfStoreys");
		String noOfStorey = !CollectionUtils.isEmpty(noOfStoreyArray) && (null != noOfStoreyArray.get(0))
				? noOfStoreyArray.get(0).toString()
				: "";
		JSONArray buildingHeightArray = edcrDetail.read("edcrDetail.*.planDetail.virtualBuilding.buildingHeight");
		String buildingHeight = !CollectionUtils.isEmpty(buildingHeightArray) && (null != buildingHeightArray.get(0))
				? buildingHeightArray.get(0).toString()
				: "";
		JSONArray builtUpAreaArray = edcrDetail.read("edcrDetail.*.planDetail.virtualBuilding.totalBuitUpArea");
		String builtupAreaEdcr = !CollectionUtils.isEmpty(builtUpAreaArray) && (null != builtUpAreaArray.get(0))
				? builtUpAreaArray.get(0).toString()
				: "";
		paramMap.put("noOfBlocks", noOfBlocks);
		paramMap.put("proposedOccupancy", proposedOccupancy);
		paramMap.put("occupancyTypeEdcr", occupancyTypeEdcr);
		paramMap.put("noOfStorey", noOfStorey);
		paramMap.put("buildingHeight", buildingHeight);
		paramMap.put("builtupAreaEdcr", builtupAreaEdcr);
		return paramMap;
	}

	private String getBinaryEncodedDocFromDocuments(ThirdPartyNOCPushRequestWrapper infoWrapper, String typeOfDoc,
			String docName) {
		List<Document> documents = new ArrayList<>();
		switch (typeOfDoc) {
		case "NOC":
			documents = infoWrapper.getNoc().getDocuments().stream()
					.filter(doc -> doc.getDocumentType().contains(docName)).collect(Collectors.toList());
			break;
		case "BPA":
			documents = infoWrapper.getBpa().getDocuments().stream()
					.filter(doc -> doc.getDocumentType().contains(docName)).collect(Collectors.toList());
			break;
		}
		return !CollectionUtils.isEmpty(documents) ? fileStoreService.getBinaryEncodedDocument(
				infoWrapper.getUserResponse().getTenantId(), documents.get(0).getFileStoreId()) : "";
	}

	@Override
	public Workflow pullProcess(ThirdPartyNOCPullRequestWrapper pullRequestWrapper) {
		Workflow workflow = new Workflow();
		//step1. check if applicationId is present or not in db--
		Map<String, String> additionalDetails = (Map<String, String>) pullRequestWrapper.getNoc().getAdditionalDetails();
		if(Objects.nonNull(additionalDetails.get("applicationId"))) {
		//step2. if applicationId is there, then call status API below-
		// recommendationStatus API--
		StringBuilder fetchStatusUrl = new StringBuilder(config.getFireNocHost());
		fetchStatusUrl.append(FETCH_FIRE_NOC_STATUS_ENDPOINT);
		Object fetchStatusResponse = serviceRequestRepository.fetchResult(fetchStatusUrl,
				new FetchRecommendationStatusContract(config.getFireNocToken(), null));
		String applicationStatus = "";
		if (fetchStatusResponse instanceof Map) {
			Map<String, Object> statusResponse = (Map<String, Object>) fetchStatusResponse;
			Map<String, Object> result = (Map<String, Object>) statusResponse.get("result");
			applicationStatus = String.valueOf(result.get("applicationStatus"));
			if (!"In Process".equalsIgnoreCase(applicationStatus))
				throw new CustomException(NOCConstants.FIRE_NOC_ERROR, "fire noc failed");
		}
		
		switch (applicationStatus) {
		case "In Process":
			throw new CustomException(NOCConstants.FIRE_NOC_ERROR, "already in process");
		case "Rejected":
			workflow.setAction(NOCConstants.ACTION_REJECT);
			workflow.setComment("noc rejected by fire department");
		case "Approved Recommendation Issued":
			workflow.setAction(NOCConstants.ACTION_APPROVE);
			workflow.setComment("noc approved by fire department");
		}
		}
		else {
			//if applicationid is not present in db then call fetch applicationids and populate applicationid.If it returns null for applicaitonid, it means payment not done and throw exception
			//like the code before
			//two scenarios in in response of fetchapplicationId api- it returns non-null applicationid then set it in db.it returns null then set comment
			//and status=submit
			StringBuilder fetchApplicationIdsUrl = new StringBuilder(config.getFireNocHost());
			fetchApplicationIdsUrl.append(FETCH_APPLICATION_IDS_ENDPOINT);
			Object fetchApplicationIdsResponse = serviceRequestRepository.fetchResult(fetchApplicationIdsUrl,
					new FetchApplicationIdsContract(config.getFireNocToken(), pullRequestWrapper.getUserResponse().getEmailId()));
			//String str="{\"status\":1,\"message\":\"Application ID\",\"result\":{\"applicationID\":[\"123456\"]}}";
			//Object fetchApplicationIdsResponse = new Gson().fromJson(str, HashMap.class);
			if(fetchApplicationIdsResponse instanceof Map && Objects.nonNull(((Map) fetchApplicationIdsResponse).get("result"))
					&& Objects.nonNull(((Map) ((Map) fetchApplicationIdsResponse).get("result")).get("applicationID"))) {
				//store application id in db --
				List<String> applicationIds = (List) ((Map) ((Map) fetchApplicationIdsResponse).get("result")).get("applicationID");
				String applicationId=applicationIds.get(0);//hardcoded as first applicationId as of now
				additionalDetails.put("applicationId", applicationId);/*
				try {
					pullRequestWrapper.getNoc().setAdditionalDetails(mapper.writeValueAsString(additionalDetails));
				} catch (JsonProcessingException e) {
					e.printStackTrace();
				}*/
				workflow.setComment("applicationId set in database:"+applicationId);
				NocRequest nocRequest = new NocRequest();
				nocRequest.setNoc(pullRequestWrapper.getNoc());
				nocRequest.setRequestInfo(pullRequestWrapper.getRequestInfo());
				nocRepository.update(nocRequest, false);
			}else {
				workflow.setComment("applicationId is null in applicationids API");
			}
			workflow.setAction(NOCConstants.ACTION_SUBMIT);
		}

		return workflow;
	}

}