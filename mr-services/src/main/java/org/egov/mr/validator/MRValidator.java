package org.egov.mr.validator;


import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.validation.Valid;

import org.apache.commons.lang.StringUtils;
import org.egov.common.contract.request.RequestInfo;
import org.egov.mr.config.MRConfiguration;
import org.egov.mr.repository.MRRepository;
import org.egov.mr.util.MRConstants;
import org.egov.mr.web.models.MarriageRegistration;
import org.egov.mr.web.models.MarriageRegistrationRequest;
import org.egov.mr.web.models.MarriageRegistrationSearchCriteria;
import org.egov.tracer.model.CustomException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import static org.egov.mr.util.MRConstants.businessService_MR;

@Component
public class MRValidator {
	
	@Value("${egov.allowed.businessServices}")
    private String allowedBusinessService;
	
	@Autowired
	 private MRRepository mrRepository;
	 
	@Autowired
	 private MRConfiguration config;

	public void validateBusinessService(@Valid MarriageRegistrationRequest marriageRegistrationRequest,String businessServicefromPath) {
	

        List<String> allowedservices = Arrays.asList(allowedBusinessService.split(","));
        if (!allowedservices.contains(businessServicefromPath)) {
            throw new CustomException("BUSINESSSERVICE_NOTALLOWED", " The business service is not allowed in this module");
        }
        for (MarriageRegistration marriageRegistartions : marriageRegistrationRequest.getMarriageRegistrations()) {
            String marriageRegistrationBusinessService = marriageRegistartions.getBusinessService()==null?businessService_MR:marriageRegistartions.getBusinessService();
            if (!StringUtils.equals(businessServicefromPath, marriageRegistrationBusinessService)) {
                throw new CustomException("BUSINESSSERVICE_NOTMATCHING", " The business service inside marriageRegistration not matching with the one sent in path variable");
            }
        }
    
	}

	public void validateCreate(MarriageRegistrationRequest marriageRegistrationRequest) {
        List<MarriageRegistration> marriageRegistrations = marriageRegistrationRequest.getMarriageRegistrations();
        String businessService = marriageRegistrationRequest.getMarriageRegistrations().isEmpty()?null:marriageRegistrationRequest.getMarriageRegistrations().get(0).getBusinessService();
        
        if(marriageRegistrations.get(0).getApplicationType() != null && marriageRegistrations.get(0).getApplicationType().toString().equals(MRConstants.APPLICATION_TYPE_CORRECTION)){
            validateMRCorrection(marriageRegistrationRequest);
        }
        
        if (businessService == null)
            businessService = businessService_MR;
        switch (businessService) {
            case businessService_MR:
                validateMRSpecificNotNullFields(marriageRegistrationRequest);
                break;

        }
       

    }


    public void validateMRCorrection(MarriageRegistrationRequest request){
            
        MarriageRegistrationSearchCriteria criteria = new MarriageRegistrationSearchCriteria();
        List<String> mrNumbers = new LinkedList<>();
        request.getMarriageRegistrations().forEach(marriageRegistration -> {
            if(marriageRegistration.getMrNumber() != null){
            	mrNumbers.add(marriageRegistration.getMrNumber());
            } else{
                throw new CustomException("INVALID MARRIAGE REGISTRATION","Please select the existing Marriage Registration for correction ");  
                
            }
        });
        criteria.setTenantId(request.getMarriageRegistrations().get(0).getTenantId());
        criteria.setStatus(MRConstants.STATUS_APPROVED);
        criteria.setBusinessService(request.getMarriageRegistrations().get(0).getBusinessService());
        criteria.setMrNumbers(mrNumbers);
        List<MarriageRegistration> searchResult = mrRepository.getMarriageRegistartions(criteria);
        Map<String , MarriageRegistration> marriageRegistrationMap = new HashMap<>();
        searchResult.forEach(marriageRegistration -> {
            marriageRegistrationMap.put(marriageRegistration.getMrNumber() , marriageRegistration);
        });
        
        request.getMarriageRegistrations().forEach(license -> {
            if(license.getApplicationType() != null && license.getApplicationType().toString().equals(MRConstants.APPLICATION_TYPE_CORRECTION)){
                if(marriageRegistrationMap.containsKey(license.getMrNumber())){
        
                   
                }else{
                    throw new CustomException("CORRECTION ERROR","The Marriage Registration applied for correction is not present in the repository");
                }
            }
        });
    }
	
