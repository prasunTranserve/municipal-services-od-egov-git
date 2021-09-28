package org.egov.mr.service.notification;

import org.apache.commons.lang3.StringUtils;
import org.egov.mr.config.MRConfiguration;
import org.egov.mr.model.user.Citizen;
import org.egov.mr.util.MarriageRegistrationUtil;
import org.egov.mr.util.NotificationUtil;
import org.egov.mr.web.models.Difference;
import org.egov.mr.web.models.MarriageRegistration;
import org.egov.mr.web.models.MarriageRegistrationRequest;
import org.egov.mr.web.models.SMSRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import  static org.egov.mr.util.MRConstants.*;


@Service
public class EditNotificationService {


    private NotificationUtil util;
    
    @Autowired
    private MarriageRegistrationUtil marriageRegistrationUtil ;

    @Autowired
    private MRConfiguration config;

    @Autowired
    public EditNotificationService(NotificationUtil util) {
        this.util = util;
    }

    public void sendEditNotification(MarriageRegistrationRequest request, Map<String, Difference> diffMap) {
        List<SMSRequest> smsRequests = enrichSMSRequest(request, diffMap);
        String businessService = request.getMarriageRegistrations().isEmpty()?null:request.getMarriageRegistrations().get(0).getBusinessService();
        if (businessService == null)
            businessService = businessService_MR;
        switch(businessService)
        {
            case businessService_MR:
                util.sendSMS(smsRequests,config.getIsMRSMSEnabled());
                break;

        }
    }

    /**
     * Creates smsRequest for edits done in update
     * @param request The update Request
     * @param diffMap The map of id to Difference for each marriageRegistration
     * @return The smsRequest
     */
    private List<SMSRequest> enrichSMSRequest(MarriageRegistrationRequest request, Map<String, Difference> diffMap) {
        List<SMSRequest> smsRequests = new LinkedList<>();
        String tenantId = request.getMarriageRegistrations().get(0).getTenantId();
        String localizationMessages = util.getLocalizationMessages(tenantId, request.getRequestInfo());
        for (MarriageRegistration marriageRegistration : request.getMarriageRegistrations()) {
            Difference diff = diffMap.get(marriageRegistration.getId());
            String message = util.getCustomizedMsg(diff, marriageRegistration, localizationMessages);
            if (StringUtils.isEmpty(message)) continue;

            Map<String, String> mobileNumberToOwner = new HashMap<>();

            Citizen citizen = marriageRegistrationUtil.getMobileNumberWithUuid(marriageRegistration.getAccountId(), request.getRequestInfo(), tenantId);
            
                if (citizen != null)
                    mobileNumberToOwner.put(citizen.getMobileNumber() , citizen.getName());
                
                
            smsRequests.addAll(util.createSMSRequest(message, mobileNumberToOwner));
        }
        return smsRequests;
    }

}
