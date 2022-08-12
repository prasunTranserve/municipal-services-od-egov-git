package org.egov.bpa.calculator.services;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.tomcat.jni.BIOCallback;
import org.egov.bpa.calculator.config.BPACalculatorConfig;
import org.egov.bpa.calculator.kafka.broker.BPACalculatorProducer;
import org.egov.bpa.calculator.repository.InstallmentRepository;
import org.egov.bpa.calculator.repository.PreapprovedPlanRepository;
import org.egov.bpa.calculator.repository.RevisionRepository;
import org.egov.bpa.calculator.repository.ServiceRequestRepository;
import org.egov.bpa.calculator.utils.BPACalculatorConstants;
import org.egov.bpa.calculator.utils.CalculationUtils;
import org.egov.bpa.calculator.utils.EdcrHelperUtils;
import org.egov.bpa.calculator.validators.InstallmentValidator;
import org.egov.bpa.calculator.web.models.BillingSlabSearchCriteria;
import org.egov.bpa.calculator.web.models.Calculation;
import org.egov.bpa.calculator.web.models.CalculationReq;
import org.egov.bpa.calculator.web.models.CalculationRes;
import org.egov.bpa.calculator.web.models.CalulationCriteria;
import org.egov.bpa.calculator.web.models.Installment;
import org.egov.bpa.calculator.web.models.PreapprovedPlan;
import org.egov.bpa.calculator.web.models.PreapprovedPlanSearchCriteria;
import org.egov.bpa.calculator.web.models.Revision;
import org.egov.bpa.calculator.web.models.RevisionSearchCriteria;
import org.egov.bpa.calculator.web.models.bpa.BPA;
import org.egov.bpa.calculator.web.models.bpa.EstimatesAndSlabs;
import org.egov.bpa.calculator.web.models.demand.Category;
import org.egov.bpa.calculator.web.models.demand.Demand;
import org.egov.bpa.calculator.web.models.demand.DemandDetail;
import org.egov.bpa.calculator.web.models.Installment.StatusEnum;
import org.egov.bpa.calculator.web.models.InstallmentRequest;
import org.egov.bpa.calculator.web.models.InstallmentSearchCriteria;
import org.egov.bpa.calculator.edcr.model.Occupancy;
import org.egov.bpa.calculator.web.models.demand.TaxHeadEstimate;
import org.egov.common.contract.request.RequestInfo;
import org.egov.tracer.model.CustomException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;

import lombok.extern.slf4j.Slf4j;
import net.minidev.json.JSONArray;

@Service
@Slf4j
public class CalculationService {

	@Autowired
	private MDMSService mdmsService;

	@Autowired
	private DemandService demandService;

	@Autowired
	private EDCRService edcrService;

	@Autowired
	private BPACalculatorConfig config;

	@Autowired
	private CalculationUtils utils;

	@Autowired
	private BPACalculatorProducer producer;

	@Autowired
	private BPAService bpaService;
	
	@Autowired
	private AlterationCalculationService alterationCalculationService;
	
	@Autowired
	private ServiceRequestRepository serviceRequestRepository;
	
	@Autowired
	private PreapprovedPlanRepository preapprovedPlanRepository;
	
	@Autowired
	private InstallmentRepository installmentRepository;
	
	@Autowired
	private InstallmentValidator installmentValidator;
	
	@Autowired
	private RevisionRepository revisionRepository;
	
	
	@Autowired
	private EdcrHelperUtils edcrHelperUtils;

	private static final BigDecimal ZERO_TWO_FIVE = new BigDecimal("0.25");// BigDecimal.valueOf(0.25);
	private static final BigDecimal ZERO_FIVE = new BigDecimal("0.5");// BigDecimal.valueOf(0.5);
	private static final BigDecimal TEN = new BigDecimal("10");// BigDecimal.valueOf(10);
	private static final BigDecimal FIFTEEN = new BigDecimal("15");// BigDecimal.valueOf(15);
//	private static final BigDecimal SEVENTEEN_FIVE = new BigDecimal("17.50");// BigDecimal.valueOf(17.50);
	//private static final BigDecimal SEVENTEEN_POINT_EIGHT_FIVE = new BigDecimal("17.85");// BigDecimal.valueOf(17.50);
	private static final BigDecimal EIGHTEEN_POINT_TWO_ONE = new BigDecimal("18.21");
	private static final BigDecimal TWENTY = new BigDecimal("20");// BigDecimal.valueOf(20);
	private static final BigDecimal TWENTY_FIVE = new BigDecimal("25");// BigDecimal.valueOf(25);
	private static final BigDecimal THIRTY = new BigDecimal("30");// BigDecimal.valueOf(30);
	private static final BigDecimal FIFTY = new BigDecimal("50");// BigDecimal.valueOf(50);
	private static final BigDecimal HUNDRED = new BigDecimal("100");// BigDecimal.valueOf(100);
	private static final BigDecimal TWO_HUNDRED = new BigDecimal("200");// BigDecimal.valueOf(200);
	private static final BigDecimal TWO_HUNDRED_FIFTY = new BigDecimal("250");// BigDecimal.valueOf(250);
	private static final BigDecimal THREE_HUNDRED = new BigDecimal("300");// BigDecimal.valueOf(300);
	private static final BigDecimal FIVE_HUNDRED = new BigDecimal("500");// BigDecimal.valueOf(500);
	private static final BigDecimal FIFTEEN_HUNDRED = new BigDecimal("1500");// BigDecimal.valueOf(1500);
	private static final BigDecimal SEVENTEEN_FIFTY = new BigDecimal("1750");// BigDecimal.valueOf(1750);
	private static final BigDecimal TWO_THOUSAND = new BigDecimal("2000");// BigDecimal.valueOf(2000);
	private static final BigDecimal THOUSAND = new BigDecimal("1000");// BigDecimal.valueOf(2000);
	private static final BigDecimal TEN_LAC = new BigDecimal("1000000");// BigDecimal.valueOf(1000000);
	private static final BigDecimal SQMT_SQFT_MULTIPLIER = new BigDecimal("10.764");// BigDecimal.valueOf(10.764);
	private static final BigDecimal ACRE_SQMT_MULTIPLIER = new BigDecimal("4046.85");// BigDecimal.valueOf(4046.85);

	private static final BigDecimal ONE_HUNDRED_FIFTY = new BigDecimal("150");

	private static final BigDecimal TWO_HUNDRED_TWENTYFIVE = new BigDecimal("225");

	private static final BigDecimal SIX_HUNDRED = new BigDecimal("600");

	private static final BigDecimal EIGHT_HUNDRED_SVENTYFIVE = new BigDecimal("875");

	private static final BigDecimal THREE_HUNDRED_SEVENTYFIVE = new BigDecimal("375");
	private static final String INSTALLMENTS_FIELD = "installments";

	private static final BigDecimal ONE_HUNDRED_TWENTYFIVE =  new BigDecimal("125");

	private static final BigDecimal SEVEN_HUNDRED_FIFTY = new BigDecimal("750");

	private static final BigDecimal SEVEN_POINT_FIVE = new BigDecimal("7.50");

	private static final BigDecimal FIVE = new BigDecimal("5.0");

	private static final BigDecimal TWELVE_POINT_FIVE = new BigDecimal("12.5");
	

	/**
	 * Calculates tax estimates and creates demand
	 * 
	 * @param calculationReq The calculationCriteria request
	 * @return List of calculations for all applicationNumbers or tradeLicenses in
	 *         calculationReq
	 */
	public List<Calculation> calculate(CalculationReq calculationReq) {
		utils.validateOwnerDetails(calculationReq);
		String tenantId = calculationReq.getCalulationCriteria().get(0).getTenantId();
		Object mdmsData = mdmsService.mDMSCall(calculationReq, tenantId);
		Boolean isSparit = mdmsService.getMdmsSparitValue(calculationReq,tenantId);
		// List<Calculation> calculations =
		// getCalculation(calculationReq.getRequestInfo(),calculationReq.getCalulationCriteria(),
		// mdmsData);
		Map<String,Object> extraParamsForCalculationMap = new HashMap<>();
		extraParamsForCalculationMap.put("tenantId", tenantId);
		extraParamsForCalculationMap.put("mdmsData", mdmsData);
		extraParamsForCalculationMap.put(BPACalculatorConstants.SPARIT_CHECK, isSparit);
		//System.out.println("checkSparit:"+isSparit);
		List<Calculation> calculations = getCalculationV2(calculationReq.getRequestInfo(),
				calculationReq.getCalulationCriteria(), extraParamsForCalculationMap);
		demandService.generateDemand(calculationReq.getRequestInfo(), calculations, mdmsData);
		CalculationRes calculationRes = CalculationRes.builder().calculations(calculations).build();
		producer.push(config.getSaveTopic(), calculationRes);
		return calculations;
	}
	
	/**
	 * Calculates tax estimates without creating demand
	 * 
	 * @param calculationReq The calculationCriteria request
	 * @return List of calculations for all applicationNumbers in calculationReq
	 */
	public List<Calculation> getEstimate(CalculationReq calculationReq) {
		utils.validateOwnerDetails(calculationReq);
		String tenantId = calculationReq.getCalulationCriteria().get(0).getTenantId();
		Object mdmsData = mdmsService.mDMSCall(calculationReq, tenantId);
		Boolean isSparit = mdmsService.getMdmsSparitValue(calculationReq,tenantId);
		
		Map<String,Object> extraParamsForCalculationMap = new HashMap<>();
		extraParamsForCalculationMap.put("tenantId", tenantId);
		extraParamsForCalculationMap.put("mdmsData", mdmsData);
		extraParamsForCalculationMap.put(BPACalculatorConstants.SPARIT_CHECK, isSparit);
		//System.out.println("checkSparit:"+isSparit);
		List<Calculation> calculations = getCalculationV2(calculationReq.getRequestInfo(),
				calculationReq.getCalulationCriteria(), extraParamsForCalculationMap);
		CalculationRes calculationRes = CalculationRes.builder().calculations(calculations).build();
		return calculations;
	}
	
	/**
	 * Calculates tax estimates and stores them in installments table
	 * 
	 * @param calculationReq The calculationCriteria request
	 * @return List of calculations for all applicationNumbers in calculationReq
	 */
	public List<Calculation> calculateInInstallments(CalculationReq calculationReq) {
		utils.validateOwnerDetails(calculationReq);
		String tenantId = calculationReq.getCalulationCriteria().get(0).getTenantId();
		Object mdmsData = mdmsService.mDMSCall(calculationReq, tenantId);
		Boolean isSparit = mdmsService.getMdmsSparitValue(calculationReq,tenantId);
		Map<String,Object> extraParamsForCalculationMap = new HashMap<>();
		extraParamsForCalculationMap.put("tenantId", tenantId);
		extraParamsForCalculationMap.put("mdmsData", mdmsData);
		extraParamsForCalculationMap.put(BPACalculatorConstants.SPARIT_CHECK, isSparit);
		List<Calculation> calculations = getCalculationV2(calculationReq.getRequestInfo(),
				calculationReq.getCalulationCriteria(), extraParamsForCalculationMap);
		//store the calculations in installments table
		List<Installment> installmentsToInsert = generateInstallmentsFromCalculations(calculationReq, calculations);
		
		/*uncomment to generate installments script
		StringBuilder query = new StringBuilder();
		installmentsToInsert.forEach(installment->{
			query.append("("+"'"+installment.getId()+"','"+"od.cuttack"+"',"+installment.getInstallmentNo()+",'ACTIVE','"+installment.getConsumerCode()
			+"','"+installment.getTaxHeadCode()+"',"+installment.getTaxAmount()+",null,false,null,'"+installment.getAuditDetails().getCreatedBy()
			+"','"+installment.getAuditDetails().getLastModifiedBy()+"',"+installment.getAuditDetails().getCreatedTime()+","
			+installment.getAuditDetails().getLastModifiedTime()+"),\n");
		});
		System.out.println("********\n"+query.toString());
		*/
		
		Map<String, List<Installment>> persisterMap = new HashMap<>();
		persisterMap.put(INSTALLMENTS_FIELD, installmentsToInsert);
		producer.push(config.getSaveInstallmentTopic(), persisterMap);
		return calculations;
	}
	
	
	
	/**
	 * Fetch all installments from db
	 * 
	 */
	public Object getAllInstallmentsV2(InstallmentRequest request) {
		List<Installment> installments = installmentRepository.getInstallments(request.getInstallmentSearchCriteria());
		Map<Integer, List<Installment>> groupedInstallmentsMap = installments.stream()
				.collect(Collectors.groupingBy(installment -> installment.getInstallmentNo()));
		Map<String, Object> returnMap = new HashMap<>();
		if(groupedInstallmentsMap.containsKey(-1)) {
			returnMap.put("fullPayment", groupedInstallmentsMap.get(-1));
		}
		groupedInstallmentsMap.remove(-1);
		Collection<List<Installment>> installmentsGroupedByInstallmentNo = groupedInstallmentsMap
				.values();
		returnMap.put(INSTALLMENTS_FIELD, installmentsGroupedByInstallmentNo);
		return returnMap;
	}
	
	/**
	 * Generate demands from installment
	 * 
	 */
	public Object generateDemandsFromInstallment(InstallmentRequest request) {
		installmentValidator.validateConsumerCode(request);
		InstallmentSearchCriteria allInstallmentsCriteria = new InstallmentSearchCriteria();
		allInstallmentsCriteria.setConsumerCode(request.getInstallmentSearchCriteria().getConsumerCode());
		List<Installment> allInstallments = installmentRepository.getInstallments(allInstallmentsCriteria);
		if (CollectionUtils.isEmpty(allInstallments))
			throw new CustomException(
					"no installments found for this consumercode:"
							+ request.getInstallmentSearchCriteria().getConsumerCode(),
					"no installments found for this consumercode:"
							+ request.getInstallmentSearchCriteria().getConsumerCode());
		installmentValidator.validateInstallmentNoSequence(request, allInstallments);
		List<Installment> installmentsToGenerateDemand = installmentRepository
				.getInstallments(request.getInstallmentSearchCriteria());
		installmentValidator.validateForDemandGeneration(request, allInstallments, installmentsToGenerateDemand);
		//
		List<Demand> demands = demandService.createDemandFromInstallment(request.getRequestInfo(),
				installmentsToGenerateDemand);
		
		// update installments demandId,additionalDetails(for document on 2nd
		// installment onwards) and auditDetails field-
		Map<String,Object> additionalDetailsToUpdateFromRequest = null;
		if (Objects.nonNull(request.getInstallmentSearchCriteria().getAdditionalDetails())
				&& request.getInstallmentSearchCriteria().getAdditionalDetails() instanceof Map
				&& !CollectionUtils.isEmpty((Map) request.getInstallmentSearchCriteria().getAdditionalDetails())) {
			additionalDetailsToUpdateFromRequest = (Map<String, Object>) request.getInstallmentSearchCriteria()
					.getAdditionalDetails();
		}
		String demandId = demands.get(0).getId();
		String lastModifiedBy = request.getRequestInfo().getUserInfo().getUuid();
		updateInstallments(installmentsToGenerateDemand, demandId, lastModifiedBy,
				additionalDetailsToUpdateFromRequest);
		
		//add installment audit table
		Map<String, Object> returnObject = new HashMap<>();
		returnObject.put("demands", demands);
		return returnObject;
	}
	
	private void updateInstallments(List<Installment> installments, String demandId, String modifiedBy,
			Map<String, Object> additionalDetails) {
		Long time = System.currentTimeMillis();
		for (Installment installment : installments) {
			installment.setDemandId(demandId);
			if (Objects.nonNull(additionalDetails)) {
				Map<String, Object> additionalDetailsExisting = Objects.nonNull(installment.getAdditionalDetails())
						? (Map<String, Object>) installment.getAdditionalDetails()
						: new HashMap<>();
				additionalDetailsExisting.putAll(additionalDetails);
				installment.setAdditionalDetails(additionalDetailsExisting);
			}
			installment.getAuditDetails().setLastModifiedBy(modifiedBy);
			installment.getAuditDetails().setLastModifiedTime(time);
		}
		Map<String, List<Installment>> persisterMap = new HashMap<>();
		persisterMap.put(INSTALLMENTS_FIELD, installments);
		installmentRepository.update(persisterMap);
	}
	
	private List<Installment> generateInstallmentsFromCalculations(CalculationReq calculationReq, List<Calculation> calculations) {
		List<Installment> installmentsToInsert = new ArrayList<>();
		String tenantId = calculationReq.getCalulationCriteria().get(0).getTenantId();
		Object installmentsMdms = mdmsService.fetchInstallmentsApplicableForTaxheads(calculationReq, tenantId);
		for (Calculation calculation : calculations) {
			for (TaxHeadEstimate taxHeadEstimate : calculation.getTaxHeadEstimates()) {
				Map<String,Object> installmentDetail=mdmsService.getInstallmentforTaxHeadCode(taxHeadEstimate.getTaxHeadCode(), installmentsMdms);
				//full payment installment -1--
				Installment installmentEntryForFullPayment = Installment.builder().id(UUID.randomUUID().toString())
						.tenantId(calculation.getTenantId())
						.installmentNo(-1)
						.status(StatusEnum.ACTIVE)
						.consumerCode(calculationReq.getCalulationCriteria().get(0).getApplicationNo())
						.taxHeadCode(taxHeadEstimate.getTaxHeadCode())
						.taxAmount(taxHeadEstimate.getEstimateAmount())
						.auditDetails(utils.getAuditDetails(calculationReq.getRequestInfo().getUserInfo().getUuid(),
								true))
						.build();
				installmentsToInsert.add(installmentEntryForFullPayment);
				int noOfInstallments = (int) installmentDetail.get(BPACalculatorConstants.MDMS_NO_OF_INSTALLMENTS);
					
				for (int i = 0; i < noOfInstallments; i++) {
					// create installment only if estimateAmount is not 0-
					if (BigDecimal.ZERO.compareTo(taxHeadEstimate.getEstimateAmount()) != 0) {
						Installment installment = Installment.builder().id(UUID.randomUUID().toString())
								.tenantId(calculation.getTenantId()).installmentNo(i + 1).status(StatusEnum.ACTIVE)
								.consumerCode(calculationReq.getCalulationCriteria().get(0).getApplicationNo())
								.taxHeadCode(taxHeadEstimate.getTaxHeadCode())
								.taxAmount(taxHeadEstimate.getEstimateAmount().divide(new BigDecimal(noOfInstallments),
										0, BigDecimal.ROUND_HALF_UP))
								.auditDetails(utils
										.getAuditDetails(calculationReq.getRequestInfo().getUserInfo().getUuid(), true))
								.build();
						installmentsToInsert.add(installment);
					}
				}
			}
		}
		return installmentsToInsert;
	}

	/**
	 * @param requestInfo
	 * @param calulationCriteria
	 * @param extraParamsForCalculationMap
	 * @return
	 */
	private List<Calculation> getCalculationV2(RequestInfo requestInfo, List<CalulationCriteria> calulationCriteria,
			Map<String, Object> extraParamsForCalculationMap) {
		List<Calculation> calculations = new LinkedList<>();
		if (!CollectionUtils.isEmpty(calulationCriteria)) {
			for (CalulationCriteria criteria : calulationCriteria) {
				BPA bpa;
				if (criteria.getBpa() == null && criteria.getApplicationNo() != null) {
					bpa = bpaService.getBuildingPlan(requestInfo, criteria.getTenantId(), criteria.getApplicationNo(),
							null);
					criteria.setBpa(bpa);
				}
				extraParamsForCalculationMap.put("BPA", criteria.getBpa());

				EstimatesAndSlabs estimatesAndSlabs = getTaxHeadEstimatesV2(criteria, requestInfo, extraParamsForCalculationMap);
				List<TaxHeadEstimate> taxHeadEstimates = estimatesAndSlabs.getEstimates();

				Calculation calculation = new Calculation();
				calculation.setBpa(criteria.getBpa());
				calculation.setTenantId(criteria.getTenantId());
				calculation.setTaxHeadEstimates(taxHeadEstimates);
				calculation.setFeeType(criteria.getFeeType());
				calculations.add(calculation);

			}

		}
		return calculations;
	}

	/**
	 * @param criteria
	 * @param requestInfo
	 * @param extraParamsForCalculationMap
	 * @return
	 */
	private EstimatesAndSlabs getTaxHeadEstimatesV2(CalulationCriteria criteria, RequestInfo requestInfo,
			Map<String, Object> extraParamsForCalculationMap) {
		List<TaxHeadEstimate> estimates = new LinkedList<>();
		EstimatesAndSlabs estimatesAndSlabs;
		estimatesAndSlabs = getBaseTaxV2(criteria, requestInfo, extraParamsForCalculationMap);
		estimates.addAll(estimatesAndSlabs.getEstimates());
		estimatesAndSlabs.setEstimates(estimates);

		return estimatesAndSlabs;
	}

	/***
	 * Calculates tax estimates
	 * 
	 * @param requestInfo The requestInfo of the calculation request
	 * @param criterias   list of CalculationCriteria containing the tradeLicense or
	 *                    applicationNumber
	 * @return List of calculations for all applicationNumbers or tradeLicenses in
	 *         criterias
	 */
	public List<Calculation> getCalculation(RequestInfo requestInfo, List<CalulationCriteria> criterias,
			Object mdmsData) {
		List<Calculation> calculations = new LinkedList<>();
		for (CalulationCriteria criteria : criterias) {
			BPA bpa;
			if (criteria.getBpa() == null && criteria.getApplicationNo() != null) {
				bpa = bpaService.getBuildingPlan(requestInfo, criteria.getTenantId(), criteria.getApplicationNo(),
						null);
				criteria.setBpa(bpa);
			}

			EstimatesAndSlabs estimatesAndSlabs = getTaxHeadEstimates(criteria, requestInfo, mdmsData);
			List<TaxHeadEstimate> taxHeadEstimates = estimatesAndSlabs.getEstimates();

			Calculation calculation = new Calculation();
			calculation.setBpa(criteria.getBpa());
			calculation.setTenantId(criteria.getTenantId());
			calculation.setTaxHeadEstimates(taxHeadEstimates);
			calculation.setFeeType(criteria.getFeeType());
			calculations.add(calculation);

		}
		return calculations;
	}

	/**
	 * Creates TacHeadEstimates
	 * 
	 * @param calulationCriteria CalculationCriteria containing the tradeLicense or
	 *                           applicationNumber
	 * @param requestInfo        The requestInfo of the calculation request
	 * @return TaxHeadEstimates and the billingSlabs used to calculate it
	 */
	private EstimatesAndSlabs getTaxHeadEstimates(CalulationCriteria calulationCriteria, RequestInfo requestInfo,
			Object mdmsData) {
		List<TaxHeadEstimate> estimates = new LinkedList<>();
		EstimatesAndSlabs estimatesAndSlabs;
		if (calulationCriteria.getFeeType().equalsIgnoreCase(BPACalculatorConstants.LOW_RISK_PERMIT_FEE_TYPE)) {

//			 stopping Application fee for lowrisk applicaiton according to BBI-391
			calulationCriteria.setFeeType(BPACalculatorConstants.MDMS_CALCULATIONTYPE_LOW_APL_FEETYPE);
			estimatesAndSlabs = getBaseTax(calulationCriteria, requestInfo, mdmsData);

			estimates.addAll(estimatesAndSlabs.getEstimates());

			calulationCriteria.setFeeType(BPACalculatorConstants.MDMS_CALCULATIONTYPE_LOW_SANC_FEETYPE);
			estimatesAndSlabs = getBaseTax(calulationCriteria, requestInfo, mdmsData);

			estimates.addAll(estimatesAndSlabs.getEstimates());

			calulationCriteria.setFeeType(BPACalculatorConstants.LOW_RISK_PERMIT_FEE_TYPE);

		} else {
			estimatesAndSlabs = getBaseTax(calulationCriteria, requestInfo, mdmsData);
			estimates.addAll(estimatesAndSlabs.getEstimates());
		}

		estimatesAndSlabs.setEstimates(estimates);

		return estimatesAndSlabs;
	}

	/**
	 * Calculates base tax and cretaes its taxHeadEstimate
	 * 
	 * @param calulationCriteria CalculationCriteria containing the tradeLicense or
	 *                           applicationNumber
	 * @param requestInfo        The requestInfo of the calculation request
	 * @return BaseTax taxHeadEstimate and billingSlabs used to calculate it
	 */
	@SuppressWarnings({ "rawtypes" })
	private EstimatesAndSlabs getBaseTax(CalulationCriteria calulationCriteria, RequestInfo requestInfo,
			Object mdmsData) {
		BPA bpa = calulationCriteria.getBpa();
		EstimatesAndSlabs estimatesAndSlabs = new EstimatesAndSlabs();
		BillingSlabSearchCriteria searchCriteria = new BillingSlabSearchCriteria();
		searchCriteria.setTenantId(bpa.getTenantId());

		Map calculationTypeMap = mdmsService.getCalculationType(requestInfo, bpa, mdmsData,
				calulationCriteria.getFeeType());
		int calculatedAmout = 0;
		ArrayList<TaxHeadEstimate> estimates = new ArrayList<TaxHeadEstimate>();
		if (calculationTypeMap.containsKey("calsiLogic")) {
			LinkedHashMap ocEdcr = edcrService.getEDCRDetails(requestInfo, bpa);
			String jsonString = new JSONObject(ocEdcr).toString();
			DocumentContext context = JsonPath.using(Configuration.defaultConfiguration()).parse(jsonString);
			JSONArray permitNumber = context.read("edcrDetail.*.permitNumber");
			String jsonData = new JSONObject(calculationTypeMap).toString();
			DocumentContext calcContext = JsonPath.using(Configuration.defaultConfiguration()).parse(jsonData);
			JSONArray parameterPaths = calcContext.read("calsiLogic.*.paramPath");
			JSONArray tLimit = calcContext.read("calsiLogic.*.tolerancelimit");
			System.out.println("tolerance limit in: " + tLimit.get(0));
			DocumentContext edcrContext = null;
			if (!CollectionUtils.isEmpty(permitNumber)) {
				BPA permitBpa = bpaService.getBuildingPlan(requestInfo, bpa.getTenantId(), null,
						permitNumber.get(0).toString());
				if (permitBpa.getEdcrNumber() != null) {
					LinkedHashMap edcr = edcrService.getEDCRDetails(requestInfo, permitBpa);
					String edcrData = new JSONObject(edcr).toString();
					edcrContext = JsonPath.using(Configuration.defaultConfiguration()).parse(edcrData);
				}
			}

			for (int i = 0; i < parameterPaths.size(); i++) {
				Double ocTotalBuitUpArea = context.read(parameterPaths.get(i).toString());
				Double bpaTotalBuitUpArea = edcrContext.read(parameterPaths.get(i).toString());
				Double diffInBuildArea = ocTotalBuitUpArea - bpaTotalBuitUpArea;
				System.out.println("difference in area: " + diffInBuildArea);
				Double limit = Double.valueOf(tLimit.get(i).toString());
				if (diffInBuildArea > limit) {
					JSONArray data = calcContext.read("calsiLogic.*.deviation");
					System.out.println(data.get(0));
					JSONArray data1 = (JSONArray) data.get(0);
					for (int j = 0; j < data1.size(); j++) {
						LinkedHashMap diff = (LinkedHashMap) data1.get(j);
						Integer from = (Integer) diff.get("from");
						Integer to = (Integer) diff.get("to");
						Integer uom = (Integer) diff.get("uom");
						Integer mf = (Integer) diff.get("MF");
						if (diffInBuildArea >= from && diffInBuildArea <= to) {
							calculatedAmout = (int) (diffInBuildArea * mf * uom);
							break;
						}
					}
				} else {
					calculatedAmout = 0;
				}
				TaxHeadEstimate estimate = new TaxHeadEstimate();
				BigDecimal totalTax = BigDecimal.valueOf(calculatedAmout);
				if (totalTax.compareTo(BigDecimal.ZERO) == -1)
					throw new CustomException(BPACalculatorConstants.INVALID_AMOUNT, "Tax amount is negative");

				estimate.setEstimateAmount(totalTax);
				estimate.setCategory(Category.FEE);

				String taxHeadCode = utils.getTaxHeadCode(bpa.getBusinessService(), calulationCriteria.getFeeType());
				estimate.setTaxHeadCode(taxHeadCode);
				estimates.add(estimate);
			}
		} else {
			TaxHeadEstimate estimate = new TaxHeadEstimate();
			calculatedAmout = Integer
					.parseInt(calculationTypeMap.get(BPACalculatorConstants.MDMS_CALCULATIONTYPE_AMOUNT).toString());

			BigDecimal totalTax = BigDecimal.valueOf(calculatedAmout);
			if (totalTax.compareTo(BigDecimal.ZERO) == -1)
				throw new CustomException(BPACalculatorConstants.INVALID_AMOUNT, "Tax amount is negative");

			estimate.setEstimateAmount(totalTax);
			estimate.setCategory(Category.FEE);

			String taxHeadCode = utils.getTaxHeadCode(bpa.getBusinessService(), calulationCriteria.getFeeType());
			estimate.setTaxHeadCode(taxHeadCode);
			estimates.add(estimate);
		}
		estimatesAndSlabs.setEstimates(estimates);
		return estimatesAndSlabs;
	}