	private void validateMRSpecificNotNullFields(MarriageRegistrationRequest marriageRegistrationRequest) {
		
		marriageRegistrationRequest.getMarriageRegistrations().forEach(marriageRegistration -> {
			
			 Map<String, String> errorMap = new HashMap<>();
			
			if(marriageRegistration.getMarriageDate() == null)
				errorMap.put("NULL_MARRIAGEDATE", " Marriage Date cannot be null");
			
			if(marriageRegistration.getMarriagePlace() == null)
				errorMap.put("NULL_MARRIAGEPLACE", " Marriage Details cannot be null");
			else
			{
			if(StringUtils.isEmpty(marriageRegistration.getMarriagePlace().getWard()))
				errorMap.put("NULL_WARD", " Ward cannot be null");
			
			if(StringUtils.isEmpty(marriageRegistration.getMarriagePlace().getPinCode()))
				errorMap.put("NULL_PIN_CODE", " Pin code cannot be null");
		
			if(StringUtils.isEmpty(marriageRegistration.getMarriagePlace().getPlaceOfMarriage()))
				errorMap.put("NULL_MARRIAGEPLACE", " Marriage Place  cannot be null");
			
			if(StringUtils.isEmpty(marriageRegistration.getMarriagePlace().getLocality().getCode() ))	
				errorMap.put("NULL_LOCALITY", " Locality  cannot be null");
			}

			if(StringUtils.isEmpty(marriageRegistration.getTenantId()))
				errorMap.put("NULL_TENANTID", " Tenant id cannot be null");
			
			if(marriageRegistration.getCoupleDetails()==null)
				errorMap.put("COUPLE_DETAILS_ERROR", " Couple Details are mandatory  ");
			else
			{
			if(marriageRegistration.getCoupleDetails().size()!=2)
				errorMap.put("COUPLE_DETAILS_ERROR", " Both the Bride and Groom details should be provided .");
			
			marriageRegistration.getCoupleDetails().forEach(couple -> {
				if(couple.getIsDivyang()==null)
					errorMap.put("COUPLE_DETAILS_ERROR", " IsDivyang is mandatory ");
				if(couple.getIsGroom()==null)
					errorMap.put("COUPLE_DETAILS_ERROR", " Is Groom is mandatory ");
				if(StringUtils.isEmpty(couple.getTitle()))
					errorMap.put("COUPLE_DETAILS_ERROR", " Title is mandatory ");
				if(StringUtils.isEmpty(couple.getFirstName()))
					errorMap.put("COUPLE_DETAILS_ERROR", " Name is mandatory ");
				if(couple.getDateOfBirth() == null )
					errorMap.put("COUPLE_DETAILS_ERROR", " Date Of Birth is mandatory ");
				if(StringUtils.isEmpty(couple.getFatherName()))
					errorMap.put("COUPLE_DETAILS_ERROR", "Father Name is mandatory ");
				if(StringUtils.isEmpty(couple.getMotherName()))
					errorMap.put("COUPLE_DETAILS_ERROR", "Mother Name is mandatory ");
				
				
				if(couple.getCoupleAddress()!=null)
				{
					if(StringUtils.isEmpty(couple.getCoupleAddress().getAddressLine1()))
						errorMap.put("COUPLE_ADDRESS_ERROR", " Address is mandatory ");
					
					if(StringUtils.isEmpty(couple.getCoupleAddress().getCountry()))
						errorMap.put("COUPLE_ADDRESS_ERROR", " Country is mandatory ");
					
					if(StringUtils.isEmpty(couple.getCoupleAddress().getState()))
						errorMap.put("COUPLE_ADDRESS_ERROR", " State is mandatory ");
					
					if(StringUtils.isEmpty(couple.getCoupleAddress().getPinCode()))
						errorMap.put("COUPLE_ADDRESS_ERROR", " Pin Code is mandatory ");
					
					if(StringUtils.isEmpty(couple.getCoupleAddress().getContact()))
						errorMap.put("COUPLE_DETAILS_ERROR", "Contact is mandatory ");
					if(StringUtils.isEmpty(couple.getCoupleAddress().getEmailAddress()))
						errorMap.put("COUPLE_DETAILS_ERROR", "Email Address is mandatory ");
				}
				
				if(couple.getGuardianDetails()!=null)
				{
					if(StringUtils.isEmpty(couple.getGuardianDetails().getAddressLine1()))
						errorMap.put("GUARDIAN_ADDRESS_ERROR", " Address is mandatory ");
					
					if(StringUtils.isEmpty(couple.getGuardianDetails().getCountry()))
						errorMap.put("GUARDIAN_ADDRESS_ERROR", " Country is mandatory ");
					
					if(StringUtils.isEmpty(couple.getGuardianDetails().getState()))
						errorMap.put("GUARDIAN_ADDRESS_ERROR", " State is mandatory ");
					
					if(StringUtils.isEmpty(couple.getGuardianDetails().getPinCode()))
						errorMap.put("GUARDIAN_ADDRESS_ERROR", " Pin Code is mandatory ");
					
					if(couple.getGuardianDetails().getGroomSideGuardian()==null)
						errorMap.put("GUARDIAN_DETAILS_ERROR", "Is Groom Side Guardian details are mandatory ");
					
					if(StringUtils.isEmpty(couple.getGuardianDetails().getRelationship()))
						errorMap.put("GUARDIAN_DETAILS_ERROR", "Guardian Relationship is mandatory ");
					
					if(StringUtils.isEmpty(couple.getGuardianDetails().getName()))
						errorMap.put("GUARDIAN_DETAILS_ERROR", "Guardian Name is mandatory ");
					
					if(StringUtils.isEmpty(couple.getGuardianDetails().getContact()))
						errorMap.put("GUARDIAN_DETAILS_ERROR", "Guardian Contact is mandatory ");
					
					if(StringUtils.isEmpty(couple.getGuardianDetails().getEmailAddress()))
						errorMap.put("GUARDIAN_DETAILS_ERROR", "Guardian Email Address is mandatory ");
				}
				
			});
			
			}
			
			if(marriageRegistration.getWitness() != null)
			{
				marriageRegistration.getWitness().forEach(witness -> {
					
					if(StringUtils.isEmpty(witness.getTitle()))
						errorMap.put("WITNESS_DETAILS_ERROR", "Witness Title is mandatory ");
					
					if(StringUtils.isEmpty(witness.getAddress()))
						errorMap.put("WITNESS_DETAILS_ERROR", "Witness Address is mandatory ");
					
					if(StringUtils.isEmpty(witness.getFirstName()))
						errorMap.put("WITNESS_DETAILS_ERROR", "Witness Name is mandatory ");
					
					if(StringUtils.isEmpty(witness.getCountry()))
						errorMap.put("WITNESS_DETAILS_ERROR", "Witness Country is mandatory ");
					
					if(StringUtils.isEmpty(witness.getState()))
						errorMap.put("WITNESS_DETAILS_ERROR", "Witness State is mandatory ");
					
					if(StringUtils.isEmpty(witness.getDistrict()))
						errorMap.put("WITNESS_DETAILS_ERROR", "Witness District is mandatory ");
					
					if(StringUtils.isEmpty(witness.getPinCode()))
						errorMap.put("WITNESS_DETAILS_ERROR", "Witness PinCode is mandatory ");
					
					if(StringUtils.isEmpty(witness.getContact()))
						errorMap.put("WITNESS_DETAILS_ERROR", "Witness Contact is mandatory ");
					
					
				});
			}
			
			
			
			if (!errorMap.isEmpty())
                throw new CustomException(errorMap);
			
		});
		
	}
	
