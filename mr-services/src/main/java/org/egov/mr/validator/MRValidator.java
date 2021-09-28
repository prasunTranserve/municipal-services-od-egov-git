package org.egov.mr.validator;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import javax.validation.Valid;

import org.apache.commons.lang.StringUtils;
import org.egov.common.contract.request.RequestInfo;
import org.egov.mr.config.MRConfiguration;
import org.egov.mr.repository.MRRepository;
import org.egov.mr.util.MRConstants;
import org.egov.mr.web.models.AppointmentDetails;
import org.egov.mr.web.models.MarriageRegistration;
import org.egov.mr.web.models.MarriageRegistration.ApplicationTypeEnum;
import org.egov.mr.web.models.MarriageRegistrationRequest;
import org.egov.mr.web.models.MarriageRegistrationSearchCriteria;
import org.egov.tracer.model.CustomException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import static org.egov.mr.util.MRConstants.ACTION_APPLY;
import static org.egov.mr.util.MRConstants.businessService_MR;
import static org.egov.mr.util.MRConstants.ACTION_SCHEDULE;
import static org.egov.mr.util.MRConstants.ACTION_RESCHEDULE;
import static org.egov.mr.util.MRConstants.businessService_MR_CORRECTION;

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
        
        request.getMarriageRegistrations().forEach(marriageRegistration -> {
            if(marriageRegistration.getApplicationType() != null && marriageRegistration.getApplicationType().toString().equals(MRConstants.APPLICATION_TYPE_CORRECTION)){
                if(marriageRegistrationMap.containsKey(marriageRegistration.getMrNumber())){
        
                   
                }else{
                    throw new CustomException("CORRECTION ERROR","The Marriage Registration applied for correction is not present in the repository");
                }
            }
        });
    }
	
	private void validateMRSpecificNotNullFields(MarriageRegistrationRequest marriageRegistrationRequest) {
		
		List<String>  userRoles = new ArrayList<>();
		
		marriageRegistrationRequest.getRequestInfo().getUserInfo().getRoles().forEach(role -> {
			userRoles.add(role.getCode());
		});
		marriageRegistrationRequest.getMarriageRegistrations().forEach(marriageRegistration -> {
			
			 Map<String, String> errorMap = new HashMap<>();
			 List<Boolean> isPrimaryOwnerTrueCounter = new ArrayList<>();
			 
			 if(marriageRegistration.getApplicationType() == null )
				 errorMap.put("NULL_APPLICATIONTYPE", " Application Type cannot be null");
			 
			 if(marriageRegistration.getApplicationType() != null )
			 {
				 if(marriageRegistration.getApplicationType().toString().equalsIgnoreCase(ApplicationTypeEnum.CORRECTION.toString()) && !marriageRegistration.getWorkflowCode().equalsIgnoreCase(businessService_MR_CORRECTION))
				 errorMap.put("APPLICATIONTYPE_ERROR", "When Application Type is correction then workflowcode should be MRCORRECTION");
			 }
			
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
			if(marriageRegistration.getCoupleDetails().get(0).getBride()==null)
				errorMap.put("COUPLE_DETAILS_ERROR", " Bride Details are mandatory ");
			
			if(marriageRegistration.getCoupleDetails().get(0).getGroom()==null)
				errorMap.put("COUPLE_DETAILS_ERROR", " Groom are mandatory ");
			
			marriageRegistration.getCoupleDetails().forEach(couple -> {
				
				//===================================Bride Details=========================================
				
				if(couple.getBride().getIsDivyang()==null)
					errorMap.put("COUPLE_DETAILS_ERROR", " IsDivyang is mandatory ");
				
				
				
				if(userRoles.contains("MR_CEMP"))
				{
						if(couple.getBride().getIsPrimaryOwner()==null )
						{
							errorMap.put("COUPLE_DETAILS_ERROR", " Is Primary Owner is mandatory when application is created by counter employee ");
						}else if(couple.getBride().getIsPrimaryOwner())
						{
							isPrimaryOwnerTrueCounter.add(couple.getBride().getIsPrimaryOwner());
						}
						
				}
				
				if(couple.getBride().getIsGroom()==null)
					errorMap.put("COUPLE_DETAILS_ERROR", " Is Groom is mandatory ");
				
				if(couple.getBride().getIsGroom()==null)
					errorMap.put("COUPLE_DETAILS_ERROR", " Is Groom should be false in Bride json ");
					
				if(StringUtils.isEmpty(couple.getBride().getFirstName()))
					errorMap.put("COUPLE_DETAILS_ERROR", " Name is mandatory ");
				if(couple.getBride().getDateOfBirth() == null )
					errorMap.put("COUPLE_DETAILS_ERROR", " Date Of Birth is mandatory ");
				if(StringUtils.isEmpty(couple.getBride().getFatherName()))
					errorMap.put("COUPLE_DETAILS_ERROR", "Father Name is mandatory ");
				if(StringUtils.isEmpty(couple.getBride().getMotherName()))
					errorMap.put("COUPLE_DETAILS_ERROR", "Mother Name is mandatory ");
				
				
				if(couple.getBride().getAddress()!=null)
				{
					if(StringUtils.isEmpty(couple.getBride().getAddress().getAddressLine1()))
						errorMap.put("COUPLE_ADDRESS_ERROR", " Address is mandatory ");
					
					if(StringUtils.isEmpty(couple.getBride().getAddress().getCountry()))
						errorMap.put("COUPLE_ADDRESS_ERROR", " Country is mandatory ");
					
					if(StringUtils.isEmpty(couple.getBride().getAddress().getState()))
						errorMap.put("COUPLE_ADDRESS_ERROR", " State is mandatory ");
					
					if(StringUtils.isEmpty(couple.getBride().getAddress().getPinCode()))
						errorMap.put("COUPLE_ADDRESS_ERROR", " Pin Code is mandatory ");
					
					if(StringUtils.isEmpty(couple.getBride().getAddress().getContact()))
						errorMap.put("COUPLE_DETAILS_ERROR", "Contact is mandatory ");
					if(StringUtils.isEmpty(couple.getBride().getAddress().getEmailAddress()))
						errorMap.put("COUPLE_DETAILS_ERROR", "Email Address is mandatory ");
				}
				
				if(couple.getBride().getGuardianDetails()!=null)
				{
					if(StringUtils.isEmpty(couple.getBride().getGuardianDetails().getAddressLine1()))
						errorMap.put("GUARDIAN_ADDRESS_ERROR", " Address is mandatory ");
					
					if(StringUtils.isEmpty(couple.getBride().getGuardianDetails().getCountry()))
						errorMap.put("GUARDIAN_ADDRESS_ERROR", " Country is mandatory ");
					
					if(StringUtils.isEmpty(couple.getBride().getGuardianDetails().getState()))
						errorMap.put("GUARDIAN_ADDRESS_ERROR", " State is mandatory ");
					
					if(StringUtils.isEmpty(couple.getBride().getGuardianDetails().getPinCode()))
						errorMap.put("GUARDIAN_ADDRESS_ERROR", " Pin Code is mandatory ");
					
					
					if(StringUtils.isEmpty(couple.getBride().getGuardianDetails().getRelationship()))
						errorMap.put("GUARDIAN_DETAILS_ERROR", "Guardian Relationship is mandatory ");
					
					if(StringUtils.isEmpty(couple.getBride().getGuardianDetails().getName()))
						errorMap.put("GUARDIAN_DETAILS_ERROR", "Guardian Name is mandatory ");
					
					if(StringUtils.isEmpty(couple.getBride().getGuardianDetails().getContact()))
						errorMap.put("GUARDIAN_DETAILS_ERROR", "Guardian Contact is mandatory ");
					
					if(StringUtils.isEmpty(couple.getBride().getGuardianDetails().getEmailAddress()))
						errorMap.put("GUARDIAN_DETAILS_ERROR", "Guardian Email Address is mandatory ");
				}
				
				if(couple.getBride().getWitness() != null)
				{

					
					if(StringUtils.isEmpty(couple.getBride().getWitness().getAddress()))
						errorMap.put("WITNESS_DETAILS_ERROR", "Witness Address is mandatory ");
					
					if(StringUtils.isEmpty(couple.getBride().getWitness().getFirstName()))
						errorMap.put("WITNESS_DETAILS_ERROR", "Witness Name is mandatory ");
					
					if(StringUtils.isEmpty(couple.getBride().getWitness().getCountry()))
						errorMap.put("WITNESS_DETAILS_ERROR", "Witness Country is mandatory ");
					
					if(StringUtils.isEmpty(couple.getBride().getWitness().getState()))
						errorMap.put("WITNESS_DETAILS_ERROR", "Witness State is mandatory ");
					
					if(StringUtils.isEmpty(couple.getBride().getWitness().getDistrict()))
						errorMap.put("WITNESS_DETAILS_ERROR", "Witness District is mandatory ");
					
					if(StringUtils.isEmpty(couple.getBride().getWitness().getPinCode()))
						errorMap.put("WITNESS_DETAILS_ERROR", "Witness PinCode is mandatory ");
					
					if(StringUtils.isEmpty(couple.getBride().getWitness().getContact()))
						errorMap.put("WITNESS_DETAILS_ERROR", "Witness Contact is mandatory ");
				}
				
				
				//===================================Groom Details=========================================
				

				if(couple.getGroom().getIsDivyang()==null)
					errorMap.put("COUPLE_DETAILS_ERROR", " IsDivyang is mandatory ");
				
				
				
				if(userRoles.contains("MR_CEMP"))
				{
						if(couple.getGroom().getIsPrimaryOwner()==null )
						{
							errorMap.put("COUPLE_DETAILS_ERROR", " Is Primary Owner is mandatory when application is created by counter employee ");
						}else if(couple.getGroom().getIsPrimaryOwner())
						{
							isPrimaryOwnerTrueCounter.add(couple.getGroom().getIsPrimaryOwner());
						}
						
				}
				
				if(couple.getGroom().getIsGroom()==null)
					errorMap.put("COUPLE_DETAILS_ERROR", " Is Groom is mandatory ");
				
				if(!couple.getGroom().getIsGroom())
					errorMap.put("COUPLE_DETAILS_ERROR", " Is Groom Should be true in Groom Details ");
				
				if(StringUtils.isEmpty(couple.getGroom().getFirstName()))
					errorMap.put("COUPLE_DETAILS_ERROR", " Name is mandatory ");
				if(couple.getGroom().getDateOfBirth() == null )
					errorMap.put("COUPLE_DETAILS_ERROR", " Date Of Birth is mandatory ");
				if(StringUtils.isEmpty(couple.getGroom().getFatherName()))
					errorMap.put("COUPLE_DETAILS_ERROR", "Father Name is mandatory ");
				if(StringUtils.isEmpty(couple.getGroom().getMotherName()))
					errorMap.put("COUPLE_DETAILS_ERROR", "Mother Name is mandatory ");
				
				
				if(couple.getGroom().getAddress()!=null)
				{
					if(StringUtils.isEmpty(couple.getGroom().getAddress().getAddressLine1()))
						errorMap.put("COUPLE_ADDRESS_ERROR", " Address is mandatory ");
					
					if(StringUtils.isEmpty(couple.getGroom().getAddress().getCountry()))
						errorMap.put("COUPLE_ADDRESS_ERROR", " Country is mandatory ");
					
					if(StringUtils.isEmpty(couple.getGroom().getAddress().getState()))
						errorMap.put("COUPLE_ADDRESS_ERROR", " State is mandatory ");
					
					if(StringUtils.isEmpty(couple.getGroom().getAddress().getPinCode()))
						errorMap.put("COUPLE_ADDRESS_ERROR", " Pin Code is mandatory ");
					
					if(StringUtils.isEmpty(couple.getGroom().getAddress().getContact()))
						errorMap.put("COUPLE_DETAILS_ERROR", "Contact is mandatory ");
					if(StringUtils.isEmpty(couple.getGroom().getAddress().getEmailAddress()))
						errorMap.put("COUPLE_DETAILS_ERROR", "Email Address is mandatory ");
				}
				
				if(couple.getGroom().getGuardianDetails()!=null)
				{
					if(StringUtils.isEmpty(couple.getGroom().getGuardianDetails().getAddressLine1()))
						errorMap.put("GUARDIAN_ADDRESS_ERROR", " Address is mandatory ");
					
					if(StringUtils.isEmpty(couple.getGroom().getGuardianDetails().getCountry()))
						errorMap.put("GUARDIAN_ADDRESS_ERROR", " Country is mandatory ");
					
					if(StringUtils.isEmpty(couple.getGroom().getGuardianDetails().getState()))
						errorMap.put("GUARDIAN_ADDRESS_ERROR", " State is mandatory ");
					
					if(StringUtils.isEmpty(couple.getGroom().getGuardianDetails().getPinCode()))
						errorMap.put("GUARDIAN_ADDRESS_ERROR", " Pin Code is mandatory ");
					
					
					if(StringUtils.isEmpty(couple.getGroom().getGuardianDetails().getRelationship()))
						errorMap.put("GUARDIAN_DETAILS_ERROR", "Guardian Relationship is mandatory ");
					
					if(StringUtils.isEmpty(couple.getGroom().getGuardianDetails().getName()))
						errorMap.put("GUARDIAN_DETAILS_ERROR", "Guardian Name is mandatory ");
					
					if(StringUtils.isEmpty(couple.getGroom().getGuardianDetails().getContact()))
						errorMap.put("GUARDIAN_DETAILS_ERROR", "Guardian Contact is mandatory ");
					
					if(StringUtils.isEmpty(couple.getGroom().getGuardianDetails().getEmailAddress()))
						errorMap.put("GUARDIAN_DETAILS_ERROR", "Guardian Email Address is mandatory ");
				}
				
				
				if(couple.getGroom().getWitness() != null)
				{
					
					if(StringUtils.isEmpty(couple.getGroom().getWitness().getAddress()))
						errorMap.put("WITNESS_DETAILS_ERROR", "Witness Address is mandatory ");
					
					if(StringUtils.isEmpty(couple.getGroom().getWitness().getFirstName()))
						errorMap.put("WITNESS_DETAILS_ERROR", "Witness Name is mandatory ");
					
					if(StringUtils.isEmpty(couple.getGroom().getWitness().getCountry()))
						errorMap.put("WITNESS_DETAILS_ERROR", "Witness Country is mandatory ");
					
					if(StringUtils.isEmpty(couple.getGroom().getWitness().getState()))
						errorMap.put("WITNESS_DETAILS_ERROR", "Witness State is mandatory ");
					
					if(StringUtils.isEmpty(couple.getGroom().getWitness().getDistrict()))
						errorMap.put("WITNESS_DETAILS_ERROR", "Witness District is mandatory ");
					
					if(StringUtils.isEmpty(couple.getGroom().getWitness().getPinCode()))
						errorMap.put("WITNESS_DETAILS_ERROR", "Witness PinCode is mandatory ");
					
					if(StringUtils.isEmpty(couple.getGroom().getWitness().getContact()))
						errorMap.put("WITNESS_DETAILS_ERROR", "Witness Contact is mandatory ");
				}
			
				
				
			});
			
			
				if(userRoles.contains("MR_CEMP"))
				{
					int trueCounter = 0 ;
					for (Boolean isPrimary : isPrimaryOwnerTrueCounter) {
						if(isPrimary)
							trueCounter++;
					}
					if(trueCounter==0)
						errorMap.put("COUPLE_DETAILS_ERROR", " Either one of the Bride or Groom should be the primary owner if the application is created by counter employee ");
					if(trueCounter==2)
						errorMap.put("COUPLE_DETAILS_ERROR", " Both Bride and Groom cannot be the primary owner if the application is created by counter employee ");
				}
			}
			
			if (marriageRegistration.getAction().equalsIgnoreCase(ACTION_APPLY)) {
                if(marriageRegistration.getApplicationDocuments()==null)
                	errorMap.put("APPLICATION_DOCUMENTS_ERROR", " Application Documents are mandatory if the action is Apply ");
            }
			
			if (marriageRegistration.getAction().equalsIgnoreCase(ACTION_SCHEDULE) || marriageRegistration.getAction().equalsIgnoreCase(ACTION_RESCHEDULE)) {
                if(marriageRegistration.getAppointmentDetails()==null)
                	errorMap.put("APPOINTMENT_DETAILS_ERROR", " Appointment Details are mandatory if the action is SCHEDULE or RESCHEDULE ");
                
                if(marriageRegistration.getAppointmentDetails()!=null)
                {
                	marriageRegistration.getAppointmentDetails().forEach(appointment -> {
                		if(appointment.getStartTime()  == null)
                			errorMap.put("APPOINTMENT_DEATILS_ERROR", " Appointment Start time is mandatory if the action is SCHEDULE or RESCHEDULE ");
                		if(appointment.getEndTime() == null)
                			errorMap.put("APPOINTMENT_TIME_ERROR", " Appointment End Time is mandatory if the action is SCHEDULE or RESCHEDULE ");
                	});
                	
                	List<AppointmentDetails> apointmentDetailsActive  = marriageRegistration.getAppointmentDetails().stream().filter(appointment -> {return appointment.getActive();}).collect(Collectors.toList());
                	if(apointmentDetailsActive!= null)
                	{
                	if(apointmentDetailsActive.size() == 0)
                		errorMap.put("APPOINTMENT_DEATILS_ERROR", " Atleast One Appointment Start and End time should be active if the action is SCHEDULE or RESCHEDULE ");
                	if(apointmentDetailsActive.size() != 1)
                		errorMap.put("APPOINTMENT_DEATILS_ERROR", " Only One Appointment Start and End time should be active if the action is SCHEDULE or RESCHEDULE ");
                	}else
                		errorMap.put("APPOINTMENT_DEATILS_ERROR", " Atleast One Appointment Start and End time should be active if the action is SCHEDULE or RESCHEDULE ");
                }
                
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
            
            if ( !(marriageRegistration.getAction().equalsIgnoreCase(ACTION_SCHEDULE) || marriageRegistration.getAction().equalsIgnoreCase(ACTION_RESCHEDULE))) {
            	marriageRegistration.setAppointmentDetails(idToMarriageRegistrationFromSearch.get(marriageRegistration.getId()).getAppointmentDetails());
            }

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

            compareIdList(getAccountId(searchedMarriageRegistration),getAccountId(marriageRegistrationObj),errorMap);
            compareIdList(getCouple(searchedMarriageRegistration),getCouple(marriageRegistrationObj),errorMap);
            compareIdList(getAddress(searchedMarriageRegistration),getAddress(marriageRegistrationObj),errorMap);
            compareIdList(getGuardianDetails(searchedMarriageRegistration),getGuardianDetails(marriageRegistrationObj),errorMap);
            compareIdList(getWitness(searchedMarriageRegistration),getWitness(marriageRegistrationObj),errorMap);
            compareIdList(getApplicationDocIds(searchedMarriageRegistration),getApplicationDocIds(marriageRegistrationObj),errorMap);
            compareIdList(getAppointmentDetailIds(searchedMarriageRegistration),getAppointmentDetailIds(marriageRegistrationObj),errorMap);
            compareIdList(getVerficationDocIds(searchedMarriageRegistration),getVerficationDocIds(marriageRegistrationObj),errorMap);
        });

        if(!CollectionUtils.isEmpty(errorMap))
            throw new CustomException(errorMap);
    }
    
    
    private List<String> getAccountId(MarriageRegistration marriageRegistration)
    {
    	List<String> accountIds = new LinkedList<>();
    	accountIds.add(marriageRegistration.getAccountId());
    	
    	return accountIds;
    }
    
    
    private List<String> getCouple(MarriageRegistration marriageRegistration){
        List<String> coupleIds = new LinkedList<>();
        if(!CollectionUtils.isEmpty(marriageRegistration.getCoupleDetails())){
        	marriageRegistration.getCoupleDetails().forEach(couple -> {
                coupleIds.add(couple.getBride().getId());
                coupleIds.add(couple.getGroom().getId());
            });
        }
        return coupleIds;
    }
    
   
    
    private List<String> getAddress(MarriageRegistration marriageRegistration){
        List<String> coupleAddressIds = new LinkedList<>();
        if(!CollectionUtils.isEmpty(marriageRegistration.getCoupleDetails())){
        	marriageRegistration.getCoupleDetails().forEach(couple -> {
        		if(couple.getBride().getAddress()!=null)
                coupleAddressIds.add(couple.getBride().getAddress().getId());
        		if(couple.getGroom().getAddress()!=null)
                    coupleAddressIds.add(couple.getGroom().getAddress().getId());
            });
        }
        return coupleAddressIds;
    }
    
    private List<String> getGuardianDetails(MarriageRegistration marriageRegistration){
        List<String> coupleGuardianIds = new LinkedList<>();
        if(!CollectionUtils.isEmpty(marriageRegistration.getCoupleDetails())){
        	marriageRegistration.getCoupleDetails().forEach(couple -> {
        		if(couple.getBride().getGuardianDetails()!=null)
                coupleGuardianIds.add(couple.getBride().getGuardianDetails().getId());
        		if(couple.getGroom().getGuardianDetails()!=null)
                    coupleGuardianIds.add(couple.getGroom().getGuardianDetails().getId());
            });
        }
        return coupleGuardianIds;
    }
    
    private List<String> getWitness(MarriageRegistration marriageRegistration){
        List<String> coupleWitnessIds = new LinkedList<>();
        if(!CollectionUtils.isEmpty(marriageRegistration.getCoupleDetails())){
        	marriageRegistration.getCoupleDetails().forEach(couple -> {
        		if(couple.getBride().getWitness()!=null)
        			coupleWitnessIds.add(couple.getBride().getWitness().getId());
        		if(couple.getGroom().getWitness()!=null)
        			coupleWitnessIds.add(couple.getGroom().getWitness().getId());
            });
        }
        return coupleWitnessIds;
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
    
    private List<String> getAppointmentDetailIds(MarriageRegistration marriageRegistration){
        List<String> appointmentDetailIds = new LinkedList<>();
        if(!CollectionUtils.isEmpty(marriageRegistration.getAppointmentDetails())){
            marriageRegistration.getAppointmentDetails().forEach(appointment -> {
                appointmentDetailIds.add(appointment.getId());
            });
        }
        return appointmentDetailIds;
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

	public void validateNonUpdatableFileds(MarriageRegistrationRequest marriageRegistartionRequest,List<MarriageRegistration> searchResult) {
		
		Map<String,MarriageRegistration> idToMarriageRegistrationFromSearch = new HashMap<>();
    	searchResult.forEach(marriageRegistration -> {
    		idToMarriageRegistrationFromSearch.put(marriageRegistration.getId(),marriageRegistration);
    	});
    	
    	marriageRegistartionRequest.getMarriageRegistrations().forEach(marriageRegistration -> {
    		if(!marriageRegistration.getApplicationType().toString().equalsIgnoreCase(idToMarriageRegistrationFromSearch.get(marriageRegistration.getId()).getApplicationType().toString()))
			{
				throw new CustomException("APPLICATION TYPE ERROR","The Application type cannot be modified .");
			}
    		
    		if(!marriageRegistration.getAction().equalsIgnoreCase(MRConstants.ACTION_INITIATE))
    		{
    			if(!marriageRegistration.getTenantId().equalsIgnoreCase(idToMarriageRegistrationFromSearch.get(marriageRegistration.getId()).getTenantId()))
    			{
    				throw new CustomException("TENANT ID ERROR","The Tenant Id cannot be modified .");
    			}
    			
    			if(!marriageRegistration.getAccountId().equalsIgnoreCase(idToMarriageRegistrationFromSearch.get(marriageRegistration.getId()).getAccountId()))
    			{
    				throw new CustomException("ACCOUNT ID ERROR","The Account Id cannot be modified .");
    			}
    			
    			
    		}
    	});    	
    	
	}


}
