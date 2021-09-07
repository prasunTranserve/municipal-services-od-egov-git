package org.egov.mr.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;

import org.egov.mr.config.MRConfiguration;
import org.egov.mr.repository.ServiceRequestRepository;
import org.egov.mr.web.models.Boundary;
import org.egov.mr.web.models.MarriageRegistrationRequest;
import org.egov.mr.web.models.RequestInfoWrapper;
import org.egov.tracer.model.CustomException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class BoundaryService {

    private ServiceRequestRepository serviceRequestRepository;

    private ObjectMapper mapper;

    private MRConfiguration config;

    @Autowired
    public BoundaryService(ServiceRequestRepository serviceRequestRepository, ObjectMapper mapper, MRConfiguration config) {
        this.serviceRequestRepository = serviceRequestRepository;
        this.mapper = mapper;
        this.config = config;
    }

    /**
     *  Enriches the locality object by calling the location service
     * @param request MarriageRegistrationRequest for create
     * @param hierarchyTypeCode HierarchyTypeCode of the boundaries
     */
    public void getAreaType(MarriageRegistrationRequest request, String hierarchyTypeCode){
        if(CollectionUtils.isEmpty(request.getMarriageRegistrations()))
            return;

        String tenantId = request.getMarriageRegistrations().get(0).getTenantId();

        LinkedList<String> localities = new LinkedList<>();
        request.getMarriageRegistrations().forEach(marriageRegistration -> {
            if( marriageRegistration.getMarriagePlace().getLocality()==null)
                throw new CustomException("INVALID ADDRESS","The address or locality cannot be null");
            localities.add(marriageRegistration.getMarriagePlace().getLocality().getCode());
        });

        StringBuilder uri = new StringBuilder(config.getLocationHost());
        uri.append(config.getLocationContextPath()).append(config.getLocationEndpoint());
        uri.append("?").append("tenantId=").append(tenantId);
        if(hierarchyTypeCode!=null)
            uri.append("&").append("hierarchyTypeCode=").append(hierarchyTypeCode);
        uri.append("&").append("boundaryType=").append("Locality");

        if(!CollectionUtils.isEmpty(localities)) {
            uri.append("&").append("codes=");
            for (int i = 0; i < localities.size(); i++) {
                uri.append(localities.get(i));
                if(i!=localities.size()-1)
                    uri.append(",");
            }
        }
        RequestInfoWrapper wrapper = RequestInfoWrapper.builder().requestInfo(request.getRequestInfo()).build();
        LinkedHashMap responseMap = (LinkedHashMap)serviceRequestRepository.fetchResult(uri, wrapper);
        if(CollectionUtils.isEmpty(responseMap))
            throw new CustomException("BOUNDARY ERROR","The response from location service is empty or null");
        String jsonString = new JSONObject(responseMap).toString();

        Map<String,String> propertyIdToJsonPath = getJsonpath(request);

        DocumentContext context = JsonPath.parse(jsonString);

        request.getMarriageRegistrations().forEach(marriageRegistration -> {
            Object boundaryObject = context.read(propertyIdToJsonPath.get(marriageRegistration.getId()));
            if(!(boundaryObject instanceof ArrayList) || CollectionUtils.isEmpty((ArrayList)boundaryObject))
                throw new CustomException("BOUNDARY MDMS DATA ERROR","The boundary data was not found");

            ArrayList boundaryResponse = context.read(propertyIdToJsonPath.get(marriageRegistration.getId()));
            Boundary boundary = mapper.convertValue(boundaryResponse.get(0),Boundary.class);
            if(boundary.getName()==null)
                throw new CustomException("INVALID BOUNDARY DATA","The boundary data for the code "+marriageRegistration.getMarriagePlace().getLocality().getCode()+ " is not available");
            marriageRegistration.getMarriagePlace().setLocality(boundary);

        });
    }


    /**
     *  Prepares map of marriageRegistrationId to jsonpath which contains the code of the marriageRegistration
     * @param request MarriageRegistrationRequest for create
     * @return Map of marriageRegistrationId to jsonPath with marriageRegistration locality code
     */
    private Map<String,String> getJsonpath(MarriageRegistrationRequest request){
        Map<String,String> idToJsonPath = new LinkedHashMap<>();
        String jsonpath = "$..boundary[?(@.code==\"{}\")]";
        request.getMarriageRegistrations().forEach(marriageRegistration -> {
            idToJsonPath.put(marriageRegistration.getId(),jsonpath.replace("{}",marriageRegistration.getMarriagePlace().getLocality().getCode()
            ));
        });

        return  idToJsonPath;
    }










}
