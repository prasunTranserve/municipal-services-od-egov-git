package org.egov.noc.thirdparty.fire.service;

import java.io.File;
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
import org.egov.noc.web.model.Noc;
import org.egov.noc.web.model.NocRequest;
import org.egov.noc.web.model.Workflow;
import org.egov.tracer.model.CustomException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

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
	
	private static final ArrayList<String> MANDATORY_ADDITIONAL_DETAILS_THIRDPARTY = new ArrayList<String>() {
		{
			add("buildingType");
			add("fireDistrict");
			add("fireStation");
			add("identityProofType");
			add("identityProofNo");
		}
	};

	@Override
	public String pushProcess(ThirdPartyNOCPushRequestWrapper infoWrapper) {
		// check if noc contains the mandatory fields and documents before pushing to external dept-
		if (!isMandatoryFieldsAndDocumentsPresent(infoWrapper)) {
			String comment = "mandatory fields missing in additionalDetails.Cannot push to external dept";
			return comment;
		}
		// submit fire noc application -
		StringBuilder submitFireNocUrl = new StringBuilder(config.getFireNocHost());
		submitFireNocUrl.append(config.getRecommendationApiEndpoint());
		DocumentContext edcrDetail = infoWrapper.getEdcr();
		Map<String, String> paramMap = getParamsFromEdcr(edcrDetail);

		String sitePlanDoc = getBinaryEncodedDocFromDocuments(infoWrapper, "NOC", "NOC.FIRE.BuildingPlan");
		String plainApplicationDoc = getBinaryEncodedDocFromDocuments(infoWrapper, "NOC",
				"NOC.FIRE.PlainApplication");
		String applicantSignature = getBinaryEncodedDocFromDocuments(infoWrapper, "NOC",
				"NOC.FIRE.ApplicantSignature");
		String applicantPhoto = getBinaryEncodedDocFromDocuments(infoWrapper, "NOC", "NOC.FIRE.ApplicantPhoto");
		String identityProofDoc = getBinaryEncodedDocFromDocuments(infoWrapper, "NOC", "NOC.FIRE.IdentityProofDoc");
		String ownershipDoc = getBinaryEncodedDocFromDocuments(infoWrapper, "NOC", "NOC.FIRE.OwnershipDoc");

		Map<String, String> additionalDetails = (Map<String, String>) infoWrapper.getNoc().getAdditionalDetails();
		SubmitFireNocContract sfnc = SubmitFireNocContract.builder()
				.name(infoWrapper.getUserResponse().getName()).email(infoWrapper.getUserResponse().getEmailId())
				.mobile(infoWrapper.getUserResponse().getMobileNumber()).isOwner("NO")
				.noofBuilding(paramMap.get("noOfBlocks")).buidlingType(nocUtil.getValue(additionalDetails, "thirdPartyNOC.buildingType.id"))
				.buildingName(infoWrapper.getBpa().getLandInfo().getAddress().getBuildingName())
				.proposedOccupany(paramMap.get("occupancyTypeEdcr")).noOfFloor(paramMap.get("noOfStorey"))
				.height(paramMap.get("buildingHeight")).measureType("Mtr")
				.category(paramMap.get("proposedOccupancy")).builtupArea(paramMap.get("builtupAreaEdcr"))
				.areameasureType("Mtr").fireDistrict(nocUtil.getValue(additionalDetails, "thirdPartyNOC.fireDistrict.id"))
				.fireStation(nocUtil.getValue(additionalDetails, "thirdPartyNOC.fireStation.id"))
				.adreesOfBuilding(infoWrapper.getBpa().getLandInfo().getAddress().getCity().split("\\.")[1])
				.buildingPlan(sitePlanDoc).buildingPlanext("pdf")
				.plainApplication(plainApplicationDoc).plainApplicationext("pdf")
				.applicantSignature(applicantSignature).applicantSignatureext("jpg").applicantPhoto(applicantPhoto)
				.applicantPhotoext("jpg").identyProofDoc(identityProofDoc).identyProofDocext("pdf")
				.identyProofType(nocUtil.getValue(additionalDetails, "thirdPartyNOC.identityProofType.id"))
				.identyProofNo(nocUtil.getValue(additionalDetails, "thirdPartyNOC.identityProofNo"))
				.ownershipDoc(ownershipDoc).ownershipDocext("pdf").token(config.getFireNocToken())
				.suyogApplicationId(infoWrapper.getNoc().getApplicationNo()).build();

		Object submitFireNocResponse = serviceRequestRepository.fetchResult(submitFireNocUrl, sfnc);
		if (submitFireNocResponse instanceof Map) {
			Map<String, Object> fireNocResponse = (Map<String, Object>) submitFireNocResponse;
			String responseMessage = String.valueOf(fireNocResponse.get("message"));
			if (!"Recommendation saved successfully".equalsIgnoreCase(responseMessage))
				throw new CustomException("fire noc submission failed", "fire noc submission failed");
		}

		String comment = "Recommendation saved successfully";

		return comment;
	}
	
	private boolean isMandatoryFieldsAndDocumentsPresent(ThirdPartyNOCPushRequestWrapper infoWrapper) {
		Noc noc = infoWrapper.getNoc();
		if (Objects.isNull(noc.getAdditionalDetails()) || !(noc.getAdditionalDetails() instanceof Map)) {
			log.info("additionalDetails null or not a map in the noc");
			return false;
		}
		Map<String, Object> additionalDetails = (Map<String, Object>) noc.getAdditionalDetails();
		if (Objects.isNull(additionalDetails.get("thirdPartyNOC"))
				|| !(additionalDetails.get("thirdPartyNOC") instanceof Map)) {
			log.info("additionalDetails does not contain thirdPartyNOC node and thirdPartyNOC not a Map");
			return false;
		}
		Map<String, Object> thirdPartyNocDetails = (Map<String, Object>) additionalDetails.get("thirdPartyNOC");
		for (String key : MANDATORY_ADDITIONAL_DETAILS_THIRDPARTY) {
			if (Objects.isNull(thirdPartyNocDetails.get(key))) {
				log.info("mandatory key:" + key + " not present inside thirdPartyNOC node of additionalDetails");
				return false;
			}
		}
		// all checks done
		return true;
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
		if ("NOC.FIRE.ApplicantSignature".equalsIgnoreCase(docName)
				|| "NOC.FIRE.ApplicantPhoto".equalsIgnoreCase(docName)) {
			return !CollectionUtils.isEmpty(documents) ? fileStoreService.getBinaryEncodedDocumentAfterPdfToJpg(
					infoWrapper.getUserResponse().getTenantId(), documents.get(0).getFileStoreId()) : "";
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
		fetchStatusUrl.append(config.getRecommendationStatusEndpoint());
		String fireApplicationId=additionalDetails.get("applicationId");
		Object fetchStatusResponse = serviceRequestRepository.fetchResult(fetchStatusUrl,
				new FetchRecommendationStatusContract(config.getFireNocToken(), fireApplicationId));
		String applicationStatus = "";
		String certificate = "";
		if (fetchStatusResponse instanceof Map) {
			Map<String, Object> statusResponse = (Map<String, Object>) fetchStatusResponse;
			Map<String, Object> result = (Map<String, Object>) statusResponse.get("result");
			applicationStatus = String.valueOf(result.get("applicationStatus"));
			certificate = String.valueOf(result.get("certificate"));
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
			String decodedLocalFileName=fileStoreService.getBinaryDecodedDocument(certificate);
			//hardcoded documentType=Fire-NOC
			List<Document> fireNocDoc= fileStoreService.upload(new File(decodedLocalFileName), decodedLocalFileName,
					MediaType.APPLICATION_PDF_VALUE, "NOC", pullRequestWrapper.getNoc().getTenantId(),"Fire-NOC");
			workflow.setDocuments(fireNocDoc);
		}
		}
		else {
			//if applicationid is not present in db then call fetch applicationids and populate applicationid.If it returns null for applicaitonid, it means payment not done and throw exception
			//like the code before
			//two scenarios in in response of fetchapplicationId api- it returns non-null applicationid then set it in db.it returns null then set comment
			//and status=submit
			StringBuilder fetchApplicationIdsUrl = new StringBuilder(config.getFireNocHost());
			fetchApplicationIdsUrl.append(config.getRecommendationIdEndpoint());
			Object fetchApplicationIdsResponse = serviceRequestRepository.fetchResult(fetchApplicationIdsUrl,
					new FetchApplicationIdsContract(config.getFireNocToken(), pullRequestWrapper.getNoc().getApplicationNo()));
			//String str="{\"status\":1,\"message\":\"Application ID\",\"result\":{\"applicationID\":[\"123456\"]}}";
			//Object fetchApplicationIdsResponse = new Gson().fromJson(str, HashMap.class);
			if (fetchApplicationIdsResponse instanceof Map
					&& Objects.nonNull(((Map) fetchApplicationIdsResponse).get("result")) 
					&& !StringUtils.isEmpty(((Map) ((Map) fetchApplicationIdsResponse).get("result")).get("applicationID"))) {
				//store application id in db --
				String applicationId = String
						.valueOf(((Map) ((Map) fetchApplicationIdsResponse).get("result")).get("applicationID"));
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