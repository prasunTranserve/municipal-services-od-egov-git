package org.egov.mrcalculator.service;

import static org.egov.mrcalculator.utils.MRCalculatorConstants.businessService_MR;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import org.egov.common.contract.request.RequestInfo;
import org.egov.mr.producer.Producer;
import org.egov.mr.web.models.MarriageRegistration;
import org.egov.mr.web.models.calculation.CalculationReq;
import org.egov.mr.web.models.calculation.CalculationRes;
import org.egov.mr.web.models.calculation.Category;
import org.egov.mr.web.models.calculation.TaxHeadEstimate;
import org.egov.mrcalculator.config.MRCalculatorConfigs;
import org.egov.mrcalculator.repository.BillingslabRepository;
import org.egov.mrcalculator.repository.builder.BillingslabQueryBuilder;
import org.egov.mrcalculator.utils.CalculationUtils;
import org.egov.mrcalculator.web.models.BillingSlab;
import org.egov.mrcalculator.web.models.BillingSlabSearchCriteria;
import org.egov.mrcalculator.web.models.Calculation;
import org.egov.mrcalculator.web.models.CalulationCriteria;
import org.egov.mrcalculator.web.models.EstimatesAndSlabs;
import org.egov.mrcalculator.web.models.FeeAndBillingSlabIds;
import org.egov.mrcalculator.web.models.enums.CalculationType;
import org.egov.tracer.model.CustomException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import lombok.extern.slf4j.Slf4j;


@Service
@Slf4j
public class CalculationService {


    @Autowired
    private BillingslabRepository repository;

    @Autowired
    private BillingslabQueryBuilder queryBuilder;

    @Autowired
    private MRCalculatorConfigs config;


    @Autowired
    private CalculationUtils utils;

    @Autowired
    private DemandService demandService;

    @Autowired
    private Producer producer;


    /**
     * Calculates tax estimates and creates demand
     * @param calculationReq The calculationCriteria request
     * @return List of calculations for all applicationNumbers or marriageRegistrations in calculationReq
     */
   public List<Calculation> calculate(CalculationReq calculationReq){
       String tenantId = calculationReq.getCalulationCriteria().get(0).getTenantId();
       List<Calculation> calculations = getCalculation(calculationReq.getRequestInfo(),
               calculationReq.getCalulationCriteria());
       demandService.generateDemand(calculationReq.getRequestInfo(),calculations,businessService_MR);
       CalculationRes calculationRes = CalculationRes.builder().calculations(calculations).build();
       producer.push(config.getSaveTopic(),calculationRes);
       return calculations;
   }
   
   /**
    * Calculates tax estimates 
    * @param calculationReq The calculationCriteria request
    * @return List of calculations for all applicationNumbers or marriageRegistrations in calculationReq
    */
  public List<Calculation> estimate(CalculationReq calculationReq){
      String tenantId = calculationReq.getCalulationCriteria().get(0).getTenantId();
      List<Calculation> calculations = getCalculation(calculationReq.getRequestInfo(),
              calculationReq.getCalulationCriteria());
      return calculations;
  }


    /***
     * Calculates tax estimates
     * @param requestInfo The requestInfo of the calculation request
     * @param criterias list of CalculationCriteria containing the marriageRegistration or applicationNumber
     * @return  List of calculations for all applicationNumbers or marriageRegistrations in criterias
     */
  public List<Calculation> getCalculation(RequestInfo requestInfo, List<CalulationCriteria> criterias){
      List<Calculation> calculations = new LinkedList<>();
      for(CalulationCriteria criteria : criterias) {
    	  MarriageRegistration marriageRegistration;
          if (criteria.getMarriageRegistration()==null && criteria.getApplicationNumber() != null) {
        	  marriageRegistration = utils.getMarriageRegistration(requestInfo, criteria.getApplicationNumber(), criteria.getTenantId());
              criteria.setMarriageRegistration(marriageRegistration);
          }
          EstimatesAndSlabs estimatesAndSlabs = getTaxHeadEstimates(criteria,requestInfo);
          List<TaxHeadEstimate> taxHeadEstimates = estimatesAndSlabs.getEstimates();
          FeeAndBillingSlabIds feeAndBillingSlabIds = estimatesAndSlabs.getFeeAndBillingSlabIds();

          Calculation calculation = new Calculation();
          calculation.setMarriageRegistration(criteria.getMarriageRegistration());
          calculation.setTenantId(criteria.getTenantId());
          calculation.setTaxHeadEstimates(taxHeadEstimates);
          calculation.setBillingIds(feeAndBillingSlabIds);

          calculations.add(calculation);

      }
      return calculations;
  }


    /**
     * Creates TacHeadEstimates
     * @param calulationCriteria CalculationCriteria containing the marriageRegistration or applicationNumber
     * @param requestInfo The requestInfo of the calculation request
     * @return TaxHeadEstimates and the billingSlabs used to calculate it
     */
    private EstimatesAndSlabs getTaxHeadEstimates(CalulationCriteria calulationCriteria, RequestInfo requestInfo){
      List<TaxHeadEstimate> estimates = new LinkedList<>();
      EstimatesAndSlabs  estimatesAndSlabs = getBaseTax(calulationCriteria,requestInfo);

      estimates.addAll(estimatesAndSlabs.getEstimates());

      estimatesAndSlabs.setEstimates(estimates);

      return estimatesAndSlabs;
  }