	/**
	 * @param criteria
	 * @param requestInfo
	 * @param extraParamsForCalculationMap
	 * @return
	 */
	private EstimatesAndSlabs getBaseTaxV2(CalulationCriteria criteria, RequestInfo requestInfo, Map<String, Object> extraParamsForCalculationMap) {
		BPA bpa = criteria.getBpa();
		String feeType = criteria.getFeeType();

		EstimatesAndSlabs estimatesAndSlabs = new EstimatesAndSlabs();
		BillingSlabSearchCriteria searchCriteria = new BillingSlabSearchCriteria();
		searchCriteria.setTenantId(bpa.getTenantId());

		ArrayList<TaxHeadEstimate> estimates = new ArrayList<TaxHeadEstimate>();

		if(BPACalculatorConstants.BUILDING_PLAN_OC.equalsIgnoreCase(criteria.getApplicationType())) {
			Object mdmsData = extraParamsForCalculationMap.get("mdmsData");
			if (StringUtils.hasText(feeType)
					&& feeType.equalsIgnoreCase(BPACalculatorConstants.MDMS_CALCULATIONTYPE_APL_FEETYPE)) {
				calculateBpaOcFee(requestInfo, criteria, estimates, mdmsData,
						BPACalculatorConstants.MDMS_CALCULATIONTYPE_APL_FEETYPE);
			}
			if (StringUtils.hasText(feeType)
					&& feeType.equalsIgnoreCase(BPACalculatorConstants.MDMS_CALCULATIONTYPE_SANC_FEETYPE)) {
	
				calculateBpaOcFee(requestInfo, criteria, estimates, mdmsData,
						BPACalculatorConstants.MDMS_CALCULATIONTYPE_SANC_FEETYPE);
			}
		
		} else {
			if (StringUtils.hasText(feeType)
					&& feeType.equalsIgnoreCase(BPACalculatorConstants.MDMS_CALCULATIONTYPE_APL_FEETYPE)) {
				calculateTotalFee(requestInfo, criteria, estimates,
						BPACalculatorConstants.MDMS_CALCULATIONTYPE_APL_FEETYPE, extraParamsForCalculationMap);
	
			}
			if (StringUtils.hasText(feeType)
					&& feeType.equalsIgnoreCase(BPACalculatorConstants.MDMS_CALCULATIONTYPE_SANC_FEETYPE)) {
	
				calculateTotalFee(requestInfo, criteria, estimates,
						BPACalculatorConstants.MDMS_CALCULATIONTYPE_SANC_FEETYPE, extraParamsForCalculationMap);
			}
		}

		estimatesAndSlabs.setEstimates(estimates);
		return estimatesAndSlabs;

	}
	
	/**
	 * Calculate BPA OC fees
	 * @param requestInfo
	 * @param criteria
	 * @param estimates
	 * @param mdmsData
	 * @param feeType
	 */
	private void calculateBpaOcFee(RequestInfo requestInfo, CalulationCriteria criteria,
			ArrayList<TaxHeadEstimate> estimates, Object mdmsData, String feeType) {
		Map<String, Object> paramMap = prepareBpaOcParamMap(requestInfo, criteria, feeType);
		BigDecimal calculatedTotalAmout = calculateTotalBpaOcFeeAmount(paramMap, estimates, mdmsData);
		if (calculatedTotalAmout.compareTo(BigDecimal.ZERO) == -1) {
			throw new CustomException(BPACalculatorConstants.INVALID_AMOUNT, "Tax amount is negative");
		}
	}

	/**
	 * Calculate Total BPA OC
	 * @param paramMap
	 * @param estimates
	 * @param mdmsData
	 * @return
	 */
	private BigDecimal calculateTotalBpaOcFeeAmount(Map<String, Object> paramMap,
			ArrayList<TaxHeadEstimate> estimates, Object mdmsData) {
		BigDecimal calculatedTotalOcAmout = BigDecimal.ZERO;
		String applicationType = null;
		String serviceType = null;
		String feeType = null;
		String occupancyType = null;
		if (null != paramMap.get(BPACalculatorConstants.APPLICATION_TYPE)) {
			applicationType = (String) paramMap.get(BPACalculatorConstants.APPLICATION_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.SERVICE_TYPE)) {
			serviceType = (String) paramMap.get(BPACalculatorConstants.SERVICE_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.FEE_TYPE)) {
			feeType = (String) paramMap.get(BPACalculatorConstants.FEE_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE)) {
			occupancyType = (String) paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE);
		}
		if (StringUtils.hasText(applicationType) && (StringUtils.hasText(serviceType))
				&& StringUtils.hasText(occupancyType) && (StringUtils.hasText(feeType))) {
			if (feeType.equalsIgnoreCase(BPACalculatorConstants.MDMS_CALCULATIONTYPE_APL_FEETYPE)) {
				calculatedTotalOcAmout = calculateTotalOcApplicationFee(paramMap, estimates);
			} else if (feeType.equalsIgnoreCase(BPACalculatorConstants.MDMS_CALCULATIONTYPE_SANC_FEETYPE)) {
				calculatedTotalOcAmout = calculateTotalOcSanctionFee(paramMap, estimates, mdmsData);
			}
		}
		
