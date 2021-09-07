package org.egov.mr.service;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.egov.common.contract.request.RequestInfo;
import org.egov.mr.web.models.MarriageRegistration;
import org.egov.mr.web.models.MarriageRegistrationRequest;
import org.egov.mr.web.models.calculation.CalculationReq;
import org.egov.mr.web.models.calculation.CalculationRes;
import org.egov.mrcalculator.service.CalculationService;
import org.egov.mrcalculator.web.models.Calculation;
import org.egov.mrcalculator.web.models.CalulationCriteria;
import org.egov.tracer.model.CustomException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;


@Service
public class CalculatorService {


	@Autowired
    private CalculationService calculationService;



    /**
     * Adds the calculation object to the request
     * @param request The input create or update request
     * @return request with calculation object added
     */
    public List<MarriageRegistration> addCalculation(MarriageRegistrationRequest request){
        RequestInfo requestInfo = request.getRequestInfo();
        List<MarriageRegistration> marriageRegistrations = request.getMarriageRegistrations();

        if(CollectionUtils.isEmpty(marriageRegistrations))
            throw new CustomException("INVALID REQUEST","The request for calculation cannot be empty or null");

        CalculationRes response = getCalculation(requestInfo,marriageRegistrations);
        List<Calculation> calculations = response.getCalculations();
        Map<String,Calculation> applicationNumberToCalculation = new HashMap<>();
        calculations.forEach(calculation -> {
            applicationNumberToCalculation.put(request.getMarriageRegistrations().get(0).getApplicationNumber(),calculation);
            calculation.setMarriageRegistration(null);
        });

        marriageRegistrations.forEach(marriageRegistration ->{
            marriageRegistration.setCalculation(applicationNumberToCalculation.get(marriageRegistration.getApplicationNumber()));
        });

        return marriageRegistrations;
    }



    private CalculationRes getCalculation(RequestInfo requestInfo,List<MarriageRegistration> marriageRegistrations){
        List<CalulationCriteria> criterias = new LinkedList<>();

        marriageRegistrations.forEach(marriageRegistration -> {
            criterias.add(new CalulationCriteria(marriageRegistration,marriageRegistration.getApplicationNumber(),marriageRegistration.getTenantId()));
        });

        CalculationReq request = CalculationReq.builder().calulationCriteria(criterias)
                .requestInfo(requestInfo)
                .build();

        CalculationRes response = null;
        try{
        	List<Calculation> calculations = null;
        	
        	calculations = calculationService.calculate(request);
        	
        	response = CalculationRes.builder().calculations(calculations).build();
        }
        catch (IllegalArgumentException e){
            throw new CustomException("PARSING ERROR","Failed to parse response of calculate");
        }
        return response;
    }
    
    

  
}
