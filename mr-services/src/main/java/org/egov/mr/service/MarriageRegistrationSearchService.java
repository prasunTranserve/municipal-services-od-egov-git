package org.egov.mr.service;

import java.util.Collections;
import java.util.List;

import org.egov.common.contract.request.RequestInfo;
import org.egov.mr.repository.MRRepository;
import org.egov.mr.web.models.MarriageRegistration;
import org.egov.mr.web.models.MarriageRegistrationSearchCriteria;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;

@Service
public class MarriageRegistrationSearchService {
	
	@Autowired
	private EnrichmentService enrichmentService;
	
	@Autowired
	private MRRepository repository;

    public List<MarriageRegistration> search(MarriageRegistrationSearchCriteria criteria, RequestInfo requestInfo, String serviceFromPath, HttpHeaders headers){
        List<MarriageRegistration> marriageRegistrations;

        criteria.setBusinessService(serviceFromPath);
        enrichmentService.enrichSearchCriteriaWithAccountId(requestInfo,criteria);
         
             marriageRegistrations = getMarriageRegistrationsWithOwnerInfo(criteria,requestInfo);
             
       return marriageRegistrations;
    }
    
    public List<MarriageRegistration> getMarriageRegistrationsWithOwnerInfo(MarriageRegistrationSearchCriteria criteria,RequestInfo requestInfo){
        List<MarriageRegistration> marriageRegistrations = repository.getMarriageRegistartions(criteria);
        if(marriageRegistrations.isEmpty())
            return Collections.emptyList();
        return marriageRegistrations;
    }
    
	
}