    /**
     * Calculates base tax and cretaes its taxHeadEstimate
     * @param calulationCriteria CalculationCriteria containing the marriageRegistration or applicationNumber
     * @param requestInfo The requestInfo of the calculation request
     * @return BaseTax taxHeadEstimate and billingSlabs used to calculate it
     */
  private EstimatesAndSlabs getBaseTax(CalulationCriteria calulationCriteria, RequestInfo requestInfo){
      MarriageRegistration marriageRegistration = calulationCriteria.getMarriageRegistration();
      EstimatesAndSlabs estimatesAndSlabs = new EstimatesAndSlabs();
      BillingSlabSearchCriteria searchCriteria = new BillingSlabSearchCriteria();
      searchCriteria.setTenantId(marriageRegistration.getTenantId());


      String calculationType = config.getDefaultCalculationType();

      FeeAndBillingSlabIds feeAndBillingSlabIds = getBillingSlabIds(marriageRegistration,CalculationType
              .fromValue(calculationType));
      BigDecimal fee = feeAndBillingSlabIds.getFee();

      estimatesAndSlabs.setFeeAndBillingSlabIds(feeAndBillingSlabIds);
     



      TaxHeadEstimate estimate = new TaxHeadEstimate();
      List<TaxHeadEstimate> estimateList = new ArrayList<>();
     

      if(fee.compareTo(BigDecimal.ZERO)==-1)
          throw new CustomException("INVALID AMOUNT","Tax amount is negative");

      estimate.setEstimateAmount(fee);
      estimate.setCategory(Category.TAX);
     
          estimate.setTaxHeadCode(config.getBaseTaxHead());
          estimateList.add(estimate);
      

      estimatesAndSlabs.setEstimates(estimateList);

      return estimatesAndSlabs;
  }




    /**
     * @param  marriageRegistration for which fee has to be calculated
     * @param calculationType Calculation logic to be used
     * @return  Fee and billingSlab used to calculate it
     */
  private FeeAndBillingSlabIds getBillingSlabIds(MarriageRegistration marriageRegistration, CalculationType calculationType){

      List<BigDecimal> fees = new LinkedList<>();
      List<String> billingSlabIds = new LinkedList<>();
       
    	   if(marriageRegistration.getTenantId()!=null)
    	   {
    		   List<Object> preparedStmtList = new ArrayList<>();
    		   BillingSlabSearchCriteria searchCriteria = new BillingSlabSearchCriteria();
    		   searchCriteria.setTenantId(marriageRegistration.getTenantId());

    		   // Call the Search
    		   String query = queryBuilder.getSearchQuery(searchCriteria, preparedStmtList);
    		   log.info("query "+query);
    		   log.info("preparedStmtList "+preparedStmtList.toString());
    		   List<BillingSlab> billingSlabs = repository.getDataFromDB(query, preparedStmtList);

    		   if(billingSlabs.size()>1)
    			   throw new CustomException("BILLINGSLAB ERROR","Found multiple BillingSlabs for the given ULB");
    		   if(CollectionUtils.isEmpty(billingSlabs))
    		   {
    			   String[] tenantArray = marriageRegistration.getTenantId().split("\\.");
    			   String city = "" ;
    			   if(tenantArray.length>1)
    			   {
    				   city = tenantArray[1];
    			   }
    			   throw new CustomException("BILLINGSLAB ERROR","Found multiple BillingSlabs for the given ULB "+city+".");
    		   }
    		   System.out.println(" rate: "+billingSlabs.get(0).getRate());

    		   billingSlabIds.add(billingSlabs.get(0).getId());

    		   fees.add(billingSlabs.get(0).getRate());

    	   }
         
      

      BigDecimal totalFee = getTotalFee(fees,calculationType);

      FeeAndBillingSlabIds feeAndBillingSlabIds = new FeeAndBillingSlabIds();
      feeAndBillingSlabIds.setFee(totalFee);
      feeAndBillingSlabIds.setBillingSlabIds(billingSlabIds);
      feeAndBillingSlabIds.setId(UUID.randomUUID().toString());

      return feeAndBillingSlabIds;
  }




    /**
     * Calculates total fee of by applying logic on list based on calculationType
     * @param fees List of fee 
     * @param calculationType Calculation logic to be used
     * @return Total Fee
     */
  private BigDecimal getTotalFee(List<BigDecimal> fees,CalculationType calculationType){
      BigDecimal totalFee = BigDecimal.ZERO;
      //Summation
      if(calculationType.equals(CalculationType.SUM))
          totalFee = fees.stream().reduce(BigDecimal.ZERO, BigDecimal::add);

      //Average
      if(calculationType.equals(CalculationType.AVERAGE))
          totalFee = (fees.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                  .divide(new BigDecimal(fees.size()))).setScale(2,2);

      //Max
      if(calculationType.equals(CalculationType.MAX))
          totalFee = fees.stream().reduce(BigDecimal::max).get();

      //Min
      if(calculationType.equals(CalculationType.MIN))
          totalFee = fees.stream().reduce(BigDecimal::min).get();

       return totalFee;
  }



 

}
