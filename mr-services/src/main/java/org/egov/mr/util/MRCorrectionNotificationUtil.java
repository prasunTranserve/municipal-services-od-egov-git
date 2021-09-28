package org.egov.mr.util;

import com.jayway.jsonpath.JsonPath;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.utils.URIBuilder;
import org.egov.common.contract.request.RequestInfo;
import org.egov.mr.config.MRConfiguration;
import org.egov.mr.producer.Producer;
import org.egov.mr.repository.ServiceRequestRepository;
import org.egov.mr.web.models.Difference;
import org.egov.mr.web.models.EventRequest;
import org.egov.mr.web.models.MarriageRegistration;
import org.egov.mr.web.models.RequestInfoWrapper;
import org.egov.mr.web.models.SMSRequest;
import org.egov.tracer.model.CustomException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.*;

import  static org.egov.mr.util.MRConstants.*;


@Component
@Slf4j
public class MRCorrectionNotificationUtil {

    private MRConfiguration config;

    private ServiceRequestRepository serviceRequestRepository;

    private Producer producer;

    private NotificationUtil notificationUtil;
    
    private RestTemplate restTemplate;

    @Autowired
    public MRCorrectionNotificationUtil(MRConfiguration config, ServiceRequestRepository serviceRequestRepository, Producer producer, NotificationUtil notificationUtil, RestTemplate restTemplate) {
        this.config = config;
        this.serviceRequestRepository = serviceRequestRepository;
        this.producer = producer;
        this.notificationUtil = notificationUtil;
        this.restTemplate = restTemplate;
    }



    final String receiptNumberKey = "receiptNumber";

    final String amountPaidKey = "amountPaid";

    /**
     * Creates customized message based on MarriageRegistration
     *
     * @param marriageRegistration
     *            The MarriageRegistration for which message is to be sent
     * @param localizationMessage
     *            The messages from localization
     * @return customized message based on MarriageRegistration
     */
    public String getCustomizedMsg(RequestInfo requestInfo, MarriageRegistration marriageRegistration, String localizationMessage) {
        String message = null, messageTemplate;
        String ACTION_STATUS = marriageRegistration.getAction() + "_" + marriageRegistration.getStatus();
        switch (ACTION_STATUS) {

            case ACTION_STATUS_INITIATED:
                messageTemplate = getMessageTemplate(MRConstants.CORRECTION_NOTIFICATION_INITIATED, localizationMessage);
                message = getInitiatedMsg(marriageRegistration, messageTemplate);
                break;


    		case ACTION_STATUS_DOCVERIFICATION:
    			messageTemplate = getMessageTemplate(MRConstants.CORRECTION_NOTIFICATION_APPLIED, localizationMessage);
    			message = getDocverificationMsg(marriageRegistration, messageTemplate);
    			break;
    			
    		case ACTION_STATUS_FORWARD_DOCVERIFICATION:
    			messageTemplate = getMessageTemplate(MRConstants.CORRECTION_NOTIFICATION_FORWARD_DOC_VERIFICATION, localizationMessage);
    			message = getDocverificationMsg(marriageRegistration, messageTemplate);
    			break;
    			
    		case ACTION_STATUS_FORWARD_PENDINGAPPROVAL:
    			messageTemplate = getMessageTemplate(MRConstants.CORRECTION_NOTIFICATION_FORWARD_PENDINGAPPROVAL, localizationMessage);
    			message = getPendingApprovalMsg(marriageRegistration, messageTemplate);
    			break;


            case ACTION_STATUS_APPROVED:
                messageTemplate = getMessageTemplate(MRConstants.CORRECTION_NOTIFICATION_APPROVED, localizationMessage);
                message = getApprovedMsg(marriageRegistration, messageTemplate);
                break;
                
            case ACTION_STATUS_REJECTED:
                messageTemplate = getMessageTemplate(MRConstants.CORRECTION_NOTIFICATION_REJECTED, localizationMessage);
                message = getRejectedMsg(marriageRegistration, messageTemplate);
                break;

            case ACTION_STATUS_SENDBACKTOCITIZEN_DOCVERIFICATION:
                messageTemplate = getMessageTemplate(MRConstants.CORRECTION_NOTIFICATION_SENDBACK_CITIZEN, localizationMessage);
                message = getCitizenSendBack(marriageRegistration, messageTemplate);
                break;
                
                
            case ACTION_STATUS_SENDBACKTOCITIZEN_PENDINGAPPROVAL:
                messageTemplate = getMessageTemplate(MRConstants.CORRECTION_NOTIFICATION_SENDBACK_CITIZEN, localizationMessage);
                message = getCitizenSendBack(marriageRegistration, messageTemplate);
                break;

            case ACTION_CANCEL_CANCELLED:
                messageTemplate = getMessageTemplate(MRConstants.NOTIFICATION_CANCELLED, localizationMessage);
                message = getCancelledMsg(marriageRegistration, messageTemplate);
                break;
        }

        return message;
    }