		return calculatedTotalOcAmout;
	}

	/**
	 * Calculate Sanction Fee for BPA OC
	 * @param paramMap
	 * @param estimates
	 * @param mdmsData
	 * @return
	 */
	private BigDecimal calculateTotalOcSanctionFee(Map<String, Object> paramMap, ArrayList<TaxHeadEstimate> estimates, Object mdmsData) {
		BigDecimal totalOcSanctionFee = BigDecimal.ZERO;
		BigDecimal compSetbackFee = BigDecimal.ZERO;
		BigDecimal compFARFee = BigDecimal.ZERO;
		BigDecimal eidpFee = BigDecimal.ZERO;
		BigDecimal cessFee = BigDecimal.ZERO;
		BigDecimal grandOccupancyCertFee = BigDecimal.ZERO;

		compSetbackFee = calculateOccupancyCompoundingFeeForSetback(paramMap, estimates, mdmsData);
		compFARFee = calculateOccupancyCompoundingFeeForFAR(paramMap, estimates, mdmsData);
		eidpFee = calculateOccupancyEidpFee(paramMap, estimates);
		cessFee = calculateOccupancyConstructionWorkerWelfareCess(paramMap, estimates);
		grandOccupancyCertFee = calculateOccupancyGrandOccupancyCertificateFee(paramMap, estimates);
		
		totalOcSanctionFee = compFARFee.add(compSetbackFee).add(eidpFee).add(cessFee).add(grandOccupancyCertFee);
		
		return totalOcSanctionFee;
	}

	private BigDecimal calculateOccupancyGrandOccupancyCertificateFee(Map<String, Object> paramMap,
			ArrayList<TaxHeadEstimate> estimates) {
		BigDecimal grandOccupancyCertificateFee = BigDecimal.ZERO;
		String applicationType = null;
		String serviceType = null;
		
		if (null != paramMap.get(BPACalculatorConstants.APPLICATION_TYPE)) {
			applicationType = (String) paramMap.get(BPACalculatorConstants.APPLICATION_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.SERVICE_TYPE)) {
			serviceType = (String) paramMap.get(BPACalculatorConstants.SERVICE_TYPE);
		}
		if ((StringUtils.hasText(applicationType)
				&& applicationType.equalsIgnoreCase(BPACalculatorConstants.BUILDING_PLAN_OC))
				&& (StringUtils.hasText(serviceType)
				&& serviceType.equalsIgnoreCase(BPACalculatorConstants.NEW_CONSTRUCTION))) {
			Map<String, Object> edcrParamMap = (Map<String, Object>) paramMap.get(BPACalculatorConstants.EDCR_PARAM_MAP);
			ArrayList<TaxHeadEstimate> voidEstimates = new ArrayList<>();
			BigDecimal permitOrderScrutinyFee = calculateTotalScrutinyFee(edcrParamMap, voidEstimates);
			BigDecimal ocScrutinyFee = calculateOccupancyScrutinyFee(paramMap, voidEstimates);
			BigDecimal totalScrutinyFee = permitOrderScrutinyFee.add(ocScrutinyFee);
			grandOccupancyCertificateFee = totalScrutinyFee.multiply(ZERO_FIVE).setScale(2, RoundingMode.UP);
			
		}
		
		if(BigDecimal.ZERO.compareTo(grandOccupancyCertificateFee) < 0)
			generateTaxHeadEstimate(estimates, grandOccupancyCertificateFee, BPACalculatorConstants.TAXHEAD_BPA_OC_GRAND_OC_CERT, Category.FEE);
		return grandOccupancyCertificateFee;
	}

	/**
	 * Calculate BPA OC application fee
	 * @param paramMap
	 * @param estimates
	 * @return
	 */
	private BigDecimal calculateTotalOcApplicationFee(Map<String, Object> paramMap,
			ArrayList<TaxHeadEstimate> estimates) {
		BigDecimal totalOCApplicationFee = BigDecimal.ZERO;
		BigDecimal scrutinyFee = BigDecimal.ZERO;
		BigDecimal ocCertificateFee = BigDecimal.ZERO;
		
		ocCertificateFee = calculateOccupancyCertificateFee(paramMap, estimates);
		scrutinyFee = calculateOccupancyScrutinyFee(paramMap, estimates);
		
		totalOCApplicationFee = ocCertificateFee.add(scrutinyFee);
		
		return totalOCApplicationFee;
	}

	/**
	 * Prepare required data for BPA OC
	 * @param requestInfo
	 * @param criteria
	 * @param feeType
	 * @return
	 */
	private Map<String, Object> prepareBpaOcParamMap(RequestInfo requestInfo, CalulationCriteria criteria, String feeType) {
		BPA bpa = criteria.getBpa();
		String applicationType = criteria.getApplicationType();
		String serviceType = criteria.getServiceType();
		String riskType = criteria.getBpa().getRiskType();
		
		LinkedHashMap ocEdcr = edcrService.getEDCRDetails(requestInfo, bpa);
		String jsonString = new JSONObject(ocEdcr).toString();
		DocumentContext ocContext = JsonPath.using(Configuration.defaultConfiguration()).parse(jsonString);
		JSONArray permitNumber = ocContext.read("edcrDetail.*.permitNumber");
		DocumentContext edcrContext = null;
		if (!CollectionUtils.isEmpty(permitNumber)) {
			BPA permitBpa = bpaService.getBuildingPlan(requestInfo, bpa.getTenantId(), null,
					permitNumber.get(0).toString());
			if (permitBpa.getEdcrNumber() != null) {
				LinkedHashMap edcr = edcrService.getEDCRDetails(requestInfo, permitBpa);
				String edcrData = new JSONObject(edcr).toString();
				edcrContext = JsonPath.using(Configuration.defaultConfiguration()).parse(edcrData);
			}
		}
		
		Map<String, Object> paramMap = new HashMap<>();
		
		paramMap.put(BPACalculatorConstants.EDCR_PARAM_MAP, prepareParamMapForPermitOrder(edcrContext, riskType, serviceType));

		JSONArray occupancyTypeJSONArray = ocContext.read(BPACalculatorConstants.OCCUPANCY_TYPE_PATH);
		if (!CollectionUtils.isEmpty(occupancyTypeJSONArray)) {
			if (null != occupancyTypeJSONArray.get(0)) {
				String occupancyType = occupancyTypeJSONArray.get(0).toString();
				paramMap.put(BPACalculatorConstants.OCCUPANCY_TYPE, occupancyType);
			}
		}

		JSONArray subOccupancyTypeJSONArray = ocContext.read(BPACalculatorConstants.SUB_OCCUPANCY_TYPE_PATH);
		if (!CollectionUtils.isEmpty(subOccupancyTypeJSONArray)) {
			if (null != subOccupancyTypeJSONArray.get(0)) {
				String subOccupancyType = subOccupancyTypeJSONArray.get(0).toString();
				paramMap.put(BPACalculatorConstants.SUB_OCCUPANCY_TYPE, subOccupancyType);
			}

		}

		JSONArray ocPlotAreas = ocContext.read(BPACalculatorConstants.PLOT_AREA_PATH);
		if (!CollectionUtils.isEmpty(ocPlotAreas)) {
			if (null != ocPlotAreas.get(0)) {
				String plotAreaString = ocPlotAreas.get(0).toString();
				Double plotArea = Double.parseDouble(plotAreaString);
				paramMap.put(BPACalculatorConstants.PLOT_AREA, plotArea);
			}
		}
		
		JSONArray plotAreas = edcrContext.read(BPACalculatorConstants.PLOT_AREA_PATH);
		if (!CollectionUtils.isEmpty(plotAreas)) {
			if (null != plotAreas.get(0)) {
				String plotAreaString = plotAreas.get(0).toString();
				Double plotArea = Double.parseDouble(plotAreaString);
				paramMap.put(BPACalculatorConstants.PLOT_AREA_EDCR, plotArea);
			}
		}
		
		JSONArray edcrTotalBuitUpAreas = edcrContext.read(BPACalculatorConstants.TOTAL_FLOOR_AREA_PATH);
		if (!CollectionUtils.isEmpty(edcrTotalBuitUpAreas)) {
			if (null != edcrTotalBuitUpAreas.get(0)) {
				String edcrTotalBuitUpAreaString = edcrTotalBuitUpAreas.get(0).toString();
				Double totalBuitUpArea = Double.parseDouble(edcrTotalBuitUpAreaString);
				paramMap.put(BPACalculatorConstants.TOTAL_FLOOR_AREA_EDCR, totalBuitUpArea);
			}
		}
		
		JSONArray edcrTotalBuiltUpAreas = edcrContext.read(BPACalculatorConstants.TOTAL_BUILTUP_AREA_PATH);
		if (!CollectionUtils.isEmpty(edcrTotalBuiltUpAreas)) {
			if (null != edcrTotalBuiltUpAreas.get(0)) {
				String edcrTotalBuiltUpAreaString = edcrTotalBuiltUpAreas.get(0).toString();
				Double totalBuiltUpArea = Double.parseDouble(edcrTotalBuiltUpAreaString);
				paramMap.put(BPACalculatorConstants.TOTAL_BUILTUP_AREA_EDCR, totalBuiltUpArea);
			}
		}
		
		//totalBuiltUpArea of OC-
		JSONArray totalBuiltUpAreasOC = ocContext.read(BPACalculatorConstants.TOTAL_BUILTUP_AREA_PATH);
		if (!CollectionUtils.isEmpty(totalBuiltUpAreasOC)) {
			if (null != totalBuiltUpAreasOC.get(0)) {
				String ocTotalBuiltUpAreaString = totalBuiltUpAreasOC.get(0).toString();
				Double totalBuiltUpArea = Double.parseDouble(ocTotalBuiltUpAreaString);
				paramMap.put(BPACalculatorConstants.TOTAL_BUILTUP_AREA, totalBuiltUpArea);
			}
		}

		JSONArray totalBuitUpAreas = ocContext.read(BPACalculatorConstants.TOTAL_FLOOR_AREA_PATH);
		if (!CollectionUtils.isEmpty(totalBuitUpAreas)) {
			if (null != totalBuitUpAreas.get(0)) {
				String totalBuitUpAreaString = totalBuitUpAreas.get(0).toString();
				Double totalBuitUpArea = Double.parseDouble(totalBuitUpAreaString);
				paramMap.put(BPACalculatorConstants.TOTAL_FLOOR_AREA, totalBuitUpArea);
			}
		}
		
		JSONArray existingBuitUpAreas = edcrContext.read(BPACalculatorConstants.TOTAL_FLOOR_AREA_PATH);
		if (!CollectionUtils.isEmpty(totalBuitUpAreas) && !CollectionUtils.isEmpty(existingBuitUpAreas)) {
			if (null != totalBuitUpAreas.get(0) && null != existingBuitUpAreas.get(0)) {
				String totalBuitUpAreaString = totalBuitUpAreas.get(0).toString();
				String existingBuitUpAreaString = existingBuitUpAreas.get(0).toString();
				Double totalBuitUpArea = Double.parseDouble(totalBuitUpAreaString);
				Double exisitingBuitUpArea = Double.parseDouble(existingBuitUpAreaString);
				paramMap.put(BPACalculatorConstants.DEVIATION_FLOOR_AREA, totalBuitUpArea-exisitingBuitUpArea);
			}
		}
		
		//use builtup area rather than floor area for some OC calculations-(this is stable wrt application fees do not change anything)
		JSONArray totalExistingBuiltUpArea=edcrContext.read(BPACalculatorConstants.TOTAL_BUILTUP_AREA_PATH);
		JSONArray totalBuiltUpAreas = ocContext.read(BPACalculatorConstants.TOTAL_BUILTUP_AREA_PATH);
		if (!CollectionUtils.isEmpty(totalBuiltUpAreas) && !CollectionUtils.isEmpty(totalExistingBuiltUpArea)) {
			if (null != totalBuiltUpAreas.get(0) && null != totalExistingBuiltUpArea.get(0)) {
				String totalBuiltUpAreaString = totalBuiltUpAreas.get(0).toString();
				String totalExistingBuiltUpAreaString = totalExistingBuiltUpArea.get(0).toString();
				Double totalBuiltUpArea = Double.parseDouble(totalBuiltUpAreaString);
				Double totalExisitingBuiltUpArea = Double.parseDouble(totalExistingBuiltUpAreaString);
				paramMap.put(BPACalculatorConstants.DEVIATION_BUILTUP_AREA, totalBuiltUpArea-totalExisitingBuiltUpArea);
			}
		}
		
		JSONArray totalbenchmarkValuePerAcre = ocContext.read(BPACalculatorConstants.BENCHMARK_VALUE_PATH);
		if (!CollectionUtils.isEmpty(totalbenchmarkValuePerAcre)) {
			if (null != totalbenchmarkValuePerAcre.get(0)) {
				String benchmarkValuePerAcreString = totalbenchmarkValuePerAcre.get(0).toString();
				Double benchmarkValuePerAcre = Double.parseDouble(benchmarkValuePerAcreString);
				paramMap.put(BPACalculatorConstants.BMV_ACRE, benchmarkValuePerAcre);
			}
		}

		JSONArray totalbaseFar = ocContext.read(BPACalculatorConstants.BASE_FAR_PATH);
		if (!CollectionUtils.isEmpty(totalbaseFar)) {
			if (null != totalbaseFar.get(0)) {
				String baseFarString = totalbaseFar.get(0).toString();
				Double baseFar = Double.parseDouble(baseFarString);
				paramMap.put(BPACalculatorConstants.BASE_FAR, baseFar);
			}
		}

		JSONArray totalProvidedFar = ocContext.read(BPACalculatorConstants.PROVIDED_FAR_PATH);
		if (!CollectionUtils.isEmpty(totalProvidedFar)) {
			if (null != totalProvidedFar.get(0)) {
				String providedFarString = totalProvidedFar.get(0).toString();
				Double providedFar = Double.parseDouble(providedFarString);
				paramMap.put(BPACalculatorConstants.PROVIDED_FAR, providedFar);
			}
		}
		
		JSONArray totalpermissibleFar = ocContext.read(BPACalculatorConstants.PERMISSABLE_FAR_PATH);
		if (!CollectionUtils.isEmpty(totalpermissibleFar)) {
			if (null != totalpermissibleFar.get(0)) {
				String permissibleFarString = totalpermissibleFar.get(0).toString();
				Double permissibleFar = Double.parseDouble(permissibleFarString);
				paramMap.put(BPACalculatorConstants.PERMISSABLE_FAR, permissibleFar);
			}
		}

		JSONArray totalProjectValueForEIDPOcArray = ocContext.read(BPACalculatorConstants.PROJECT_VALUE_FOR_EIDP_PATH);
		if (!CollectionUtils.isEmpty(totalProjectValueForEIDPOcArray)) {
			String totalProjectValueForEIDPOc = totalProjectValueForEIDPOcArray.get(0).toString();
			Double projectValueForEIDPOc = Double.parseDouble(totalProjectValueForEIDPOc);
			paramMap.put(BPACalculatorConstants.PROJECT_VALUE_FOR_EIDP_OC, projectValueForEIDPOc);
		}
		
		JSONArray totalProjectValueForEIDPArray = edcrContext.read(BPACalculatorConstants.PROJECT_VALUE_FOR_EIDP_PATH);
		if (!CollectionUtils.isEmpty(totalProjectValueForEIDPArray)) {
			String totalProjectValueForEIDP = totalProjectValueForEIDPArray.get(0).toString();
			Double projectValueForEIDP = Double.parseDouble(totalProjectValueForEIDP);
			paramMap.put(BPACalculatorConstants.PROJECT_VALUE_FOR_EIDP, projectValueForEIDP);
		}

		JSONArray isProjectUndertakingByGovtArray = ocContext.read(BPACalculatorConstants.PROJECT_UNDERTAKING_BY_GOVT_PATH);
		if (!CollectionUtils.isEmpty(isProjectUndertakingByGovtArray)) {
			boolean isProjectUndertakingByGovt = ((String) isProjectUndertakingByGovtArray.get(0)).equalsIgnoreCase("YES") ? true : false;
			paramMap.put(BPACalculatorConstants.PROJECT_UNDERTAKING_BY_GOVT, isProjectUndertakingByGovt);
		}
		
		JSONArray edcrBlockDetailArray = edcrContext.read(BPACalculatorConstants.BLOCKS_PATH);
		if (!CollectionUtils.isEmpty(edcrBlockDetailArray)) {
			paramMap.put(BPACalculatorConstants.BLOCK_DETAILS_EDCR, edcrBlockDetailArray.get(0));
		}
		
		JSONArray ocBlockDetailArray = ocContext.read(BPACalculatorConstants.BLOCKS_PATH);
		if (!CollectionUtils.isEmpty(ocBlockDetailArray)) {
			paramMap.put(BPACalculatorConstants.BLOCK_DETAILS_OC, ocBlockDetailArray.get(0));
		}
		
		paramMap.put(BPACalculatorConstants.APPLICATION_TYPE, applicationType);
		paramMap.put(BPACalculatorConstants.SERVICE_TYPE, serviceType);
		paramMap.put(BPACalculatorConstants.RISK_TYPE, riskType);
		paramMap.put(BPACalculatorConstants.FEE_TYPE, feeType);
		return paramMap;
	}

	private Map<String, Object> prepareParamMapForPermitOrder(DocumentContext edcrContext, String riskType, String serviceType) {
		Map<String, Object> paramMap = new HashMap<>();

		JSONArray occupancyTypeJSONArray = edcrContext.read(BPACalculatorConstants.OCCUPANCY_TYPE_PATH);
		if (!CollectionUtils.isEmpty(occupancyTypeJSONArray)) {
			if (null != occupancyTypeJSONArray.get(0)) {
				String occupancyType = occupancyTypeJSONArray.get(0).toString();
				paramMap.put(BPACalculatorConstants.OCCUPANCY_TYPE, occupancyType);
			}
		}

		JSONArray subOccupancyTypeJSONArray = edcrContext.read(BPACalculatorConstants.SUB_OCCUPANCY_TYPE_PATH);
		if (!CollectionUtils.isEmpty(subOccupancyTypeJSONArray)) {
			if (null != subOccupancyTypeJSONArray.get(0)) {
				String subOccupancyType = subOccupancyTypeJSONArray.get(0).toString();
				paramMap.put(BPACalculatorConstants.SUB_OCCUPANCY_TYPE, subOccupancyType);
			}
		}
		
		JSONArray plotAreas = edcrContext.read(BPACalculatorConstants.PLOT_AREA_PATH);
		if (!CollectionUtils.isEmpty(plotAreas)) {
			if (null != plotAreas.get(0)) {
				String plotAreaString = plotAreas.get(0).toString();
				Double plotArea = Double.parseDouble(plotAreaString);
				paramMap.put(BPACalculatorConstants.PLOT_AREA, plotArea);
			}
		}
		
		JSONArray totalBuitUpAreas = edcrContext.read(BPACalculatorConstants.TOTAL_FLOOR_AREA_PATH);
		if (!CollectionUtils.isEmpty(totalBuitUpAreas)) {
			if (null != totalBuitUpAreas.get(0)) {
				String totalBuitUpAreaString = totalBuitUpAreas.get(0).toString();
				Double totalBuitUpArea = Double.parseDouble(totalBuitUpAreaString);
				paramMap.put(BPACalculatorConstants.TOTAL_FLOOR_AREA, totalBuitUpArea);
			}
		}
		
		paramMap.put(BPACalculatorConstants.APPLICATION_TYPE, BPACalculatorConstants.BUILDING_PLAN_SCRUTINY);
		paramMap.put(BPACalculatorConstants.SERVICE_TYPE, serviceType);
		paramMap.put(BPACalculatorConstants.RISK_TYPE, riskType);
		//paramMap.put(BPACalculatorConstants.FEE_TYPE, feeType);
		return paramMap;
	}

	/**
	 * @param requestInfo
	 * @param criteria
	 * @param estimates
	 * @param feeType
	 * @param extraParamsForCalculationMap
	 */
	private void calculateTotalFee(RequestInfo requestInfo, CalulationCriteria criteria,
			ArrayList<TaxHeadEstimate> estimates, String feeType, Map<String, Object> extraParamsForCalculationMap) {
		Map<String, Object> paramMap = prepareMaramMap(requestInfo, criteria, feeType, extraParamsForCalculationMap);
		//move all extra parameters from extraParamsForCalculationMap to paramMap-
		paramMap.put("mdmsData", extraParamsForCalculationMap.get("mdmsData"));
		paramMap.put("tenantId", extraParamsForCalculationMap.get("tenantId"));
		paramMap.put("BPA", extraParamsForCalculationMap.get("BPA"));
		paramMap.put("requestInfo", requestInfo);
		paramMap.put(BPACalculatorConstants.SPARIT_CHECK, extraParamsForCalculationMap.get(BPACalculatorConstants.SPARIT_CHECK));
		BigDecimal calculatedTotalAmout = calculateTotalFeeAmount(paramMap, estimates);
		if (calculatedTotalAmout.compareTo(BigDecimal.ZERO) == -1) {
			throw new CustomException(BPACalculatorConstants.INVALID_AMOUNT, "Tax amount is negative");
		}
		// TaxHeadEstimate estimate = new TaxHeadEstimate();
		// estimate.setEstimateAmount(calculatedTotalAmout.setScale(0,
		// BigDecimal.ROUND_UP));
		// estimate.setCategory(Category.FEE);
		// String taxHeadCode =
		// utils.getTaxHeadCode(criteria.getBpa().getBusinessService(),
		// criteria.getFeeType());
		// estimate.setTaxHeadCode(taxHeadCode);
		// estimates.add(estimate);
	}
	
	private void setRevisionDataInParamMap(BPA bpa, Map<String, Object> paramMap) {
		if (Boolean.TRUE.equals(bpa.getIsRevisionApplication())) {
			//part1 - set entire revision in paramMap irrespective of isSujogExistingApplication true or false-
			// fetch revision data and set in paramMap-
			RevisionSearchCriteria revisionSearchCriteria = RevisionSearchCriteria.builder()
					.bpaApplicationNo(bpa.getApplicationNo()).build();
			List<Revision> revisions = revisionRepository.getRevisionData(revisionSearchCriteria);
			if (CollectionUtils.isEmpty(revisions)) {
				throw new CustomException(
						"No revision data found for refBpaApplicationNo:" + bpa.getApplicationNo() + ",refPermitNo:"
								+ bpa.getApprovalNo(),
						"No revision data found for refBpaApplicationNo:" + bpa.getApplicationNo() + ",refPermitNo:"
								+ bpa.getApprovalNo());
			}
			// TODO: could there be multiple revisions for one application as using first element-
			paramMap.put(BPACalculatorConstants.REVISION, revisions.get(0));
			
		}
	}
	
	private Map<String, Object> prepareMaramMap(RequestInfo requestInfo, CalulationCriteria criteria, String feeType,
			Map<String, Object> extraParamsForCalculationMap) {
		String businessService = "";
		Boolean isRevisionApplication = Boolean.FALSE;
		if (Objects.nonNull(extraParamsForCalculationMap.get("BPA"))
				&& !StringUtils.isEmpty(((BPA) extraParamsForCalculationMap.get("BPA")).getBusinessService())) {
			BPA bpa = (BPA) extraParamsForCalculationMap.get("BPA");
			businessService = bpa.getBusinessService();
			isRevisionApplication = bpa.getIsRevisionApplication(); 
		}

		if (BPACalculatorConstants.BUSINESSSERVICE_PREAPPROVEDPLAN.equalsIgnoreCase(businessService)) {
			// for BPA 6(preapproved plan) -
			return prepareParamMapForPreapprovedPlan(requestInfo, criteria, feeType, extraParamsForCalculationMap);
		} else if (Boolean.TRUE.equals(isRevisionApplication)
				&& criteria.getFeeType().equalsIgnoreCase(BPACalculatorConstants.MDMS_CALCULATIONTYPE_SANC_FEETYPE)) {
			//Assumption: application on top of a preapproved plan could never be a revision application.
			BPA bpa = (BPA) extraParamsForCalculationMap.get("BPA");
			Map<String, Object> paramMap = new HashMap<>();
			setRevisionDataInParamMap(bpa, paramMap);
			Revision revision = (Revision) paramMap.get(BPACalculatorConstants.REVISION);
			if(revision.isSujogExistingApplication()) {
				// need to prepare paramMap as usual for BPA1,2,3,4 -
				paramMap.putAll(prepareParamMapForBpa1to4(requestInfo, criteria, feeType));
			}
			else {
				// need to prepare paramMap from refApplicationDetails - 
				prepareParamMapForRevisionNonSujogApplication(requestInfo, criteria, feeType, paramMap, bpa);
			}
			return paramMap;
		} else {
			// for BPA 1,2,3,4 -
			return prepareParamMapForBpa1to4(requestInfo, criteria, feeType);
		}
	}
	
	private void prepareParamMapForRevisionNonSujogApplication(RequestInfo requestInfo, CalulationCriteria criteria,
			String feeType, Map<String, Object> paramMap, BPA bpa) {
		Revision revision = (Revision) paramMap.get(BPACalculatorConstants.REVISION);
		if(revision.isSujogExistingApplication())
			return;
		String applicationType = criteria.getApplicationType();
		String serviceType = criteria.getServiceType();
		// assumption : applicationType, serviceType of old non-sujog application was same as that of new revision application- 
		paramMap.put(BPACalculatorConstants.APPLICATION_TYPE, applicationType);
		paramMap.put(BPACalculatorConstants.SERVICE_TYPE, serviceType);
		paramMap.put(BPACalculatorConstants.FEE_TYPE, feeType);
		if (Objects.isNull(revision.getRefApplicationDetails()) || !(revision.getRefApplicationDetails() instanceof Map)
				|| CollectionUtils.isEmpty(((Map) revision.getRefApplicationDetails()))) {
			throw new CustomException("refApplicationDetails must not be null or empty for non-sujog permit numbers",
					"refApplicationDetails must not be null or empty for non-sujog permit numbers");
		}
		Map<String, Object> refApplicationDetails = (Map<String, Object>) revision.getRefApplicationDetails();

		// edcrDetail.*.planDetail.virtualBuilding.mostRestrictiveFarHelper.type.code
		paramMap.put(BPACalculatorConstants.OCCUPANCY_TYPE,
				refApplicationDetails.get(BPACalculatorConstants.OCCUPANCY_TYPE));
		// edcrDetail.*.planDetail.virtualBuilding.mostRestrictiveFarHelper.subtype.code"
		paramMap.put(BPACalculatorConstants.SUB_OCCUPANCY_TYPE,
				refApplicationDetails.get(BPACalculatorConstants.SUB_OCCUPANCY_TYPE));
		// edcrDetail.*.planDetail.plot.area
		paramMap.put(BPACalculatorConstants.PLOT_AREA,
				getValueInDoubleFormat(BPACalculatorConstants.PLOT_AREA,
						refApplicationDetails.get(BPACalculatorConstants.PLOT_AREA)));
		// edcrDetail.*.planDetail.virtualBuilding.totalFloorArea
		paramMap.put(BPACalculatorConstants.TOTAL_FLOOR_AREA,
				getValueInDoubleFormat(BPACalculatorConstants.TOTAL_FLOOR_AREA,
						refApplicationDetails.get(BPACalculatorConstants.TOTAL_FLOOR_AREA)));
		// edcrDetail.*.planDetail.virtualBuilding.totalBuitUpArea
		paramMap.put(BPACalculatorConstants.TOTAL_BUILTUP_AREA_EDCR,
				getValueInDoubleFormat(BPACalculatorConstants.TOTAL_BUILTUP_AREA_EDCR,
						refApplicationDetails.get(BPACalculatorConstants.TOTAL_BUILTUP_AREA_EDCR)));
		// edcrDetail.*.planDetail.totalEWSFeeEffectiveArea
		paramMap.put(BPACalculatorConstants.EWS_AREA,
				getValueInDoubleFormat(BPACalculatorConstants.EWS_AREA,
						refApplicationDetails.get(BPACalculatorConstants.EWS_AREA)));
		// edcrDetail.*.planDetail.planInformation.benchmarkValuePerAcre
		paramMap.put(BPACalculatorConstants.BMV_ACRE,
				getValueInDoubleFormat(BPACalculatorConstants.BMV_ACRE,
						refApplicationDetails.get(BPACalculatorConstants.BMV_ACRE)));
		// edcrDetail.*.planDetail.farDetails.baseFar
		paramMap.put(BPACalculatorConstants.BASE_FAR,
				getValueInDoubleFormat(BPACalculatorConstants.BASE_FAR,
						refApplicationDetails.get(BPACalculatorConstants.BASE_FAR)));
		// edcrDetail.*.planDetail.farDetails.providedFar
		paramMap.put(BPACalculatorConstants.PROVIDED_FAR,
				getValueInDoubleFormat(BPACalculatorConstants.PROVIDED_FAR,
						refApplicationDetails.get(BPACalculatorConstants.PROVIDED_FAR)));
		// edcrDetail.*.planDetail.farDetails.permissableFar
		paramMap.put(BPACalculatorConstants.PERMISSABLE_FAR,
				getValueInDoubleFormat(BPACalculatorConstants.PERMISSABLE_FAR,
						refApplicationDetails.get(BPACalculatorConstants.PERMISSABLE_FAR)));
		// edcrDetail.*.planDetail.farDetails.tdrFarRelaxation
		paramMap.put(BPACalculatorConstants.TDR_FAR_RELAXATION,
				getValueInDoubleFormat(BPACalculatorConstants.TDR_FAR_RELAXATION,
						refApplicationDetails.get(BPACalculatorConstants.TDR_FAR_RELAXATION)));
		// edcrDetail.*.planDetail.planInformation.totalNoOfDwellingUnits
		paramMap.put(BPACalculatorConstants.TOTAL_NO_OF_DWELLING_UNITS,
				getValueInIntegerFormat(BPACalculatorConstants.TOTAL_NO_OF_DWELLING_UNITS,
						refApplicationDetails.get(BPACalculatorConstants.TOTAL_NO_OF_DWELLING_UNITS)));
		// edcrDetail.*.planDetail.planInformation.shelterFeeRequired
		paramMap.put(BPACalculatorConstants.SHELTER_FEE, refApplicationDetails.get(BPACalculatorConstants.SHELTER_FEE));
		// edcrDetail.*.planDetail.planInformation.isSecurityDepositRequired
		paramMap.put(BPACalculatorConstants.SECURITY_DEPOSIT,
				refApplicationDetails.get(BPACalculatorConstants.SECURITY_DEPOSIT));
		// edcrDetail.*.planDetail.planInformation.projectValueForEIDP
		paramMap.put(BPACalculatorConstants.PROJECT_VALUE_FOR_EIDP,
				getValueInDoubleFormat(BPACalculatorConstants.PROJECT_VALUE_FOR_EIDP,
						refApplicationDetails.get(BPACalculatorConstants.PROJECT_VALUE_FOR_EIDP)));
		// edcrDetail.*.planDetail.planInformation.isRetentionFeeApplicable
		paramMap.put(BPACalculatorConstants.IS_RETENTION_FEE_APPLICABLE,
				refApplicationDetails.get(BPACalculatorConstants.IS_RETENTION_FEE_APPLICABLE));
		// edcrDetail.*.planDetail.planInformation.numberOfTemporaryStructures
		paramMap.put(BPACalculatorConstants.NUMBER_OF_TEMP_STRUCTURES,
				getValueInDoubleFormat(BPACalculatorConstants.NUMBER_OF_TEMP_STRUCTURES,
						refApplicationDetails.get(BPACalculatorConstants.NUMBER_OF_TEMP_STRUCTURES)));
		paramMap.put(BPACalculatorConstants.RISK_TYPE, refApplicationDetails.get(BPACalculatorConstants.RISK_TYPE));
		
		//set the values in ocuupancy list and put in parammap
		List<Occupancy> occupancylist = new ArrayList<>();
		Occupancy occ = new Occupancy();
		occ.setBuiltUpArea((Double)paramMap.get(BPACalculatorConstants.TOTAL_BUILTUP_AREA_EDCR));
		occ.setFloorArea((Double)paramMap.get(BPACalculatorConstants.TOTAL_FLOOR_AREA));
		occ.setOccupancyCode((String)paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE));
		occ.setSubOccupancyCode((String)paramMap.get(BPACalculatorConstants.SUB_OCCUPANCY_TYPE));
		occupancylist.add(occ);
		paramMap.put(BPACalculatorConstants.OCCUPANCYLIST,occupancylist);
	}
	
	private Double getValueInDoubleFormat(String key, Object value) {
		Double doubleValue = Double.parseDouble("0");
		if (StringUtils.isEmpty(value)) {
			log.info("setting 0 value of key:" + key + " as value is null/empty");
			;
			return doubleValue;
		}
		try {
			doubleValue = Double.parseDouble(String.valueOf(value));
		} catch (NumberFormatException nfe) {
			log.error("NumberFormatException: setting value as 0 as value of key:" + key + " is not a Double:" + value);
		}
		return doubleValue;
	}
	
	private Integer getValueInIntegerFormat(String key, Object value) {
		Integer integerValue = Integer.parseInt("0");
		if (StringUtils.isEmpty(value)) {
			log.info("setting 0 value of key:" + key + " as value is null/empty");
			;
			return integerValue;
		}
		try {
			integerValue = Integer.parseInt(String.valueOf(value));
		} catch (NumberFormatException nfe) {
			log.error(
					"NumberFormatException: setting value as 0 as value of key:" + key + " is not a Integer:" + value);
		}
		return integerValue;
	}
	
	/**
	 * @param requestInfo
	 * @param criteria
	 * @param feeType
	 * @return
	 */
	private Map<String, Object> prepareParamMapForPreapprovedPlan(RequestInfo requestInfo, CalulationCriteria criteria,
			String feeType, Map<String, Object> extraParamsForCalculationMap) {
		String applicationType = criteria.getApplicationType();
		String serviceType = criteria.getServiceType();
		String riskType = criteria.getBpa().getRiskType();
		Map<String, Object> paramMap = new HashMap<>();
		paramMap.put(BPACalculatorConstants.APPLICATION_TYPE, applicationType);
		paramMap.put(BPACalculatorConstants.SERVICE_TYPE, serviceType);
		paramMap.put(BPACalculatorConstants.RISK_TYPE, riskType);
		paramMap.put(BPACalculatorConstants.FEE_TYPE, feeType);
		
		//fetch preapproved plan-
		PreapprovedPlan preapprovedPlan = fetchPreapprovedPlanFromDrawingNo(criteria.getBpa().getEdcrNumber());
		Map<String, String> drawingDetail = (Map) preapprovedPlan.getDrawingDetail();
		

		List<Occupancy> occupancy = edcrHelperUtils.getOccupancieswiseDetailsforpreApproved(preapprovedPlan.getDrawingDetail());
		
		//put the data occupancy wise
		paramMap.put(BPACalculatorConstants.OCCUPANCYLIST, occupancy);
		//TODO remove hardcoded parameters and replace with proper parameters from preapproved plan
		//"edcrDetail.*.planDetail.virtualBuilding.totalBuitUpArea"
		paramMap.put(BPACalculatorConstants.TOTAL_BUILTUP_AREA_EDCR, drawingDetail.get("totalBuitUpArea"));
		//edcrDetail.*.planDetail.virtualBuilding.mostRestrictiveFarHelper.type.code
		
		paramMap.put(BPACalculatorConstants.OCCUPANCY_TYPE, "A");
		//
		//edcrDetail.*.planDetail.virtualBuilding.mostRestrictiveFarHelper.subtype.code"
		paramMap.put(BPACalculatorConstants.SUB_OCCUPANCY_TYPE, "A-P");
		//paramMap.put("", );
		
		
		return paramMap;
	}
	
	private PreapprovedPlan fetchPreapprovedPlanFromDrawingNo(String drawingNo) {
		PreapprovedPlanSearchCriteria criteria = new PreapprovedPlanSearchCriteria();
		criteria.setDrawingNo(drawingNo);
		List<PreapprovedPlan> preapprovedPlans = preapprovedPlanRepository.getPreapprovedPlansData(criteria);
		if (CollectionUtils.isEmpty(preapprovedPlans)) {
			log.error("No preapproved plan with provided drawingNo:" + drawingNo);
			throw new CustomException("No preapproved plan with provided drawingNo",
					"No preapproved plan with provided drawingNo");
		}
		return preapprovedPlans.get(0);
	}

	/**
	 * @param requestInfo
	 * @param criteria
	 * @param feeType
	 * @return
	 */
	private Map<String, Object> prepareParamMapForBpa1to4(RequestInfo requestInfo, CalulationCriteria criteria,
			String feeType) {
		String applicationType = criteria.getApplicationType();
		String serviceType = criteria.getServiceType();
		String riskType = criteria.getBpa().getRiskType();
		@SuppressWarnings("unchecked")
		LinkedHashMap<String, Object> edcr = edcrService.getEDCRDetails(requestInfo, criteria.getBpa());
		
		String jsonString = new JSONObject(edcr).toString();
		DocumentContext context = JsonPath.using(Configuration.defaultConfiguration()).parse(jsonString);
		
		List<Occupancy> occupancy = edcrHelperUtils.getOccupancieswiseDetails(edcr);
		
		//List<Occupancy> occupancy =

		Map<String, Object> paramMap = new HashMap<>();
/*
		JSONArray occupancyTypeJSONArray = context.read(BPACalculatorConstants.OCCUPANCY_TYPE_PATH);
		if (!CollectionUtils.isEmpty(occupancyTypeJSONArray)) {
			if (null != occupancyTypeJSONArray.get(0)) {
				String occupancyType = occupancyTypeJSONArray.get(0).toString();
				paramMap.put(BPACalculatorConstants.OCCUPANCY_TYPE, occupancyType);
			}
		}

		JSONArray subOccupancyTypeJSONArray = context.read(BPACalculatorConstants.SUB_OCCUPANCY_TYPE_PATH);
		if (!CollectionUtils.isEmpty(subOccupancyTypeJSONArray)) {
			if (null != subOccupancyTypeJSONArray.get(0)) {
				String subOccupancyType = subOccupancyTypeJSONArray.get(0).toString();
				paramMap.put(BPACalculatorConstants.SUB_OCCUPANCY_TYPE, subOccupancyType);
			}

		}*/
		paramMap.put(BPACalculatorConstants.OCCUPANCYLIST, occupancy);

		JSONArray plotAreas = context.read(BPACalculatorConstants.PLOT_AREA_PATH);
		if (!CollectionUtils.isEmpty(plotAreas)) {
			if (null != plotAreas.get(0)) {
				String plotAreaString = plotAreas.get(0).toString();
				Double plotArea = Double.parseDouble(plotAreaString);
				paramMap.put(BPACalculatorConstants.PLOT_AREA, plotArea);
			}
		}

		JSONArray totalBuitUpAreas = context.read(BPACalculatorConstants.TOTAL_FLOOR_AREA_PATH);
		if (!CollectionUtils.isEmpty(totalBuitUpAreas)) {
			if (null != totalBuitUpAreas.get(0)) {
				String totalBuitUpAreaString = totalBuitUpAreas.get(0).toString();
				Double totalBuitUpArea = Double.parseDouble(totalBuitUpAreaString);
				paramMap.put(BPACalculatorConstants.TOTAL_FLOOR_AREA, totalBuitUpArea);
			}
		}
		
		JSONArray edcrTotalBuiltUpAreas = context.read(BPACalculatorConstants.TOTAL_BUILTUP_AREA_PATH);
		if (!CollectionUtils.isEmpty(edcrTotalBuiltUpAreas)) {
			if (null != edcrTotalBuiltUpAreas.get(0)) {
				String edcrTotalBuiltUpAreaString = edcrTotalBuiltUpAreas.get(0).toString();
				Double totalBuiltUpArea = Double.parseDouble(edcrTotalBuiltUpAreaString);
				paramMap.put(BPACalculatorConstants.TOTAL_BUILTUP_AREA_EDCR, totalBuiltUpArea);
			}
		}

		JSONArray totalEWSAreas = context.read(BPACalculatorConstants.EWS_AREA_PATH);
		if (!CollectionUtils.isEmpty(totalEWSAreas)) {
			if (null != totalEWSAreas.get(0)) {
				String totalEWSAreaString = totalEWSAreas.get(0).toString();
				Double totalEWSArea = Double.parseDouble(totalEWSAreaString);
				paramMap.put(BPACalculatorConstants.EWS_AREA, totalEWSArea);
			}
		}

		JSONArray totalbenchmarkValuePerAcre = context.read(BPACalculatorConstants.BENCHMARK_VALUE_PATH);
		if (!CollectionUtils.isEmpty(totalbenchmarkValuePerAcre)) {
			if (null != totalbenchmarkValuePerAcre.get(0)) {
				String benchmarkValuePerAcreString = totalbenchmarkValuePerAcre.get(0).toString();
				Double benchmarkValuePerAcre = Double.parseDouble(benchmarkValuePerAcreString);
				paramMap.put(BPACalculatorConstants.BMV_ACRE, benchmarkValuePerAcre);
			}
		}

		JSONArray totalbaseFar = context.read(BPACalculatorConstants.BASE_FAR_PATH);
		if (!CollectionUtils.isEmpty(totalbaseFar)) {
			if (null != totalbaseFar.get(0)) {
				String baseFarString = totalbaseFar.get(0).toString();
				Double baseFar = Double.parseDouble(baseFarString);
				paramMap.put(BPACalculatorConstants.BASE_FAR, baseFar);
			}
		}

		JSONArray totalpermissibleFar = context.read(BPACalculatorConstants.PROVIDED_FAR_PATH);
		if (!CollectionUtils.isEmpty(totalpermissibleFar)) {
			if (null != totalpermissibleFar.get(0)) {
				String permissibleFarString = totalpermissibleFar.get(0).toString();
				Double permissibleFar = Double.parseDouble(permissibleFarString);
				paramMap.put(BPACalculatorConstants.PROVIDED_FAR, permissibleFar);
			}
		}
		
		JSONArray maxPermissibleFarJson = context.read(BPACalculatorConstants.PERMISSABLE_FAR_PATH);
		if (!CollectionUtils.isEmpty(maxPermissibleFarJson)) {
			if (null != maxPermissibleFarJson.get(0)) {
				String maxPermissibleFarString = maxPermissibleFarJson.get(0).toString();
				Double maxPermissibleFar = Double.parseDouble(maxPermissibleFarString);
				paramMap.put(BPACalculatorConstants.PERMISSABLE_FAR, maxPermissibleFar);
			}
		}
		
		JSONArray tdrFarRelaxationJson = context.read(BPACalculatorConstants.TDR_FAR_RELAXATION_PATH);
		if (!CollectionUtils.isEmpty(tdrFarRelaxationJson)) {
			if (null != tdrFarRelaxationJson.get(0)) {
				String tdrFarRelaxationString = tdrFarRelaxationJson.get(0).toString();
				Double tdrFarRelaxation = Double.parseDouble(tdrFarRelaxationString);
				paramMap.put(BPACalculatorConstants.TDR_FAR_RELAXATION, tdrFarRelaxation);
			}
		}

		JSONArray totalNoOfDwellingUnitsArray = context.read(BPACalculatorConstants.DWELLING_UNITS_PATH);
		if (!CollectionUtils.isEmpty(totalNoOfDwellingUnitsArray)) {
			if (null != totalNoOfDwellingUnitsArray.get(0)) {
				String totalNoOfDwellingUnitsString = totalNoOfDwellingUnitsArray.get(0).toString();
				Integer totalNoOfDwellingUnits = Integer.parseInt(totalNoOfDwellingUnitsString);
				paramMap.put(BPACalculatorConstants.TOTAL_NO_OF_DWELLING_UNITS, totalNoOfDwellingUnits);
			}
		}

		JSONArray isShelterFeeRequiredArray = context.read(BPACalculatorConstants.SHELTER_FEE_PATH);
		if (!CollectionUtils.isEmpty(isShelterFeeRequiredArray)) {
			boolean isShelterFeeRequired = (boolean) isShelterFeeRequiredArray.get(0);
			paramMap.put(BPACalculatorConstants.SHELTER_FEE, isShelterFeeRequired);
		}

		JSONArray isSecurityDepositRequiredArray = context.read(BPACalculatorConstants.SECURITY_DEPOSIT_PATH);
		if (!CollectionUtils.isEmpty(isSecurityDepositRequiredArray)) {
			boolean isSecurityDepositRequired = (boolean) isSecurityDepositRequiredArray.get(0);
			paramMap.put(BPACalculatorConstants.SECURITY_DEPOSIT, isSecurityDepositRequired);
		}
		
		JSONArray totalProjectValueForEIDPArray = context.read(BPACalculatorConstants.PROJECT_VALUE_FOR_EIDP_PATH);
		if (!CollectionUtils.isEmpty(totalProjectValueForEIDPArray)) {
			String totalProjectValueForEIDP = totalProjectValueForEIDPArray.get(0).toString();
			Double projectValueForEIDP = Double.parseDouble(totalProjectValueForEIDP);
			paramMap.put(BPACalculatorConstants.PROJECT_VALUE_FOR_EIDP, projectValueForEIDP);
		}
		
		JSONArray isRetentionFeeApplicableJson = context.read(BPACalculatorConstants.RETENTION_FEE_APPLICABLE_PATH);
		if (!CollectionUtils.isEmpty(isRetentionFeeApplicableJson)) {
			boolean isRetentionFeeApplicable = (boolean) isRetentionFeeApplicableJson.get(0);
			paramMap.put(BPACalculatorConstants.IS_RETENTION_FEE_APPLICABLE, isRetentionFeeApplicable);
		}
		
		JSONArray numberOfTemporaryStructuresJson = context.read(BPACalculatorConstants.NUMBER_OF_TEMP_STRUCTURES_PATH);
		if (!CollectionUtils.isEmpty(numberOfTemporaryStructuresJson)) {
			String numberOfTemporaryStructuresString = numberOfTemporaryStructuresJson.get(0).toString();
			Double numberOfTemporaryStructures = Double.parseDouble(numberOfTemporaryStructuresString);
			paramMap.put(BPACalculatorConstants.NUMBER_OF_TEMP_STRUCTURES, numberOfTemporaryStructures);
		}
		
		// alteration builtup area related parameters-
		Double alterationTotalBuiltupArea = null;
		Double alterationExistingBuiltupArea = null;
		JSONArray alterationTotalBuiltupAreaJson = context
				.read(BPACalculatorConstants.ALTERATION_TOTAL_BUILTUP_AREA_PATH);
		if (!CollectionUtils.isEmpty(alterationTotalBuiltupAreaJson)) {
			String alterationTotalBuiltupAreaString = alterationTotalBuiltupAreaJson.get(0).toString();
			alterationTotalBuiltupArea = Double.parseDouble(alterationTotalBuiltupAreaString);
			paramMap.put(BPACalculatorConstants.ALTERATION_TOTAL_BUILTUP_AREA, alterationTotalBuiltupArea);
		}

		JSONArray alterationExistingBuiltupAreaJson = context
				.read(BPACalculatorConstants.ALTERATION_EXISTING_BUILTUP_AREA_PATH);
		if (!CollectionUtils.isEmpty(alterationExistingBuiltupAreaJson)) {
			String alterationExistingBuiltupAreaString = alterationExistingBuiltupAreaJson.get(0).toString();
			alterationExistingBuiltupArea = Double.parseDouble(alterationExistingBuiltupAreaString);
			paramMap.put(BPACalculatorConstants.ALTERATION_EXISTING_BUILTUP_AREA, alterationExistingBuiltupArea);
		}
		// subtract above two and put as proposed builtup area-
		if (Objects.nonNull(alterationTotalBuiltupArea) && Objects.nonNull(alterationExistingBuiltupArea)) {
			Double alterationProposedBuiltupArea = alterationTotalBuiltupArea - alterationExistingBuiltupArea;
			paramMap.put(BPACalculatorConstants.ALTERATION_PROPOSED_BUILTUP_AREA, alterationProposedBuiltupArea);
		}
		// alteration floor area related parameters-
		Double alterationTotalFloorArea = null;
		Double alterationExistingFloorArea = null;
		JSONArray alterationTotalFloorAreaJson = context.read(BPACalculatorConstants.ALTERATION_TOTAL_FLOOR_AREA_PATH);
		if (!CollectionUtils.isEmpty(alterationTotalFloorAreaJson)) {
			String alterationTotalFloorAreaString = alterationTotalFloorAreaJson.get(0).toString();
			alterationTotalFloorArea = Double.parseDouble(alterationTotalFloorAreaString);
			paramMap.put(BPACalculatorConstants.ALTERATION_TOTAL_FLOOR_AREA, alterationTotalFloorArea);
		}

		JSONArray alterationExistingFloorAreaJson = context
				.read(BPACalculatorConstants.ALTERATION_EXISTING_FLOOR_AREA_PATH);
		if (!CollectionUtils.isEmpty(alterationExistingFloorAreaJson)) {
			String alterationExistingFloorAreaString = alterationExistingFloorAreaJson.get(0).toString();
			alterationExistingFloorArea = Double.parseDouble(alterationExistingFloorAreaString);
			paramMap.put(BPACalculatorConstants.ALTERATION_EXISTING_FLOOR_AREA, alterationExistingFloorArea);
		}
		// subtract above two and put as proposed builtup area-
		if (Objects.nonNull(alterationTotalFloorArea) && Objects.nonNull(alterationExistingFloorArea)) {
			Double alterationProposedFloorArea = alterationTotalFloorArea - alterationExistingFloorArea;
			paramMap.put(BPACalculatorConstants.ALTERATION_PROPOSED_FLOOR_AREA, alterationProposedFloorArea);
		}

		paramMap.put(BPACalculatorConstants.APPLICATION_TYPE, applicationType);
		paramMap.put(BPACalculatorConstants.SERVICE_TYPE, serviceType);
		paramMap.put(BPACalculatorConstants.RISK_TYPE, riskType);
		paramMap.put(BPACalculatorConstants.FEE_TYPE, feeType);
		return paramMap;
	}

	/**
	 * @param paramMap
	 * @param estimates
	 * @return
	 */
	private BigDecimal calculateTotalFeeAmount(Map<String, Object> paramMap, ArrayList<TaxHeadEstimate> estimates) {
		BigDecimal calculatedTotalAmout = BigDecimal.ZERO;
		String applicationType = null;
		String serviceType = null;
		String feeType = null;
		//String occupancyType = null;
		List<Occupancy> occupancyType = null;
		
		if (null != paramMap.get(BPACalculatorConstants.APPLICATION_TYPE)) {
			applicationType = (String) paramMap.get(BPACalculatorConstants.APPLICATION_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.SERVICE_TYPE)) {
			serviceType = (String) paramMap.get(BPACalculatorConstants.SERVICE_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.FEE_TYPE)) {
			feeType = (String) paramMap.get(BPACalculatorConstants.FEE_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.OCCUPANCYLIST)) {
			occupancyType = (List<Occupancy>) paramMap.get(BPACalculatorConstants.OCCUPANCYLIST);
		}
		
		if (StringUtils.hasText(applicationType) && (StringUtils.hasText(serviceType))
				&& (occupancyType!=null) && (StringUtils.hasText(feeType))) {
			if (feeType.equalsIgnoreCase(BPACalculatorConstants.MDMS_CALCULATIONTYPE_APL_FEETYPE)) {
				calculatedTotalAmout = calculateTotalScrutinyFee(paramMap, estimates);

			} else if (feeType.equalsIgnoreCase(BPACalculatorConstants.MDMS_CALCULATIONTYPE_SANC_FEETYPE)) {
				calculatedTotalAmout = calculateTotalPermitFee(paramMap, estimates);
			}

		}

		return calculatedTotalAmout;
	}
	
	/**
	 * Calculate CWWC fee for BPA OC
	 * @param paramMap
	 * @param estimates
	 * @return
	 */
	private BigDecimal calculateOccupancyConstructionWorkerWelfareCess(Map<String, Object> paramMap, ArrayList<TaxHeadEstimate> estimates) {
		BigDecimal welfareCess = BigDecimal.ZERO;
		String applicationType = null;
		String serviceType = null;
		Double deviationBuitUpArea = null;
		Double totalBuaEdcr = null;
		if (null != paramMap.get(BPACalculatorConstants.APPLICATION_TYPE)) {
			applicationType = (String) paramMap.get(BPACalculatorConstants.APPLICATION_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.SERVICE_TYPE)) {
			serviceType = (String) paramMap.get(BPACalculatorConstants.SERVICE_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.DEVIATION_BUILTUP_AREA)) {
			deviationBuitUpArea = (Double) paramMap.get(BPACalculatorConstants.DEVIATION_BUILTUP_AREA);
		}
		if (null != paramMap.get(BPACalculatorConstants.TOTAL_BUILTUP_AREA_EDCR)) {
			totalBuaEdcr = (Double) paramMap.get(BPACalculatorConstants.TOTAL_BUILTUP_AREA_EDCR);
		}
		if ((StringUtils.hasText(applicationType)
				&& applicationType.equalsIgnoreCase(BPACalculatorConstants.BUILDING_PLAN_OC))
				&& (StringUtils.hasText(serviceType)
				&& serviceType.equalsIgnoreCase(BPACalculatorConstants.NEW_CONSTRUCTION))
				&& deviationBuitUpArea.compareTo(0D) > 0) {
			
			BigDecimal totalCostOfConstruction = (SEVENTEEN_FIFTY.multiply(BigDecimal.valueOf(totalBuaEdcr))
					.multiply(SQMT_SQFT_MULTIPLIER)).setScale(2, RoundingMode.UP);
			
			if (totalCostOfConstruction.compareTo(TEN_LAC) > 0) {
				welfareCess = (EIGHTEEN_POINT_TWO_ONE.multiply(BigDecimal.valueOf(deviationBuitUpArea))
						.multiply(SQMT_SQFT_MULTIPLIER)).setScale(2, RoundingMode.UP);
			}

		}
		if(BigDecimal.ZERO.compareTo(welfareCess) < 0)
			generateTaxHeadEstimate(estimates, welfareCess, BPACalculatorConstants.TAXHEAD_BPA_OC_SANC_CWWC_FEE, Category.FEE);
		return welfareCess;
	}

	/**
	 * Calculate OC EIDP Fee
	 * @param paramMap
	 * @param estimates
	 * @return
	 */
	private BigDecimal calculateOccupancyEidpFee(Map<String, Object> paramMap, ArrayList<TaxHeadEstimate> estimates) {
		BigDecimal eidpFee = BigDecimal.ZERO;
		String applicationType = null;
		String serviceType = null;
		BigDecimal projectCost = null;
		Double edcrTotalBUA = null;
		Double deviationBUA = null;
		if (null != paramMap.get(BPACalculatorConstants.APPLICATION_TYPE)) {
			applicationType = (String) paramMap.get(BPACalculatorConstants.APPLICATION_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.SERVICE_TYPE)) {
			serviceType = (String) paramMap.get(BPACalculatorConstants.SERVICE_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.PROJECT_VALUE_FOR_EIDP)) {			
			projectCost = paramMap.get(BPACalculatorConstants.PROJECT_VALUE_FOR_EIDP) != null
					? new BigDecimal(paramMap.get(BPACalculatorConstants.PROJECT_VALUE_FOR_EIDP) + "")
					: BigDecimal.ZERO;
		}
		if (null != paramMap.get(BPACalculatorConstants.TOTAL_FLOOR_AREA_EDCR)) {
			edcrTotalBUA = (Double) paramMap.get(BPACalculatorConstants.TOTAL_BUILTUP_AREA_EDCR);
		}
		if (null != paramMap.get(BPACalculatorConstants.DEVIATION_BUILTUP_AREA)) {
			deviationBUA = (Double) paramMap.get(BPACalculatorConstants.DEVIATION_BUILTUP_AREA);
		}
		if ((StringUtils.hasText(applicationType)
				&& applicationType.equalsIgnoreCase(BPACalculatorConstants.BUILDING_PLAN_OC))
				&& (StringUtils.hasText(serviceType)
				&& serviceType.equalsIgnoreCase(BPACalculatorConstants.NEW_CONSTRUCTION))
				&& projectCost != null && deviationBUA.compareTo(0D) > 0) {
		
			BigDecimal deviationPercentage = BigDecimal.valueOf(deviationBUA)
					.divide(BigDecimal.valueOf(edcrTotalBUA), 4);
			
			eidpFee = projectCost.multiply(deviationPercentage).divide(HUNDRED,2, RoundingMode.UP);
		}
		if(BigDecimal.ZERO.compareTo(eidpFee) < 0)
			generateTaxHeadEstimate(estimates, eidpFee, BPACalculatorConstants.TAXHEAD_BPA_OC_SANC_EIDP_FEE, Category.FEE);
		return eidpFee;
	}

	/**
	 * Calculate FAR Fee for BPA OC
	 * @param paramMap
	 * @param estimates
	 * @param mdmsData
	 * @return
	 */
	private BigDecimal calculateOccupancyCompoundingFeeForFAR(Map<String, Object> paramMap, ArrayList<TaxHeadEstimate> estimates, Object mdmsData) {
		BigDecimal compoundFARFee = BigDecimal.ZERO;
		String applicationType = null;
		String serviceType = null;
		BigDecimal baseFAR = null;
		BigDecimal providedFAR = null;
		BigDecimal permissableFAR = null;
		BigDecimal plotArea = null;
		BigDecimal deviation = null;
		String subOccupancyType = null;
		if (null != paramMap.get(BPACalculatorConstants.APPLICATION_TYPE)) {
			applicationType = (String) paramMap.get(BPACalculatorConstants.APPLICATION_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.SERVICE_TYPE)) {
			serviceType = (String) paramMap.get(BPACalculatorConstants.SERVICE_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.BASE_FAR)) {
			baseFAR = BigDecimal.valueOf((Double) paramMap.get(BPACalculatorConstants.BASE_FAR));
		}
		if (null != paramMap.get(BPACalculatorConstants.PROVIDED_FAR)) {
			providedFAR = BigDecimal.valueOf((Double) paramMap.get(BPACalculatorConstants.PROVIDED_FAR));
		}
		if (null != paramMap.get(BPACalculatorConstants.PERMISSABLE_FAR)) {
			permissableFAR = BigDecimal.valueOf((Double) paramMap.get(BPACalculatorConstants.PERMISSABLE_FAR));
		}
		if (null != paramMap.get(BPACalculatorConstants.PLOT_AREA)) {
			plotArea = BigDecimal.valueOf((Double) paramMap.get(BPACalculatorConstants.PLOT_AREA));
		}
		if (null != paramMap.get(BPACalculatorConstants.DEVIATION_BUILTUP_AREA)) {
			deviation = BigDecimal.valueOf((Double) paramMap.get(BPACalculatorConstants.DEVIATION_BUILTUP_AREA));
		}
		if (null != paramMap.get(BPACalculatorConstants.SUB_OCCUPANCY_TYPE)) {
			subOccupancyType = (String) paramMap.get(BPACalculatorConstants.SUB_OCCUPANCY_TYPE);
		}
		if ((StringUtils.hasText(applicationType)
				&& applicationType.equalsIgnoreCase(BPACalculatorConstants.BUILDING_PLAN_OC))
				&& (StringUtils.hasText(serviceType)
				&& serviceType.equalsIgnoreCase(BPACalculatorConstants.NEW_CONSTRUCTION))
				&& deviation.compareTo(BigDecimal.ZERO) > 0) {
			
			BigDecimal baseFarBUA = plotArea.multiply(baseFAR);
			BigDecimal permissableFarBUA = plotArea.multiply(permissableFAR);
			BigDecimal builtUpAreaBP = BigDecimal.valueOf((Double) paramMap.get(BPACalculatorConstants.TOTAL_BUILTUP_AREA_EDCR));
			BigDecimal builtUpAreaOC = BigDecimal.valueOf((Double) paramMap.get(BPACalculatorConstants.TOTAL_BUILTUP_AREA));
			
			if(baseFarBUA.compareTo(builtUpAreaOC) >= 0) {
				compoundFARFee = calculateOcCompoundingFar(paramMap, mdmsData, deviation);
			} else if(baseFarBUA.compareTo(builtUpAreaOC) < 0 && permissableFarBUA.compareTo(builtUpAreaOC) >= 0) {
				BigDecimal fee1 = calculateOcCompoundingFar(paramMap, mdmsData, baseFarBUA.subtract(builtUpAreaBP));
				BigDecimal fee2 = BigDecimal.ZERO;
				// calculation of fee2(Purchasable far) for MIG sub-occupancy-
				if (StringUtils.hasText(subOccupancyType)
						&& BPACalculatorConstants.A_MIH.equalsIgnoreCase(subOccupancyType)) {
					BigDecimal applicableDiscountFarArea = (permissableFarBUA.subtract(baseFarBUA)
							.multiply(new BigDecimal("0.25"))).setScale(2, BigDecimal.ROUND_UP);
					BigDecimal deltaFarBUA = builtUpAreaOC.subtract(baseFarBUA);
					if (deltaFarBUA.compareTo(applicableDiscountFarArea) > 0) {
						fee2 = calculateOcPurchableFAR(paramMap,
								builtUpAreaOC.subtract(baseFarBUA).subtract(applicableDiscountFarArea));
					}
				} else {
					fee2 = calculateOcPurchableFAR(paramMap, builtUpAreaOC.subtract(baseFarBUA));
				}
				compoundFARFee = fee1.add(fee2);
			} else if(builtUpAreaOC.compareTo(permissableFarBUA) > 0) {
				compoundFARFee = calculateOcPurchableFAR(paramMap, deviation);
			}
			
		}
		generateTaxHeadEstimate(estimates, compoundFARFee, BPACalculatorConstants.TAXHEAD_BPA_OC_SANC_COMPOUND_FAR_FEE, Category.FEE);
		return compoundFARFee;
	}

	/**
	 * Calculate Compounding FAR fee for BPA OC
	 * @param paramMap
	 * @param mdmsData
	 * @param applicableBUA
	 * @return
	 */
	private BigDecimal calculateOcCompoundingFar(Map<String, Object> paramMap, Object mdmsData, BigDecimal applicableBUA) {
		BigDecimal compoundingFarFee = BigDecimal.ZERO;
		Map mdmsCompoundingFee = mdmsService.getOcCompoundingFee(mdmsData, BPACalculatorConstants.MDMS_FAR);
		
		if(applicableBUA != null && mdmsCompoundingFee != null) {
			Double rate = getRateForFAR(mdmsCompoundingFee, paramMap);
			compoundingFarFee = applicableBUA.multiply(BigDecimal.valueOf(rate));
		}
		return compoundingFarFee;
	}

	private Double getRateForFAR(Map mdmsCompoundingFee, Map<String, Object> paramMap) {
		Double rate = 0D;
		boolean isProjectUndertakingByGovt = false;
		String occupancyType = null;
		String subOccupancyType = null;
		String applicableRateType = null;
		if (null != paramMap.get(BPACalculatorConstants.PROJECT_UNDERTAKING_BY_GOVT)) {
			isProjectUndertakingByGovt = (boolean) paramMap.get(BPACalculatorConstants.PROJECT_UNDERTAKING_BY_GOVT);
		}
		if (null != paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE)) {
			occupancyType = (String) paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.SUB_OCCUPANCY_TYPE)) {
			subOccupancyType = (String) paramMap.get(BPACalculatorConstants.SUB_OCCUPANCY_TYPE);
		}
		
		if(isProjectUndertakingByGovt) {
			applicableRateType = BPACalculatorConstants.OC_COMPOUNDING_GOVT;
		} else if(occupancyType != null && subOccupancyType != null) {
			if(BPACalculatorConstants.A.equals(occupancyType)
					&& (BPACalculatorConstants.A_P.equals(subOccupancyType)
					|| BPACalculatorConstants.A_S.equals(subOccupancyType)
					|| BPACalculatorConstants.A_R.equals(subOccupancyType))) {
				applicableRateType = BPACalculatorConstants.OC_COMPOUNDING_INDIVIDUAL_RESIDENTIAL;
			} else {
				applicableRateType = BPACalculatorConstants.OC_COMPOUNDING_OTHER;
			}
		}
		
		
		String jsonData = new JSONObject(mdmsCompoundingFee).toString();
		DocumentContext compoundingContext = JsonPath.using(Configuration.defaultConfiguration()).parse(jsonData);
		JSONArray criteriaArray = compoundingContext.read("criteria");
		LinkedHashMap item = (LinkedHashMap) criteriaArray.get(0);
		JSONArray feeArray = (JSONArray) item.get("fee");
		for (int i = 0; i < feeArray.size(); i++) {
			LinkedHashMap diff = (LinkedHashMap) feeArray.get(i);
			String applicable = diff.get("applicable").toString();
			if(applicable.equalsIgnoreCase(applicableRateType)) {
				rate = Double.parseDouble(diff.get("rate").toString());
				break;
			}
		}
		return rate;
	}

	/**
	 * CAlculate OC purchable FAR fee
	 * @param paramMap
	 * @param applicableBUA
	 * @return
	 */
	private BigDecimal calculateOcPurchableFAR(Map<String, Object> paramMap, BigDecimal applicableBUA) {
		BigDecimal purchasableFARFee = BigDecimal.ZERO;
		Double benchmarkValuePerAcre = null;
		Double baseFar = null;
		Double permissableFar = null;
		Double deviationBUA = null;
		if (null != paramMap.get(BPACalculatorConstants.BMV_ACRE)) {
			benchmarkValuePerAcre = (Double) paramMap.get(BPACalculatorConstants.BMV_ACRE);
		}

		if(benchmarkValuePerAcre != null) {
			BigDecimal benchmarkValuePerSQM = BigDecimal.valueOf(benchmarkValuePerAcre).divide(ACRE_SQMT_MULTIPLIER,
					2, RoundingMode.UP);
	
			BigDecimal purchasableFARRate = (benchmarkValuePerSQM.multiply(ZERO_TWO_FIVE)).setScale(2,
					RoundingMode.UP);
	
			purchasableFARFee = (purchasableFARRate.multiply(applicableBUA))
					.setScale(2, RoundingMode.UP);
		}
		log.info("OC PurchasableFARFee:::::::::::::::::" + purchasableFARFee);
		return purchasableFARFee;
		
	}

	/**
	 * Calculate BPA OC Fee for Setback
	 * @param paramMap
	 * @param estimates
	 * @param mdmsData
	 * @return
	 */
	private BigDecimal calculateOccupancyCompoundingFeeForSetback(Map<String, Object> paramMap,
			ArrayList<TaxHeadEstimate> estimates, Object mdmsData) {
		BigDecimal compoundSetbackFee = BigDecimal.ZERO;
		String applicationType = null;
		String serviceType = null;
		JSONArray blockDetailOc = null;
		JSONArray blockDetailEdcr = null;
		
		if (null != paramMap.get(BPACalculatorConstants.APPLICATION_TYPE)) {
			applicationType = (String) paramMap.get(BPACalculatorConstants.APPLICATION_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.SERVICE_TYPE)) {
			serviceType = (String) paramMap.get(BPACalculatorConstants.SERVICE_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.BLOCK_DETAILS_OC)) {
			blockDetailOc = (JSONArray) paramMap.get(BPACalculatorConstants.BLOCK_DETAILS_OC);
		}
		if (null != paramMap.get(BPACalculatorConstants.BLOCK_DETAILS_EDCR)) {
			blockDetailEdcr = (JSONArray) paramMap.get(BPACalculatorConstants.BLOCK_DETAILS_EDCR);
		}
		
		Map mdmsCompoundingFee = mdmsService.getOcCompoundingFee(mdmsData, BPACalculatorConstants.MDMS_SETBACK);
		
		if ((StringUtils.hasText(applicationType)
				&& applicationType.equalsIgnoreCase(BPACalculatorConstants.BUILDING_PLAN_OC))
				&& (StringUtils.hasText(serviceType)
				&& serviceType.equalsIgnoreCase(BPACalculatorConstants.NEW_CONSTRUCTION))
				&& blockDetailOc != null && blockDetailEdcr != null) {
			
			compoundSetbackFee = calculateOccupancyCompoundingSetbackFee(mdmsCompoundingFee, blockDetailOc, blockDetailEdcr, paramMap);
			
		}
		generateTaxHeadEstimate(estimates, compoundSetbackFee, BPACalculatorConstants.TAXHEAD_BPA_OC_SANC_COMPOUND_SETBACK_FEE, Category.FEE);
		return compoundSetbackFee;
	}

	/**
	 * Calculate compounding fee for all setback in BPA OC
	 * @param mdmsCompoundingFee
	 * @param blockDetailOc
	 * @param blockDetailEdcr
	 * @param paramMap
	 * @return
	 */
	private BigDecimal calculateOccupancyCompoundingSetbackFee(Map mdmsCompoundingFee, JSONArray blockDetailOc,
			JSONArray blockDetailEdcr, Map<String, Object> paramMap) {
		BigDecimal totalSetbackFee = BigDecimal.ZERO;
		BigDecimal frontYardFee = BigDecimal.ZERO;
		BigDecimal rearYardFee = BigDecimal.ZERO;
		BigDecimal sideYard1Fee = BigDecimal.ZERO;
		BigDecimal sideYard2Fee = BigDecimal.ZERO;
		
		for(int i=0; i<blockDetailOc.size(); i++) {
			LinkedHashMap blockOc = (LinkedHashMap) blockDetailOc.get(i);
			LinkedHashMap levelZeroSetBackOc = (LinkedHashMap) blockOc.get("levelZeroSetBack");
			
			LinkedHashMap blockEdcr = (LinkedHashMap) blockDetailEdcr.get(i);
			LinkedHashMap levelZeroSetBackEdcr = (LinkedHashMap) blockEdcr.get("levelZeroSetBack");
			
			frontYardFee = calculateSetbackFee(BPACalculatorConstants.JSON_FRONT_YARD, levelZeroSetBackOc,
					levelZeroSetBackEdcr, mdmsCompoundingFee, paramMap);
			
			rearYardFee = calculateSetbackFee(BPACalculatorConstants.JSON_REAR_YARD, levelZeroSetBackOc,
					levelZeroSetBackEdcr, mdmsCompoundingFee, paramMap);
			
			sideYard1Fee = calculateSetbackFee(BPACalculatorConstants.JSON_SIDE_YARD1, levelZeroSetBackOc,
					levelZeroSetBackEdcr, mdmsCompoundingFee, paramMap);
			
			sideYard2Fee = calculateSetbackFee(BPACalculatorConstants.JSON_SIDE_YARD2, levelZeroSetBackOc,
					levelZeroSetBackEdcr, mdmsCompoundingFee, paramMap);
			
			totalSetbackFee = totalSetbackFee.add(frontYardFee).add(rearYardFee).add(sideYard1Fee).add(sideYard2Fee);
		}
		
		return totalSetbackFee;
	}

	/**
	 * Calculate setback fee in BPA OC
	 * @param setbackSide
	 * @param levelZeroSetBackOc
	 * @param levelZeroSetBackEdcr
	 * @param mdmsCompoundingFee
	 * @param paramMap
	 * @return
	 */
	private BigDecimal calculateSetbackFee(String setbackSide, LinkedHashMap levelZeroSetBackOc,
			LinkedHashMap levelZeroSetBackEdcr, Map mdmsCompoundingFee, Map<String, Object> paramMap) {
		BigDecimal setBackFee = BigDecimal.ZERO;
		BigDecimal meanDeviationPercentage = BigDecimal.ZERO;
		BigDecimal meanOc = BigDecimal.ZERO;
		BigDecimal meanEdcr = BigDecimal.ZERO;
		BigDecimal areaOc = BigDecimal.ZERO;
		BigDecimal areaEdcr = BigDecimal.ZERO;
		
		if(levelZeroSetBackOc != null && levelZeroSetBackEdcr != null) {
			LinkedHashMap setbackItemOc = (LinkedHashMap) levelZeroSetBackOc.get(setbackSide);
			LinkedHashMap setbackItemEdcr = (LinkedHashMap) levelZeroSetBackEdcr.get(setbackSide);
			if(setbackItemOc != null && setbackItemEdcr != null) {
				meanOc = setbackItemOc.get("mean") != null ?new BigDecimal(setbackItemOc.get("mean")+""):BigDecimal.ZERO;
				meanEdcr = setbackItemOc.get("mean") != null ?new BigDecimal(setbackItemEdcr.get("mean")+""):BigDecimal.ZERO;
				meanDeviationPercentage = meanEdcr.subtract(meanOc).multiply(HUNDRED).divide(meanEdcr, 2, RoundingMode.UP);
				Double rate = getRateForSetback(paramMap, mdmsCompoundingFee, meanDeviationPercentage);
				
				areaOc = BigDecimal.valueOf((Double) setbackItemOc.get("area"));
				areaEdcr = BigDecimal.valueOf((Double) setbackItemEdcr.get("area"));
				if(areaEdcr.compareTo(areaOc) > 0) {
					setBackFee = areaEdcr.subtract(areaOc).multiply(BigDecimal.valueOf(rate)).setScale(2, RoundingMode.UP);
				}
			}
		}
		
		return setBackFee;
	}

	/**
	 * Get setback Rate 
	 * @param paramMap
	 * @param mdmsCompoundingFee
	 * @param deviationPercentage
	 * @return
	 */
	private Double getRateForSetback(Map<String, Object> paramMap, Map mdmsCompoundingFee, BigDecimal deviationPercentage) {
		Double rate = 0D;
		boolean isProjectUndertakingByGovt = false;
		String occupancyType = null;
		String subOccupancyType = null;
		String applicableRateType = null;
		if (null != paramMap.get(BPACalculatorConstants.PROJECT_UNDERTAKING_BY_GOVT)) {
			isProjectUndertakingByGovt = (boolean) paramMap.get(BPACalculatorConstants.PROJECT_UNDERTAKING_BY_GOVT);
		}
		if (null != paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE)) {
			occupancyType = (String) paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.SUB_OCCUPANCY_TYPE)) {
			subOccupancyType = (String) paramMap.get(BPACalculatorConstants.SUB_OCCUPANCY_TYPE);
		}
		
		if(isProjectUndertakingByGovt) {
			applicableRateType = BPACalculatorConstants.OC_COMPOUNDING_GOVT;
		} else if(occupancyType != null && subOccupancyType != null) {
			if(BPACalculatorConstants.A.equals(occupancyType)
					&& (BPACalculatorConstants.A_P.equals(subOccupancyType)
					|| BPACalculatorConstants.A_S.equals(subOccupancyType)
					|| BPACalculatorConstants.A_R.equals(subOccupancyType))) {
				applicableRateType = BPACalculatorConstants.OC_COMPOUNDING_INDIVIDUAL_RESIDENTIAL;
			} else {
				applicableRateType = BPACalculatorConstants.OC_COMPOUNDING_OTHER;
			}
		}
		String jsonData = new JSONObject(mdmsCompoundingFee).toString();
		DocumentContext compoundingContext = JsonPath.using(Configuration.defaultConfiguration()).parse(jsonData);
		JSONArray criteriaArray = compoundingContext.read("criteria");
		for (int j = 0; j < criteriaArray.size(); j++) {
			LinkedHashMap item = (LinkedHashMap) criteriaArray.get(j);
			LinkedHashMap deviationData = (LinkedHashMap) item.get("deviation");
			Double from = Double.parseDouble(deviationData.get("from").toString());
			Double to = Double.parseDouble(deviationData.get("to").toString());
			if(BigDecimal.valueOf(from).compareTo(deviationPercentage) < 0 
					&& BigDecimal.valueOf(to).compareTo(deviationPercentage) >= 0) {
				JSONArray feeArray = (JSONArray) item.get("fee");
				for (int i = 0; i < feeArray.size(); i++) {
					LinkedHashMap diff = (LinkedHashMap) feeArray.get(i);
					String applicableType = diff.get("applicable").toString();
					if(applicableType.equalsIgnoreCase(applicableRateType)) {
						rate = Double.parseDouble(diff.get("rate").toString());
						break;
					}
				}
			}
		}
		return rate;
	}

	/**
	 * Calculate BPA OC Scrutiny fee
	 * @param paramMap
	 * @param estimates
	 * @return
	 */
	private BigDecimal calculateOccupancyScrutinyFee(Map<String, Object> paramMap, ArrayList<TaxHeadEstimate> estimates) {
		BigDecimal ocScrutinyFee = BigDecimal.ZERO;
		Double deviationBUA = null;
		String occupancyType = null;
		deviationBUA=getDeviationBUAForOCFeesCalculation(paramMap);
		if (null != paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE)) {
			occupancyType = (String) paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE);
		}
		if (deviationBUA != null && deviationBUA.compareTo(0D) > 0) {
			if ((occupancyType.equalsIgnoreCase(BPACalculatorConstants.A))) {
				ocScrutinyFee = calculateOccupancyScrutinyFeeForResidentialOccupancy(paramMap);
			}
			if ((occupancyType.equalsIgnoreCase(BPACalculatorConstants.B))) {
				ocScrutinyFee = calculateOccupancyScrutinyFeeForCommercialOccupancy(paramMap);
			}
			if ((occupancyType.equalsIgnoreCase(BPACalculatorConstants.C))) {
				ocScrutinyFee = calculateOccupancyScrutinyFeeForPublicSemiPublicInstitutionalOccupancy(
						paramMap);
			}
			if ((occupancyType.equalsIgnoreCase(BPACalculatorConstants.D))) {
				ocScrutinyFee = calculateOccupancyScrutinyFeeForPublicUtilityOccupancy(paramMap);
			}
			if ((occupancyType.equalsIgnoreCase(BPACalculatorConstants.E))) {
				ocScrutinyFee = calculateOccupancyScrutinyFeeForIndustrialZoneOccupancy(paramMap);
			}
			if ((occupancyType.equalsIgnoreCase(BPACalculatorConstants.F))) {
				ocScrutinyFee = calculateOccupancyScrutinyFeeForEducationOccupancy(paramMap);
			}
			if ((occupancyType.equalsIgnoreCase(BPACalculatorConstants.G))) {
				ocScrutinyFee = calculateOccupancyScrutinyFeeForTransportationOccupancy(paramMap);
			}
			if ((occupancyType.equalsIgnoreCase(BPACalculatorConstants.H))) {
				ocScrutinyFee = calculateOccupancyScrutinyFeeForAgricultureOccupancy(paramMap);
			}

		}
		generateTaxHeadEstimate(estimates, ocScrutinyFee, BPACalculatorConstants.TAXHEAD_BPA_OC_SCRUTINY_FEE, Category.FEE);
		
		return ocScrutinyFee;
	}

	/**
	 * OC Scrutiny Fee for Agriculture
	 * @param paramMap
	 * @return
	 */
	private BigDecimal calculateOccupancyScrutinyFeeForAgricultureOccupancy(Map<String, Object> paramMap) {
		BigDecimal ocScrutinyFee = BigDecimal.ZERO;
		String applicationType = null;
		String serviceType = null;
		String occupancyType = null;
		Double deviationBUA = null;
		if (null != paramMap.get(BPACalculatorConstants.APPLICATION_TYPE)) {
			applicationType = (String) paramMap.get(BPACalculatorConstants.APPLICATION_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.SERVICE_TYPE)) {
			serviceType = (String) paramMap.get(BPACalculatorConstants.SERVICE_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE)) {
			occupancyType = (String) paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE);
		}
		deviationBUA = getDeviationBUAForOCFeesCalculation(paramMap);
		if ((occupancyType.equalsIgnoreCase(BPACalculatorConstants.H))) {
			if ((StringUtils.hasText(applicationType)
					&& applicationType.equalsIgnoreCase(BPACalculatorConstants.BUILDING_PLAN_OC))
					&& (StringUtils.hasText(serviceType)
							&& serviceType.equalsIgnoreCase(BPACalculatorConstants.NEW_CONSTRUCTION))) {
				ocScrutinyFee = calculateVariableFee1(deviationBUA);
			}
		}
		return ocScrutinyFee;
	}

	/**
	 * OC Scrutiny Fee for Transportation
	 * @param paramMap
	 * @return
	 */
	private BigDecimal calculateOccupancyScrutinyFeeForTransportationOccupancy(Map<String, Object> paramMap) {
		BigDecimal ocScrutinyFee = BigDecimal.ZERO;
		String applicationType = null;
		String serviceType = null;
		String occupancyType = null;
		Double deviationBUA = null;
		if (null != paramMap.get(BPACalculatorConstants.APPLICATION_TYPE)) {
			applicationType = (String) paramMap.get(BPACalculatorConstants.APPLICATION_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.SERVICE_TYPE)) {
			serviceType = (String) paramMap.get(BPACalculatorConstants.SERVICE_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE)) {
			occupancyType = (String) paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE);
		}
		deviationBUA = getDeviationBUAForOCFeesCalculation(paramMap);
		if ((occupancyType.equalsIgnoreCase(BPACalculatorConstants.G))) {
			if ((StringUtils.hasText(applicationType)
					&& applicationType.equalsIgnoreCase(BPACalculatorConstants.BUILDING_PLAN_OC))
					&& (StringUtils.hasText(serviceType)
							&& serviceType.equalsIgnoreCase(BPACalculatorConstants.NEW_CONSTRUCTION))) {
				// feeForBuildingOperation = calculateConstantFee(paramMap, 5);
				ocScrutinyFee = calculateConstantFeeNew(deviationBUA, 5);
			}
		}
		return ocScrutinyFee;
	}

	/**
	 * OC Scrutiny Fee for Education
	 * @param paramMap
	 * @return
	 */
	private BigDecimal calculateOccupancyScrutinyFeeForEducationOccupancy(Map<String, Object> paramMap) {
		BigDecimal ocScrutinyFee = BigDecimal.ZERO;
		String applicationType = null;
		String serviceType = null;
		String occupancyType = null;
		Double deviationBUA = null;
		if (null != paramMap.get(BPACalculatorConstants.APPLICATION_TYPE)) {
			applicationType = (String) paramMap.get(BPACalculatorConstants.APPLICATION_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.SERVICE_TYPE)) {
			serviceType = (String) paramMap.get(BPACalculatorConstants.SERVICE_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE)) {
			occupancyType = (String) paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE);
		}
		deviationBUA = getDeviationBUAForOCFeesCalculation(paramMap);
		if ((occupancyType.equalsIgnoreCase(BPACalculatorConstants.F))) {
			if ((StringUtils.hasText(applicationType)
					&& applicationType.equalsIgnoreCase(BPACalculatorConstants.BUILDING_PLAN_OC))
					&& (StringUtils.hasText(serviceType)
							&& serviceType.equalsIgnoreCase(BPACalculatorConstants.NEW_CONSTRUCTION))) {
				// feeForBuildingOperation = calculateConstantFee(paramMap, 5);
				ocScrutinyFee = calculateConstantFeeNew(deviationBUA, 5);
			}
		}
		return ocScrutinyFee;
	}

	/**
	 * OC Scrutiny Fee for Industrial Zone
	 * @param paramMap
	 * @return
	 */
	private BigDecimal calculateOccupancyScrutinyFeeForIndustrialZoneOccupancy(Map<String, Object> paramMap) {
		BigDecimal ocScrutinyFee = BigDecimal.ZERO;
		String applicationType = null;
		String serviceType = null;
		String occupancyType = null;
		Double deviationBUA = null;
		if (null != paramMap.get(BPACalculatorConstants.APPLICATION_TYPE)) {
			applicationType = (String) paramMap.get(BPACalculatorConstants.APPLICATION_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.SERVICE_TYPE)) {
			serviceType = (String) paramMap.get(BPACalculatorConstants.SERVICE_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE)) {
			occupancyType = (String) paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE);
		}
		deviationBUA = getDeviationBUAForOCFeesCalculation(paramMap);
		if ((occupancyType.equalsIgnoreCase(BPACalculatorConstants.E))) {
			if ((StringUtils.hasText(applicationType)
					&& applicationType.equalsIgnoreCase(BPACalculatorConstants.BUILDING_PLAN_OC))
					&& (StringUtils.hasText(serviceType)
							&& serviceType.equalsIgnoreCase(BPACalculatorConstants.NEW_CONSTRUCTION))) {
				ocScrutinyFee = calculateVariableFee3(deviationBUA);
			}
		}
		return ocScrutinyFee;
	}

	/**
	 * OC Scrutiny Fee for public utility
	 * @param paramMap
	 * @return
	 */
	private BigDecimal calculateOccupancyScrutinyFeeForPublicUtilityOccupancy(Map<String, Object> paramMap) {
		BigDecimal ocScrutinyFee = BigDecimal.ZERO;
		String applicationType = null;
		String serviceType = null;
		String occupancyType = null;
		Double deviationBUA = null;
		if (null != paramMap.get(BPACalculatorConstants.APPLICATION_TYPE)) {
			applicationType = (String) paramMap.get(BPACalculatorConstants.APPLICATION_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.SERVICE_TYPE)) {
			serviceType = (String) paramMap.get(BPACalculatorConstants.SERVICE_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE)) {
			occupancyType = (String) paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE);
		}
		deviationBUA = getDeviationBUAForOCFeesCalculation(paramMap);
		if ((occupancyType.equalsIgnoreCase(BPACalculatorConstants.D))) {
			if ((StringUtils.hasText(applicationType)
					&& applicationType.equalsIgnoreCase(BPACalculatorConstants.BUILDING_PLAN_OC))
					&& (StringUtils.hasText(serviceType)
							&& serviceType.equalsIgnoreCase(BPACalculatorConstants.NEW_CONSTRUCTION))) {
				
				ocScrutinyFee = calculateConstantFeeNew(deviationBUA, 5);
			}
		}
		return ocScrutinyFee;
	}

	/**
	 * OC Scrutiny Fee for public semi
	 * @param paramMap
	 * @return
	 */
	private BigDecimal calculateOccupancyScrutinyFeeForPublicSemiPublicInstitutionalOccupancy(
			Map<String, Object> paramMap) {
		BigDecimal ocScrutinyFee = BigDecimal.ZERO;
		String applicationType = null;
		String serviceType = null;
		String occupancyType = null;
		String subOccupancyType = null;
		Double deviationBUA = null;
		if (null != paramMap.get(BPACalculatorConstants.APPLICATION_TYPE)) {
			applicationType = (String) paramMap.get(BPACalculatorConstants.APPLICATION_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.SERVICE_TYPE)) {
			serviceType = (String) paramMap.get(BPACalculatorConstants.SERVICE_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE)) {
			occupancyType = (String) paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.SUB_OCCUPANCY_TYPE)) {
			subOccupancyType = (String) paramMap.get(BPACalculatorConstants.SUB_OCCUPANCY_TYPE);
		}
		deviationBUA = getDeviationBUAForOCFeesCalculation(paramMap);
		if (((occupancyType.equalsIgnoreCase(BPACalculatorConstants.C))) && (StringUtils.hasText(subOccupancyType))) {
			if ((StringUtils.hasText(applicationType)
					&& applicationType.equalsIgnoreCase(BPACalculatorConstants.BUILDING_PLAN_OC))
					&& (StringUtils.hasText(serviceType)
							&& serviceType.equalsIgnoreCase(BPACalculatorConstants.NEW_CONSTRUCTION))) {
				if ((subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_A))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_B))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_C))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_CL))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_MP))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_CH))) {
					ocScrutinyFee = calculateVariableFee2(deviationBUA);

				} else if ((subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_O))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_OAH))) {
					// feeForBuildingOperation = calculateConstantFee(paramMap, 5);
					ocScrutinyFee = calculateConstantFeeNew(deviationBUA, 5);

				} else if ((subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_SC))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_C1H))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_C2H))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_SCC))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_CC))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_EC))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_G))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_MH))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_ML))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_M))) {
					ocScrutinyFee = calculateVariableFee2(deviationBUA);

				} else if ((subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_PW))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_PL))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_REB))) {
					// feeForBuildingOperation = calculateConstantFee(paramMap, 5);
					ocScrutinyFee = calculateConstantFeeNew(deviationBUA, 5);

				} else if ((subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_SPC))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_S))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_T))) {
					ocScrutinyFee = calculateVariableFee2(deviationBUA);

				} else if ((subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_AB))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_GO))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_LSGO))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_P))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_RB))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_SWC))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_CI))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_D))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_YC))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_DC))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_GSGH))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_RT))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_HC))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_H))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_L))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_MTH))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_MB))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_NH))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_PLY))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_RC))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_VHAB))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_RTI))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_PS))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_FS))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_J))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_PO))) {

					// feeForBuildingOperation = calculateConstantFee(paramMap, 5);
					ocScrutinyFee = calculateConstantFeeNew(deviationBUA, 5);

				}
			}

		}
		return ocScrutinyFee;
	}

	/**
	 * OC Scrutiny Fee for Commercial
	 * @param paramMap
	 * @return
	 */
	private BigDecimal calculateOccupancyScrutinyFeeForCommercialOccupancy(Map<String, Object> paramMap) {
		BigDecimal ocScrutinyFee = BigDecimal.ZERO;
		String applicationType = null;
		String serviceType = null;
		String occupancyType = null;
		Double deviationBUA = null;
		if (null != paramMap.get(BPACalculatorConstants.APPLICATION_TYPE)) {
			applicationType = (String) paramMap.get(BPACalculatorConstants.APPLICATION_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.SERVICE_TYPE)) {
			serviceType = (String) paramMap.get(BPACalculatorConstants.SERVICE_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE)) {
			occupancyType = (String) paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE);
		}
		deviationBUA = getDeviationBUAForOCFeesCalculation(paramMap);
		if ((occupancyType.equalsIgnoreCase(BPACalculatorConstants.B))) {
			if ((StringUtils.hasText(applicationType)
					&& applicationType.equalsIgnoreCase(BPACalculatorConstants.BUILDING_PLAN_OC))
					&& (StringUtils.hasText(serviceType)
							&& serviceType.equalsIgnoreCase(BPACalculatorConstants.NEW_CONSTRUCTION))) {
				ocScrutinyFee = calculateVariableFee2(deviationBUA);
			}

		}
		return ocScrutinyFee;
	}

	/**
	 * Scrutiny Fee for Residential
	 * @param paramMap
	 * @return
	 */
	private BigDecimal calculateOccupancyScrutinyFeeForResidentialOccupancy(Map<String, Object> paramMap) {
		BigDecimal ocScrutinyFee = BigDecimal.ZERO;
		String applicationType = null;
		String serviceType = null;
		String occupancyType = null;
		Double deviationBUA = null;
		if (null != paramMap.get(BPACalculatorConstants.APPLICATION_TYPE)) {
			applicationType = (String) paramMap.get(BPACalculatorConstants.APPLICATION_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.SERVICE_TYPE)) {
			serviceType = (String) paramMap.get(BPACalculatorConstants.SERVICE_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE)) {
			occupancyType = (String) paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE);
		}
		// using deviation in builtup area for calculation of application fees for OC- 
		deviationBUA = getDeviationBUAForOCFeesCalculation(paramMap);
		if ((occupancyType.equalsIgnoreCase(BPACalculatorConstants.A))) {
			if ((StringUtils.hasText(applicationType)
					&& applicationType.equalsIgnoreCase(BPACalculatorConstants.BUILDING_PLAN_OC))
					&& (StringUtils.hasText(serviceType)
							&& serviceType.equalsIgnoreCase(BPACalculatorConstants.NEW_CONSTRUCTION))) {
				ocScrutinyFee = calculateVariableFee1(deviationBUA);
			}

		}
		return ocScrutinyFee;
	}

	/**
	 * Calculate BPA OC certificate Fee
	 * @param paramMap
	 * @param estimates
	 * @return
	 */
	private BigDecimal calculateOccupancyCertificateFee(Map<String, Object> paramMap, ArrayList<TaxHeadEstimate> estimates) {
		BigDecimal flatFee = BigDecimal.ZERO;
		String applicationType = null;
		String serviceType = null;
		String occupancyType = null;
		if (null != paramMap.get(BPACalculatorConstants.APPLICATION_TYPE)) {
			applicationType = (String) paramMap.get(BPACalculatorConstants.APPLICATION_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.SERVICE_TYPE)) {
			serviceType = (String) paramMap.get(BPACalculatorConstants.SERVICE_TYPE);
		}
		
		if ((StringUtils.hasText(applicationType)
				&& applicationType.equalsIgnoreCase(BPACalculatorConstants.BUILDING_PLAN_OC))
				&& (StringUtils.hasText(serviceType)
						&& serviceType.equalsIgnoreCase(BPACalculatorConstants.NEW_CONSTRUCTION))) {
			flatFee = THOUSAND;
		}
		
		generateTaxHeadEstimate(estimates, flatFee, BPACalculatorConstants.TAXHEAD_BPA_OC_CERT_FEE, Category.FEE);
		return flatFee;
	}

	/**
	 * @param paramMap
	 * @param estimates
	 * @param tenntIdf
	 * @return
	 */
	private BigDecimal calculateTotalPermitFee(Map<String, Object> paramMap, ArrayList<TaxHeadEstimate> estimates) {
		
		// check if alteration application-

		
		if (!StringUtils.isEmpty(paramMap.get(BPACalculatorConstants.SERVICE_TYPE))
				&& paramMap.get(BPACalculatorConstants.SERVICE_TYPE).equals(BPACalculatorConstants.ALTERATION)) {
			return alterationCalculationService.calculateTotalSanctionFeeForPermit(paramMap, estimates);
		}
		
		// calculate application fees again if reworkhistory is there and compare with
		// payment done and add calculations for adjustments in separate taxheads--
		processApplicationFeesAfterRework(paramMap, estimates);
		
		BigDecimal calculatedTotalPermitFee = BigDecimal.ZERO;
		BigDecimal sanctionFee = calculateSanctionFee(paramMap, estimates);
		BigDecimal constructionWorkerWelfareCess = calculateConstructionWorkerWelfareCess(paramMap, estimates);
		BigDecimal shelterFee = calculateShelterFee(paramMap, estimates);
		BigDecimal temporaryRetentionFee = calculateTemporaryRetentionFee(paramMap, estimates);
		BigDecimal securityDeposit = calculateSecurityDeposit(paramMap, estimates);
		BigDecimal purchasableFAR = calculatePurchasableFAR(paramMap, estimates);
		BigDecimal eidpFee = calculateEIDPFee(paramMap, estimates);
		BigDecimal adjustmentAmount = calculateAdjustmentAmount(paramMap, estimates);

		calculatedTotalPermitFee = (calculatedTotalPermitFee.add(sanctionFee).add(constructionWorkerWelfareCess)
				.add(shelterFee).add(temporaryRetentionFee).add(securityDeposit).add(purchasableFAR).add(adjustmentAmount)).setScale(2,
						BigDecimal.ROUND_UP);
		// if revision application then calculate total amount after adjustments-
		BPA bpa = (BPA) paramMap.get(BPACalculatorConstants.PARAM_MAP_BPA);
		if (Boolean.TRUE.equals(bpa.getIsRevisionApplication())) {
			Revision revision = (Revision) paramMap.get(BPACalculatorConstants.REVISION);
			if(!revision.isSujogExistingApplication()) {
				calculateForRevisionNonSujogAppl(paramMap, estimates, revision);
				calculatedTotalPermitFee = calculateTotalAmountForRevision(estimates);
				return calculatedTotalPermitFee;
			}
			calculateForRevisionSujogExistingAppl(paramMap, estimates, revision);
			calculatedTotalPermitFee = calculateTotalAmountForRevision(estimates);
		}
			
		return calculatedTotalPermitFee;
	}
	
	private void calculateForRevisionNonSujogAppl(Map<String, Object> paramMap, ArrayList<TaxHeadEstimate> estimates,
			Revision revision) {
		BPA bpa = (BPA) paramMap.get("BPA");
		// set IsRevisionApplication to false for normal calculation-
		bpa.setIsRevisionApplication(false);
		CalulationCriteria calculationCriteria = CalulationCriteria.builder()
				.applicationNo(revision.getBpaApplicationNo())
				.applicationType(BPACalculatorConstants.BUILDING_PLAN_SCRUTINY).bpa(bpa)
				.feeType(BPACalculatorConstants.MDMS_CALCULATIONTYPE_SANC_FEETYPE)
				.serviceType(BPACalculatorConstants.NEW_CONSTRUCTION).tenantId(bpa.getTenantId()).build();
		List<CalulationCriteria> calculationCriterias = new ArrayList<>();
		calculationCriterias.add(calculationCriteria);
		CalculationReq calculationReq = CalculationReq.builder().calulationCriteria(calculationCriterias)
				.requestInfo((RequestInfo) paramMap.get("requestInfo")).build();
		// note that we calculate the estimates as per old parameters so that we could
		// know how much the user would have paid for old permit.We have to subtract
		// that much amount from current application's calculation-
		List<Calculation> calculationsForCurrentApplicationWithoutRevision = getEstimate(calculationReq);
		List<TaxHeadEstimate> estimatesForCurrentApplicationWithoutRevision = calculationsForCurrentApplicationWithoutRevision
				.get(0).getTaxHeadEstimates();
		for (TaxHeadEstimate estimateAsPerCurrentApplicationWORevision : estimatesForCurrentApplicationWithoutRevision) {
			//note: do not touch other fee(adjustment amount) as it might be required in new estimate
			Optional<TaxHeadEstimate> taxHeadEstimateSearchAsPerOldParameters = estimates.stream()
					.filter(estimate -> (!BPACalculatorConstants.TAXHEAD_BPA_ADJUSTMENT_AMOUNT
							.equals(estimate.getTaxHeadCode()))
							&& estimate.getTaxHeadCode()
									.equals(estimateAsPerCurrentApplicationWORevision.getTaxHeadCode()))
					.findFirst();
			if (taxHeadEstimateSearchAsPerOldParameters.isPresent()
					&& estimateAsPerCurrentApplicationWORevision.getEstimateAmount()
							.compareTo(taxHeadEstimateSearchAsPerOldParameters.get().getEstimateAmount()) > 0) {
				estimateAsPerCurrentApplicationWORevision
						.setEstimateAmount(estimateAsPerCurrentApplicationWORevision.getEstimateAmount()
								.subtract(taxHeadEstimateSearchAsPerOldParameters.get().getEstimateAmount()));
			}
			else if (taxHeadEstimateSearchAsPerOldParameters.isPresent()) {
				estimateAsPerCurrentApplicationWORevision.setEstimateAmount(BigDecimal.ZERO);
			}
		}
		estimates.clear();
		estimates.addAll(estimatesForCurrentApplicationWithoutRevision);
		bpa.setIsRevisionApplication(true);

	}
	
	private void calculateForRevisionSujogExistingAppl(Map<String, Object> paramMap, ArrayList<TaxHeadEstimate> estimates,
			Revision revision) {
		// part1 :if isSujogExistingApplication=true, then fetch old application demand details-
		if (revision.isSujogExistingApplication()) {
			BPA bpa = (BPA) paramMap.get(BPACalculatorConstants.PARAM_MAP_BPA);
			String tenantId = String.valueOf(paramMap.get("tenantId"));
			Set<String> consumerCode = new HashSet<>();
			consumerCode.add(revision.getRefBpaApplicationNo());
			Calculation calculation = Calculation.builder().applicationNumber(revision.getRefBpaApplicationNo())
					.feeType(BPACalculatorConstants.MDMS_CALCULATIONTYPE_SANC_FEETYPE).tenantId(tenantId).bpa(bpa)
					.build();
			RequestInfo requestInfo = (RequestInfo) paramMap.get("requestInfo");
			List<Demand> demands = demandService.searchDemand(tenantId, consumerCode, requestInfo, calculation);
			if (CollectionUtils.isEmpty(demands))
				return;
			// assuming only one demand-
			Demand demand = demands.get(0);
			for (TaxHeadEstimate estimate : estimates) {
				Optional<DemandDetail> demandDetail = demand.getDemandDetails().stream()
						.filter(dd -> dd.getTaxHeadMasterCode().equals(estimate.getTaxHeadCode())).findFirst();
				if (demandDetail.isPresent()) {
					DemandDetail dd = demandDetail.get();
					if (estimate.getEstimateAmount().compareTo(dd.getTaxAmount()) > 0)
						estimate.setEstimateAmount(estimate.getEstimateAmount().subtract(dd.getTaxAmount()));
					else
						estimate.setEstimateAmount(BigDecimal.ZERO);
				}
			}
		}

	}

	private BigDecimal calculateTotalAmountForRevision(ArrayList<TaxHeadEstimate> estimates) {
		BigDecimal calculatedTotalPermitFee = BigDecimal.ZERO;
		for (TaxHeadEstimate estimate : estimates) {
			calculatedTotalPermitFee.add(estimate.getEstimateAmount());
		}
		return calculatedTotalPermitFee;
	}
	
	private void processApplicationFeesAfterRework(Map<String, Object> paramMap, ArrayList<TaxHeadEstimate> estimates) {
		// calculate application fees again if reworkhistory is there--
		if (Objects.nonNull(paramMap.get(BPACalculatorConstants.PARAM_MAP_BPA))
				&& Objects.nonNull(((BPA) paramMap.get(BPACalculatorConstants.PARAM_MAP_BPA)).getReWorkHistory())) {
			ArrayList<TaxHeadEstimate> scrutinyFeeEstimates = new ArrayList<>();
			calculateTotalScrutinyFee(paramMap, scrutinyFeeEstimates);
			BigDecimal buildingOperationFeeReCalculated = BigDecimal.ZERO;
			BigDecimal landDevelopmentFeeReCalculated = BigDecimal.ZERO;
			BigDecimal buildingOperationFeePaid = BigDecimal.ZERO;
			BigDecimal landDevelopmentFeePaid = BigDecimal.ZERO;

			for (TaxHeadEstimate estimate : scrutinyFeeEstimates) {
				if (estimate.getTaxHeadCode().equals(BPACalculatorConstants.TAXHEAD_BPA_BUILDING_OPERATION_FEE))
					buildingOperationFeeReCalculated = estimate.getEstimateAmount();
				else if (estimate.getTaxHeadCode().equals(BPACalculatorConstants.TAXHEAD_BPA_LAND_DEVELOPMENT_FEE))
					landDevelopmentFeeReCalculated = estimate.getEstimateAmount();
			}

			// fetch payment details-
			Object paymentResponse = fetchPaymentDetails(paramMap);
			int paymentsLength = ((List) ((Map) paymentResponse).get("Payments")).size();
			String paymentAmountbyTaxHeadPath = BPACalculatorConstants.PAYMENT_TAXHEAD_AMOUNT_PATH;
			String buildingOpernFeePaidString = getValue((Map) paymentResponse,
					String.format(paymentAmountbyTaxHeadPath, (paymentsLength - 1),
							BPACalculatorConstants.TAXHEAD_BPA_BUILDING_OPERATION_FEE));
			paymentAmountbyTaxHeadPath = BPACalculatorConstants.PAYMENT_TAXHEAD_AMOUNT_PATH;
			buildingOpernFeePaidString = buildingOpernFeePaidString.replace("[", "").replace("]", "");
			buildingOpernFeePaidString = buildingOpernFeePaidString.isEmpty() ? "0" : buildingOpernFeePaidString;
			String landDevelopmentFeePaidString = getValue((Map) paymentResponse,
					String.format(paymentAmountbyTaxHeadPath, (paymentsLength - 1),
							BPACalculatorConstants.TAXHEAD_BPA_LAND_DEVELOPMENT_FEE));
			landDevelopmentFeePaidString = landDevelopmentFeePaidString.replace("[", "").replace("]", "");
			landDevelopmentFeePaidString = landDevelopmentFeePaidString.isEmpty() ? "0" : landDevelopmentFeePaidString;
			buildingOperationFeePaid = new BigDecimal(buildingOpernFeePaidString);
			landDevelopmentFeePaid = new BigDecimal(landDevelopmentFeePaidString);
			calculateBuildingOperationFeeReWorkAdjustment(buildingOperationFeeReCalculated, buildingOperationFeePaid,
					estimates);
			calculateLandDevelopmentFeeReWorkAdjustment(landDevelopmentFeeReCalculated, landDevelopmentFeePaid,
					estimates);
		}
	}
	
	private Object fetchPaymentDetails(Map<String, Object> paramMap) {
		StringBuilder fetchPaymentUrl = new StringBuilder(config.getCollectionServiceHost())
				.append(config.getCollectionServiceSearchPermitFeeEndpoint()).append("?consumerCodes=")
				.append(((BPA) paramMap.get("BPA")).getApplicationNo()).append("&tenantId=")
				.append(paramMap.get("tenantId"));
		Map<String, Object> payload = new HashMap<>();
		payload.put("RequestInfo", paramMap.get("requestInfo"));
		Object paymentResponse = serviceRequestRepository.fetchResult(fetchPaymentUrl, payload);
		return paymentResponse;
	}
	
	private BigDecimal calculateBuildingOperationFeeReWorkAdjustment(BigDecimal buildingOperationFeeReCalculated,
			BigDecimal buildingOperationFeePaid, ArrayList<TaxHeadEstimate> estimates) {
		BigDecimal buildingOperationFeeReWorkAdjustmentAmount = buildingOperationFeeReCalculated
				.compareTo(buildingOperationFeePaid) > 0
						? buildingOperationFeeReCalculated.subtract(buildingOperationFeePaid)
						: BigDecimal.ZERO;
		generateTaxHeadEstimate(estimates, buildingOperationFeeReWorkAdjustmentAmount,
				BPACalculatorConstants.TAXHEAD_BPA_BLDNG_OPRN_FEE_REWORK_ADJUSTMENT, Category.FEE);
		return buildingOperationFeeReWorkAdjustmentAmount;
	}

	private BigDecimal calculateLandDevelopmentFeeReWorkAdjustment(BigDecimal landDevelopmentFeeReCalculated,
			BigDecimal landDevelopmentFeePaid, ArrayList<TaxHeadEstimate> estimates) {
		BigDecimal landDevelopmentFeeReWorkAdjustmentAmount = landDevelopmentFeeReCalculated
				.compareTo(landDevelopmentFeePaid) > 0 ? landDevelopmentFeeReCalculated.subtract(landDevelopmentFeePaid)
						: BigDecimal.ZERO;
		generateTaxHeadEstimate(estimates, landDevelopmentFeeReWorkAdjustmentAmount,
				BPACalculatorConstants.TAXHEAD_BPA_LAND_DEV_FEE_REWORK_ADJUSTMENT, Category.FEE);
		return landDevelopmentFeeReWorkAdjustmentAmount;
	}
	
	public String getValue(Map dataMap, String key) {
		String jsonString = new JSONObject(dataMap).toString();
		DocumentContext context = JsonPath.using(Configuration.defaultConfiguration()).parse(jsonString);
		return context.read(key) + "";
	}
	
	private BigDecimal calculateAdjustmentAmount(Map<String, Object> paramMap, ArrayList<TaxHeadEstimate> estimates) {
		BigDecimal adjustmentAmount = BigDecimal.ZERO;
		BPA bpa = null;
		if (null != paramMap.get("BPA")) {
			bpa = (BPA) paramMap.get("BPA");
		}
		if (Objects.nonNull(bpa) && Objects.nonNull(bpa.getAdditionalDetails())
				&& !StringUtils.isEmpty(((Map) bpa.getAdditionalDetails())
						.get(BPACalculatorConstants.BPA_ADD_DETAILS_SANCTION_FEE_ADJUSTMENT_AMOUNT_KEY))) {
			adjustmentAmount = new BigDecimal(((Map) bpa.getAdditionalDetails())
					.get(BPACalculatorConstants.BPA_ADD_DETAILS_SANCTION_FEE_ADJUSTMENT_AMOUNT_KEY).toString());
		}
		generateTaxHeadEstimate(estimates, adjustmentAmount, BPACalculatorConstants.TAXHEAD_BPA_ADJUSTMENT_AMOUNT,
				Category.FEE);
		log.info("Adjustment Amount:::::::::::::::::" + adjustmentAmount);
		return adjustmentAmount;
	}

	private BigDecimal calculateEIDPFee(Map<String, Object> paramMap, ArrayList<TaxHeadEstimate> estimates) {
		BigDecimal eidpFee = BigDecimal.ZERO;
		String applicationType = null;
		String serviceType = null;
		Double projectValue = null;
		
		if (null != paramMap.get(BPACalculatorConstants.APPLICATION_TYPE)) {
			applicationType = (String) paramMap.get(BPACalculatorConstants.APPLICATION_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.SERVICE_TYPE)) {
			serviceType = (String) paramMap.get(BPACalculatorConstants.SERVICE_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.PROJECT_VALUE_FOR_EIDP)) {
			projectValue = (Double) paramMap.get(BPACalculatorConstants.PROJECT_VALUE_FOR_EIDP);
		}
		
		if ((StringUtils.hasText(applicationType)
				&& applicationType.equalsIgnoreCase(BPACalculatorConstants.BUILDING_PLAN_SCRUTINY))
				&& (StringUtils.hasText(serviceType)
				&& serviceType.equalsIgnoreCase(BPACalculatorConstants.NEW_CONSTRUCTION))
				&& projectValue != null) {
			
			eidpFee = BigDecimal.valueOf(projectValue).divide(HUNDRED);
		}
		generateTaxHeadEstimate(estimates, eidpFee, BPACalculatorConstants.TAXHEAD_BPA_EIDP_FEE, Category.FEE);

		log.info("EIDP Fee:::::::::::::::::" + eidpFee);
		return eidpFee;
	}

	/**
	 * @param paramMap
	 * @param estimates
	 * @return
	 */
	private BigDecimal calculatePurchasableFAR(Map<String, Object> paramMap, ArrayList<TaxHeadEstimate> estimates) {
		BigDecimal purchasableFARFee = BigDecimal.ZERO;
		String applicationType = null;
		String serviceType = null;
		Double benchmarkValuePerAcre = null;
		Double baseFar = null;
		Double providedFar = null;
		Double maxPermissibleFar = null;
		Double tdrFarRelaxation = null;
		Double plotArea = null;
		String subOccupancyType = null;
	    List<Occupancy> occupancyies = new ArrayList<>();
		
		if (null != paramMap.get(BPACalculatorConstants.OCCUPANCYLIST)) {
			occupancyies = (List<Occupancy>) paramMap.get(BPACalculatorConstants.OCCUPANCYLIST);
		}
		Set<String>  occCode = occupancyies.stream().filter(o->o.getSubOccupancyCode()!=null).map(Occupancy::getSubOccupancyCode).collect(Collectors.toSet());
		String code = occCode.stream().filter(occ->occ.equalsIgnoreCase(BPACalculatorConstants.A_MIH)).findAny().orElse(null);
		if (code!=null) {
			paramMap.put(BPACalculatorConstants.SUB_OCCUPANCY_TYPE,code);
		}else {
			code =occCode.stream().findFirst().get();
			if(code!=null)
			paramMap.put(BPACalculatorConstants.SUB_OCCUPANCY_TYPE,code);
		}
		if (null != paramMap.get(BPACalculatorConstants.APPLICATION_TYPE)) {
			applicationType = (String) paramMap.get(BPACalculatorConstants.APPLICATION_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.SERVICE_TYPE)) {
			serviceType = (String) paramMap.get(BPACalculatorConstants.SERVICE_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.BMV_ACRE)) {
			benchmarkValuePerAcre = (Double) paramMap.get(BPACalculatorConstants.BMV_ACRE);
		}
		if (null != paramMap.get(BPACalculatorConstants.BASE_FAR)) {
			baseFar = (Double) paramMap.get(BPACalculatorConstants.BASE_FAR);
		}
		if (null != paramMap.get(BPACalculatorConstants.PROVIDED_FAR)) {
			providedFar = (Double) paramMap.get(BPACalculatorConstants.PROVIDED_FAR);
		}
		if (null != paramMap.get(BPACalculatorConstants.PLOT_AREA)) {
			plotArea = (Double) paramMap.get(BPACalculatorConstants.PLOT_AREA);
		}
		if (null != paramMap.get(BPACalculatorConstants.SUB_OCCUPANCY_TYPE)) {
			subOccupancyType = (String) paramMap.get(BPACalculatorConstants.SUB_OCCUPANCY_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.PERMISSABLE_FAR)) {
			maxPermissibleFar = (Double) paramMap.get(BPACalculatorConstants.PERMISSABLE_FAR);
		}
		if (null != paramMap.get(BPACalculatorConstants.TDR_FAR_RELAXATION)) {
			tdrFarRelaxation = (Double) paramMap.get(BPACalculatorConstants.TDR_FAR_RELAXATION);
		}

		//calculation for MIG sub-occupancy-
		if ((null != providedFar) && (null != baseFar) && (providedFar > baseFar) && (null != plotArea)
				&& StringUtils.hasText(subOccupancyType)
				&& BPACalculatorConstants.A_MIH.equalsIgnoreCase(subOccupancyType)
				&& (StringUtils.hasText(applicationType)
						&& applicationType.equalsIgnoreCase(BPACalculatorConstants.BUILDING_PLAN_SCRUTINY))
				&& (StringUtils.hasText(serviceType)
						&& serviceType.equalsIgnoreCase(BPACalculatorConstants.NEW_CONSTRUCTION))) {
				BigDecimal benchmarkValuePerSQM = BigDecimal.valueOf(benchmarkValuePerAcre).divide(ACRE_SQMT_MULTIPLIER, 2,
					BigDecimal.ROUND_UP);

				BigDecimal purchasableFARRate = (benchmarkValuePerSQM.multiply(ZERO_TWO_FIVE)).setScale(2,
					BigDecimal.ROUND_UP);

				BigDecimal deltaFAR = (BigDecimal.valueOf(providedFar).subtract(BigDecimal.valueOf(baseFar))).setScale(2,
					BigDecimal.ROUND_UP);
				
				BigDecimal applicableDiscountFar = (BigDecimal.valueOf(maxPermissibleFar)
						.subtract(BigDecimal.valueOf(baseFar)).multiply(new BigDecimal("0.25"))).setScale(2,
								BigDecimal.ROUND_UP);
				if (deltaFAR.compareTo(applicableDiscountFar) > 0) {
					deltaFAR = deltaFAR.subtract(applicableDiscountFar);
				} else {
					deltaFAR = BigDecimal.ZERO;
				}

				purchasableFARFee = (purchasableFARRate.multiply(deltaFAR).multiply(BigDecimal.valueOf(plotArea)))
					.setScale(2, BigDecimal.ROUND_UP);
		}
		//calculation for all cases other than MIG sub-occupancy-
		else if ((null != providedFar) && (null != baseFar) && (providedFar > baseFar) && (null != plotArea)) {
			if ((StringUtils.hasText(applicationType)
					&& applicationType.equalsIgnoreCase(BPACalculatorConstants.BUILDING_PLAN_SCRUTINY))
					&& (StringUtils.hasText(serviceType)
							&& serviceType.equalsIgnoreCase(BPACalculatorConstants.NEW_CONSTRUCTION))) {

				BigDecimal benchmarkValuePerSQM = BigDecimal.valueOf(benchmarkValuePerAcre).divide(ACRE_SQMT_MULTIPLIER,
						2, BigDecimal.ROUND_UP);

				BigDecimal purchasableFARRate = (benchmarkValuePerSQM.multiply(ZERO_TWO_FIVE)).setScale(2,
						BigDecimal.ROUND_UP);

				BigDecimal deltaFAR = (BigDecimal.valueOf(providedFar).subtract(BigDecimal.valueOf(baseFar)))
						.setScale(2, BigDecimal.ROUND_UP);
				
				//tdr relaxation- decrease deltaFar based on tdrFarRelaxation-
				if(null!=tdrFarRelaxation) {
					deltaFAR=deltaFAR.subtract(new BigDecimal(tdrFarRelaxation)).setScale(2, BigDecimal.ROUND_UP);
				}
				if (deltaFAR.compareTo(BigDecimal.ZERO) < 0) {
					deltaFAR = BigDecimal.ZERO;
				}

				purchasableFARFee = (purchasableFARRate.multiply(deltaFAR).multiply(BigDecimal.valueOf(plotArea)))
						.setScale(2, BigDecimal.ROUND_UP);

			}

		}

		generateTaxHeadEstimate(estimates, purchasableFARFee, BPACalculatorConstants.TAXHEAD_BPA_PURCHASABLE_FAR, Category.FEE);

		// System.out.println("PurchasableFARFee:::::::::::::::::" + purchasableFARFee);
		log.info("PurchasableFARFee:::::::::::::::::" + purchasableFARFee);
		return purchasableFARFee;

	}

	/**
	 * @param paramMap
	 * @param estimates
	 * @return
	 */
	private BigDecimal calculateSecurityDeposit(Map<String, Object> paramMap, ArrayList<TaxHeadEstimate> estimates) {
		BigDecimal securityDeposit = BigDecimal.ZERO;
		BigDecimal securityDeposits = BigDecimal.ZERO;
		Double totalBuitUpArea = null;
		String occupancyType = null;
		boolean isSecurityDepositRequired = false;
		List<Occupancy> occupancyies = new ArrayList<>();
	
	if (null != paramMap.get(BPACalculatorConstants.OCCUPANCYLIST)) {
		occupancyies = (List<Occupancy>) paramMap.get(BPACalculatorConstants.OCCUPANCYLIST);
	}
	for(Occupancy occupancy : occupancyies) {
	totalBuitUpArea = occupancy.getFloorArea();
	log.info("totalBuitUpArea inside:"+totalBuitUpArea);
	occupancyType = occupancy.getOccupancyCode();
	paramMap.put(BPACalculatorConstants.OCCUPANCY_TYPE, occupancyType);
	paramMap.put(BPACalculatorConstants.TOTAL_FLOOR_AREA,totalBuitUpArea);
	paramMap.put(BPACalculatorConstants.SUB_OCCUPANCY_TYPE,occupancy.getSubOccupancyCode());
	log.info("occupancy inside:"+occupancyType);
	log.info("suboccupancy inside:"+occupancy.getSubOccupancyCode());
	
	
		if (null != paramMap.get(BPACalculatorConstants.TOTAL_FLOOR_AREA)) {
			totalBuitUpArea = (Double) paramMap.get(BPACalculatorConstants.TOTAL_FLOOR_AREA);
		}
		if (null != paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE)) {
			occupancyType = (String) paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.SECURITY_DEPOSIT)) {
			isSecurityDepositRequired = (boolean) paramMap.get(BPACalculatorConstants.SECURITY_DEPOSIT);
		}

		if (totalBuitUpArea != null && isSecurityDepositRequired) {
			if ((occupancyType.equalsIgnoreCase(BPACalculatorConstants.A))) {
				securityDeposit = calculateSecurityDepositForResidentialOccupancy(paramMap);
			}
			if ((occupancyType.equalsIgnoreCase(BPACalculatorConstants.B))) {
				securityDeposit = calculateSecurityDepositForCommercialOccupancy(paramMap);
			}
			if ((occupancyType.equalsIgnoreCase(BPACalculatorConstants.C))) {
				securityDeposit = calculateSecurityDepositForPublicSemiPublicInstitutionalOccupancy(paramMap);
			}
			if ((occupancyType.equalsIgnoreCase(BPACalculatorConstants.F))) {
				securityDeposit = calculateSecurityDepositForEducationOccupancy(paramMap);
			}
		}
		securityDeposits=securityDeposits.add(securityDeposit); 
	}
		generateTaxHeadEstimate(estimates, securityDeposits, BPACalculatorConstants.TAXHEAD_BPA_SECURITY_DEPOSIT, Category.FEE);
		// System.out.println("SecurityDeposit::::::::::::::" + securityDeposit);
		log.info("SecurityDeposit::::::::::::::" + securityDeposits);

		return securityDeposit;

	}

	/**
	 * @param paramMap
	 * @return
	 */
	private BigDecimal calculateSecurityDepositForEducationOccupancy(Map<String, Object> paramMap) {
		BigDecimal securityDeposit = BigDecimal.ZERO;
		String applicationType = null;
		String serviceType = null;
		String occupancyType = null;
		String subOccupancyType = null;
		Double totalBuitUpArea = null;
		if (null != paramMap.get(BPACalculatorConstants.APPLICATION_TYPE)) {
			applicationType = (String) paramMap.get(BPACalculatorConstants.APPLICATION_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.SERVICE_TYPE)) {
			serviceType = (String) paramMap.get(BPACalculatorConstants.SERVICE_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE)) {
			occupancyType = (String) paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.SUB_OCCUPANCY_TYPE)) {
			subOccupancyType = (String) paramMap.get(BPACalculatorConstants.SUB_OCCUPANCY_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.TOTAL_FLOOR_AREA)) {
			totalBuitUpArea = (Double) paramMap.get(BPACalculatorConstants.TOTAL_FLOOR_AREA);
		}
		if ((occupancyType.equalsIgnoreCase(BPACalculatorConstants.F) && StringUtils.hasText(subOccupancyType))) {
			if ((StringUtils.hasText(applicationType)
					&& applicationType.equalsIgnoreCase(BPACalculatorConstants.BUILDING_PLAN_SCRUTINY))
					&& (StringUtils.hasText(serviceType)
							&& serviceType.equalsIgnoreCase(BPACalculatorConstants.NEW_CONSTRUCTION))) {
				if (null != totalBuitUpArea && totalBuitUpArea >= 200) {
					// securityDeposit = calculateConstantFee(paramMap, 100);
					securityDeposit = calculateConstantFeeNew(totalBuitUpArea, 100);
				}
			}
		}
		return securityDeposit;
	}

	/**
	 * @param paramMap
	 * @return
	 */
	private BigDecimal calculateSecurityDepositForPublicSemiPublicInstitutionalOccupancy(Map<String, Object> paramMap) {
		BigDecimal securityDeposit = BigDecimal.ZERO;
		String applicationType = null;
		String serviceType = null;
		String occupancyType = null;
		String subOccupancyType = null;
		Double totalBuitUpArea = null;
		if (null != paramMap.get(BPACalculatorConstants.APPLICATION_TYPE)) {
			applicationType = (String) paramMap.get(BPACalculatorConstants.APPLICATION_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.SERVICE_TYPE)) {
			serviceType = (String) paramMap.get(BPACalculatorConstants.SERVICE_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE)) {
			occupancyType = (String) paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.SUB_OCCUPANCY_TYPE)) {
			subOccupancyType = (String) paramMap.get(BPACalculatorConstants.SUB_OCCUPANCY_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.TOTAL_FLOOR_AREA)) {
			totalBuitUpArea = (Double) paramMap.get(BPACalculatorConstants.TOTAL_FLOOR_AREA);
		}
		if ((occupancyType.equalsIgnoreCase(BPACalculatorConstants.C) && StringUtils.hasText(subOccupancyType))) {
			if ((StringUtils.hasText(applicationType)
					&& applicationType.equalsIgnoreCase(BPACalculatorConstants.BUILDING_PLAN_SCRUTINY))
					&& (StringUtils.hasText(serviceType)
							&& serviceType.equalsIgnoreCase(BPACalculatorConstants.NEW_CONSTRUCTION))) {
				if (null != totalBuitUpArea && totalBuitUpArea >= 200) {
					// securityDeposit = calculateConstantFee(paramMap, 100);
					securityDeposit = calculateConstantFeeNew(totalBuitUpArea, 100);

				}
			}
		}
		return securityDeposit;
	}

	/**
	 * @param paramMap
	 * @return
	 */
	private BigDecimal calculateSecurityDepositForCommercialOccupancy(Map<String, Object> paramMap) {
		BigDecimal securityDeposit = BigDecimal.ZERO;
		String applicationType = null;
		String serviceType = null;
		String occupancyType = null;
		String subOccupancyType = null;
		Double totalBuitUpArea = null;
		if (null != paramMap.get(BPACalculatorConstants.APPLICATION_TYPE)) {
			applicationType = (String) paramMap.get(BPACalculatorConstants.APPLICATION_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.SERVICE_TYPE)) {
			serviceType = (String) paramMap.get(BPACalculatorConstants.SERVICE_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE)) {
			occupancyType = (String) paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.SUB_OCCUPANCY_TYPE)) {
			subOccupancyType = (String) paramMap.get(BPACalculatorConstants.SUB_OCCUPANCY_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.TOTAL_FLOOR_AREA)) {
			totalBuitUpArea = (Double) paramMap.get(BPACalculatorConstants.TOTAL_FLOOR_AREA);
		}
		if ((occupancyType.equalsIgnoreCase(BPACalculatorConstants.B) && StringUtils.hasText(subOccupancyType))) {
			if ((StringUtils.hasText(applicationType)
					&& applicationType.equalsIgnoreCase(BPACalculatorConstants.BUILDING_PLAN_SCRUTINY))
					&& (StringUtils.hasText(serviceType)
							&& serviceType.equalsIgnoreCase(BPACalculatorConstants.NEW_CONSTRUCTION))) {
				if (null != totalBuitUpArea && totalBuitUpArea >= 200) {
					// securityDeposit = calculateConstantFee(paramMap, 100);
					securityDeposit = calculateConstantFeeNew(totalBuitUpArea, 100);

				}
			}
		}
		return securityDeposit;
	}

	/**
	 * @param paramMap
	 * @return
	 */
	private BigDecimal calculateSecurityDepositForResidentialOccupancy(Map<String, Object> paramMap) {
		BigDecimal securityDeposit = BigDecimal.ZERO;
		String applicationType = null;
		String serviceType = null;
		String occupancyType = null;
		String subOccupancyType = null;
		Double totalBuitUpArea = null;
		if (null != paramMap.get(BPACalculatorConstants.APPLICATION_TYPE)) {
			applicationType = (String) paramMap.get(BPACalculatorConstants.APPLICATION_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.SERVICE_TYPE)) {
			serviceType = (String) paramMap.get(BPACalculatorConstants.SERVICE_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE)) {
			occupancyType = (String) paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.SUB_OCCUPANCY_TYPE)) {
			subOccupancyType = (String) paramMap.get(BPACalculatorConstants.SUB_OCCUPANCY_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.TOTAL_FLOOR_AREA)) {
			totalBuitUpArea = (Double) paramMap.get(BPACalculatorConstants.TOTAL_FLOOR_AREA);
		}
		if ((occupancyType.equalsIgnoreCase(BPACalculatorConstants.A) && StringUtils.hasText(subOccupancyType))) {
			if ((StringUtils.hasText(applicationType)
					&& applicationType.equalsIgnoreCase(BPACalculatorConstants.BUILDING_PLAN_SCRUTINY))
					&& (StringUtils.hasText(serviceType)
							&& serviceType.equalsIgnoreCase(BPACalculatorConstants.NEW_CONSTRUCTION))) {

				if ((subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.A_AB))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.A_HP))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.A_WCR))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.A_SA))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.A_E))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.A_LIH))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.A_MIH))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.A_SQ))) {

					// securityDeposit = calculateConstantFee(paramMap, 100);
					securityDeposit = calculateConstantFeeNew(totalBuitUpArea, 100);

				}

			}

		}
		return securityDeposit;
	}

	/**
	 * @param paramMap
	 * @param estimates
	 * @return
	 */
	private BigDecimal calculateTemporaryRetentionFee(Map<String, Object> paramMap,
			ArrayList<TaxHeadEstimate> estimates) {
		BigDecimal retentionFee = BigDecimal.ZERO;
		Double numberOfTemporaryStructures = null;
		String applicationType = null;
		String serviceType = null;
		if (null != paramMap.get(BPACalculatorConstants.APPLICATION_TYPE)) {
			applicationType = (String) paramMap.get(BPACalculatorConstants.APPLICATION_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.SERVICE_TYPE)) {
			serviceType = (String) paramMap.get(BPACalculatorConstants.SERVICE_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.NUMBER_OF_TEMP_STRUCTURES)) {
			numberOfTemporaryStructures = (Double) paramMap.get(BPACalculatorConstants.NUMBER_OF_TEMP_STRUCTURES);
		}
		boolean isRetentionFeeApplicable = false;
		if (null != paramMap.get(BPACalculatorConstants.IS_RETENTION_FEE_APPLICABLE)) {
			isRetentionFeeApplicable = (boolean) paramMap.get(BPACalculatorConstants.IS_RETENTION_FEE_APPLICABLE);
		}
		if ((StringUtils.hasText(applicationType)
				&& applicationType.equalsIgnoreCase(BPACalculatorConstants.BUILDING_PLAN_SCRUTINY))
				&& (StringUtils.hasText(serviceType)
						&& serviceType.equalsIgnoreCase(BPACalculatorConstants.NEW_CONSTRUCTION))
				&& isRetentionFeeApplicable && Objects.nonNull(numberOfTemporaryStructures)) {
			Object mdmsData = paramMap.get("mdmsData");
			String tenantId = String.valueOf(paramMap.get("tenantId"));
			List jsonOutput = JsonPath.read(mdmsData, BPACalculatorConstants.MDMS_RETENTION_FEE_PATH);
			String filterExp = "$.[?(@.ulb == '" + tenantId + "')]";
			List<Map<String, String>> retentionFeeForTenantJson = JsonPath.read(jsonOutput, filterExp);
			if (!CollectionUtils.isEmpty(retentionFeeForTenantJson)) {
				String retentionFeeForTenant = retentionFeeForTenantJson.get(0)
						.get(BPACalculatorConstants.MDMS_RETENTION_FEE);
				retentionFee = new BigDecimal(retentionFeeForTenant).multiply(new BigDecimal(numberOfTemporaryStructures));
			}
		}

		generateTaxHeadEstimate(estimates, retentionFee, BPACalculatorConstants.TAXHEAD_BPA_TEMP_RETENTION_FEE, Category.FEE);

		// System.out.println("RetentionFee:::::::::::" + retentionFee);
		log.info("RetentionFee:::::::::::" + retentionFee);
		return retentionFee;

	}

	/**
	 * @param paramMap
	 * @param estimates
	 * @return
	 */
	private BigDecimal calculateShelterFee(Map<String, Object> paramMap, ArrayList<TaxHeadEstimate> estimates) {
		
		BigDecimal shelterFee = BigDecimal.ZERO;
		BigDecimal shelterFees = BigDecimal.ZERO;
		Double totalBuitUpArea = null;
		String occupancyType = null;
		List<Occupancy> occupancyies = new ArrayList<>();
		
		if (null != paramMap.get(BPACalculatorConstants.OCCUPANCYLIST)) {
			occupancyies = (List<Occupancy>) paramMap.get(BPACalculatorConstants.OCCUPANCYLIST);
		}
		for(Occupancy occupancy : occupancyies) {
		totalBuitUpArea = occupancy.getFloorArea();
		log.info("totalBuitUpArea inside:"+totalBuitUpArea);
		occupancyType = occupancy.getOccupancyCode();
		paramMap.put(BPACalculatorConstants.OCCUPANCY_TYPE, occupancyType);
		paramMap.put(BPACalculatorConstants.TOTAL_FLOOR_AREA,totalBuitUpArea);
		paramMap.put(BPACalculatorConstants.SUB_OCCUPANCY_TYPE,occupancy.getSubOccupancyCode());
		log.info("occupancy shelter inside :"+occupancyType);
		log.info("suboccupancy inside:"+occupancy.getSubOccupancyCode());
		if (null != paramMap.get(BPACalculatorConstants.TOTAL_FLOOR_AREA)) {
			totalBuitUpArea = (Double) paramMap.get(BPACalculatorConstants.TOTAL_FLOOR_AREA);
		}
		if (null != paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE)) {
			occupancyType = (String) paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE);
		}
		if (totalBuitUpArea != null) {
			if ((occupancyType.equalsIgnoreCase(BPACalculatorConstants.A))) {
				shelterFee = calculateShelterFeeForResidentialOccupancy(paramMap);
			}
		}
		shelterFees=shelterFees.add(shelterFee);
		if(shelterFees.compareTo(BigDecimal.ZERO)>0) {
			break;
		}
		}
		generateTaxHeadEstimate(estimates, shelterFees, BPACalculatorConstants.TAXHEAD_BPA_SHELTER_FEE, Category.FEE);

		// System.out.println("ShelterFee::::::::::::::::" + shelterFee);
		log.info("ShelterFee::::::::::::::::" + shelterFees);
		return shelterFees;

	}

	/**
	 * @param paramMap
	 * @return
	 */
	private BigDecimal calculateShelterFeeForResidentialOccupancy(Map<String, Object> paramMap) {
		BigDecimal shelterFee = BigDecimal.ZERO;
		String applicationType = null;
		String serviceType = null;
		String occupancyType = null;
		String subOccupancyType = null;
		Double totalEWSArea = null;
		boolean isShelterFeeRequired = false;
		int totalNoOfDwellingUnits = 0;
		if (null != paramMap.get(BPACalculatorConstants.APPLICATION_TYPE)) {
			applicationType = (String) paramMap.get(BPACalculatorConstants.APPLICATION_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.SERVICE_TYPE)) {
			serviceType = (String) paramMap.get(BPACalculatorConstants.SERVICE_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE)) {
			occupancyType = (String) paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.SUB_OCCUPANCY_TYPE)) {
			subOccupancyType = (String) paramMap.get(BPACalculatorConstants.SUB_OCCUPANCY_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.EWS_AREA)) {
			totalEWSArea = (Double) paramMap.get(BPACalculatorConstants.EWS_AREA);
			log.info("shelter part:"+totalEWSArea);
		}
		if (null != paramMap.get(BPACalculatorConstants.SHELTER_FEE)) {
			isShelterFeeRequired = (boolean) paramMap.get(BPACalculatorConstants.SHELTER_FEE);
		}
		if (null != paramMap.get(BPACalculatorConstants.TOTAL_NO_OF_DWELLING_UNITS)) {
			totalNoOfDwellingUnits = (int) paramMap.get(BPACalculatorConstants.TOTAL_NO_OF_DWELLING_UNITS);
			log.info("			totalNoOfDwellingUnits part:"+			totalNoOfDwellingUnits);
		}
		if (isShelterFeeRequired && totalNoOfDwellingUnits > 8) {
			if ((occupancyType.equalsIgnoreCase(BPACalculatorConstants.A) && StringUtils.hasText(subOccupancyType))) {
				if ((StringUtils.hasText(applicationType)
						&& applicationType.equalsIgnoreCase(BPACalculatorConstants.BUILDING_PLAN_SCRUTINY))
						&& (StringUtils.hasText(serviceType)
								&& serviceType.equalsIgnoreCase(BPACalculatorConstants.NEW_CONSTRUCTION))) {

					if ((subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.A_P))
							|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.A_S))
							|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.A_R))
							|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.A_AB))
							|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.A_HP))
							|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.A_WCR))
							|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.A_SA))
							|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.A_MIH))) {
                     log.info("shelter part:"+totalEWSArea);
						shelterFee = (BigDecimal.valueOf(totalEWSArea).multiply(SQMT_SQFT_MULTIPLIER)
								.multiply(SEVENTEEN_FIFTY).multiply(ZERO_TWO_FIVE)).setScale(2, BigDecimal.ROUND_UP);

					}

				}

			}

		}
		return shelterFee;
	}

	/**
	 * @param paramMap
	 * @param estimates
	 * @return
	 */
	private BigDecimal calculateConstructionWorkerWelfareCess(Map<String, Object> paramMap,
			ArrayList<TaxHeadEstimate> estimates) {
		BigDecimal welfareCess = BigDecimal.ZERO;
		String applicationType = null;
		String serviceType = null;
		Double totalFloorArea = null;
		Double totalBuiltupArea = null;
		Double totalBuiltup =0.0;
		Double totalFloor =0.0;
		List<Occupancy> occupancyies = new ArrayList<>();
		if (null != paramMap.get(BPACalculatorConstants.OCCUPANCYLIST)) {
			occupancyies = (List<Occupancy>) paramMap.get(BPACalculatorConstants.OCCUPANCYLIST);
		}
		for(Occupancy occupancy : occupancyies) {
			
			totalFloor += occupancy.getFloorArea();
			log.info("totalfllorArea inside:"+totalFloor);
			totalBuiltup += occupancy.getBuiltUpArea();
			log.info("totalBuitUpArea inside:"+totalBuiltup);
			
		}
		paramMap.put(BPACalculatorConstants.TOTAL_BUILTUP_AREA_EDCR,totalBuiltup);
		paramMap.put(BPACalculatorConstants.TOTAL_FLOOR_AREA,totalFloor);
		if (null != paramMap.get(BPACalculatorConstants.APPLICATION_TYPE)) {
			applicationType = (String) paramMap.get(BPACalculatorConstants.APPLICATION_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.SERVICE_TYPE)) {
			serviceType = (String) paramMap.get(BPACalculatorConstants.SERVICE_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.TOTAL_FLOOR_AREA)) {
			totalFloorArea = (Double) paramMap.get(BPACalculatorConstants.TOTAL_FLOOR_AREA);
		}
		totalBuiltupArea=getAreaParameterForBPFeesCalculation(paramMap);
		if ((StringUtils.hasText(applicationType)
				&& applicationType.equalsIgnoreCase(BPACalculatorConstants.BUILDING_PLAN_SCRUTINY))
				&& (StringUtils.hasText(serviceType)
						&& serviceType.equalsIgnoreCase(BPACalculatorConstants.NEW_CONSTRUCTION))) {
			// Double costOfConstruction = (1750 * totalBuitUpArea * 10.764);
			//Use builtup area instead of floor area to calculate totalCostOfConstruction and check if totalCostOfConstruction>10Lakh.If true,
			//then use builtup area instead of floor area to calculate ConstructionWorkerWelfareCess-
			BigDecimal totalCostOfConstruction = (SEVENTEEN_FIFTY.multiply(BigDecimal.valueOf(totalBuiltupArea))
					.multiply(SQMT_SQFT_MULTIPLIER)).setScale(2, BigDecimal.ROUND_UP);
			if (totalCostOfConstruction.compareTo(TEN_LAC) > 0) {
				welfareCess = (EIGHTEEN_POINT_TWO_ONE.multiply(BigDecimal.valueOf(totalBuiltupArea))
						.multiply(SQMT_SQFT_MULTIPLIER)).setScale(2, BigDecimal.ROUND_UP);
			}

		}
		generateTaxHeadEstimate(estimates, welfareCess, BPACalculatorConstants.TAXHEAD_BPA_WORKER_WELFARE_CESS, Category.FEE);

		// System.out.println("WelfareCess::::::::::::::" + welfareCess);
		log.info("WelfareCess::::::::::::::" + welfareCess);
		return welfareCess;

	}

	/**
	 * @param paramMap
	 * @param estimates
	 * @return
	 */
	private BigDecimal calculateSanctionFee(Map<String, Object> paramMap, ArrayList<TaxHeadEstimate> estimates) {
		BigDecimal sanctionFee = BigDecimal.ZERO;
		BigDecimal sanctionFees = BigDecimal.ZERO;
		Double totalBuitUpArea = null;
		String occupancyType = null;
		totalBuitUpArea=getAreaParameterForBPFeesCalculation(paramMap);
		List<Occupancy> occupancyies= new ArrayList<>();
//		if (null != paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE)) {
//			occupancyType = (String) paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE);
//		}
		
		
		if (null != paramMap.get(BPACalculatorConstants.OCCUPANCYLIST)) {
			occupancyies = (List<Occupancy>) paramMap.get(BPACalculatorConstants.OCCUPANCYLIST);
		}
		for(Occupancy occupancy : occupancyies) {
			
			totalBuitUpArea = occupancy.getBuiltUpArea();
			log.info("totalBuitUpArea inside:"+totalBuitUpArea);
			occupancyType = occupancy.getOccupancyCode();
			paramMap.put(BPACalculatorConstants.OCCUPANCY_TYPE, occupancyType);
			paramMap.put(BPACalculatorConstants.TOTAL_BUILTUP_AREA_EDCR,totalBuitUpArea);
			paramMap.put(BPACalculatorConstants.SUB_OCCUPANCY_TYPE,occupancy.getSubOccupancyCode());
			log.info("occupancy inside:"+occupancyType);
			log.info("suboccupancy inside:"+occupancy.getSubOccupancyCode());
			
		if (totalBuitUpArea != null) {
			if ((occupancyType.equalsIgnoreCase(BPACalculatorConstants.A))) {
				sanctionFee = calculateSanctionFeeForResidentialOccupancy(paramMap);
			}
			if ((occupancyType.equalsIgnoreCase(BPACalculatorConstants.B))) {
				sanctionFee = calculateSanctionFeeForCommercialOccupancy(paramMap);
			}
			if ((occupancyType.equalsIgnoreCase(BPACalculatorConstants.C))) {
				sanctionFee = calculateSanctionFeeForPublicSemiPublicInstitutionalOccupancy(paramMap);
			}
			if ((occupancyType.equalsIgnoreCase(BPACalculatorConstants.D))) {
				sanctionFee = calculateSanctionFeeForPublicUtilityOccupancy(paramMap);
			}
			if ((occupancyType.equalsIgnoreCase(BPACalculatorConstants.E))) {
				sanctionFee = calculateSanctionFeeForIndustrialZoneOccupancy(paramMap);
			}
			if ((occupancyType.equalsIgnoreCase(BPACalculatorConstants.F))) {
				sanctionFee = calculateSanctionFeeForEducationOccupancy(paramMap);
			}
			if ((occupancyType.equalsIgnoreCase(BPACalculatorConstants.G))) {
				sanctionFee = calculateSanctionFeeForTransportationOccupancy(paramMap);
			}
			if ((occupancyType.equalsIgnoreCase(BPACalculatorConstants.H))) {
				sanctionFee = calculateSanctionFeeForAgricultureOccupancy(paramMap);
			}

		}
		sanctionFees =  sanctionFees.add(sanctionFee);
		}

		generateTaxHeadEstimate(estimates, sanctionFees, BPACalculatorConstants.TAXHEAD_BPA_SANCTION_FEE, Category.FEE);

		// System.out.println("SanctionFee::::::::" + sanctionFee);
		log.info("SanctionFee::::::::" + sanctionFees);
		return sanctionFees;
	}

	/**
	 * @param paramMap
	 * @return
	 */
	private BigDecimal calculateSanctionFeeForPublicSemiPublicInstitutionalOccupancy(Map<String, Object> paramMap) {
		BigDecimal sanctionFee = BigDecimal.ZERO;
		String applicationType = null;
		String serviceType = null;
		String occupancyType = null;
		String subOccupancyType = null;
		Double totalBuitUpArea = null;
		Boolean isSparit = null;
		if (null != paramMap.get(BPACalculatorConstants.APPLICATION_TYPE)) {
			applicationType = (String) paramMap.get(BPACalculatorConstants.APPLICATION_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.SERVICE_TYPE)) {
			serviceType = (String) paramMap.get(BPACalculatorConstants.SERVICE_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE)) {
			occupancyType = (String) paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.SUB_OCCUPANCY_TYPE)) {
			subOccupancyType = (String) paramMap.get(BPACalculatorConstants.SUB_OCCUPANCY_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.SPARIT_CHECK)) {
			isSparit = (Boolean) paramMap.get(BPACalculatorConstants.SPARIT_CHECK);
		}
		totalBuitUpArea=getAreaParameterForBPFeesCalculation(paramMap);
		if ((occupancyType.equalsIgnoreCase(BPACalculatorConstants.C)) && (StringUtils.hasText(subOccupancyType))) {
			if ((StringUtils.hasText(applicationType)
					&& applicationType.equalsIgnoreCase(BPACalculatorConstants.BUILDING_PLAN_SCRUTINY))
					&& (StringUtils.hasText(serviceType)
							&& serviceType.equalsIgnoreCase(BPACalculatorConstants.NEW_CONSTRUCTION))) {
				if ((subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_A))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_B))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_C))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_CL))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_MP))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_CH))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_O))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_OAH))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_SC))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_C1H))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_C2H))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_SCC))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_CC))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_EC))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_G))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_MH))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_ML))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_M))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_PW))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_PL))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_REB))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_SPC))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_S))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_T))) {

					// sanctionFee = calculateConstantFee(paramMap, 30);
					if(isSparit)
						sanctionFee = calculateConstantFeeNew(totalBuitUpArea, 15);
					else
					sanctionFee = calculateConstantFeeNew(totalBuitUpArea, 30);

				} else if ((subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_AB))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_GO))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_LSGO))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_P))) {
					// sanctionFee = calculateConstantFee(paramMap, 10);
					if(isSparit)
						sanctionFee = calculateConstantFeeNew(totalBuitUpArea, 5);
					else
					sanctionFee = calculateConstantFeeNew(totalBuitUpArea, 10);
				} else if ((subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_SWC))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_CI))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_D))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_YC))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_DC))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_GSGH))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_RT))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_HC))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_H))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_L))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_MTH))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_MB))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_NH))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_PLY))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_RC))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_VHAB))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_RTI))) {
					// sanctionFee = calculateConstantFee(paramMap, 30);
					if(isSparit)
						sanctionFee = calculateConstantFeeNew(totalBuitUpArea, 15);
					else
					sanctionFee = calculateConstantFeeNew(totalBuitUpArea, 30);
					
				} else if ((subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_PS))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_FS))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_J))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_PO))) {
					// sanctionFee = calculateConstantFee(paramMap, 10);
					if(isSparit)
						sanctionFee = calculateConstantFeeNew(totalBuitUpArea, 5);
					else
					sanctionFee = calculateConstantFeeNew(totalBuitUpArea, 10);
				}
			}
		}
		return sanctionFee;
	}

	/**
	 * @param paramMap
	 * @return
	 */
	private BigDecimal calculateSanctionFeeForAgricultureOccupancy(Map<String, Object> paramMap) {
		BigDecimal sanctionFee = BigDecimal.ZERO;
		String applicationType = null;
		String serviceType = null;
		String occupancyType = null;
		Double totalBuitUpArea = null;
		Boolean isSparit = null;
		if (null != paramMap.get(BPACalculatorConstants.APPLICATION_TYPE)) {
			applicationType = (String) paramMap.get(BPACalculatorConstants.APPLICATION_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.SERVICE_TYPE)) {
			serviceType = (String) paramMap.get(BPACalculatorConstants.SERVICE_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE)) {
			occupancyType = (String) paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.SPARIT_CHECK)) {
			isSparit = (Boolean) paramMap.get(BPACalculatorConstants.SPARIT_CHECK);
		}
		totalBuitUpArea=getAreaParameterForBPFeesCalculation(paramMap);
		if ((occupancyType.equalsIgnoreCase(BPACalculatorConstants.H))) {
			if ((StringUtils.hasText(applicationType)
					&& applicationType.equalsIgnoreCase(BPACalculatorConstants.BUILDING_PLAN_SCRUTINY))
					&& (StringUtils.hasText(serviceType)
							&& serviceType.equalsIgnoreCase(BPACalculatorConstants.NEW_CONSTRUCTION))) {
				// sanctionFee = calculateConstantFee(paramMap, 30);
				if(isSparit)
					sanctionFee = calculateConstantFeeNew(totalBuitUpArea, 15);
				else
				sanctionFee = calculateConstantFeeNew(totalBuitUpArea, 30);
			}
		}
		return sanctionFee;
	}

	/**
	 * @param paramMap
	 * @return
	 */
	private BigDecimal calculateSanctionFeeForTransportationOccupancy(Map<String, Object> paramMap) {
		BigDecimal sanctionFee = BigDecimal.ZERO;
		String applicationType = null;
		String serviceType = null;
		String occupancyType = null;
		Double totalBuitUpArea = null;
		Boolean isSparit = null;
		if (null != paramMap.get(BPACalculatorConstants.APPLICATION_TYPE)) {
			applicationType = (String) paramMap.get(BPACalculatorConstants.APPLICATION_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.SERVICE_TYPE)) {
			serviceType = (String) paramMap.get(BPACalculatorConstants.SERVICE_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE)) {
			occupancyType = (String) paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.SPARIT_CHECK)) {
			isSparit = (Boolean) paramMap.get(BPACalculatorConstants.SPARIT_CHECK);
		}
		totalBuitUpArea=getAreaParameterForBPFeesCalculation(paramMap);
		if ((occupancyType.equalsIgnoreCase(BPACalculatorConstants.G))) {
			if ((StringUtils.hasText(applicationType)
					&& applicationType.equalsIgnoreCase(BPACalculatorConstants.BUILDING_PLAN_SCRUTINY))
					&& (StringUtils.hasText(serviceType)
							&& serviceType.equalsIgnoreCase(BPACalculatorConstants.NEW_CONSTRUCTION))) {
				// sanctionFee = calculateConstantFee(paramMap, 10);
				if(isSparit)
					sanctionFee = calculateConstantFeeNew(totalBuitUpArea, 5);	
				else
				 sanctionFee = calculateConstantFeeNew(totalBuitUpArea, 10);
			}
		}
		return sanctionFee;
	}

	/**
	 * @param paramMap
	 * @return
	 */
	private BigDecimal calculateSanctionFeeForEducationOccupancy(Map<String, Object> paramMap) {
		BigDecimal sanctionFee = BigDecimal.ZERO;
		String applicationType = null;
		String serviceType = null;
		String occupancyType = null;
		Double totalBuitUpArea = null;
		Boolean isSparit = null;
		if (null != paramMap.get(BPACalculatorConstants.APPLICATION_TYPE)) {
			applicationType = (String) paramMap.get(BPACalculatorConstants.APPLICATION_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.SERVICE_TYPE)) {
			serviceType = (String) paramMap.get(BPACalculatorConstants.SERVICE_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE)) {
			occupancyType = (String) paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.SPARIT_CHECK)) {
			isSparit = (Boolean) paramMap.get(BPACalculatorConstants.SPARIT_CHECK);
		}

		totalBuitUpArea=getAreaParameterForBPFeesCalculation(paramMap);
		if ((occupancyType.equalsIgnoreCase(BPACalculatorConstants.F))) {
			if ((StringUtils.hasText(applicationType)
					&& applicationType.equalsIgnoreCase(BPACalculatorConstants.BUILDING_PLAN_SCRUTINY))
					&& (StringUtils.hasText(serviceType)
							&& serviceType.equalsIgnoreCase(BPACalculatorConstants.NEW_CONSTRUCTION))) {
				// sanctionFee = calculateConstantFee(paramMap, 30);
				if(isSparit)
					sanctionFee = calculateConstantFeeNew(totalBuitUpArea, 15);	
				else
				  sanctionFee = calculateConstantFeeNew(totalBuitUpArea, 30);
			}
		}
		return sanctionFee;
	}

	/**
	 * @param paramMap
	 * @return
	 */
	private BigDecimal calculateSanctionFeeForPublicUtilityOccupancy(Map<String, Object> paramMap) {
		BigDecimal sanctionFee = BigDecimal.ZERO;
		String applicationType = null;
		String serviceType = null;
		String occupancyType = null;
		Double totalBuitUpArea = null;
		Boolean isSparit = null;
		if (null != paramMap.get(BPACalculatorConstants.APPLICATION_TYPE)) {
			applicationType = (String) paramMap.get(BPACalculatorConstants.APPLICATION_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.SERVICE_TYPE)) {
			serviceType = (String) paramMap.get(BPACalculatorConstants.SERVICE_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE)) {
			occupancyType = (String) paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.SPARIT_CHECK)) {
			isSparit = (Boolean) paramMap.get(BPACalculatorConstants.SPARIT_CHECK);
		}
		totalBuitUpArea=getAreaParameterForBPFeesCalculation(paramMap);
		if ((occupancyType.equalsIgnoreCase(BPACalculatorConstants.D))) {
			if ((StringUtils.hasText(applicationType)
					&& applicationType.equalsIgnoreCase(BPACalculatorConstants.BUILDING_PLAN_SCRUTINY))
					&& (StringUtils.hasText(serviceType)
							&& serviceType.equalsIgnoreCase(BPACalculatorConstants.NEW_CONSTRUCTION))) {
				// sanctionFee = calculateConstantFee(paramMap, 10);
				if(isSparit)
					sanctionFee = calculateConstantFeeNew(totalBuitUpArea, 10);
				else
				  sanctionFee = calculateConstantFeeNew(totalBuitUpArea, 5);
			}
		}
		return sanctionFee;
	}

	/**
	 * @param paramMap
	 * @return
	 */
	private BigDecimal calculateSanctionFeeForIndustrialZoneOccupancy(Map<String, Object> paramMap) {
		BigDecimal sanctionFee = BigDecimal.ZERO;
		String applicationType = null;
		String serviceType = null;
		String occupancyType = null;
		Double totalBuitUpArea = null;
		Boolean isSparit = null;
		if (null != paramMap.get(BPACalculatorConstants.APPLICATION_TYPE)) {
			applicationType = (String) paramMap.get(BPACalculatorConstants.APPLICATION_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.SERVICE_TYPE)) {
			serviceType = (String) paramMap.get(BPACalculatorConstants.SERVICE_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE)) {
			occupancyType = (String) paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.SPARIT_CHECK)) {
			isSparit = (Boolean) paramMap.get(BPACalculatorConstants.SPARIT_CHECK);
		}
		totalBuitUpArea=getAreaParameterForBPFeesCalculation(paramMap);
		if ((occupancyType.equalsIgnoreCase(BPACalculatorConstants.E))) {
			if ((StringUtils.hasText(applicationType)
					&& applicationType.equalsIgnoreCase(BPACalculatorConstants.BUILDING_PLAN_SCRUTINY))
					&& (StringUtils.hasText(serviceType)
							&& serviceType.equalsIgnoreCase(BPACalculatorConstants.NEW_CONSTRUCTION))) {
				// sanctionFee = calculateConstantFee(paramMap, 60);
				if(isSparit)
					sanctionFee = calculateConstantFeeNew(totalBuitUpArea, 30);
				else
				  sanctionFee = calculateConstantFeeNew(totalBuitUpArea, 60);
			}
		}
		return sanctionFee;
	}

	/**
	 * @param paramMap
	 * @return
	 */
	private BigDecimal calculateSanctionFeeForCommercialOccupancy(Map<String, Object> paramMap) {
		BigDecimal sanctionFee = BigDecimal.ZERO;
		String applicationType = null;
		String serviceType = null;
		String occupancyType = null;
		Double totalBuitUpArea = null;
		Boolean isSparit = null;
		if (null != paramMap.get(BPACalculatorConstants.APPLICATION_TYPE)) {
			applicationType = (String) paramMap.get(BPACalculatorConstants.APPLICATION_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.SERVICE_TYPE)) {
			serviceType = (String) paramMap.get(BPACalculatorConstants.SERVICE_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE)) {
			occupancyType = (String) paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.SPARIT_CHECK)) {
			isSparit = (Boolean) paramMap.get(BPACalculatorConstants.SPARIT_CHECK);
		}
		totalBuitUpArea=getAreaParameterForBPFeesCalculation(paramMap);
		if ((occupancyType.equalsIgnoreCase(BPACalculatorConstants.B))) {
			if ((StringUtils.hasText(applicationType)
					&& applicationType.equalsIgnoreCase(BPACalculatorConstants.BUILDING_PLAN_SCRUTINY))
					&& (StringUtils.hasText(serviceType)
							&& serviceType.equalsIgnoreCase(BPACalculatorConstants.NEW_CONSTRUCTION))) {
				// sanctionFee = calculateConstantFee(paramMap, 60);
				if(isSparit)
					sanctionFee = calculateConstantFeeNew(totalBuitUpArea, 30);
				else
				 sanctionFee = calculateConstantFeeNew(totalBuitUpArea, 60);
			}
		}
		return sanctionFee;
	}

	/**
	 * @param paramMap
	 * @return
	 */
	private BigDecimal calculateSanctionFeeForResidentialOccupancy(Map<String, Object> paramMap) {
		BigDecimal sanctionFee = BigDecimal.ZERO;
		String applicationType = null;
		String serviceType = null;
		String occupancyType = null;
		String subOccupancyType = null;
		Double totalBuitUpArea = null;
		Boolean isSparit = null;
		if (null != paramMap.get(BPACalculatorConstants.APPLICATION_TYPE)) {
			applicationType = (String) paramMap.get(BPACalculatorConstants.APPLICATION_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.SERVICE_TYPE)) {
			serviceType = (String) paramMap.get(BPACalculatorConstants.SERVICE_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE)) {
			occupancyType = (String) paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.SUB_OCCUPANCY_TYPE)) {
			subOccupancyType = (String) paramMap.get(BPACalculatorConstants.SUB_OCCUPANCY_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.SPARIT_CHECK)) {
			isSparit = (Boolean) paramMap.get(BPACalculatorConstants.SPARIT_CHECK);
		}
		totalBuitUpArea=getAreaParameterForBPFeesCalculation(paramMap);
		if ((occupancyType.equalsIgnoreCase(BPACalculatorConstants.A) && StringUtils.hasText(subOccupancyType))) {
			if ((StringUtils.hasText(applicationType)
					&& applicationType.equalsIgnoreCase(BPACalculatorConstants.BUILDING_PLAN_SCRUTINY))
					&& (StringUtils.hasText(serviceType)
							&& serviceType.equalsIgnoreCase(BPACalculatorConstants.NEW_CONSTRUCTION))) {
				if ((subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.A_P))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.A_S))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.A_R))) {
					// sanctionFee = calculateConstantFee(paramMap, 15);
					if(isSparit)
						sanctionFee = calculateConstantFeeNewSparit(totalBuitUpArea, 7.50);
					else
					sanctionFee = calculateConstantFeeNew(totalBuitUpArea, 15);

				} else {
					// sanctionFee = calculateConstantFee(paramMap, 50);
					if(isSparit)
						sanctionFee = calculateConstantFeeNewSparit(totalBuitUpArea, 25.0);
					else
					sanctionFee = calculateConstantFeeNew(totalBuitUpArea, 50);
				}

			}

		}
		return sanctionFee;
	}

	private BigDecimal calculateConstantFeeNewSparit(Double totalBuitUpArea, double multiplicationFactor) {
		BigDecimal totalAmount = BigDecimal.ZERO;
		if (null != totalBuitUpArea) {
			totalAmount = (BigDecimal.valueOf(totalBuitUpArea).multiply(BigDecimal.valueOf(multiplicationFactor)))
					.setScale(2, BigDecimal.ROUND_UP);

		}
		return totalAmount;
	}


	/**
	 * @param paramMap
	 * @param estimates
	 * @return
	 */
	private BigDecimal calculateTotalScrutinyFee(Map<String, Object> paramMap, ArrayList<TaxHeadEstimate> estimates) {
		BigDecimal calculatedTotalScrutinyFee = BigDecimal.ZERO;
		//Boolean isSparit = checkUlbForSparit(paramMap);
		BigDecimal feeForDevelopmentOfLand = calculateFeeForDevelopmentOfLand(paramMap, estimates);
		BigDecimal feeForBuildingOperation = calculateFeeForBuildingOperation(paramMap, estimates);
		calculatedTotalScrutinyFee = (calculatedTotalScrutinyFee.add(feeForDevelopmentOfLand)
				.add(feeForBuildingOperation)).setScale(2, BigDecimal.ROUND_UP);
		return calculatedTotalScrutinyFee;
	}
	


	/**
	 * @param paramMap
	 * @param estimates
	 * @return
	 */
	private BigDecimal calculateFeeForDevelopmentOfLand(Map<String, Object> paramMap,
			ArrayList<TaxHeadEstimate> estimates) {
		BigDecimal feeForDevelopmentOfLand = BigDecimal.ZERO;
		String applicationType = null;
		String serviceType = null;
		String riskType = null;
		Double plotArea = null;
		Boolean isSparit = null;
		if (null != paramMap.get(BPACalculatorConstants.APPLICATION_TYPE)) {
			applicationType = (String) paramMap.get(BPACalculatorConstants.APPLICATION_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.SERVICE_TYPE)) {
			serviceType = (String) paramMap.get(BPACalculatorConstants.SERVICE_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.RISK_TYPE)) {
			riskType = (String) paramMap.get(BPACalculatorConstants.RISK_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.PLOT_AREA)) {
			plotArea = (Double) paramMap.get(BPACalculatorConstants.PLOT_AREA);
		}
		if (null != paramMap.get(BPACalculatorConstants.SPARIT_CHECK)) {
			isSparit = (Boolean) paramMap.get(BPACalculatorConstants.SPARIT_CHECK);
		}
		
		if(!BPACalculatorConstants.RISK_TYPE_LOW.equalsIgnoreCase(riskType)) {
			if ((StringUtils.hasText(applicationType)
					&& applicationType.equalsIgnoreCase(BPACalculatorConstants.BUILDING_PLAN_SCRUTINY))
					&& (StringUtils.hasText(serviceType)
							&& serviceType.equalsIgnoreCase(BPACalculatorConstants.NEW_CONSTRUCTION))) {
				if (null != plotArea) {
					paramMap.put(BPACalculatorConstants.AREA_TYPE, BPACalculatorConstants.AREA_TYPE_PLOT);
					// feeForDevelopmentOfLand = calculateConstantFee(paramMap, 5);

					if(isSparit) {
									feeForDevelopmentOfLand = calculateConstantFeeNewSparit(plotArea, 2.50);	
								}else {
					feeForDevelopmentOfLand = calculateConstantFeeNew(plotArea, 5);
								}
					paramMap.put(BPACalculatorConstants.AREA_TYPE, null);
				}
	
			}
	
			generateTaxHeadEstimate(estimates, feeForDevelopmentOfLand, BPACalculatorConstants.TAXHEAD_BPA_LAND_DEVELOPMENT_FEE, Category.FEE);
		}
		// System.out.println("FeeForDevelopmentOfLand:::::::::::" +
		// feeForDevelopmentOfLand);
		log.info("FeeForDevelopmentOfLand:::::::::::" + feeForDevelopmentOfLand);
		return feeForDevelopmentOfLand;

	}

	/**
	 * @param paramMap
	 * @param estimates
	 * @return
	 */
	private BigDecimal calculateFeeForBuildingOperation(Map<String, Object> paramMap,
			ArrayList<TaxHeadEstimate> estimates) {
		
		
		
		if(paramMap.get(BPACalculatorConstants.SERVICE_TYPE).equals(BPACalculatorConstants.ALTERATION)) {
			log.info("altertion application");
			return alterationCalculationService.calculateFeeForBuildingOperation(paramMap, estimates);
	}
		
		BigDecimal feeForBuildingOperation = BigDecimal.ZERO;
		BigDecimal feesForBuildingOperation = BigDecimal.ZERO;
		Double totalBuitUpArea = null;
		String occupancyType = null;
		List<Occupancy> occupancyies = null;
		totalBuitUpArea=getAreaParameterForBPFeesCalculation(paramMap);
		log.info("totalBuitUpArea outside:"+totalBuitUpArea);
//		if (null != paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE)) {
//			occupancyType = (String) paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE);
//		}
		if (null != paramMap.get(BPACalculatorConstants.OCCUPANCYLIST)) {
			occupancyies = (List<Occupancy>) paramMap.get(BPACalculatorConstants.OCCUPANCYLIST);
		}
		for(Occupancy occupancy : occupancyies) {
		totalBuitUpArea = occupancy.getBuiltUpArea();
		log.info("totalBuitUpArea inside:"+totalBuitUpArea);
		occupancyType = occupancy.getOccupancyCode();
		paramMap.put(BPACalculatorConstants.OCCUPANCY_TYPE, occupancyType);
		paramMap.put(BPACalculatorConstants.TOTAL_BUILTUP_AREA_EDCR,totalBuitUpArea);
		paramMap.put(BPACalculatorConstants.SUB_OCCUPANCY_TYPE,occupancy.getSubOccupancyCode());
		log.info("occupancy inside:"+occupancyType);
		log.info("suboccupancy inside:"+occupancy.getSubOccupancyCode());
		
		if (totalBuitUpArea != null) {
			if ((occupancyType.equalsIgnoreCase(BPACalculatorConstants.A))) {
				feeForBuildingOperation = calculateBuildingOperationFeeForResidentialOccupancy(paramMap);
			}
			if ((occupancyType.equalsIgnoreCase(BPACalculatorConstants.B))) {
				feeForBuildingOperation = calculateBuildingOperationFeeForCommercialOccupancy(paramMap);
			}
			if ((occupancyType.equalsIgnoreCase(BPACalculatorConstants.C))) {
				feeForBuildingOperation = calculateBuildingOperationFeeForPublicSemiPublicInstitutionalOccupancy(
						paramMap);
			}
			if ((occupancyType.equalsIgnoreCase(BPACalculatorConstants.D))) {
				feeForBuildingOperation = calculateBuildingOperationFeeForPublicUtilityOccupancy(paramMap);
			}
			if ((occupancyType.equalsIgnoreCase(BPACalculatorConstants.E))) {
				feeForBuildingOperation = calculateBuildingOperationFeeForIndustrialZoneOccupancy(paramMap);
			}
			if ((occupancyType.equalsIgnoreCase(BPACalculatorConstants.F))) {
				feeForBuildingOperation = calculateBuildingOperationFeeForEducationOccupancy(paramMap);
			}
			if ((occupancyType.equalsIgnoreCase(BPACalculatorConstants.G))) {
				feeForBuildingOperation = calculateBuildingOperationFeeForTransportationOccupancy(paramMap);
			}
			if ((occupancyType.equalsIgnoreCase(BPACalculatorConstants.H))) {
				feeForBuildingOperation = calculateBuildingOperationFeeForAgricultureOccupancy(paramMap);
			}

		}
		log.info("FeeFrBuildingOperation:::::::::::" + feeForBuildingOperation);
		//Summation of fees
		feesForBuildingOperation=feesForBuildingOperation.add(feeForBuildingOperation);
		log.info("FeeFromBuildingOperation:::::::::::" + feesForBuildingOperation);
		}
		generateTaxHeadEstimate(estimates, feesForBuildingOperation, BPACalculatorConstants.TAXHEAD_BPA_BUILDING_OPERATION_FEE, Category.FEE);
		// System.out.println("FeeForBuildingOperation:::::::::::" +
		// feeForBuildingOperation);
		log.info("FeeForBuildingOperation:::::::::::" + feesForBuildingOperation);
		return feesForBuildingOperation;
	
	}

	/**
	 * @param paramMap
	 * @return
	 */
	private BigDecimal calculateBuildingOperationFeeForAgricultureOccupancy(Map<String, Object> paramMap) {
		BigDecimal feeForBuildingOperation = BigDecimal.ZERO;
		String applicationType = null;
		String serviceType = null;
		String occupancyType = null;
		Double totalBuitUpArea = null;
		Boolean isSparit = null;
		if (null != paramMap.get(BPACalculatorConstants.APPLICATION_TYPE)) {
			applicationType = (String) paramMap.get(BPACalculatorConstants.APPLICATION_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.SERVICE_TYPE)) {
			serviceType = (String) paramMap.get(BPACalculatorConstants.SERVICE_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE)) {
			occupancyType = (String) paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.SPARIT_CHECK)) {
			isSparit = (Boolean) paramMap.get(BPACalculatorConstants.SPARIT_CHECK);
		}
		totalBuitUpArea=getAreaParameterForBPFeesCalculation(paramMap);
		if ((occupancyType.equalsIgnoreCase(BPACalculatorConstants.H))) {
			if ((StringUtils.hasText(applicationType)
					&& applicationType.equalsIgnoreCase(BPACalculatorConstants.BUILDING_PLAN_SCRUTINY))
					&& (StringUtils.hasText(serviceType)
							&& serviceType.equalsIgnoreCase(BPACalculatorConstants.NEW_CONSTRUCTION))) {
				if(isSparit)
					feeForBuildingOperation = calculateVariableFeeForSparitUlbs1(totalBuitUpArea);
				else
				feeForBuildingOperation = calculateVariableFee1(totalBuitUpArea);
			}
		}
		return feeForBuildingOperation;
	}

	/**
	 * @param paramMap
	 * @return
	 */
	private BigDecimal calculateBuildingOperationFeeForTransportationOccupancy(Map<String, Object> paramMap) {
		BigDecimal feeForBuildingOperation = BigDecimal.ZERO;
		String applicationType = null;
		String serviceType = null;
		String occupancyType = null;
		Double totalBuitUpArea = null;
		Boolean isSparit = null;
		if (null != paramMap.get(BPACalculatorConstants.APPLICATION_TYPE)) {
			applicationType = (String) paramMap.get(BPACalculatorConstants.APPLICATION_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.SERVICE_TYPE)) {
			serviceType = (String) paramMap.get(BPACalculatorConstants.SERVICE_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE)) {
			occupancyType = (String) paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.SPARIT_CHECK)) {
			isSparit = (Boolean) paramMap.get(BPACalculatorConstants.SPARIT_CHECK);
		}
		totalBuitUpArea=getAreaParameterForBPFeesCalculation(paramMap);
		if ((occupancyType.equalsIgnoreCase(BPACalculatorConstants.G))) {
			if ((StringUtils.hasText(applicationType)
					&& applicationType.equalsIgnoreCase(BPACalculatorConstants.BUILDING_PLAN_SCRUTINY))
					&& (StringUtils.hasText(serviceType)
							&& serviceType.equalsIgnoreCase(BPACalculatorConstants.NEW_CONSTRUCTION))) {
				// feeForBuildingOperation = calculateConstantFee(paramMap, 5);
				if(isSparit)
					feeForBuildingOperation = calculateConstantSparitFee(totalBuitUpArea);	
				else	
				feeForBuildingOperation = calculateConstantFeeNew(totalBuitUpArea, 5);
			}
		}
		return feeForBuildingOperation;
	}

	/**
	 * @param paramMap
	 * @return
	 */
	private BigDecimal calculateBuildingOperationFeeForEducationOccupancy(Map<String, Object> paramMap) {
		BigDecimal feeForBuildingOperation = BigDecimal.ZERO;
		String applicationType = null;
		String serviceType = null;
		String occupancyType = null;
		Double totalBuitUpArea = null;
		Boolean isSparit = null;
		if (null != paramMap.get(BPACalculatorConstants.APPLICATION_TYPE)) {
			applicationType = (String) paramMap.get(BPACalculatorConstants.APPLICATION_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.SERVICE_TYPE)) {
			serviceType = (String) paramMap.get(BPACalculatorConstants.SERVICE_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE)) {
			occupancyType = (String) paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.SPARIT_CHECK)) {
			isSparit = (Boolean) paramMap.get(BPACalculatorConstants.SPARIT_CHECK);
		}
		totalBuitUpArea=getAreaParameterForBPFeesCalculation(paramMap);
		if ((occupancyType.equalsIgnoreCase(BPACalculatorConstants.F))) {
			if ((StringUtils.hasText(applicationType)
					&& applicationType.equalsIgnoreCase(BPACalculatorConstants.BUILDING_PLAN_SCRUTINY))
					&& (StringUtils.hasText(serviceType)
							&& serviceType.equalsIgnoreCase(BPACalculatorConstants.NEW_CONSTRUCTION))) {
				// feeForBuildingOperation = calculateConstantFee(paramMap, 5);
				if(isSparit) {
					feeForBuildingOperation = calculateConstantSparitFee(totalBuitUpArea);
				}else {
				feeForBuildingOperation = calculateConstantFeeNew(totalBuitUpArea, 5);
				}
			}
		}
		return feeForBuildingOperation;
	}

	/**
	 * @param paramMap
	 * @return
	 */
	private BigDecimal calculateBuildingOperationFeeForIndustrialZoneOccupancy(Map<String, Object> paramMap) {
		BigDecimal feeForBuildingOperation = BigDecimal.ZERO;
		String applicationType = null;
		String serviceType = null;
		String occupancyType = null;
		Double totalBuitUpArea = null;
		Boolean isSparit = null;
		if (null != paramMap.get(BPACalculatorConstants.APPLICATION_TYPE)) {
			applicationType = (String) paramMap.get(BPACalculatorConstants.APPLICATION_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.SERVICE_TYPE)) {
			serviceType = (String) paramMap.get(BPACalculatorConstants.SERVICE_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE)) {
			occupancyType = (String) paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.SPARIT_CHECK)) {
			isSparit = (Boolean) paramMap.get(BPACalculatorConstants.SPARIT_CHECK);
		}
		
		totalBuitUpArea=getAreaParameterForBPFeesCalculation(paramMap);
		if ((occupancyType.equalsIgnoreCase(BPACalculatorConstants.E))) {
			if ((StringUtils.hasText(applicationType)
					&& applicationType.equalsIgnoreCase(BPACalculatorConstants.BUILDING_PLAN_SCRUTINY))
					&& (StringUtils.hasText(serviceType)
							&& serviceType.equalsIgnoreCase(BPACalculatorConstants.NEW_CONSTRUCTION))) {
				if(isSparit)
					feeForBuildingOperation = calculateVariableFeeForSparitUlbs3(totalBuitUpArea);
				else
				feeForBuildingOperation = calculateVariableFee3(totalBuitUpArea);
			}
		}
		return feeForBuildingOperation;
	}

	/**
	 * @param paramMap
	 * @return
	 */
	private BigDecimal calculateBuildingOperationFeeForPublicUtilityOccupancy(Map<String, Object> paramMap) {
		BigDecimal feeForBuildingOperation = BigDecimal.ZERO;
		String applicationType = null;
		String serviceType = null;
		String occupancyType = null;
		Double totalBuitUpArea = null;
		Boolean isSparit = null;
		if (null != paramMap.get(BPACalculatorConstants.APPLICATION_TYPE)) {
			applicationType = (String) paramMap.get(BPACalculatorConstants.APPLICATION_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.SERVICE_TYPE)) {
			serviceType = (String) paramMap.get(BPACalculatorConstants.SERVICE_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE)) {
			occupancyType = (String) paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.SPARIT_CHECK)) {
			isSparit = (Boolean) paramMap.get(BPACalculatorConstants.SPARIT_CHECK);
		}
		totalBuitUpArea=getAreaParameterForBPFeesCalculation(paramMap);
		if ((occupancyType.equalsIgnoreCase(BPACalculatorConstants.D))) {
			if ((StringUtils.hasText(applicationType)
					&& applicationType.equalsIgnoreCase(BPACalculatorConstants.BUILDING_PLAN_SCRUTINY))
					&& (StringUtils.hasText(serviceType)
							&& serviceType.equalsIgnoreCase(BPACalculatorConstants.NEW_CONSTRUCTION))) {
				// feeForBuildingOperation = calculateConstantFee(paramMap, 5);
				if(isSparit) {
					feeForBuildingOperation = calculateConstantSparitFee(totalBuitUpArea);
				}else {
				feeForBuildingOperation = calculateConstantFeeNew(totalBuitUpArea, 5);
				}

			}
		}
		return feeForBuildingOperation;
	}

	/**
	 * @param paramMap
	 * @return
	 */
	private BigDecimal calculateBuildingOperationFeeForPublicSemiPublicInstitutionalOccupancy(
			Map<String, Object> paramMap) {
		BigDecimal feeForBuildingOperation = BigDecimal.ZERO;
		String applicationType = null;
		String serviceType = null;
		String occupancyType = null;
		String subOccupancyType = null;
		Double totalBuitUpArea = null;
		Boolean isSparit = null;
		if (null != paramMap.get(BPACalculatorConstants.APPLICATION_TYPE)) {
			applicationType = (String) paramMap.get(BPACalculatorConstants.APPLICATION_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.SERVICE_TYPE)) {
			serviceType = (String) paramMap.get(BPACalculatorConstants.SERVICE_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE)) {
			occupancyType = (String) paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.SUB_OCCUPANCY_TYPE)) {
			subOccupancyType = (String) paramMap.get(BPACalculatorConstants.SUB_OCCUPANCY_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.SPARIT_CHECK)) {
			isSparit = (Boolean) paramMap.get(BPACalculatorConstants.SPARIT_CHECK);
		}
		totalBuitUpArea=getAreaParameterForBPFeesCalculation(paramMap);
		if (((occupancyType.equalsIgnoreCase(BPACalculatorConstants.C))) && (StringUtils.hasText(subOccupancyType))) {
			if ((StringUtils.hasText(applicationType)
					&& applicationType.equalsIgnoreCase(BPACalculatorConstants.BUILDING_PLAN_SCRUTINY))
					&& (StringUtils.hasText(serviceType)
							&& serviceType.equalsIgnoreCase(BPACalculatorConstants.NEW_CONSTRUCTION))) {
				if ((subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_A))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_B))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_C))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_CL))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_MP))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_CH))) {
					if(isSparit)
						feeForBuildingOperation = calculateVariableFeeForSparitUlbs2(totalBuitUpArea);
					else
					feeForBuildingOperation = calculateVariableFee2(totalBuitUpArea);

				} else if ((subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_O))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_OAH))) {
					// feeForBuildingOperation = calculateConstantFee(paramMap, 5);
					if(isSparit) {
						feeForBuildingOperation = calculateConstantSparitFee(totalBuitUpArea);
					}else {
					feeForBuildingOperation = calculateConstantFeeNew(totalBuitUpArea, 5);
					}

				} else if ((subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_SC))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_C1H))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_C2H))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_SCC))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_CC))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_EC))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_G))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_MH))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_ML))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_M))) {
					if(isSparit)
						feeForBuildingOperation = calculateVariableFeeForSparitUlbs2(totalBuitUpArea);
					else
					feeForBuildingOperation = calculateVariableFee2(totalBuitUpArea);

				} else if ((subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_PW))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_PL))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_REB))) {
					// feeForBuildingOperation = calculateConstantFee(paramMap, 5);
					if(isSparit) 
						feeForBuildingOperation = calculateConstantSparitFee(totalBuitUpArea);
					else
					feeForBuildingOperation = calculateConstantFeeNew(totalBuitUpArea, 5);

				} else if ((subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_SPC))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_S))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_T))) {
					if(isSparit)
						feeForBuildingOperation = calculateVariableFeeForSparitUlbs2(totalBuitUpArea);	
					else
					feeForBuildingOperation = calculateVariableFee2(totalBuitUpArea);

				} else if ((subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_AB))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_GO))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_LSGO))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_P))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_RB))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_SWC))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_CI))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_D))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_YC))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_DC))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_GSGH))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_RT))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_HC))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_H))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_L))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_MTH))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_MB))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_NH))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_PLY))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_RC))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_VHAB))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_RTI))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_PS))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_FS))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_J))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_PO))) {

					// feeForBuildingOperation = calculateConstantFee(paramMap, 5);
					if(isSparit) 
						feeForBuildingOperation = calculateConstantSparitFee(totalBuitUpArea);
					else
					feeForBuildingOperation = calculateConstantFeeNew(totalBuitUpArea, 5);

				}
			}

		}
		return feeForBuildingOperation;
	}

	/**
	 * @param paramMap
	 * @return
	 */
	private BigDecimal calculateBuildingOperationFeeForCommercialOccupancy(Map<String, Object> paramMap) {
		BigDecimal feeForBuildingOperation = BigDecimal.ZERO;
		String applicationType = null;
		String serviceType = null;
		String occupancyType = null;
		Double totalBuitUpArea = null;
		Boolean isSparit = null;
		if (null != paramMap.get(BPACalculatorConstants.APPLICATION_TYPE)) {
			applicationType = (String) paramMap.get(BPACalculatorConstants.APPLICATION_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.SERVICE_TYPE)) {
			serviceType = (String) paramMap.get(BPACalculatorConstants.SERVICE_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE)) {
			occupancyType = (String) paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.SPARIT_CHECK)) {
			isSparit = (Boolean) paramMap.get(BPACalculatorConstants.SPARIT_CHECK);
		}
		totalBuitUpArea=getAreaParameterForBPFeesCalculation(paramMap);
		if ((occupancyType.equalsIgnoreCase(BPACalculatorConstants.B))) {
			if ((StringUtils.hasText(applicationType)
					&& applicationType.equalsIgnoreCase(BPACalculatorConstants.BUILDING_PLAN_SCRUTINY))
					&& (StringUtils.hasText(serviceType)
							&& serviceType.equalsIgnoreCase(BPACalculatorConstants.NEW_CONSTRUCTION))) {
				
				if(isSparit) {
					feeForBuildingOperation = calculateVariableFeeForSparitUlbs2(totalBuitUpArea);
				}else {
				feeForBuildingOperation = calculateVariableFee2(totalBuitUpArea);
				}
			}

		}
		return feeForBuildingOperation;
	}

	/**
	 * @param paramMap
	 * @return
	 */
	private BigDecimal calculateBuildingOperationFeeForResidentialOccupancy(Map<String, Object> paramMap) {
		BigDecimal feeForBuildingOperation = BigDecimal.ZERO;
		String applicationType = null;
		String serviceType = null;
		String occupancyType = null;
		Double totalBuitUpArea = null;
		Boolean isSparit = null;
		if (null != paramMap.get(BPACalculatorConstants.APPLICATION_TYPE)) {
			applicationType = (String) paramMap.get(BPACalculatorConstants.APPLICATION_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.SERVICE_TYPE)) {
			serviceType = (String) paramMap.get(BPACalculatorConstants.SERVICE_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE)) {
			occupancyType = (String) paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.SPARIT_CHECK)) {
			isSparit = (Boolean) paramMap.get(BPACalculatorConstants.SPARIT_CHECK);
			
		}
		totalBuitUpArea=getAreaParameterForBPFeesCalculation(paramMap);
		if ((occupancyType.equalsIgnoreCase(BPACalculatorConstants.A))) {
			if ((StringUtils.hasText(applicationType)
					&& applicationType.equalsIgnoreCase(BPACalculatorConstants.BUILDING_PLAN_SCRUTINY))
					&& (StringUtils.hasText(serviceType)
							&& serviceType.equalsIgnoreCase(BPACalculatorConstants.NEW_CONSTRUCTION))) {
				if(isSparit) {
					feeForBuildingOperation = calculateVariableFeeForSparitUlbs1(totalBuitUpArea);
					log.info("inside sparit");
				}else {
				feeForBuildingOperation = calculateVariableFee1(totalBuitUpArea);
				}
			}

		}
		return feeForBuildingOperation;
	}

	
	private BigDecimal calculateVariableFeeForSparitUlbs1(Double totalBuitUpArea) {
		BigDecimal amount = BigDecimal.ZERO;
		
		
		if (null != totalBuitUpArea) {
			if (totalBuitUpArea <= 100) {
				amount = ONE_HUNDRED_TWENTYFIVE;
			} else if (totalBuitUpArea <= 300) {
				amount = (ONE_HUNDRED_TWENTYFIVE
						.add(SEVEN_POINT_FIVE.multiply(BigDecimal.valueOf(totalBuitUpArea).subtract(HUNDRED)))).setScale(2,
								BigDecimal.ROUND_UP);
			} else if (totalBuitUpArea > 300) {
				amount = (ONE_HUNDRED_TWENTYFIVE.add(SEVEN_POINT_FIVE.multiply(TWO_HUNDRED))
						.add(FIVE.multiply(BigDecimal.valueOf(totalBuitUpArea).subtract(THREE_HUNDRED)))).setScale(2,
								BigDecimal.ROUND_UP);
			}

		}
		return amount;
}
	
	
	private BigDecimal calculateVariableFeeForSparitUlbs2(Double totalBuitUpArea) {
		//totalBuitUpArea =122.00;
		BigDecimal amount = BigDecimal.ZERO;
		if (null != totalBuitUpArea) {
			if (totalBuitUpArea <= 20) {
				amount = TWO_HUNDRED_FIFTY;
			} else if (totalBuitUpArea <= 50) {
				amount = (TWO_HUNDRED_FIFTY.add(TWENTY_FIVE.multiply(BigDecimal.valueOf(totalBuitUpArea).subtract(TWENTY))))
						.setScale(2, BigDecimal.ROUND_UP);
			} else if (totalBuitUpArea > 50) {
				amount = (TWO_HUNDRED_FIFTY.add(TWENTY_FIVE.multiply(THIRTY))
						.add(TWENTY.multiply(BigDecimal.valueOf(totalBuitUpArea).subtract(FIFTY)))).setScale(2,
								BigDecimal.ROUND_UP);
			}

		}
		//System.out.println("sparitcheck:"+amount);
		return amount;

	}
	
	private BigDecimal calculateVariableFeeForSparitUlbs3(Double totalBuitUpArea) {
		//totalBuitUpArea =122.00;
		BigDecimal amount = BigDecimal.ZERO;
		if (null != totalBuitUpArea) {
			if (totalBuitUpArea <= 100) {
				amount = SEVEN_HUNDRED_FIFTY;
			} else if (totalBuitUpArea <= 300) {
				amount = (SEVEN_HUNDRED_FIFTY
						.add(TWELVE_POINT_FIVE.multiply(BigDecimal.valueOf(totalBuitUpArea).subtract(HUNDRED)))).setScale(2,
								BigDecimal.ROUND_UP);
			} else if (totalBuitUpArea > 300) {
				amount = (SEVEN_HUNDRED_FIFTY.add(TWELVE_POINT_FIVE.multiply(TWO_HUNDRED))
						.add(SEVEN_POINT_FIVE.multiply(BigDecimal.valueOf(totalBuitUpArea).subtract(THREE_HUNDRED)))).setScale(2,
								BigDecimal.ROUND_UP);
			}

		}
				//System.out.println("sparitcheck:"+amount);
				return amount;
		
	}
	
