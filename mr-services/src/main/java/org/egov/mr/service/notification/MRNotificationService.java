package org.egov.mr.service.notification;

import com.jayway.jsonpath.JsonPath;
import lombok.extern.slf4j.Slf4j;
import org.egov.common.contract.request.RequestInfo;
import org.egov.mr.config.MRConfiguration;
import org.egov.mr.model.user.Citizen;
import org.egov.mr.repository.ServiceRequestRepository;
import org.egov.mr.util.MRConstants;
import org.egov.mr.util.MRCorrectionNotificationUtil;
import org.egov.mr.util.MarriageRegistrationUtil;
import org.egov.mr.util.NotificationUtil;
import org.egov.mr.web.models.Action;
import org.egov.mr.web.models.ActionItem;
import org.egov.mr.web.models.Event;
import org.egov.mr.web.models.EventRequest;
import org.egov.mr.web.models.MarriageRegistration;
import org.egov.mr.web.models.MarriageRegistrationRequest;
import org.egov.mr.web.models.Recepient;
import org.egov.mr.web.models.SMSRequest;
import org.egov.mr.web.models.Source;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

import static org.egov.mr.util.MRConstants.*;


@Slf4j
@Service
public class MRNotificationService {


    private MRConfiguration config;

    private ServiceRequestRepository serviceRequestRepository;

    private NotificationUtil util;


	private MRCorrectionNotificationUtil mrCorrectionNotificationUtil;
	
    private MarriageRegistrationUtil marriageRegistrationUtil ;

	@Autowired
	public MRNotificationService(MRConfiguration config, ServiceRequestRepository serviceRequestRepository, NotificationUtil util,MRCorrectionNotificationUtil mrCorrectionNotificationUtil ,MarriageRegistrationUtil marriageRegistrationUtil) {
		this.config = config;
		this.serviceRequestRepository = serviceRequestRepository;
		this.util = util;
		this.mrCorrectionNotificationUtil = mrCorrectionNotificationUtil;
		this.marriageRegistrationUtil = marriageRegistrationUtil;
	}

    /**
     * Creates and send the sms based on the MarriageRegistrationRequest
     * @param request The MarriageRegistrationRequest listenend on the kafka topic
     */
    public void process(MarriageRegistrationRequest request){

        String businessService = request.getMarriageRegistrations().isEmpty()?null:request.getMarriageRegistrations().get(0).getBusinessService();
		if (businessService == null)
			businessService = businessService_MR;
		switch(businessService)
		{
			case businessService_MR:
				List<SMSRequest> smsRequestsTL = new LinkedList<>();
				if(null != config.getIsMRSMSEnabled()) {
					if(config.getIsMRSMSEnabled()) {
						enrichSMSRequest(request,smsRequestsTL);
						if(!CollectionUtils.isEmpty(smsRequestsTL))
							util.sendSMS(smsRequestsTL,true);
					}
				}
				if(null != config.getIsUserEventsNotificationEnabledForMR()) {
					if(config.getIsUserEventsNotificationEnabledForMR()) {
						EventRequest eventRequest = getEventsForMR(request);
						if(null != eventRequest)
							util.sendEventNotification(eventRequest);
					}
				}
				break;

		
		}
    }

