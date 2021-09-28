package org.egov.mr.service.notification;

import com.jayway.jsonpath.DocumentContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import lombok.extern.slf4j.Slf4j;
import net.minidev.json.JSONArray;
import org.apache.commons.lang.StringUtils;
import org.egov.common.contract.request.RequestInfo;
import org.egov.mr.config.MRConfiguration;
import org.egov.mr.model.user.Citizen;
import org.egov.mr.service.MarriageRegistrationService;
import org.egov.mr.util.MRCorrectionNotificationUtil;
import org.egov.mr.util.MarriageRegistrationUtil;
import org.egov.mr.util.NotificationUtil;
import org.egov.mr.web.models.MarriageRegistration;
import org.egov.mr.web.models.MarriageRegistrationSearchCriteria;
import org.egov.mr.web.models.SMSRequest;
import org.egov.tracer.model.CustomException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;

import static org.egov.mr.util.MRConstants.*;



@Service
@Slf4j
public class PaymentNotificationService {


    private MRConfiguration config;

    private MarriageRegistrationService marriageRegistrationService;

    private NotificationUtil util;
    
    private ObjectMapper mapper;


    private MRNotificationService mrNotificationService;

    private MRCorrectionNotificationUtil mrCorrectionNotificationUtil;
    
    private MarriageRegistrationUtil marriageRegistrationUtil ;

    @Autowired
    public PaymentNotificationService(MRConfiguration config, MarriageRegistrationService marriageRegistrationService,
                                      NotificationUtil util, ObjectMapper mapper ,MRNotificationService mrNotificationService,MRCorrectionNotificationUtil mrCorrectionNotificationUtil , MarriageRegistrationUtil marriageRegistrationUtil) {
        this.config = config;
        this.marriageRegistrationService = marriageRegistrationService;
        this.util = util;
        this.mapper = mapper;
        this.mrNotificationService = mrNotificationService;
        this.mrCorrectionNotificationUtil=mrCorrectionNotificationUtil;
        this.marriageRegistrationUtil=marriageRegistrationUtil;
    }





    final String tenantIdKey = "tenantId";

    final String businessServiceKey = "businessService";

    final String consumerCodeKey = "consumerCode";

    final String payerMobileNumberKey = "mobileNumber";

    final String paidByKey = "paidBy";

    final String amountPaidKey = "amountPaid";

    final String receiptNumberKey = "receiptNumber";

    final String payerNameKey = "payerName";

    /**
     * Generates sms from the input record and Sends smsRequest to SMSService
     * @param record The kafka message from receipt create topic
     */
    public void process(HashMap<String, Object> record){
        processBusinessService(record, businessService_MR);
    }


    private void processBusinessService(HashMap<String, Object> record, String businessService)
    {
        try{
            String jsonString = new JSONObject(record).toString();
            DocumentContext documentContext = JsonPath.parse(jsonString);
            Map<String,String> valMap = enrichValMap(documentContext, businessService);
            if(!StringUtils.equals(businessService,valMap.get(businessServiceKey)))
                return;
            Map<String, Object> info = documentContext.read("$.RequestInfo");
            RequestInfo requestInfo = mapper.convertValue(info, RequestInfo.class);

            if(valMap.get(businessServiceKey).equalsIgnoreCase(config.getBusinessServiceMR())){
                MarriageRegistration marriageRegistration = getMarriageRegistrationFromConsumerCode(valMap.get(tenantIdKey),valMap.get(consumerCodeKey),
                        requestInfo,valMap.get(businessServiceKey));
                switch(valMap.get(businessServiceKey))
                {
                    case businessService_MR:
                        String applicationType = String.valueOf(marriageRegistration.getApplicationType());
                        if(applicationType.equals(APPLICATION_TYPE_NEW)){
                            String localizationMessages = util.getLocalizationMessages(marriageRegistration.getTenantId(), requestInfo);
                            List<SMSRequest> smsRequests = getSMSRequests(requestInfo , marriageRegistration, valMap, localizationMessages);
                            util.sendSMS(smsRequests, config.getIsMRSMSEnabled());
                        }
                        
                        break;

                
                }
            }
        }
        catch (Exception e){
			log.error("NOTIFICATION_ERROR", e);
        }
    }
    
    
    private List<SMSRequest> getSMSRequests(RequestInfo requestInfo , MarriageRegistration marriageRegistration, Map<String,String> valMap,String localizationMessages){
            List<SMSRequest> ownersSMSRequest = getOwnerSMSRequest(requestInfo, marriageRegistration,valMap,localizationMessages);
            SMSRequest payerSMSRequest = getPayerSMSRequest(marriageRegistration,valMap,localizationMessages);

            List<SMSRequest> totalSMS = new LinkedList<>();
            totalSMS.addAll(ownersSMSRequest);
            totalSMS.add(payerSMSRequest);

            return totalSMS;
    }


