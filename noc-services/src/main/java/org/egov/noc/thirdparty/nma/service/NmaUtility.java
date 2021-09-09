package org.egov.noc.thirdparty.nma.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.egov.noc.service.FileStoreService;
import org.egov.noc.thirdparty.model.ThirdPartyNOCPushRequestWrapper;
import org.egov.noc.thirdparty.nma.model.ApplicantDetails;
import org.egov.noc.thirdparty.nma.model.DistanceOfTheSiteOfTheConstructionFromProtectedBoundaryOfMonument;
import org.egov.noc.thirdparty.nma.model.Document;
import org.egov.noc.thirdparty.nma.model.Documents;
import org.egov.noc.thirdparty.nma.model.LocalityOfTheProposedConstruction;
import org.egov.noc.thirdparty.nma.model.MaximumHeightOfExistingModernBuildingInCloseVicinityOf;
import org.egov.noc.thirdparty.nma.model.NameOfTheNearestMonumentOrSite;
import org.egov.noc.thirdparty.nma.model.NmaApplicationRequest;
import org.egov.noc.thirdparty.nma.model.NmaArchitectRegistration;
import org.egov.noc.thirdparty.nma.model.OwnerDetails;
import org.egov.noc.thirdparty.nma.model.ProposedWorkDetails;
import org.egov.noc.thirdparty.nma.model.ProposedWorkLocalityDtails;
import org.egov.noc.util.NOCConstants;
import org.egov.noc.web.model.BPA;
import org.egov.noc.web.model.Noc;
import org.egov.noc.web.model.UserSearchResponse;
import org.egov.noc.web.model.landinfo.Address;
import org.egov.noc.web.model.landinfo.OwnerInfo;
import org.egov.tracer.model.ServiceCallException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class NmaUtility {

	private static final String YES = "Yes";

	@Autowired
	private ObjectMapper mapper;

	@Autowired
	private RestTemplate restTemplate;

	@Autowired
	private FileStoreService fileStoreService;

	public NmaApplicationRequest buildNmaApplicationRequest(ThirdPartyNOCPushRequestWrapper infoWrapper,NmaArchitectRegistration nmaArchitectRegistration) {
//		Noc noc, BPA bpa, UserSearchResponse user,
//		NmaArchitectRegistration nmaArchitectRegistration, NmaFormData nmaFormData;
		Noc noc = infoWrapper.getNoc();
		UserSearchResponse user = infoWrapper.getUserResponse();
		BPA bpa = infoWrapper.getBpa();
		DocumentContext edcrDetail = infoWrapper.getEdcr();
		String applicationUniqueNumebr = noc.getApplicationNo();
		String architectEmailId = user.getEmailId();
		String department = nmaArchitectRegistration.getTenantid();
		String nOCRequestScreen = getNOCRequestScreen(infoWrapper.getEdcr());
		NmaApplicationRequest nmaApplicationRequest = NmaApplicationRequest.builder()
				.applicantDetails(getApplicantDetails(user, nmaArchitectRegistration))
				.applicationUniqueNumebr(applicationUniqueNumebr).architectEmailId(architectEmailId)
				.department(department).documents(getDocuments(noc, bpa)).nOCRequestScreen(nOCRequestScreen)
				.ownerDetails(getOwnerDetails(bpa))
				.proposedWorkDetails(getProposedWorkDetails(bpa, noc, infoWrapper.getEdcr()))
				.proposedWorkLocalityDtails(getProposedWorkLocalityDtails(infoWrapper.getNoc(), bpa)).build();

		return nmaApplicationRequest;
	}

	private String getNOCRequestScreen(DocumentContext edcr) {
		// S/R/SC/SS/RS/SSC/A/A1/A2/A3/A4/RA/RA1/RA2/RA3/RA4/AC/AC1/AC2/AC3/AC4
		StringBuffer nOCRequestScreen = new StringBuffer("");
		switch (getServiceType(edcr)) {
		case NOCConstants.NEW_CONSTRUCTION:
			nOCRequestScreen.append("S");
			break;
		case NOCConstants.ALTERATION:
			nOCRequestScreen.append("A");
			break;
		}

		return nOCRequestScreen.toString();
	}

	private String getServiceType(DocumentContext edcr) {
		List<String> services = edcr.read("edcrDetail.*.planDetail.planInformation.serviceType");
		if (services != null && services.size() > 0)
			return services.get(0);
		return null;
	}

	private ApplicantDetails getApplicantDetails(UserSearchResponse user,
			NmaArchitectRegistration nmaArchitectRegistration) {
		return ApplicantDetails.builder().applicantName(user.getName()).permanentAddress(getPermanentAddress(user))
				.presentAddress(getPresentAddress(user)).build();
	}

	private String getPermanentAddress(UserSearchResponse user) {
		StringBuilder permanentAddress = new StringBuilder();
		permanentAddress.append(user.getPermanentAddress()).append(" ").append(user.getPermanentCity()).append(" ")
				.append(user.getPermanentPincode());
		return permanentAddress.toString();
	}

	private String getPresentAddress(UserSearchResponse user) {
		StringBuilder presentAddress = new StringBuilder();
		presentAddress.append(user.getCorrespondenceAddress()).append(" ").append(user.getCorrespondenceCity())
				.append(" ").append(user.getCorrespondencePincode());
		return presentAddress.toString();
	}

	private Documents getDocuments(Noc noc, BPA bpa) {
		return Documents.builder().firmFiles(getFirmFiles(noc)).googleEarthImage(getGoogleEarthImage(noc))
				.modernConstructionsImage(getModernConstructionsImage(bpa))
				.ownershipDocuments(getOwnershipDocuments(bpa))
				.termAndCondition(getValue((Map) noc.getAdditionalDetails(),
						"thirdPartNOC.MaximumHeightOfExistingModernBuildingInCloseVicinityOf.TermAndCondition"))
				.build();
	}

	private List<Document> getModernConstructionsImage(BPA bpa) {
		List<org.egov.noc.web.model.Document> list = new ArrayList<>();
		if (bpa.getDocuments() != null)
			list = bpa.getDocuments().stream()
					.filter(d -> d.getDocumentType().contains(NOCConstants.DOC_TYPE_BPD_SITEPHOTO))
					.collect(Collectors.toList());
		return getDocuments(bpa.getTenantId(), list);
	}

	private List<Document> getGoogleEarthImage(Noc noc) {
		List<org.egov.noc.web.model.Document> list = new ArrayList<>();
		if (noc.getDocuments() != null)
			list = noc.getDocuments().stream()
					.filter(d -> d.getDocumentType().contains(NOCConstants.DOC_TYPE_NOC_NMA_GOOGLE_EARTH_IMAGE))
					.collect(Collectors.toList());
		return getDocuments(noc.getTenantId(), list);

	}

	private List<Document> getFirmFiles(Noc noc) {
		List<org.egov.noc.web.model.Document> list = new ArrayList<>();
		if (noc.getDocuments() != null)
			list = noc.getDocuments().stream()
					.filter(d -> d.getDocumentType().contains(NOCConstants.DOC_TYPE_NOC_NMA_FIRM_FILES))
					.collect(Collectors.toList());
		return getDocuments(noc.getTenantId(), list);

	}

	private OwnerDetails getOwnerDetails(BPA bpa) {
		OwnerDetails ownerDetails = null;
		OwnerInfo primeryOwner = null;
		OwnerInfo otherOwner = null;
		for (OwnerInfo ownerInfo : bpa.getLandInfo().getOwners()) {
			if (ownerInfo.isIsPrimaryOwner())
				primeryOwner = ownerInfo;
			else
				otherOwner = ownerInfo;
		}
		if (primeryOwner != null) {
			StringBuilder permanentAddress = new StringBuilder();
			permanentAddress.append(primeryOwner.getPermanentAddress() + ", " + primeryOwner.getPermanentCity() + ", "
					+ primeryOwner.getPermanentPincode());
			StringBuilder presentAddress = new StringBuilder();
			presentAddress.append(primeryOwner.getCorrespondenceAddress() + ", " + primeryOwner.getCorrespondenceCity()
					+ ", " + primeryOwner.getCorrespondencePincode());
			String[] ownershipCategory = bpa.getLandInfo().getOwnershipCategory().split("\\.");
			ownerDetails = OwnerDetails.builder().applicantIsOtherThanOwner(YES).nameOfTheOwner(primeryOwner.getName())
					.permanentAddress(permanentAddress.toString()).presentAddress(presentAddress.toString())
					.whetherThePropertyIsOwnedBy(ownershipCategory[0])
					.whetherThePropertyIsOwnedBySecond(otherOwner != null ? otherOwner.getName() : "Other")
					.other(ownershipCategory[1]).propertyDocument(getPropertyDocument(bpa)).build();
		}
		return ownerDetails;
	}

	private List<Document> getOwnershipDocuments(BPA bpa) {
		List<org.egov.noc.web.model.Document> list = new ArrayList<>();
		if (bpa.getDocuments() != null)
			list = bpa.getDocuments().stream().filter(d -> d.getDocumentType().contains(NOCConstants.DOC_TYPE_BPD_BPL))
					.collect(Collectors.toList());
		return getDocuments(bpa.getTenantId(), list);
	}

	private List<Document> getTypicalFloorPlan(BPA bpa) {
		List<org.egov.noc.web.model.Document> list = bpa.getDocuments().stream()
				.filter(d -> d.getDocumentType().contains(NOCConstants.DOC_TYPE_BPD_BPL)).collect(Collectors.toList());
		return getDocuments(bpa.getTenantId(), list);
	}

	private List<Document> getPropertyDocument(BPA bpa) {
		List<org.egov.noc.web.model.Document> list = bpa.getDocuments().stream()
				.filter(d -> d.getDocumentType().contains(NOCConstants.DOC_TYPE_APPL_SALEGIFTDEED))
				.collect(Collectors.toList());
		return getDocuments(bpa.getTenantId(), list);
	}

	private List<Document> getDocuments(String tenantId, List<org.egov.noc.web.model.Document> list) {
		List<Document> documents = new ArrayList<>();
		for (org.egov.noc.web.model.Document document : list) {
			documents.add(Document.builder()
					.file(fileStoreService.getFileStorePath(tenantId, document.getFileStoreId())).build());
		}
		return documents;
	}

	private ProposedWorkDetails getProposedWorkDetails(BPA bpa, Noc noc, DocumentContext edcr) {
		Map<String, String> details = (Map<String, String>) noc.getAdditionalDetails();

		return ProposedWorkDetails.builder()
				.approximateDateOfCommencementOfWorks(
						getValue(details, "thirdPartNOC.ApproximateDateOfCommencementOfWorks"))
				.approximateDurationOfCommencementOfWorks(
						getValue(details, "thirdPartNOC.ApproximateDurationOfCommencementOfWorks"))
				.basementIfAnyProposedWithDetails(getValue(details, "thirdPartNOC.BasementIfAnyProposedWithDetails"))
				.detailsOfRepairAndRenovation(getValue(details, "thirdPartNOC.DetailsOfRepairAndRenovation"))
				.elevationDocument(getElevationDocument(noc))
				.floorAreaInSquareMetresStoreyWise(getFloorAreaInSquareMetresStoreyWise(edcr))
				.heightInMetresExcludingMumtyParapetWaterStorageTankEtc(
						getHeightInMetresExcludingMumtyParapetWaterStorageTankEtc(edcr))
				.heightInMetresIncludingMumtyParapetWaterStorageTankEtc(
						getHeightInMetresIncludingMumtyParapetWaterStorageTankEtc(edcr))
				.maximumHeightOfExistingModernBuildingInCloseVicinityOf(
						getMaximumHeightOfExistingModernBuildingInCloseVicinityOf(noc))
				.natureOfWorkProposed(getNatureOfWorkProposed(edcr))
				.natureOfWorkProposedOther(getNatureOfWorkProposedOther(edcr)).numberOfStoreys(getNumberOfStoreys(edcr))
				.otherDocument(getOtherDocument(noc)).purposeOfProposedWork(getPurposeOfProposedWork(edcr))
				.purposeOfProposedWorkOther(getPurposeOfProposedWorkOther(edcr))
				.sectionDocument(getSectionDocument(noc)).typicalFloorPlan(getTypicalFloorPlan(bpa)).build();
	}

	private List<Document> getSectionDocument(Noc noc) {
		List<org.egov.noc.web.model.Document> list = new ArrayList<>();
		if (noc.getDocuments() != null)
			list = noc.getDocuments().stream()
					.filter(d -> d.getDocumentType().contains(NOCConstants.DOC_TYPE_NOC_NMA_SECTION))
					.collect(Collectors.toList());
		return getDocuments(noc.getTenantId(), list);
	}

	private String getPurposeOfProposedWorkOther(DocumentContext edcr) {// write code
		List<String> list = edcr.read("edcrDetail.*.planDetail.planInformation.subOccupancy");
		if (list != null && list.size() > 0)
			return list.get(0);
		return null;
	}

	private String getPurposeOfProposedWork(DocumentContext edcr) {// write code
		List<String> list = edcr.read("edcrDetail.*.planDetail.planInformation.occupancy");
		if (list != null && list.size() > 0)
			return list.get(0);
		return null;
	}

	private List<Document> getOtherDocument(Noc noc) {
		List<org.egov.noc.web.model.Document> list = new ArrayList<>();
		if (noc.getDocuments() != null)
			list = noc.getDocuments().stream()
					.filter(d -> d.getDocumentType().contains(NOCConstants.DOC_TYPE_NOC_NMA_OTHER))
					.collect(Collectors.toList());
		return getDocuments(noc.getTenantId(), list);
	}

	private String getNumberOfStoreys(DocumentContext edcr) {
		List<String> list = edcr.read("edcrDetail.*.planDetail.planInformation.numberOfStoreys");
		if (list != null && list.size() > 0)
			return list.get(0);
		return null;
	}

	private String getNatureOfWorkProposedOther(DocumentContext edcr) {
		List<String> list = edcr.read("edcrDetail.*.planDetail.planInformation.occupancy");
		if (list != null && list.size() > 0)
			return list.get(0);
		return null;
	}

	private String getNatureOfWorkProposed(DocumentContext edcr) {
		String str = null;
		switch (getServiceType(edcr)) {
		case NOCConstants.NEW_CONSTRUCTION:
			str = "Construction";
			break;
		case NOCConstants.ALTERATION:
			str = "Addition/Alteration";
			break;
		default:
			str = "Other";
			break;

		}
		return str;
	}

	private MaximumHeightOfExistingModernBuildingInCloseVicinityOf getMaximumHeightOfExistingModernBuildingInCloseVicinityOf(
			Noc noc) {
		// write code
		Map map = (Map) noc.getAdditionalDetails();
		String nearTheMonument = getValue(map,
				"thirdPartNOC.MaximumHeightOfExistingModernBuildingInCloseVicinityOf.NearTheMonument");
		return MaximumHeightOfExistingModernBuildingInCloseVicinityOf.builder()
				.doesMasterPlanApprovedByConcernedAuthoritiesExistsForTheCityTownVillage(getValue(map,
						"thirdPartNOC.MaximumHeightOfExistingModernBuildingInCloseVicinityOf.DoesMasterPlanApprovedByConcernedAuthoritiesExistsForTheCityTownVillage"))
				.inCaseOfRepairsOrRenovationReportFromDulyAuthorisedOrLicencedArchitectSubmittedByApplicant(
						getInCaseOfRepairsOrRenovationReportFromDulyAuthorisedOrLicencedArchitectSubmittedByApplicant(
								noc))
				.nearTheMonument(nearTheMonument != null ? Double.parseDouble(nearTheMonument) : 0)
				.openSpaceOrParkOrGreenAreaCloseToProtectedMonumentOrProtectedArea(getValue(map,
						"thirdPartNOC.MaximumHeightOfExistingModernBuildingInCloseVicinityOf.OpenSpaceOrParkOrGreenAreaCloseToProtectedMonumentOrProtectedArea"))
				.remarks(getValue(map, "thirdPartNOC.MaximumHeightOfExistingModernBuildingInCloseVicinityOf.Remarks"))
				.signature(getSignature(noc))
				.statusOfModernConstructions(getValue(map,
						"thirdPartNOC.MaximumHeightOfExistingModernBuildingInCloseVicinityOf.StatusOfModernConstructions"))
				.whetherAnyRoadExistsBetweenTheMonumentAndTheSiteOfConstruction(getValue(map,
						"thirdPartNOC.MaximumHeightOfExistingModernBuildingInCloseVicinityOf.WhetherAnyRoadExistsBetweenTheMonumentAndTheSiteOfConstruction"))
				.whetherMonumentIsLocatedWithinLimitOf(getValue(map,
						"thirdPartNOC.MaximumHeightOfExistingModernBuildingInCloseVicinityOf.WhetherMonumentIsLocatedWithinLimitOf"))
				.build();
	}

	private List<Document> getSignature(Noc noc) {
		List<org.egov.noc.web.model.Document> list = new ArrayList<>();
		if (noc.getDocuments() != null)
			list = noc.getDocuments().stream()
					.filter(d -> d.getDocumentType().contains(NOCConstants.DOC_TYPE_NOC_NMA_SIGNATURE))
					.collect(Collectors.toList());
		return getDocuments(noc.getTenantId(), list);

	}

	private List<Document> getInCaseOfRepairsOrRenovationReportFromDulyAuthorisedOrLicencedArchitectSubmittedByApplicant(
			Noc noc) {
		List<org.egov.noc.web.model.Document> list = new ArrayList<>();
		if (noc.getDocuments() != null)
			list = noc.getDocuments().stream()
					.filter(d -> d.getDocumentType().contains(NOCConstants.DOC_TYPE_NOC_NMA_IN_CASE_OF_REPAIRS))
					.collect(Collectors.toList());
		return getDocuments(noc.getTenantId(), list);

	}

	private String getHeightInMetresIncludingMumtyParapetWaterStorageTankEtc(DocumentContext edcr) {
		List<String> list = edcr.read("edcrDetail.*.planDetail.planInformation.buildingHeightIncludingMumty");
		if (list != null && list.size() > 0)
			return list.get(0);
		return "";
	}

	private String getHeightInMetresExcludingMumtyParapetWaterStorageTankEtc(DocumentContext edcr) {
		List<String> list = edcr.read("edcrDetail.*.planDetail.planInformation.buildingHeightExcludingMumty");
		if (list != null && list.size() > 0)
			return list.get(0);
		return "";
	}

	private String getFloorAreaInSquareMetresStoreyWise(DocumentContext edcr) {// write code
		List<String> list = edcr.read("edcrDetail.*.planDetail.planInformation.floorAreaInSquareMetresStoreyWise");
		if (list != null && list.size() > 0)
			return list.get(0);
		return "";
	}

	private List<Document> getElevationDocument(Noc noc) {
		List<org.egov.noc.web.model.Document> list = new ArrayList<>();
		if (noc.getDocuments() != null)
			list = noc.getDocuments().stream()
					.filter(d -> d.getDocumentType().contains(NOCConstants.DOC_TYPE_NOC_NMA_ELEVATION))
					.collect(Collectors.toList());
		return getDocuments(noc.getTenantId(), list);

	}

	private ProposedWorkLocalityDtails getProposedWorkLocalityDtails(Noc noc, BPA bpa) {
		ProposedWorkLocalityDtails proposedWorkLocalityDtails = ProposedWorkLocalityDtails.builder()
				.distanceOfTheSiteOfTheConstructionFromProtectedBoundaryOfMonument(
						getDistanceOfTheSiteOfTheConstructionFromProtectedBoundaryOfMonument(noc))
				.localityOfTheProposedConstruction(getLocalityOfTheProposedConstruction(bpa, noc))
				.nameOfTheNearestMonumentOrSite(getNameOfTheNearestMonumentOrSite(noc)).build();

		return proposedWorkLocalityDtails;
	}

	private NameOfTheNearestMonumentOrSite getNameOfTheNearestMonumentOrSite(Noc noc) {
		Map<String, String> map = (Map<String, String>) noc.getAdditionalDetails();
		return NameOfTheNearestMonumentOrSite.builder()
				.district(getValue(map, "thirdPartNOC.NameOfTheNearestMonumentOrSite.District"))
				.locality(getValue(map, "thirdPartNOC.NameOfTheNearestMonumentOrSite.Locality"))
				.monumentName(getValue(map, "thirdPartNOC.NameOfTheNearestMonumentOrSite.MonumentName"))
				.state(getValue(map, "thirdPartNOC.NameOfTheNearestMonumentOrSite.State"))
				.taluk(getValue(map, "thirdPartNOC.NameOfTheNearestMonumentOrSite.Taluk")).build();
	}

	private String getValue(Map responseMap, String key) {
		String jsonString = new JSONObject(responseMap).toString();
		DocumentContext context = JsonPath.using(Configuration.defaultConfiguration()).parse(jsonString);
		return context.read(key) + "";
//		if (list != null && list.size() > 0)
//			return list.get(0);
//		return null;
	}

	private LocalityOfTheProposedConstruction getLocalityOfTheProposedConstruction(BPA bpa, Noc noc) {
		Address address = bpa.getLandInfo().getAddress();
		Map<String, String> map = (Map<String, String>) noc.getAdditionalDetails();
		return LocalityOfTheProposedConstruction.builder().district(getValue(map, "thirdPartNOC.NameOfTheNearestMonumentOrSite.District"))
				.locality(address.getLocality().getName()).plotSurveyNo(getValue(map, "thirdPartNOC.PlotSurveyNo"))
				.state(getValue(map, "thirdPartNOC.NameOfTheNearestMonumentOrSite.State")).taluk(address.getCity()).build();
	}

	private DistanceOfTheSiteOfTheConstructionFromProtectedBoundaryOfMonument getDistanceOfTheSiteOfTheConstructionFromProtectedBoundaryOfMonument(
			Noc noc) {
		Map<String, String> map = (Map<String, String>) noc.getAdditionalDetails();
		return DistanceOfTheSiteOfTheConstructionFromProtectedBoundaryOfMonument.builder()
				.distanceFromTheMainMonument(getValue(map,
						"thirdPartNOC.DistanceOfTheSiteOfTheConstructionFromProtectedBoundaryOfMonument.DistanceFromTheMainMonument"))
				.distanceFromTheProtectedBoundaryWall(getValue(map,
						"thirdPartNOC.DistanceOfTheSiteOfTheConstructionFromProtectedBoundaryOfMonument.DistanceFromTheProtectedBoundaryWall"))
				.build();
	}

	public Object fetchResult(StringBuilder uri, Object request) {
		Object response = null;
		log.info("URI: " + uri.toString());
		try {
			log.info("Request: " + mapper.writeValueAsString(request));
			response = restTemplate.postForObject(uri.toString(), request, String.class);
		} catch (HttpClientErrorException e) {
			log.error("External Service threw an Exception: ", e);
			throw new ServiceCallException(e.getResponseBodyAsString());
		} catch (Exception e) {
			log.error("Exception while fetching from searcher: ", e);
		}

		return response;
	}
}