    /**
     *  Validates the update request
     * @param request The input MarriageRegistrationRequest Object
     */
    public void validateUpdate(MarriageRegistrationRequest request, List<MarriageRegistration> searchResult) {
        List<MarriageRegistration> marriageRegistrations = request.getMarriageRegistrations();
        if (searchResult.size() != marriageRegistrations.size())
            throw new CustomException("INVALID UPDATE", "The marriageRegistration to be updated is not in database");
        validateAllIds(searchResult, marriageRegistrations);
        String businessService = request.getMarriageRegistrations().isEmpty()?null:marriageRegistrations.get(0).getBusinessService();
             
        if (businessService == null)
            businessService = businessService_MR;
        switch (businessService) {
            case businessService_MR:
                validateMRSpecificNotNullFields(request);
                break;

        }

        validateDuplicateDocuments(request);
        setFieldsFromSearch(request, searchResult);
        
    }

    
    private void setFieldsFromSearch(MarriageRegistrationRequest request, List<MarriageRegistration> searchResult) {
        Map<String,MarriageRegistration> idToMarriageRegistrationFromSearch = new HashMap<>();
        searchResult.forEach(marriageRegistration -> {
            idToMarriageRegistrationFromSearch.put(marriageRegistration.getId(),marriageRegistration);
        });
        request.getMarriageRegistrations().forEach(marriageRegistration -> {
            marriageRegistration.getAuditDetails().setCreatedBy(idToMarriageRegistrationFromSearch.get(marriageRegistration.getId()).getAuditDetails().getCreatedBy());
            marriageRegistration.getAuditDetails().setCreatedTime(idToMarriageRegistrationFromSearch.get(marriageRegistration.getId()).getAuditDetails().getCreatedTime());
            marriageRegistration.setStatus(idToMarriageRegistrationFromSearch.get(marriageRegistration.getId()).getStatus());
            marriageRegistration.setMrNumber(idToMarriageRegistrationFromSearch.get(marriageRegistration.getId()).getMrNumber());

        });
    }
    
  
    private void validateDuplicateDocuments(MarriageRegistrationRequest request){
        List<String> documentFileStoreIds = new LinkedList();
        request.getMarriageRegistrations().forEach(marriageRegistration -> {
            if(marriageRegistration.getApplicationDocuments()!=null){
                marriageRegistration.getApplicationDocuments().forEach(
                        document -> {
                                if(documentFileStoreIds.contains(document.getFileStoreId()))
                                    throw new CustomException("DUPLICATE_DOCUMENT ERROR","Same document cannot be used multiple times");
                                else documentFileStoreIds.add(document.getFileStoreId());
                        }
                );
            }
        });
    }
    