    /**
     * Enriches the smsRequest with the customized messages
     * @param request The MarriageRegistrationRequest from kafka topic
     * @param smsRequests List of SMSRequets
     */
    private void enrichSMSRequest(MarriageRegistrationRequest request,List<SMSRequest> smsRequests){
        String tenantId = request.getMarriageRegistrations().get(0).getTenantId();
        for(MarriageRegistration marriageRegistration : request.getMarriageRegistrations()){
			String businessService = marriageRegistration.getBusinessService();
			if (businessService == null)
				businessService = businessService_MR;
			String message = null;
			String applicationType = String.valueOf(marriageRegistration.getApplicationType());
			if (businessService.equals(businessService_MR)) {
				if(applicationType.equals(APPLICATION_TYPE_CORRECTION)){
					String localizationMessages = mrCorrectionNotificationUtil.getLocalizationMessages(tenantId, request.getRequestInfo());
					message = mrCorrectionNotificationUtil.getCustomizedMsg(request.getRequestInfo(), marriageRegistration, localizationMessages);
				}
				else{
					String localizationMessages = util.getLocalizationMessages(tenantId, request.getRequestInfo());
					message = util.getCustomizedMsg(request.getRequestInfo(), marriageRegistration, localizationMessages);
				}

			}
			
			
            if(message==null) continue;

            Map<String,String > mobileNumberToOwner = new HashMap<>();

            Citizen citizen = marriageRegistrationUtil.getMobileNumberWithUuid(marriageRegistration.getAccountId(), request.getRequestInfo(), tenantId);
            
            if (citizen != null)
                mobileNumberToOwner.put(citizen.getMobileNumber() , citizen.getName());
            
            smsRequests.addAll(util.createSMSRequest(message,mobileNumberToOwner));
        }
    }
    
