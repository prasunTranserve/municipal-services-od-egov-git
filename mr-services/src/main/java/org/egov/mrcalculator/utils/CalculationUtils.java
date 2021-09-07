package org.egov.mrcalculator.utils;

import java.util.List;

import org.egov.common.contract.request.RequestInfo;
import org.egov.mr.repository.ServiceRequestRepository;
import org.egov.mr.service.MarriageRegistrationService;
import org.egov.mr.web.models.AuditDetails;
import org.egov.mr.web.models.MarriageRegistration;
import org.egov.mr.web.models.MarriageRegistrationSearchCriteria;
import org.egov.mrcalculator.config.MRCalculatorConfigs;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import static org.egov.mrcalculator.utils.MRCalculatorConstants.businessService_MR;

import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class CalculationUtils {


    @Autowired
    private MRCalculatorConfigs config;

    @Autowired
    private ServiceRequestRepository serviceRequestRepository;

    @Autowired
    private ObjectMapper mapper;
    
    @Autowired
    private MarriageRegistrationService marriageRegistrationService;





    /**
     * Creates demand Search url based on tenanatId,businessService and ConsumerCode
     * @return demand search url
     */
    public String getDemandSearchURL(){
        StringBuilder url = new StringBuilder(config.getBillingHost());
        url.append(config.getDemandSearchEndpoint());
        url.append("?");
        url.append("tenantId=");
        url.append("{1}");
        url.append("&");
        url.append("businessService=");
        url.append("{2}");
        url.append("&");
        url.append("consumerCode=");
        url.append("{3}");
        return url.toString();
    }


    /**
     * Creates generate bill url using tenantId,consumerCode and businessService
     * @return Bill Generate url
     */
    public String getBillGenerateURI(){
        StringBuilder url = new StringBuilder(config.getBillingHost());
        url.append(config.getBillGenerateEndpoint());
        url.append("?");
        url.append("tenantId=");
        url.append("{1}");
        url.append("&");
        url.append("consumerCode=");
        url.append("{2}");
        url.append("&");
        url.append("businessService=");
        url.append("{3}");

        return url.toString();
    }

    public AuditDetails getAuditDetails(String by, Boolean isCreate) {
        Long time = System.currentTimeMillis();
        if(isCreate)
            return AuditDetails.builder().createdBy(by).lastModifiedBy(by).createdTime(time).lastModifiedTime(time).build();
        else
            return AuditDetails.builder().lastModifiedBy(by).lastModifiedTime(time).build();
    }




    /**
     * Returns the url for mdms search endpoint
     *
     * @return
     */
    public StringBuilder getMdmsSearchUrl() {
        return new StringBuilder().append(config.getMdmsHost()).append(config.getMdmsSearchEndpoint());
    }


	public MarriageRegistration getMarriageRegistration(RequestInfo requestInfo, String applicationNumber,String tenantId) {
		
		MarriageRegistrationSearchCriteria criteria = new MarriageRegistrationSearchCriteria();
		
		criteria.setApplicationNumber(applicationNumber);
		criteria.setTenantId(tenantId);
		
		List<MarriageRegistration> marriageRegistrations = marriageRegistrationService.search(criteria, requestInfo, businessService_MR, null);
		
		if(marriageRegistrations!=null)
			marriageRegistrations.get(0);
		
		return null;
	}




}