    /**
     * Validates if all ids are same as obtained from search result
     * @param searchResult The marriageRegistration from search
     * @param marriageRegistrations The marriageRegistrations from the update Request
     */
    private void validateAllIds(List<MarriageRegistration> searchResult,List<MarriageRegistration> marriageRegistrations){

        Map<String,MarriageRegistration> idToMarriageRegistrationFromSearch = new HashMap<>();
        searchResult.forEach(marriageRegistration -> {
            idToMarriageRegistrationFromSearch.put(marriageRegistration.getId(),marriageRegistration);
        });

        Map<String,String> errorMap = new HashMap<>();
        marriageRegistrations.forEach(marriageRegistrationObj -> {
        	MarriageRegistration searchedMarriageRegistration = idToMarriageRegistrationFromSearch.get(marriageRegistrationObj.getId());

            if(!searchedMarriageRegistration.getApplicationNumber().equalsIgnoreCase(marriageRegistrationObj.getApplicationNumber()))
                errorMap.put("INVALID UPDATE","The application number from search: "+searchedMarriageRegistration.getApplicationNumber()
                        +" and from update: "+marriageRegistrationObj.getApplicationNumber()+" does not match");

            if(!searchedMarriageRegistration.getMarriagePlace().getId().
                    equalsIgnoreCase(marriageRegistrationObj.getMarriagePlace().getId()))
                errorMap.put("INVALID UPDATE","The id "+marriageRegistrationObj.getMarriagePlace().getId()+" does not exist");

            compareIdList(getCouple(searchedMarriageRegistration),getCouple(marriageRegistrationObj),errorMap);
            compareIdList(getCoupleAddress(searchedMarriageRegistration),getCoupleAddress(marriageRegistrationObj),errorMap);
            compareIdList(getGuardianDetails(searchedMarriageRegistration),getGuardianDetails(marriageRegistrationObj),errorMap);
            compareIdList(getWitness(searchedMarriageRegistration),getWitness(marriageRegistrationObj),errorMap);
            compareIdList(getApplicationDocIds(searchedMarriageRegistration),getApplicationDocIds(marriageRegistrationObj),errorMap);
            compareIdList(getVerficationDocIds(searchedMarriageRegistration),getVerficationDocIds(marriageRegistrationObj),errorMap);
        });

        if(!CollectionUtils.isEmpty(errorMap))
            throw new CustomException(errorMap);
    }
    
    