    /**
     * Creates SMSRequest for the owners
     * @param marriageRegistration The MarriageRegistration for which the receipt is created
     * @param valMap The Map containing the values from receipt
     * @param localizationMessages The localization message to be sent
     * @return The list of the SMS Requests
     */
    private List<SMSRequest> getOwnerSMSRequest(RequestInfo requestInfo , MarriageRegistration marriageRegistration, Map<String,String> valMap,String localizationMessages){
        String applicationType = String.valueOf(marriageRegistration.getApplicationType());
        String message=null;
        if(applicationType.equals(APPLICATION_TYPE_NEW)){
        	message = util.getOwnerPaymentMsg(marriageRegistration,valMap,localizationMessages);
        }
       

        HashMap<String,String> mobileNumberToOwnerName = new HashMap<>();
        Citizen citizen = marriageRegistrationUtil.getMobileNumberWithUuid(marriageRegistration.getAccountId(), requestInfo , marriageRegistration.getTenantId());
        
        if (citizen != null)
        	mobileNumberToOwnerName.put(citizen.getMobileNumber() , citizen.getName());

        List<SMSRequest> smsRequests = new LinkedList<>();

        for(Map.Entry<String,String> entrySet : mobileNumberToOwnerName.entrySet()){
            String customizedMsg = message.replace("<1>",entrySet.getValue());
            smsRequests.add(new SMSRequest(entrySet.getKey(),customizedMsg));
        }
        return smsRequests;
    }


    /**
     * Creates SMSRequest to be send to the payer
     * @param valMap The Map containing the values from receipt
     * @param localizationMessages The localization message to be sent
     * @return
     */
    private SMSRequest getPayerSMSRequest(MarriageRegistration marriageRegistration,Map<String,String> valMap,String localizationMessages){
        String applicationType = String.valueOf(marriageRegistration.getApplicationType());
        String message=null;
        if(applicationType.equals(APPLICATION_TYPE_NEW)){
        	message = util.getPayerPaymentMsg(marriageRegistration,valMap,localizationMessages);
        }
        

        String customizedMsg = message.replace("<1>",valMap.get(payerNameKey));
        SMSRequest smsRequest = new SMSRequest(valMap.get(payerMobileNumberKey),customizedMsg);
        return smsRequest;
    }


    /**
     * Enriches the map with values from receipt
     * @param context The documentContext of the receipt
     * @return The map containing required fields from receipt
     */
    private Map<String,String> enrichValMap(DocumentContext context, String businessService){
        Map<String,String> valMap = new HashMap<>();
        try{

            List <String>businessServiceList=context.read("$.Payment.paymentDetails[?(@.businessService=='"+businessService+"')].businessService");
            List <String>consumerCodeList=context.read("$.Payment.paymentDetails[?(@.businessService=='"+businessService+"')].bill.consumerCode");
            List <String>mobileNumberList=context.read("$.Payment.paymentDetails[?(@.businessService=='"+businessService+"')].bill.mobileNumber");
            List <Integer>amountPaidList=context.read("$.Payment.paymentDetails[?(@.businessService=='"+businessService+"')].bill.amountPaid");
            List <String>receiptNumberList=context.read("$.Payment.paymentDetails[?(@.businessService=='"+businessService+"')].receiptNumber");
            valMap.put(businessServiceKey,businessServiceList.isEmpty()?null:businessServiceList.get(0));
            valMap.put(consumerCodeKey,consumerCodeList.isEmpty()?null:consumerCodeList.get(0));
            valMap.put(tenantIdKey,context.read("$.Payment.tenantId"));
            valMap.put(payerMobileNumberKey,context.read("$.Payment.mobileNumber"));
            valMap.put(paidByKey,context.read("$.Payment.paidBy"));
            valMap.put(amountPaidKey,amountPaidList.isEmpty()?null:String.valueOf(amountPaidList.get(0)));
            valMap.put(receiptNumberKey,receiptNumberList.isEmpty()?null:receiptNumberList.get(0));
            valMap.put(payerNameKey,context.read("$.Payment.payerName"));
        }
        catch (Exception e){
            throw new CustomException("RECEIPT ERROR","Unable to fetch values from receipt");
        }
        return valMap;
    }


    /**
     * Searches the MarriageRegistration based on the consumer code as applicationNumber
     * @param tenantId tenantId of the MarriageRegistration
     * @param consumerCode The consumerCode of the receipt
     * @param requestInfo The requestInfo of the request
     * @return MarriageRegistration for the particular consumerCode
     */
    private MarriageRegistration getMarriageRegistrationFromConsumerCode(String tenantId,String consumerCode,RequestInfo requestInfo, String businessService){

    	MarriageRegistrationSearchCriteria searchCriteria = new MarriageRegistrationSearchCriteria();
        searchCriteria.setApplicationNumber(consumerCode);
        searchCriteria.setTenantId(tenantId);
        searchCriteria.setBusinessService(businessService);
        List<MarriageRegistration> marriageRegistrations = marriageRegistrationService.getMarriageRegistrationsWithOwnerInfo(searchCriteria,requestInfo);

        if(CollectionUtils.isEmpty(marriageRegistrations))
            throw new CustomException("INVALID RECEIPT","No marriageRegistration found for the consumerCode: "
                    +consumerCode+" and tenantId: "+tenantId);

        if(marriageRegistrations.size()!=1)
            throw new CustomException("INVALID RECEIPT","Multiple marriageRegistrations found for the consumerCode: "
                    +consumerCode+" and tenantId: "+tenantId);

        return marriageRegistrations.get(0);

    }
}