    /**
     * Extracts message for the specific code
     *
     * @param notificationCode
     *            The code for which message is required
     * @param localizationMessage
     *            The localization messages
     * @return message for the specific code
     */
    private String getMessageTemplate(String notificationCode, String localizationMessage) {
        String path = "$..messages[?(@.code==\"{}\")].message";
        path = path.replace("{}", notificationCode);
        String message = null;
        try {
            Object messageObj = JsonPath.parse(localizationMessage).read(path);
            message = ((ArrayList<String>) messageObj).get(0);
        } catch (Exception e) {
            log.warn("Fetching from localization failed", e);
        }
        return message;
    }

    /**
     * Returns the uri for the localization call
     *
     * @param tenantId
     *            TenantId of the propertyRequest
     * @return The uri for localization search call
     */
    public StringBuilder getUri(String tenantId, RequestInfo requestInfo) {

        if (config.getIsLocalizationStateLevel())
            tenantId = tenantId.split("\\.")[0];

        String locale = NOTIFICATION_LOCALE;
        if (!StringUtils.isEmpty(requestInfo.getMsgId()) && requestInfo.getMsgId().split("|").length >= 2)
            locale = requestInfo.getMsgId().split("\\|")[1];

        StringBuilder uri = new StringBuilder();
        uri.append(config.getLocalizationHost()).append(config.getLocalizationContextPath())
                .append(config.getLocalizationSearchEndpoint()).append("?").append("locale=").append(locale)
                .append("&tenantId=").append(tenantId).append("&module=").append(MRConstants.MODULE);

        return uri;
    }

    /**
     * Fetches messages from localization service
     *
     * @param tenantId
     *            tenantId of the MarriageRegistration
     * @param requestInfo
     *            The requestInfo of the request
     * @return Localization messages for the module
     */
    public String getLocalizationMessages(String tenantId, RequestInfo requestInfo) {
        LinkedHashMap responseMap = (LinkedHashMap) serviceRequestRepository.fetchResult(getUri(tenantId, requestInfo),
                requestInfo);
        String jsonString = new JSONObject(responseMap).toString();
        return jsonString;
    }

    /**
     * Creates customized message for initiate
     *
     * @param marriageRegistration
     *            tenantId of the MarriageRegistration
     * @param message
     *            Message from localization for initiate
     * @return customized message for initiate
     */
    private String getInitiatedMsg(MarriageRegistration marriageRegistration, String message) {
        // message = message.replace("<1>",marriageRegistration.);
        message = message.replace("<2>", marriageRegistration.getApplicationNumber());

        return message;
    }





    /**
     * Creates customized message for approved
     *
     * @param marriageRegistration
     *            tenantId of the MarriageRegistration
     * @param message
     *            Message from localization for approved
     * @return customized message for approved
     */
    private String getApprovedMsg(MarriageRegistration marriageRegistration, String message) {
    	message = message.replace("<2>", marriageRegistration.getMrNumber());


		return message;
	}

    private String epochToDate(Long validTo){
        Long timeStamp= validTo / 1000L;
        java.util.Date time=new java.util.Date((Long)timeStamp*1000);
        Calendar cal = Calendar.getInstance();
        cal.setTime(time);
        String day = String.valueOf(cal.get(Calendar.DAY_OF_MONTH));
        Integer mon = cal.get(Calendar.MONTH);
        mon=mon+1;
        String month = String.valueOf(mon);
        String year = String.valueOf(cal.get(Calendar.YEAR));
        StringBuilder date = new StringBuilder(day);
        date.append("/").append(month).append("/").append(year);

        return date.toString();
    }

    /**
     * Creates customized message for rejected
     *
     * @param marriageRegistration
     *            tenantId of the MarriageRegistration
     * @param message
     *            Message from localization for rejected
     * @return customized message for rejected
     */
    private String getRejectedMsg(MarriageRegistration marriageRegistration, String message) {
        // message = message.replace("<1>",);
        message = message.replace("<2>", marriageRegistration.getApplicationNumber());

        return message;
    }



	/**
	 * Creates customized message for doc verification
	 * 
	 * @param marriageRegistration
	 *            tenantId of the MarriageRegistration
	 * @param message
	 *            Message from localization for apply
	 * @return customized message for apply
	 */
	private String getDocverificationMsg(MarriageRegistration marriageRegistration, String message) {
		message = message.replace("<2>", marriageRegistration.getApplicationNumber());

		return message;
	}

    /**
     * Creates customized message for rejected
     *
     * @param marriageRegistration
     *            tenantId of the MarriageRegistration
     * @param message
     *            Message from localization for rejected
     * @return customized message for rejected
     */
    private String getPendingApprovalMsg(MarriageRegistration marriageRegistration, String message) {
        message = message.replace("<2>", marriageRegistration.getApplicationNumber());
        return message;
    }