private BigDecimal calculateConstantSparitFee(Double totalBuitUpArea) {
		
		BigDecimal amount = BigDecimal.ZERO;
		amount = (BigDecimal.valueOf(totalBuitUpArea).multiply(new BigDecimal("2.50")))
				.setScale(2, BigDecimal.ROUND_UP);
		
		return amount;
		
	}
	/**
	 * @param totalBuitUpArea
	 * @return
	 */
	private BigDecimal calculateVariableFee1(Double totalBuitUpArea) {
		BigDecimal amount = BigDecimal.ZERO;
		
		if (null != totalBuitUpArea) {
			if (totalBuitUpArea <= 100) {
				amount = TWO_HUNDRED_FIFTY;
			} else if (totalBuitUpArea <= 300) {
				amount = (TWO_HUNDRED_FIFTY
						.add(FIFTEEN.multiply(BigDecimal.valueOf(totalBuitUpArea).subtract(HUNDRED)))).setScale(2,
								BigDecimal.ROUND_UP);
			} else if (totalBuitUpArea > 300) {
				amount = (TWO_HUNDRED_FIFTY.add(FIFTEEN.multiply(TWO_HUNDRED))
						.add(TEN.multiply(BigDecimal.valueOf(totalBuitUpArea).subtract(THREE_HUNDRED)))).setScale(2,
								BigDecimal.ROUND_UP);
			}

		}
		return amount;
	}

	/**
	 * @param totalBuitUpArea
	 * @return
	 */
	private BigDecimal calculateVariableFee2(Double totalBuitUpArea) {
		BigDecimal amount = BigDecimal.ZERO;
		if (null != totalBuitUpArea) {
			if (totalBuitUpArea <= 20) {
				amount = FIVE_HUNDRED;
			} else if (totalBuitUpArea <= 50) {
				amount = (FIVE_HUNDRED.add(FIFTY.multiply(BigDecimal.valueOf(totalBuitUpArea).subtract(TWENTY))))
						.setScale(2, BigDecimal.ROUND_UP);
			} else if (totalBuitUpArea > 50) {
				amount = (FIVE_HUNDRED.add(FIFTY.multiply(THIRTY))
						.add(TWENTY.multiply(BigDecimal.valueOf(totalBuitUpArea).subtract(FIFTY)))).setScale(2,
								BigDecimal.ROUND_UP);
			}

		}
		return amount;
	}

	/**
	 * @param totalBuitUpArea
	 * @return
	 */
	private BigDecimal calculateVariableFee3(Double totalBuitUpArea) {
		BigDecimal amount = BigDecimal.ZERO;
		if (null != totalBuitUpArea) {
			if (totalBuitUpArea <= 100) {
				amount = FIFTEEN_HUNDRED;
			} else if (totalBuitUpArea <= 300) {
				amount = (FIFTEEN_HUNDRED
						.add(TWENTY_FIVE.multiply(BigDecimal.valueOf(totalBuitUpArea).subtract(HUNDRED)))).setScale(2,
								BigDecimal.ROUND_UP);
			} else if (totalBuitUpArea > 300) {
				amount = (FIFTEEN_HUNDRED.add(TWENTY_FIVE.multiply(TWO_HUNDRED))
						.add(FIFTEEN.multiply(BigDecimal.valueOf(totalBuitUpArea).subtract(THREE_HUNDRED)))).setScale(2,
								BigDecimal.ROUND_UP);
			}

		}
		return amount;
	}

	/**
	 * @param paramMap
	 * @param multiplicationFactor
	 * @return
	 */
	@SuppressWarnings("unused")
	private BigDecimal calculateConstantFee(Map<String, Object> paramMap, int multiplicationFactor) {
		BigDecimal totalAmount = BigDecimal.ZERO;
		Double plotArea = null;
		Double totalBuitUpArea = null;
		if (null != paramMap.get(BPACalculatorConstants.PLOT_AREA)) {
			plotArea = (Double) paramMap.get(BPACalculatorConstants.PLOT_AREA);
		}
		if (null != paramMap.get(BPACalculatorConstants.TOTAL_FLOOR_AREA)) {
			totalBuitUpArea = (Double) paramMap.get(BPACalculatorConstants.TOTAL_FLOOR_AREA);

		}
		if ((null != paramMap.get(BPACalculatorConstants.AREA_TYPE))
				&& (paramMap.get(BPACalculatorConstants.AREA_TYPE).equals(BPACalculatorConstants.AREA_TYPE_PLOT))) {

			totalAmount = (BigDecimal.valueOf(plotArea).multiply(BigDecimal.valueOf(multiplicationFactor))).setScale(2,
					BigDecimal.ROUND_UP);

		} else {
			totalAmount = (BigDecimal.valueOf(totalBuitUpArea).multiply(BigDecimal.valueOf(multiplicationFactor)))
					.setScale(2, BigDecimal.ROUND_UP);
		}

		return totalAmount;
	}

	/**
	 * @param effectiveArea
	 * @param multiplicationFactor
	 * @return
	 */
	private BigDecimal calculateConstantFeeNew(Double effectiveArea, int multiplicationFactor) {
		BigDecimal totalAmount = BigDecimal.ZERO;
		if (null != effectiveArea) {
			totalAmount = (BigDecimal.valueOf(effectiveArea).multiply(BigDecimal.valueOf(multiplicationFactor)))
					.setScale(2, BigDecimal.ROUND_UP);

		}
		return totalAmount;
	}

	private void generateTaxHeadEstimate(ArrayList<TaxHeadEstimate> estimates, BigDecimal feeAmount, String taxHeadCode,
			Category category) {
		TaxHeadEstimate estimate = new TaxHeadEstimate();
		estimate.setEstimateAmount(feeAmount.setScale(0, BigDecimal.ROUND_UP));
		estimate.setCategory(category);
		estimate.setTaxHeadCode(taxHeadCode);
		estimates.add(estimate);
	}
	
	private Double getAreaParameterForBPFeesCalculation(Map<String, Object> paramMap) {
		//String applicableAreaParameterName = BPACalculatorConstants.TOTAL_FLOOR_AREA;
		String applicableAreaParameterName = BPACalculatorConstants.TOTAL_BUILTUP_AREA_EDCR;
		log.info(applicableAreaParameterName);
		return null != paramMap.get(applicableAreaParameterName)?(Double) paramMap.get(applicableAreaParameterName):null;
	}
	
	private Double getDeviationBUAForOCFeesCalculation(Map<String, Object> paramMap) {
		String applicableAreaParameterName = BPACalculatorConstants.DEVIATION_BUILTUP_AREA;
		return null != paramMap.get(applicableAreaParameterName)?(Double) paramMap.get(applicableAreaParameterName):null;
	}

	/*
	 * public BigDecimal calculateTotalFeeAmountDuplicate(Map<String, Object>
	 * paramMap) { return calculateTotalFeeAmount(paramMap);
	 * 
	 * }
	 */
}