    private List<String> getCouple(MarriageRegistration marriageRegistration){
        List<String> coupleIds = new LinkedList<>();
        if(!CollectionUtils.isEmpty(marriageRegistration.getCoupleDetails())){
        	marriageRegistration.getCoupleDetails().forEach(couple -> {
                coupleIds.add(couple.getId());
            });
        }
        return coupleIds;
    }
    
    private List<String> getCoupleAddress(MarriageRegistration marriageRegistration){
        List<String> coupleAddressIds = new LinkedList<>();
        if(!CollectionUtils.isEmpty(marriageRegistration.getCoupleDetails())){
        	marriageRegistration.getCoupleDetails().forEach(couple -> {
        		if(couple.getCoupleAddress()!=null)
                coupleAddressIds.add(couple.getCoupleAddress().getId());
            });
        }
        return coupleAddressIds;
    }
    
    private List<String> getGuardianDetails(MarriageRegistration marriageRegistration){
        List<String> coupleAddressIds = new LinkedList<>();
        if(!CollectionUtils.isEmpty(marriageRegistration.getCoupleDetails())){
        	marriageRegistration.getCoupleDetails().forEach(couple -> {
        		if(couple.getGuardianDetails()!=null)
                coupleAddressIds.add(couple.getGuardianDetails().getId());
            });
        }
        return coupleAddressIds;
    }
    
    private List<String> getWitness(MarriageRegistration marriageRegistration){
        List<String> coupleIds = new LinkedList<>();
        if(!CollectionUtils.isEmpty(marriageRegistration.getWitness())){
        	if(marriageRegistration.getWitness()!=null)
        	marriageRegistration.getWitness().forEach(witness -> {
                coupleIds.add(witness.getId());
            });
        }
        return coupleIds;
    }
    

    private List<String> getApplicationDocIds(MarriageRegistration marriageRegistration){
        List<String> applicationDocIds = new LinkedList<>();
        if(!CollectionUtils.isEmpty(marriageRegistration.getApplicationDocuments())){
            marriageRegistration.getApplicationDocuments().forEach(document -> {
                applicationDocIds.add(document.getId());
            });
        }
        return applicationDocIds;
    }


    private List<String> getVerficationDocIds(MarriageRegistration marriageRegistration){
        List<String> verficationDocIds = new LinkedList<>();
        if(!CollectionUtils.isEmpty(marriageRegistration.getVerificationDocuments())) {
            marriageRegistration.getVerificationDocuments().forEach(document -> {
                verficationDocIds.add(document.getId());
            });
        }
        return verficationDocIds;
    }
    
    /**
     * Checks if the ids are present in the searchedIds
     * @param searchIds Ids got from search
     * @param updateIds The ids received from update Request
     * @param errorMap The map for collecting errors
     */
    private void compareIdList(List<String> searchIds,List<String> updateIds,Map<String,String> errorMap){
        if(!CollectionUtils.isEmpty(searchIds))
            searchIds.forEach(searchId -> {
                if(!updateIds.contains(searchId))
                    errorMap.put("INVALID UPDATE","The id: "+searchId+" was not present in update request");
            });
    }
    
    
   