    /**
     * Creates and registers an event at the egov-user-event service at defined trigger points as that of sms notifs.
     * 
     * Assumption - The MarriageRegistrationRequest received will always contain only one Marriage Registration.
     * 
     * @param request
     * @return
     */
    private EventRequest getEventsForMR(MarriageRegistrationRequest request) {
    	List<Event> events = new ArrayList<>();
        String tenantId = request.getMarriageRegistrations().get(0).getTenantId();
		
        for(MarriageRegistration marriageRegistration : request.getMarriageRegistrations()){
			String message = null;
			String applicationType = String.valueOf(marriageRegistration.getApplicationType());
			String businessService = marriageRegistration.getBusinessService();
			if(businessService.equals(businessService_MR)){
				if(applicationType.equals(APPLICATION_TYPE_CORRECTION))
				{
					String localizationMessages = mrCorrectionNotificationUtil.getLocalizationMessages(tenantId,request.getRequestInfo());
					message = mrCorrectionNotificationUtil.getCustomizedMsg(request.getRequestInfo(), marriageRegistration, localizationMessages);
				}
				else
				{
					String localizationMessages = util.getLocalizationMessages(tenantId,request.getRequestInfo());
					message = util.getCustomizedMsg(request.getRequestInfo(), marriageRegistration, localizationMessages);
				}
			}
			
            if(message == null) continue;
            Map<String,String > mobileNumberToOwner = new HashMap<>();
            
            Citizen citizen = marriageRegistrationUtil.getMobileNumberWithUuid(marriageRegistration.getAccountId(), request.getRequestInfo(), tenantId);
            
            if (citizen != null)
                mobileNumberToOwner.put(citizen.getMobileNumber() , citizen.getName());
            
            List<SMSRequest> smsRequests = util.createSMSRequest(message,mobileNumberToOwner);
        	Set<String> mobileNumbers = smsRequests.stream().map(SMSRequest :: getMobileNumber).collect(Collectors.toSet());
        	Map<String, String> mapOfPhnoAndUUIDs = fetchUserUUIDs(mobileNumbers, request.getRequestInfo(), request.getMarriageRegistrations().get(0).getTenantId());
    		if (CollectionUtils.isEmpty(mapOfPhnoAndUUIDs.keySet())) {
    			log.info("UUID search failed!");
    			continue;
    		}
            Map<String,String > mobileNumberToMsg = smsRequests.stream().collect(Collectors.toMap(SMSRequest::getMobileNumber, SMSRequest::getMessage));		
            for(String mobile: mobileNumbers) {
    			if(null == mapOfPhnoAndUUIDs.get(mobile) || null == mobileNumberToMsg.get(mobile)) {
    				log.error("No UUID/SMS for mobile {} skipping event", mobile);
    				continue;
    			}
    			List<String> toUsers = new ArrayList<>();
    			toUsers.add(mapOfPhnoAndUUIDs.get(mobile));
    			Recepient recepient = Recepient.builder().toUsers(toUsers).toRoles(null).build();
    			List<String> payTriggerList = Arrays.asList(config.getPayTriggers().split("[,]"));
				List<String> viewTriggerList = Arrays.asList(config.getViewApplicationTriggers().split("[,]"));
	   			Action action = null;
    			if(payTriggerList.contains(marriageRegistration.getStatus())) {
                    List<ActionItem> items = new ArrayList<>();
        			String actionLink = config.getPayLink().replace("$mobile", mobile)
        						.replace("$applicationNo", marriageRegistration.getApplicationNumber())
        						.replace("$tenantId", marriageRegistration.getTenantId())
        						.replace("$businessService", marriageRegistration.getBusinessService());
        			actionLink = config.getUiAppHost() + actionLink;
        			ActionItem item = ActionItem.builder().actionUrl(actionLink).code(config.getPayCode()).build();
        			items.add(item);
        			action = Action.builder().actionUrls(items).build();
    			}
    			if(viewTriggerList.contains(marriageRegistration.getStatus())){
					List<ActionItem> items = new ArrayList<>();
					String actionLink = config.getViewApplicationLink().replace("$mobile", mobile)
							.replace("$applicationNo", marriageRegistration.getApplicationNumber())
							.replace("$tenantId", marriageRegistration.getTenantId());
					actionLink = config.getUiAppHost() + actionLink;
					ActionItem item = ActionItem.builder().actionUrl(actionLink).code(config.getViewApplicationCode()).build();
					items.add(item);
					action = Action.builder().actionUrls(items).build();

				}

				
				events.add(Event.builder().tenantId(marriageRegistration.getTenantId()).description(mobileNumberToMsg.get(mobile))
						.eventType(MRConstants.USREVENTS_EVENT_TYPE).name(MRConstants.USREVENTS_EVENT_NAME)
						.postedBy(MRConstants.USREVENTS_EVENT_POSTEDBY).source(Source.WEBAPP).recepient(recepient)
						.eventDetails(null).actions(action).build());
    			
    		}
        }
        if(!CollectionUtils.isEmpty(events)) {
    		return EventRequest.builder().requestInfo(request.getRequestInfo()).events(events).build();
        }else {
        	return null;
        }
		
    }


    
    
    
    /**
     * Fetches UUIDs of CITIZENs based on the phone number.
     * 
     * @param mobileNumbers
     * @param requestInfo
     * @param tenantId
     * @return
     */
    private Map<String, String> fetchUserUUIDs(Set<String> mobileNumbers, RequestInfo requestInfo, String tenantId) {
    	Map<String, String> mapOfPhnoAndUUIDs = new HashMap<>();
    	StringBuilder uri = new StringBuilder();
    	uri.append(config.getUserHost()).append(config.getUserSearchEndpoint());
    	Map<String, Object> userSearchRequest = new HashMap<>();
    	userSearchRequest.put("RequestInfo", requestInfo);
		userSearchRequest.put("tenantId", tenantId);
		userSearchRequest.put("userType", "CITIZEN");
    	for(String mobileNo: mobileNumbers) {
    		userSearchRequest.put("userName", mobileNo);
    		try {
    			Object user = serviceRequestRepository.fetchResult(uri, userSearchRequest);
    			if(null != user) {
    				String uuid = JsonPath.read(user, "$.user[0].uuid");
    				mapOfPhnoAndUUIDs.put(mobileNo, uuid);
    			}else {
        			log.error("Service returned null while fetching user for username - "+mobileNo);
    			}
    		}catch(Exception e) {
    			log.error("Exception while fetching user for username - "+mobileNo);
    			log.error("Exception trace: ",e);
    			continue;
    		}
    	}
    	return mapOfPhnoAndUUIDs;
    }







}