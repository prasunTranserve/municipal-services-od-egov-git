package org.egov.pt.util;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.egov.common.contract.request.RequestInfo;
import org.egov.pt.models.Assessment;
import org.egov.pt.models.Property;
import org.egov.pt.models.PropertyCriteria;
import org.egov.pt.service.PropertyService;
import org.egov.pt.web.contracts.AssessmentRequest;
import org.egov.pt.web.contracts.Demand;
import org.egov.pt.web.contracts.DemandDetail;
import org.egov.tracer.model.CustomException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class AssessmentUtils extends CommonUtils {


    private PropertyService propertyService;
    
    @Autowired
	private ObjectMapper mapper;

    @Autowired
    public AssessmentUtils(PropertyService propertyService) {
        this.propertyService = propertyService;
    }

    public Property getPropertyForAssessment(AssessmentRequest assessmentRequest){
    	
        RequestInfo requestInfo = assessmentRequest.getRequestInfo();
        Assessment assessment = assessmentRequest.getAssessment();
        PropertyCriteria criteria = PropertyCriteria.builder()
                .tenantId(assessment.getTenantId())
                .propertyIds(Collections.singleton(assessment.getPropertyId()))
                .build();
        List<Property> properties = propertyService.searchProperty(criteria, requestInfo);

        if(CollectionUtils.isEmpty(properties))
            throw new CustomException("PROPERTY_NOT_FOUND","The property with id: "+assessment.getPropertyId()+" is not found");

        return properties.get(0);
    }
    
    public JsonNode prepareAdditionalDetailsFromDemand(List<Demand> demands) {
		Map<String,String> additionalDetail = new HashMap<>();
		Collections.sort(demands, Comparator.comparing(Demand::getTaxPeriodFrom).thenComparing(Demand::getTaxPeriodTo).reversed());
		Demand demand = demands.get(0);
		for(DemandDetail demandDetail : demand.getDemandDetails()) {
			additionalDetail.put(demandDetail.getTaxHeadMasterCode(), String.valueOf(demandDetail.getTaxAmount()));
		}
		return mapper.convertValue(additionalDetail,JsonNode.class);
	}


}
