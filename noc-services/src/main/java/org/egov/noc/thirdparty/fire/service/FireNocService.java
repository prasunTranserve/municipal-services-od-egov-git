package org.egov.noc.thirdparty.fire.service;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.egov.noc.config.NOCConfiguration;
import org.egov.noc.repository.ServiceRequestRepository;
import org.egov.noc.service.FileStoreService;
import org.egov.noc.thirdparty.fire.model.FetchRecommendationStatusContract;
import org.egov.noc.thirdparty.fire.model.SubmitFireNocContract;
import org.egov.noc.thirdparty.model.ThirdPartyNOCRequestInfoWrapper;
import org.egov.noc.thirdparty.service.ThirdPartyNocService;
import org.egov.noc.util.NOCConstants;
import org.egov.noc.web.model.Document;
import org.egov.tracer.model.CustomException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import com.jayway.jsonpath.DocumentContext;

import lombok.extern.slf4j.Slf4j;
import net.minidev.json.JSONArray;

@Slf4j
@Service(NOCConstants.FIRE_NOC_TYPE)
public class FireNocService implements ThirdPartyNocService{
	
	@Autowired
	private ObjectMapper mapper;
	
	@Autowired
	private NOCConfiguration config;
	
	@Autowired
	ServiceRequestRepository serviceRequestRepository;
	
	@Autowired
	FileStoreService fileStoreService;
	
	private static final String GET_DISTRICTS_ENDPOINT= "/fire_safety/webservices/getFiredistricts";
	private static final String GET_FIRESTATIONS_ENDPOINT = "/fire_safety/webservices/getFirestations";
	private static final String SUBMIT_FIRE_NOC_APPL_ENDPOINT =  "/fire_safety/webservices/recommendationApi";
	private static final String FETCH_FIRE_NOC_STATUS_ENDPOINT = "/fire_safety/webservices/recommendationStatus";

	@Override
	public String process(ThirdPartyNOCRequestInfoWrapper infoWrapper) {
		try {
		//TODO--
		//add in config of noc-services the new property fire.host,token
		
		//submit fire noc application - 
		StringBuilder submitFireNocUrl = new StringBuilder(config.getFireNocHost());
		submitFireNocUrl.append(SUBMIT_FIRE_NOC_APPL_ENDPOINT);
		DocumentContext edcrDetail = infoWrapper.getEdcr();
		Map<String, String> paramMap = getParamsFromEdcr(edcrDetail);
		
		String sitePlanDoc = getBinaryEncodedDocFromDocuments(infoWrapper, "BPA", "APPL.REVSITEPLAN.REVSITEPLAN");
		String plainApplicationDoc = getBinaryEncodedDocFromDocuments(infoWrapper, "NOC", "NOC.FIRE.PlainApplication");
		String applicantSignature = getBinaryEncodedDocFromDocuments(infoWrapper, "NOC", "NOC.FIRE.ApplicantSignature");
		String applicantPhoto = getBinaryEncodedDocFromDocuments(infoWrapper, "NOC", "NOC.FIRE.ApplicantPhoto");
		String identityProofDoc = getBinaryEncodedDocFromDocuments(infoWrapper, "BPA", "APPL.IDENTITYPROOF");
		String ownershipDoc = getBinaryEncodedDocFromDocuments(infoWrapper, "BPA", "APPL.OWNERIDPROOF");
		
			SubmitFireNocContract sfnc = SubmitFireNocContract.builder().name(infoWrapper.getUserResponse().getUserName())
					.email(infoWrapper.getUserResponse().getEmailId())
					.mobile(infoWrapper.getUserResponse().getMobileNumber())
					.isOwner("NO")
					.noofBuilding(paramMap.get("noOfBlocks"))
					.buidlingType("2")//hardcoded as 3rd party API to be provided
					.buildingName(infoWrapper.getBpa().getLandInfo().getAddress().getBuildingName())
					.proposedOccupany(paramMap.get("occupancyTypeEdcr"))
					.noOfFloor(paramMap.get("noOfStorey"))
					.height(paramMap.get("buildingHeight"))
					.measureType("Mtr")
					.category(paramMap.get("proposedOccupancy"))
					.builtupArea(paramMap.get("builtupAreaEdcr"))
					.areameasureType("Mtr")
					.fireDistrict("28")//hardcoded .Later read from UI params
					.fireStation("181")
					.adreesOfBuilding(mapper.writeValueAsString(infoWrapper.getBpa().getLandInfo().getAddress()))
					.buildingPlan(sitePlanDoc)
					.buildingPlanext("pdf") //to resolve later
					.plainApplication(plainApplicationDoc)
					.plainApplicationext("pdf")
					.applicantSignature(applicantSignature)
					.applicantSignatureext("jpg")
					.applicantPhoto(applicantPhoto)
					.applicantPhotoext("jpg")
					.identyProofDoc(identityProofDoc)
					.identyProofDocext("pdf")
					.ownershipDoc(ownershipDoc)
					.ownershipDocext("pdf")
					.token(config.getFireNocToken())
					.build();
		
		Object submitFireNocResponse = serviceRequestRepository.fetchResult(submitFireNocUrl, sfnc);
		if(submitFireNocResponse instanceof Map) {
			Map<String,Object> fireNocResponse = (Map<String, Object>) submitFireNocResponse;
			String responseMessage = String.valueOf(fireNocResponse.get("message"));
			if(!"Recommendation saved successfully".equalsIgnoreCase(responseMessage))
				throw new CustomException("fire noc submission failed","fire noc submission failed");
		}
		
		//recommendationStatus API--
		StringBuilder fetchStatusUrl = new StringBuilder(config.getFireNocHost());
		fetchStatusUrl.append(FETCH_FIRE_NOC_STATUS_ENDPOINT);
		Object fetchStatusResponse = serviceRequestRepository.fetchResult(fetchStatusUrl,
				new FetchRecommendationStatusContract(config.getFireNocToken(), null));
		if(fetchStatusResponse instanceof Map) {
			Map<String,Object> statusResponse = (Map<String, Object>) fetchStatusResponse;
			Map<String,Object> result = (Map<String, Object>) statusResponse.get("result");
			String applicationStatus = String.valueOf(result.get("applicationStatus"));
			if(!"In Process".equalsIgnoreCase(applicationStatus))
				throw new CustomException("fire noc failed","fire noc failed");
		}
		
		String comment="Recommendation saved successfully";
		
		return comment;
		} catch (JsonProcessingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new CustomException("error while parsing json","error while parsing json");
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
	
	private String getBinaryEncodedDocFromDocuments(ThirdPartyNOCRequestInfoWrapper infoWrapper, String typeOfDoc,
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
}
