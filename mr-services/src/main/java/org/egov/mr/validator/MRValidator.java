package org.egov.mr.validator;


import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.validation.Valid;

import org.apache.commons.lang.StringUtils;
import org.egov.mr.web.models.MarriageRegistration;
import org.egov.mr.web.models.MarriageRegistrationRequest;
import org.egov.tracer.model.CustomException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import static org.egov.mr.util.MRConstants.businessService_MR;

@Component
public class MRValidator {
	
	@Value("${egov.allowed.businessServices}")
    private String allowedBusinessService;

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
        
        if (businessService == null)
            businessService = businessService_MR;
        switch (businessService) {
            case businessService_MR:
                validateMRSpecificNotNullFields(marriageRegistrationRequest);
                break;

        }
       

    }

	private void validateMRSpecificNotNullFields(MarriageRegistrationRequest marriageRegistrationRequest) {
		
		marriageRegistrationRequest.getMarriageRegistrations().forEach(marriageRegistration -> {
			
			 Map<String, String> errorMap = new HashMap<>();
			
			if(marriageRegistration.getMarriageDate() == null)
				errorMap.put("NULL_MARRIAGEDATE", " Marriage Date cannot be null");
			
			if(marriageRegistration.getMarriagePlace().getWard() == null)
				errorMap.put("NULL_WARD", " Ward cannot be null");
		
			if(marriageRegistration.getMarriagePlace().getPlaceOfMarriage() == null)
				errorMap.put("NULL_MARRIAGEPLACE", " Marriage Place  cannot be null");
			
			if(marriageRegistration.getMarriagePlace().getLocality().getCode() == null)	
				errorMap.put("NULL_LOCALITY", " Locality  cannot be null");

			if(marriageRegistration.getTenantId() == null)
				errorMap.put("NULL_TENANTID", " Tenant id cannot be null");
			
			if(marriageRegistration.getCoupleDetails()==null)
				errorMap.put("COUPLE_DETAILS_ERROR", " Couple Details are mandatory  ");
			
			if(marriageRegistration.getCoupleDetails().size()!=2)
				errorMap.put("COUPLE_DETAILS_ERROR", " Both the Bride and Groom details should be provided .");
			
			
			
			
			
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

}
