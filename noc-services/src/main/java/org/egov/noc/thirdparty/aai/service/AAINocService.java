package org.egov.noc.thirdparty.aai.service;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.egov.noc.config.NOCConfiguration;
import org.egov.noc.repository.NOCRepository;
import org.egov.noc.repository.ServiceRequestRepository;
import org.egov.noc.service.BpaService;
import org.egov.noc.service.EdcrService;
import org.egov.noc.service.FileStoreService;
import org.egov.noc.service.UserService;
import org.egov.noc.thirdparty.aai.model.ApplicationData;
import org.egov.noc.thirdparty.aai.model.Coordinates;
import org.egov.noc.thirdparty.aai.model.FilesAAI;
import org.egov.noc.thirdparty.aai.model.ServiceObpasData;
import org.egov.noc.thirdparty.aai.model.SiteDetails;
import org.egov.noc.thirdparty.aai.model.ToAAIData;
import org.egov.noc.thirdparty.aai.model.UlbServiceResponse;
import org.egov.noc.thirdparty.model.ThirdPartyNOCPushRequestWrapper;
import org.egov.noc.util.NOCConstants;
import org.egov.noc.util.NOCUtil;
import org.egov.noc.web.model.BPA;
import org.egov.noc.web.model.Noc;
import org.egov.noc.web.model.NocSearchCriteria;
import org.egov.noc.web.model.RequestInfoWrapper;
import org.egov.noc.web.model.UserResponse;
import org.egov.noc.web.model.UserSearchResponse;
import org.egov.noc.web.model.landinfo.Address;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;

import lombok.extern.slf4j.Slf4j;
import net.minidev.json.JSONArray;

@Slf4j
@Service
public class AAINocService {

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

	@Autowired
	private UserService userService;

	@Autowired
	private BpaService bpaService;

	@Autowired
	private EdcrService edcrService;

	private static final String STRUCTURE_TYPE_PATH = "thirdPartyNOC.STRUCTURETYPE";
	private static final String STRUCTURE_PURPOSE_PATH = "thirdPartyNOC.STRUCTUREPURPOSE";
	private static final String IS_IN_AIRPORT_PATH = "thirdPartyNOC.ISINAIRPORTPREMISES";
	private static final String IS_PERMISSION_TAKEN_PATH = "thirdPartyNOC.PERMISSIONTAKEN";
	private static final String COORDINATES_PATH = "thirdPartyNOC.SiteDetails.Coordinates";
	private static final String FILES_PATH = "thirdPartyNOC.FILES";
	private static final String REQUIRED_DATE_FORMAT = "MM/dd/yyyy";
	private static final ArrayList<String> MANDATORY_ADDITIONAL_DETAILS_THIRDPARTY = new ArrayList<String>() {
		{
			add("FILES");
			add("SiteDetails");
			add("STRUCTURETYPE");
			add("PERMISSIONTAKEN");
			add("STRUCTUREPURPOSE");
			add("ISINAIRPORTPREMISES");
		}
	};