    /**
     * Creates customized message for citizen sendback
     *
     * @param marriageRegistration
     *            tenantId of the MarriageRegistration
     * @param message
     *            Message from localization for cancelled
     * @return customized message for cancelled
     */
    private String getCitizenSendBack(MarriageRegistration marriageRegistration, String message) {
        message = message.replace("<2>", marriageRegistration.getApplicationNumber());

        return message;
    }

 

    /**
     * Creates customized message for cancelled
     *
     * @param marriageRegistration
     *            tenantId of the MarriageRegistration
     * @param message
     *            Message from localization for cancelled
     * @return customized message for cancelled
     */
    private String getCancelledMsg(MarriageRegistration marriageRegistration, String message) {
        message = message.replace("<2>", marriageRegistration.getMrNumber());

        return message;
    }

  

  

    /**
     * Send the SMSRequest on the SMSNotification kafka topic
     *
     * @param smsRequestList
     *            The list of SMSRequest to be sent
     */
    public void sendSMS(List<SMSRequest> smsRequestList, boolean isSMSEnabled) {
        if (isSMSEnabled) {
            if (CollectionUtils.isEmpty(smsRequestList))
                log.info("Messages from localization couldn't be fetched!");
            for (SMSRequest smsRequest : smsRequestList) {
                producer.push(config.getSmsNotifTopic(), smsRequest);
                log.info("MobileNumber: " + smsRequest.getMobileNumber() + " Messages: " + smsRequest.getMessage());
            }
        }
    }





    /**
     * Creates sms request for the each owners
     *
     * @param message
     *            The message for the specific MarriageRegistration
     * @param mobileNumberToOwnerName
     *            Map of mobileNumber to OwnerName
     * @return List of SMSRequest
     */
    public List<SMSRequest> createSMSRequest(String message, Map<String, String> mobileNumberToOwnerName) {
        List<SMSRequest> smsRequest = new LinkedList<>();
        for (Map.Entry<String, String> entryset : mobileNumberToOwnerName.entrySet()) {
            String customizedMsg = message.replace("<1>", entryset.getValue());
            smsRequest.add(new SMSRequest(entryset.getKey(), customizedMsg));
        }
        return smsRequest;
    }

    public String getCustomizedMsg(Difference diff, MarriageRegistration marriageRegistration, String localizationMessage) {
        String message = null, messageTemplate;
        // StringBuilder finalMessage = new StringBuilder();

        /*
         * if(!CollectionUtils.isEmpty(diff.getFieldsChanged())){ messageTemplate =
         * getMessageTemplate(MRConstants.NOTIFICATION_FIELD_CHANGED,localizationMessage
         * ); message = getEditMsg(marriageRegistration,diff.getFieldsChanged(),messageTemplate);
         * finalMessage.append(message); }
         *
         * if(!CollectionUtils.isEmpty(diff.getClassesAdded())){ messageTemplate =
         * getMessageTemplate(MRConstants.NOTIFICATION_OBJECT_ADDED,localizationMessage)
         * ; message = getEditMsg(marriageRegistration,diff.getClassesAdded(),messageTemplate);
         * finalMessage.append(message); }
         *
         * if(!CollectionUtils.isEmpty(diff.getClassesRemoved())){ messageTemplate =
         * getMessageTemplate(MRConstants.NOTIFICATION_OBJECT_REMOVED,
         * localizationMessage); message =
         * getEditMsg(marriageRegistration,diff.getClassesRemoved(),messageTemplate);
         * finalMessage.append(message); }
         */

        if (!CollectionUtils.isEmpty(diff.getFieldsChanged()) || !CollectionUtils.isEmpty(diff.getClassesAdded())
                || !CollectionUtils.isEmpty(diff.getClassesRemoved())) {
            messageTemplate = getMessageTemplate(MRConstants.NOTIFICATION_OBJECT_MODIFIED, localizationMessage);
            if (messageTemplate == null)
                messageTemplate = DEFAULT_OBJECT_MODIFIED_MSG;
            message = getEditMsg(marriageRegistration, messageTemplate);
        }

        return message;
    }

    /**
     * Creates customized message for field chnaged
     *
     * @param message
     *            Message from localization for field change
     * @return customized message for field change
     */
    private String getEditMsg(MarriageRegistration marriageRegistration, List<String> list, String message) {
        message = message.replace("<APPLICATION_NUMBER>", marriageRegistration.getApplicationNumber());
        message = message.replace("<FIELDS>", StringUtils.join(list, ","));
        return message;
    }

    private String getEditMsg(MarriageRegistration marriageRegistration, String message) {
        message = message.replace("<APPLICATION_NUMBER>", marriageRegistration.getApplicationNumber());
        return message;
    }

    /**
     * Pushes the event request to Kafka Queue.
     *
     * @param request
     */
    public void sendEventNotification(EventRequest request) {
        producer.push(config.getSaveUserEventsTopic(), request);
    }
    


}