    public void validateSearch(RequestInfo requestInfo, MarriageRegistrationSearchCriteria criteria, String serviceFromPath, boolean isInterServiceCall) {
        String serviceInSearchCriteria = criteria.getBusinessService();
        if ((serviceInSearchCriteria != null) && (!StringUtils.equals(serviceFromPath, serviceInSearchCriteria))) {
            throw new CustomException("INVALID SEARCH", "Business service in Path param and requestbody not matching");
        }

        List<String> allowedservices = Arrays.asList(allowedBusinessService.split(","));
        if ((serviceFromPath != null) && (!allowedservices.contains(serviceFromPath))) {
            throw new CustomException("INVALID SEARCH", "Search not allowed on this business service");
        }

        if(!requestInfo.getUserInfo().getType().equalsIgnoreCase("CITIZEN" )&& criteria.isEmpty())
            throw new CustomException("INVALID SEARCH","Search without any paramters is not allowed");

        if(!requestInfo.getUserInfo().getType().equalsIgnoreCase("CITIZEN" )&& criteria.tenantIdOnly())
            throw new CustomException("INVALID SEARCH","Search based only on tenantId is not allowed");

        if(!requestInfo.getUserInfo().getType().equalsIgnoreCase("CITIZEN" )&& !criteria.tenantIdOnly()
                && criteria.getTenantId()==null)
            throw new CustomException("INVALID SEARCH","TenantId is mandatory in search");

        if(requestInfo.getUserInfo().getType().equalsIgnoreCase("CITIZEN" ) && !criteria.isEmpty()
                && !criteria.tenantIdOnly() && criteria.getTenantId()==null)
            throw new CustomException("INVALID SEARCH","TenantId is mandatory in search");

        if(requestInfo.getUserInfo().getType().equalsIgnoreCase("CITIZEN" )&& criteria.tenantIdOnly())
            throw new CustomException("INVALID SEARCH","Search only on tenantId is not allowed");

        String allowedParamStr = null;

        if(requestInfo.getUserInfo().getType().equalsIgnoreCase("CITIZEN" ))
            allowedParamStr = config.getAllowedCitizenSearchParameters();
        else if(requestInfo.getUserInfo().getType().equalsIgnoreCase("EMPLOYEE" ))
            allowedParamStr = config.getAllowedEmployeeSearchParameters();
        else throw new CustomException("INVALID SEARCH","The userType: "+requestInfo.getUserInfo().getType()+
                    " does not have any search config");

        if(StringUtils.isEmpty(allowedParamStr) && !criteria.isEmpty())
            throw new CustomException("INVALID SEARCH","No search parameters are expected");
        else {
            List<String> allowedParams = Arrays.asList(allowedParamStr.split(","));
            validateSearchParams(criteria, allowedParams, isInterServiceCall, requestInfo);
        }
    }
    

    private void validateSearchParams(MarriageRegistrationSearchCriteria criteria, List<String> allowedParams, boolean isInterServiceCall
            , RequestInfo requestInfo) {

        if(criteria.getApplicationNumber()!=null && !allowedParams.contains("applicationNumber"))
            throw new CustomException("INVALID SEARCH","Search on applicationNumber is not allowed");

        if(criteria.getTenantId()!=null && !allowedParams.contains("tenantId"))
            throw new CustomException("INVALID SEARCH","Search on tenantId is not allowed");

        if(criteria.getStatus()!=null && !allowedParams.contains("status"))
            throw new CustomException("INVALID SEARCH","Search on Status is not allowed");

        if(criteria.getIds()!=null && !allowedParams.contains("ids"))
            throw new CustomException("INVALID SEARCH","Search on ids is not allowed");

        if(criteria.getMobileNumber()!=null && !allowedParams.contains("mobileNumber"))
        {
            if(!isInterServiceCall || !requestInfo.getUserInfo().getType().equalsIgnoreCase("CITIZEN" ))
                throw new CustomException("INVALID SEARCH","Search on mobileNumber is not allowed");
        }

        if(criteria.getMrNumbers()!=null && !allowedParams.contains("mrNumbers"))
            throw new CustomException("INVALID SEARCH","Search on mrNumbers is not allowed");

        if(criteria.getOffset()!=null && !allowedParams.contains("offset"))
            throw new CustomException("INVALID SEARCH","Search on offset is not allowed");

        if(criteria.getLimit()!=null && !allowedParams.contains("limit"))
            throw new CustomException("INVALID SEARCH","Search on limit is not allowed");

    }


}