	/**
	 * fetch in-progress AAI nocs and prepares AAI format data
	 * 
	 * @param
	 * @return
	 */
	public UlbServiceResponse fetchAAINocsInProgress(RequestInfoWrapper requestInfoWrapper) {
		NocSearchCriteria nocSearchCriteria = NocSearchCriteria.builder()
				.applicationStatus(NOCConstants.NOC_STATUS_INPROGRESS).nocType(NOCConstants.AAI_NOC_TYPE).build();
		List<Noc> nocs = nocRepository.getNocData(nocSearchCriteria);
		List<ToAAIData> nocsToPush = new ArrayList<>();
		// prepare AAI compliant data from sujog noc data-
		for (Noc noc : nocs) {
			// check if noc additionalDetails contains all mandatory fields before pushing to external dept-
			if(!isMandatoryFieldsAndDocumentsPresent(noc)) {
				// do not add this noc to push
				continue;
			}
			UserResponse userResponse = getUser(noc, requestInfoWrapper);
			UserSearchResponse userSearchResponse = userResponse.getUser().get(0);
			BPA bpa = bpaService.getBuildingPlan(requestInfoWrapper.getRequestInfo(), noc.getTenantId(),
					noc.getSourceRefId(), null);
			DocumentContext edcr = edcrService.getEDCRDetails(bpa.getTenantId(), bpa.getEdcrNumber(),
					requestInfoWrapper.getRequestInfo());

			String applicantAddress = getApplicantAddress(bpa);
			Map<String, String> additionalDetails = (Map<String, String>) noc.getAdditionalDetails();
			String ownerAddress = getOwnerAddress(bpa);
			Address address = bpa.getLandInfo().getAddress();
			Double plotArea = getPlotArea(edcr);

			ApplicationData applicationData = ApplicationData.builder().uniqueid(noc.getApplicationNo())
					.applicationdate(getApplicationDateInProperFormat(noc)).applicantname(userSearchResponse.getName())
					.applicantaddress(applicantAddress).applicantno(userSearchResponse.getMobileNumber())
					.applicantemail(userSearchResponse.getEmailId()).applicationno(noc.getApplicationNo())
					.ownername(getOwnerName(bpa)).owneraddress(ownerAddress)
					.structuretype(nocUtil.getValue(additionalDetails, STRUCTURE_TYPE_PATH))
					.structurepurpose(nocUtil.getValue(additionalDetails, STRUCTURE_PURPOSE_PATH))
					.siteaddress(applicantAddress)
					//.sitecity(address.getCity()) commenting as coming like od.cuttack
					.sitecity(bpa.getTenantId().split("\\.")[1])
					//.sitestate(address.getState()) commenting as coming null
					.sitestate("Odisha")
					.plotsize(plotArea).isinairportpremises(nocUtil.getValue(additionalDetails, IS_IN_AIRPORT_PATH))
					.permissiontaken(nocUtil.getValue(additionalDetails, IS_PERMISSION_TAKEN_PATH)).build();
			List<Coordinates> coordinates = getCoordinates(additionalDetails);
			SiteDetails siteDetails = SiteDetails.builder().coordinates(coordinates).build();
			FilesAAI files = getFilesForAAINoc(additionalDetails);
			ToAAIData aaiDataForNoc = ToAAIData.builder().applicationData(applicationData).siteDetails(siteDetails)
					.files(files).build();
			nocsToPush.add(aaiDataForNoc);
		}

		ServiceObpasData serviceObpasData = ServiceObpasData.builder().toAAI(nocsToPush).build();
		UlbServiceResponse ulbServiceResponse = UlbServiceResponse.builder().swsObpas(serviceObpasData).build();
		return ulbServiceResponse;

	}

	private UserResponse getUser(Noc noc, RequestInfoWrapper requestInfoWrapper) {
		return userService.getUser(nocUtil.getNocSearchCriteriaForSearchUser(noc), requestInfoWrapper.getRequestInfo());
	}

	private String getOwnershipCategory(BPA bpa) {
		String ownershipCategory = "";
		ownershipCategory = bpa.getLandInfo().getOwnershipCategory();
		return ownershipCategory;
	}

	private String getOwnerAddress(BPA bpa) {
		String ownershipCategory = getOwnershipCategory(bpa);
		String ownerAddress = "";
		if (ownershipCategory.contains(NOCConstants.OWNERSHIP_MAJOR_TYPE_INSTITUTIONAL_PRIVATE))
			ownerAddress = bpa.getLandInfo().getOwners().get(0).getCorrespondenceAddress();
		else if (ownershipCategory.contains(NOCConstants.OWNERSHIP_MAJOR_TYPE_INDIVIDUAL))
			ownerAddress = bpa.getLandInfo().getOwners().stream().filter(owner -> owner.isIsPrimaryOwner()).findFirst()
					.get().getCorrespondenceAddress();
		else if (ownershipCategory.contains(NOCConstants.OWNERSHIP_MAJOR_TYPE_INSTITUTIONAL_GOVERNMENT))
			ownerAddress = bpa.getLandInfo().getOwners().get(0).getCorrespondenceAddress();
		return ownerAddress;
	}

	private String getOwnerName(BPA bpa) {
		String ownershipCategory = getOwnershipCategory(bpa);
		String ownerName = "";
		if (ownershipCategory.contains(NOCConstants.OWNERSHIP_MAJOR_TYPE_INSTITUTIONAL_PRIVATE))
			ownerName = String
					.valueOf(((Map) bpa.getLandInfo().getAdditionalDetails()).get(NOCConstants.INSTITUTION_NAME));
		else if (ownershipCategory.contains(NOCConstants.OWNERSHIP_MAJOR_TYPE_INDIVIDUAL))
			ownerName = bpa.getLandInfo().getOwners().get(0).getName();
		else if (ownershipCategory.contains(NOCConstants.OWNERSHIP_MAJOR_TYPE_INSTITUTIONAL_GOVERNMENT))
			ownerName = String
					.valueOf(((Map) bpa.getLandInfo().getAdditionalDetails()).get(NOCConstants.INSTITUTION_NAME));
		return ownerName;
	}

	private List<Coordinates> getCoordinates(Map additionalDetails) {
		try {
			String jsonString = new JSONObject(additionalDetails).toString();
			DocumentContext context = JsonPath.using(Configuration.defaultConfiguration()).parse(jsonString);
			return context.read(COORDINATES_PATH);
		} catch (Exception ex) {
			log.error("error while extracting coordinates from additionalDetails of noc", ex);
			return Collections.EMPTY_LIST;
		}
	}

	private FilesAAI getFilesForAAINoc(Map additionalDetails) {
		try {
			String jsonString = new JSONObject(additionalDetails).toString();
			DocumentContext context = JsonPath.using(Configuration.defaultConfiguration()).parse(jsonString);
			FilesAAI files = mapper.convertValue(context.read(FILES_PATH), FilesAAI.class);
			return files;
		} catch (Exception ex) {
			log.error("error while extracting coordinates from additionalDetails of noc", ex);
			return new FilesAAI();
		}
	}

	private String getApplicationDateInProperFormat(Noc noc) {
		DateFormat formatter = new SimpleDateFormat(REQUIRED_DATE_FORMAT);
		Long applicationDateMillis = noc.getAuditDetails().getCreatedTime();
		Calendar calendar = Calendar.getInstance();
		calendar.setTimeInMillis(applicationDateMillis);
		return formatter.format(calendar.getTime());
	}

	private String getApplicantAddress(BPA bpa) {
		Address address = bpa.getLandInfo().getAddress();
		StringBuilder applicantAddressBuilder = new StringBuilder();
		if (!StringUtils.isEmpty(address.getBuildingName()))
			applicantAddressBuilder.append(address.getBuildingName() + ",");
		if (!StringUtils.isEmpty(address.getStreet()))
			applicantAddressBuilder.append(address.getStreet() + ",");
		// mandatorily present-
		applicantAddressBuilder.append(address.getLocality().getName() + ",");
		applicantAddressBuilder.append(bpa.getTenantId().split("\\.")[1]);
		if (!StringUtils.isEmpty(address.getStreet()))
			applicantAddressBuilder.append("-" + address.getPincode());
		//String applicantAddress = address.getBuildingName() + "," + address.getStreet() + ","
		//		+ address.getLocality().getName() + "," + bpa.getTenantId().split("\\.")[1] + "-"
		//		+ address.getPincode();
		return applicantAddressBuilder.toString();
	}

	private Double getPlotArea(DocumentContext edcr) {
		Double plotArea = Double.valueOf("0");
		JSONArray plotAreas = edcr.read(NOCConstants.PLOT_AREA_PATH);
		if (!CollectionUtils.isEmpty(plotAreas)) {
			if (null != plotAreas.get(0)) {
				String plotAreaString = plotAreas.get(0).toString();
				plotArea = Double.parseDouble(plotAreaString);
			}
		}
		return plotArea;
	}
	
	private boolean isMandatoryFieldsAndDocumentsPresent(Noc noc) {
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

}