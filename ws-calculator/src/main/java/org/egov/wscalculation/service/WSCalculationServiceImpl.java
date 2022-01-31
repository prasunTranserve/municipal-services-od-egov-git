package org.egov.wscalculation.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.egov.common.contract.request.RequestInfo;
import org.egov.common.contract.request.User;
import org.egov.mdms.model.MdmsCriteriaReq;
import org.egov.tracer.model.CustomException;
import org.egov.wscalculation.config.WSCalculationConfiguration;
import org.egov.wscalculation.constants.WSCalculationConstant;
import org.egov.wscalculation.repository.ServiceRequestRepository;
import org.egov.wscalculation.repository.WSCalculationDao;
import org.egov.wscalculation.util.CalculatorUtil;
import org.egov.wscalculation.web.models.AdhocTaxReq;
import org.egov.wscalculation.web.models.BillSchedulerCriteria;
import org.egov.wscalculation.web.models.Calculation;
import org.egov.wscalculation.web.models.CalculationCriteria;
import org.egov.wscalculation.web.models.CalculationReq;
import org.egov.wscalculation.web.models.DemandWard;
import org.egov.wscalculation.web.models.TaxHeadCategory;
import org.egov.wscalculation.web.models.TaxHeadEstimate;
import org.egov.wscalculation.web.models.TaxHeadMaster;
import org.egov.wscalculation.web.models.WaterConnection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.jayway.jsonpath.JsonPath;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class WSCalculationServiceImpl implements WSCalculationService {

	@Autowired
	private PayService payService;

	@Autowired
	private EstimationService estimationService;
	
	@Autowired
	private CalculatorUtil calculatorUtil;
	
	@Autowired
	private DemandService demandService;
	
	@Autowired
	private MasterDataService masterDataService; 

	@Autowired
	private WSCalculationDao wSCalculationDao;
	
	@Autowired
	private ServiceRequestRepository repository;
	
	@Autowired
	private WSCalculationConfiguration wsCalculationConfiguration;

	/**
	 * Get CalculationReq and Calculate the Tax Head on Water Charge And Estimation Charge
	 */
	public List<Calculation> getCalculation(CalculationReq request) {
		List<Calculation> calculations;

		Map<String, Object> masterMap;
		if (request.getIsReconnectionCalculation()) {
			masterMap = masterDataService.loadMasterData(request.getRequestInfo(),
					request.getCalculationCriteria().get(0).getTenantId());
			calculations = getReconnectionFeeCalculation(request, masterMap);
		} else if (request.getIsOwnershipChangeCalculation()) {
			masterMap = masterDataService.loadMasterData(request.getRequestInfo(),
					request.getCalculationCriteria().get(0).getTenantId());
			calculations = getOwnershipChangeFeeCalculation(request, masterMap);
		} else if (request.getIsconnectionCalculation()) {
			//Calculate and create demand for connection
			masterMap = masterDataService.loadMasterData(request.getRequestInfo(),
					request.getCalculationCriteria().get(0).getTenantId());
			masterDataService.loadMeterReadingMasterData(request.getRequestInfo(),
					request.getCalculationCriteria().get(0).getTenantId(), masterMap);
			calculations = getCalculations(request, masterMap);
		} else {
			//Calculate and create demand for application
			masterMap = masterDataService.loadExemptionMaster(request.getRequestInfo(),
					request.getCalculationCriteria().get(0).getTenantId());
			calculations = getFeeCalculation(request, masterMap);
		}
		demandService.generateDemand(request.getRequestInfo(), calculations, masterMap, request.getIsconnectionCalculation());
		unsetWaterConnection(calculations);
		return calculations;
	}

	/**
	 * 
	 * 
	 * @param request - Calculation Request Object
	 * @return List of calculation.
	 */
	public List<Calculation> bulkDemandGeneration(CalculationReq request, Map<String, Object> masterMap) {
		List<Calculation> calculations = getCalculations(request, masterMap);
		demandService.generateDemand(request.getRequestInfo(), calculations, masterMap, true);
		return calculations;
	}

	/**
	 * 
	 * @param request - Calculation Request Object
	 * @return list of calculation based on request
	 */
	public List<Calculation> getEstimation(CalculationReq request) {
		Map<String, Object> masterData = masterDataService.loadExemptionMaster(request.getRequestInfo(),
				request.getCalculationCriteria().get(0).getTenantId());
		List<Calculation> calculations = getFeeCalculation(request, masterData);
		unsetWaterConnection(calculations);
		return calculations;
	}
	/**
	 * It will take calculation and return calculation with tax head code 
	 * 
	 * @param requestInfo Request Info Object
	 * @param criteria Calculation criteria on meter charge
	 * @param estimatesAndBillingSlabs Billing Slabs
	 * @param masterMap Master MDMS Data
	 * @return Calculation With Tax head
	 */
	public Calculation getCalculation(RequestInfo requestInfo, CalculationCriteria criteria,
			Map<String, List> estimatesAndBillingSlabs, Map<String, Object> masterMap, boolean isConnectionFee) {

		@SuppressWarnings("unchecked")
		List<TaxHeadEstimate> estimates = estimatesAndBillingSlabs.get("estimates");
		@SuppressWarnings("unchecked")
		List<String> billingSlabIds = estimatesAndBillingSlabs.get("billingSlabIds");
		WaterConnection waterConnection = criteria.getWaterConnection();
		// Property property = wSCalculationUtil.getProperty(
		// 		WaterConnectionRequest.builder().waterConnection(waterConnection).requestInfo(requestInfo).build());
		
		String tenantId = null != waterConnection.getTenantId() ? waterConnection.getTenantId() : criteria.getTenantId();

		@SuppressWarnings("unchecked")
		Map<String, TaxHeadCategory> taxHeadCategoryMap = ((List<TaxHeadMaster>) masterMap
				.get(WSCalculationConstant.TAXHEADMASTER_MASTER_KEY)).stream()
						.collect(Collectors.toMap(TaxHeadMaster::getCode, TaxHeadMaster::getCategory, (OldValue, NewValue) -> NewValue));

		BigDecimal taxAmt = BigDecimal.ZERO;
		BigDecimal waterCharge = BigDecimal.ZERO;
		BigDecimal penalty = BigDecimal.ZERO;
		BigDecimal exemption = BigDecimal.ZERO;
		BigDecimal rebate = BigDecimal.ZERO;
		BigDecimal fee = BigDecimal.ZERO;

		for (TaxHeadEstimate estimate : estimates) {

			TaxHeadCategory category = taxHeadCategoryMap.get(estimate.getTaxHeadCode());
			estimate.setCategory(category);

			switch (category) {

			case CHARGES:
				waterCharge = waterCharge.add(estimate.getEstimateAmount());
				break;

			case PENALTY:
				penalty = penalty.add(estimate.getEstimateAmount());
				break;

			case REBATE:
				rebate = rebate.add(estimate.getEstimateAmount());
				break;

			case EXEMPTION:
				exemption = exemption.add(estimate.getEstimateAmount());
				break;
			case FEE:
				fee = fee.add(estimate.getEstimateAmount());
				break;
			default:
				taxAmt = taxAmt.add(estimate.getEstimateAmount());
				break;
			}
		}
		TaxHeadEstimate decimalEstimate = payService.roundOfDecimals(taxAmt.add(penalty).add(waterCharge).add(fee),
				rebate.add(exemption), isConnectionFee);
		if (null != decimalEstimate) {
			decimalEstimate.setCategory(taxHeadCategoryMap.get(decimalEstimate.getTaxHeadCode()));
			estimates.add(decimalEstimate);
			if (decimalEstimate.getEstimateAmount().compareTo(BigDecimal.ZERO) >= 0)
				taxAmt = taxAmt.add(decimalEstimate.getEstimateAmount());
			else
				rebate = rebate.add(decimalEstimate.getEstimateAmount());
		}

		BigDecimal totalAmount = taxAmt.add(penalty).add(rebate).add(exemption).add(waterCharge).add(fee);
		return Calculation.builder().totalAmount(totalAmount).taxAmount(taxAmt).penalty(penalty).exemption(exemption)
				.charge(waterCharge).fee(fee).waterConnection(waterConnection).rebate(rebate).tenantId(tenantId)
				.taxHeadEstimates(estimates).billingSlabIds(billingSlabIds).connectionNo(criteria.getConnectionNo()).applicationNO(criteria.getApplicationNo())
				.build();
	}
	
	/**
	 * 
	 * @param request would be calculations request
	 * @param masterMap master data
	 * @return all calculations including water charge and taxhead on that
	 */
	List<Calculation> getCalculations(CalculationReq request, Map<String, Object> masterMap) {
		List<Calculation> calculations = new ArrayList<>(request.getCalculationCriteria().size());
		for (CalculationCriteria criteria : request.getCalculationCriteria()) {
			Map<String, List> estimationMap = estimationService.getEstimationMap(criteria, request.getRequestInfo(),
					masterMap);
//			ArrayList<?> billingFrequencyMap = (ArrayList<?>) masterMap
//					.get(WSCalculationConstant.Billing_Period_Master);
//			masterDataService.enrichBillingPeriod(criteria, billingFrequencyMap, masterMap);
			Calculation calculation = getCalculation(request.getRequestInfo(), criteria, estimationMap, masterMap, true);
			calculations.add(calculation);
		}
		return calculations;
	}


	@Override
	public void jobScheduler() {
		// TODO Auto-generated method stub
		ArrayList<String> tenantIds = wSCalculationDao.searchTenantIds();

		for (String tenantId : tenantIds) {
			RequestInfo requestInfo = new RequestInfo();
			User user = new User();
			user.setTenantId(tenantId);
			requestInfo.setUserInfo(user);
			String jsonPath = WSCalculationConstant.JSONPATH_ROOT_FOR_BilingPeriod;
			MdmsCriteriaReq mdmsCriteriaReq = calculatorUtil.getBillingFrequency(requestInfo, tenantId);
			StringBuilder url = calculatorUtil.getMdmsSearchUrl();
			Object res = repository.fetchResult(url, mdmsCriteriaReq);
			if (res == null) {
				throw new CustomException("MDMS_ERROR_FOR_BILLING_FREQUENCY",
						"ERROR IN FETCHING THE BILLING FREQUENCY");
			}
			ArrayList<?> mdmsResponse = JsonPath.read(res, jsonPath);
			getBillingPeriod(mdmsResponse, requestInfo, tenantId);
		}
	}

	@SuppressWarnings("unchecked")
	public void getBillingPeriod(ArrayList<?> mdmsResponse, RequestInfo requestInfo, String tenantId) {
		log.info("Billing Frequency Map" + mdmsResponse.toString());
		Map<String, Object> master = (Map<String, Object>) mdmsResponse.get(0);
		LocalDateTime demandStartingDate = LocalDateTime.now();
		Long demandGenerateDateMillis = (Long) master.get(WSCalculationConstant.Demand_Generate_Date_String);

		String connectionType = "Non-metred";
		// the value 86400 is wrong as to convert millis to days the millis need to divide by 86400000
		if (demandStartingDate.getDayOfMonth() == (demandGenerateDateMillis) / 86400) {

//			ArrayList<String> connectionNos = wSCalculationDao.searchConnectionNos(connectionType, tenantId);
			ArrayList<WaterConnection> connections = wSCalculationDao.searchConnectionNos(connectionType, tenantId);
			List<String> connectionNos = connections.stream().map(wc -> wc.getConnectionNo()).distinct().collect(Collectors.toList());
			for (String connectionNo : connectionNos) {

				CalculationReq calculationReq = new CalculationReq();
				CalculationCriteria calculationCriteria = new CalculationCriteria();
				calculationCriteria.setTenantId(tenantId);
				calculationCriteria.setConnectionNo(connectionNo);

				List<CalculationCriteria> calculationCriteriaList = new ArrayList<>();
				calculationCriteriaList.add(calculationCriteria);

				calculationReq.setRequestInfo(requestInfo);
				calculationReq.setCalculationCriteria(calculationCriteriaList);
				calculationReq.setIsconnectionCalculation(true);
				getCalculation(calculationReq);

			}
		}
	}

	/**
	 * Generate Demand Based on Time (Monthly, Quarterly, Yearly)
	 */
	public void generateDemandBasedOnTimePeriod(RequestInfo requestInfo) {
		DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
		LocalDateTime date = LocalDateTime.now();
		log.info("Time schedule start for water demand generation on : " + date.format(dateTimeFormatter));
		List<String> tenantIds = new ArrayList<>();
		if(StringUtils.hasText(wsCalculationConfiguration.getSchedulerTenants()) && wsCalculationConfiguration.getSchedulerTenants().trim().equalsIgnoreCase("ALL")) {
			log.info("Processing for all tenants");
			tenantIds = wSCalculationDao.getTenantId();
		} else {
			String tenants = wsCalculationConfiguration.getSchedulerTenants();
			log.info("Processing for specific tenants: " + tenants);
			if(StringUtils.hasText(tenants)) {
				tenantIds = Arrays.asList(tenants.trim().split(","));
			}
		}
		
		if(StringUtils.hasText(wsCalculationConfiguration.getSkipSchedulerTenants()) && !wsCalculationConfiguration.getSkipSchedulerTenants().trim().equalsIgnoreCase("NONE")) {
			log.info("Skip tenants: " + wsCalculationConfiguration.getSkipSchedulerTenants());
			List<String> skipTenants = Arrays.asList(wsCalculationConfiguration.getSkipSchedulerTenants().trim().split(","));
			tenantIds = tenantIds.stream().filter(tenant -> !skipTenants.contains(tenant)).collect(Collectors.toList());
		}
		
		if (tenantIds.isEmpty())
			return;
		log.info("Effective processing tenant Ids : " + tenantIds.toString());
		tenantIds.forEach(tenantId -> {
			tenantId = tenantId.trim();
			demandService.generateDemandForTenantId(tenantId, requestInfo, null);
		});
	}
	
	/**
	 * 
	 * @param request - Calculation Request Object
	 * @param masterMap - Master MDMS Data
	 * @return list of calculation based on estimation criteria
	 */
	List<Calculation> getFeeCalculation(CalculationReq request, Map<String, Object> masterMap) {
		List<Calculation> calculations = new ArrayList<>(request.getCalculationCriteria().size());
		for (CalculationCriteria criteria : request.getCalculationCriteria()) {
			Map<String, List> estimationMap = estimationService.getFeeEstimation(criteria, request.getRequestInfo(),
					masterMap);
			masterDataService.enrichBillingPeriodForFee(masterMap);
			Calculation calculation = getCalculation(request.getRequestInfo(), criteria, estimationMap, masterMap, false);
			calculations.add(calculation);
		}
		return calculations;
	}
	
	public void unsetWaterConnection(List<Calculation> calculation) {
		calculation.forEach(cal -> cal.setWaterConnection(null));
	}
	
	/**
	 * Add adhoc tax to demand
	 * @param adhocTaxReq - Adhox Tax Request Object
	 * @return List of Calculation
	 */
	public List<Calculation> applyAdhocTax(AdhocTaxReq adhocTaxReq) {
		List<TaxHeadEstimate> estimates = new ArrayList<>();
		if (!(adhocTaxReq.getAdhocpenalty().compareTo(BigDecimal.ZERO) == 0))
			estimates.add(TaxHeadEstimate.builder().taxHeadCode(WSCalculationConstant.WS_TIME_ADHOC_PENALTY)
					.estimateAmount(adhocTaxReq.getAdhocpenalty().setScale(2, 2)).build());
		if (!(adhocTaxReq.getAdhocrebate().compareTo(BigDecimal.ZERO) == 0))
			estimates.add(TaxHeadEstimate.builder().taxHeadCode(WSCalculationConstant.WS_TIME_ADHOC_REBATE)
					.estimateAmount(adhocTaxReq.getAdhocrebate().setScale(2, 2).negate()).build());
		Calculation calculation = Calculation.builder()
				.tenantId(adhocTaxReq.getRequestInfo().getUserInfo().getTenantId())
				.applicationNO(adhocTaxReq.getDemandId()).taxHeadEstimates(estimates).build();
		List<Calculation> calculations = Collections.singletonList(calculation);
		return demandService.updateDemandForAdhocTax(adhocTaxReq.getRequestInfo(), calculations);
	}
	
	
	private List<Calculation> getOwnershipChangeFeeCalculation(CalculationReq request, Map<String, Object> masterMap) {
		List<Calculation> calculations = new ArrayList<>(request.getCalculationCriteria().size());
		for (CalculationCriteria criteria : request.getCalculationCriteria()) {
			Map<String, List> estimationMap = estimationService.getOwnershipChangeFeeEstimation(criteria, request.getRequestInfo());
			masterDataService.enrichBillingPeriodForFee(masterMap);
			Calculation calculation = getCalculation(request.getRequestInfo(), criteria, estimationMap, masterMap, false);
			calculations.add(calculation);
		}
		return calculations;
	}


	private List<Calculation> getReconnectionFeeCalculation(CalculationReq request, Map<String, Object> masterMap) {
		List<Calculation> calculations = new ArrayList<>(request.getCalculationCriteria().size());
		for (CalculationCriteria criteria : request.getCalculationCriteria()) {
			Map<String, List> estimationMap = estimationService.getReconnectionFeeEstimation(criteria, request.getRequestInfo());
			masterDataService.enrichBillingPeriodForFee(masterMap);
			Calculation calculation = getCalculation(request.getRequestInfo(), criteria, estimationMap, masterMap, false);
			calculations.add(calculation);
		}
		return calculations;
	}
	
	@Override
	public void generateDemandBasedOnTimePeriod(RequestInfo requestInfo, BillSchedulerCriteria billCriteria) {
		ValidateRequest(billCriteria);
		enrichRequest(billCriteria);
		enrichConfiguration(billCriteria);

		DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
		LocalDateTime date = LocalDateTime.now();
		log.info("Time schedule start for water demand generation on : " + date.format(dateTimeFormatter));
		List<String> tenantIds = new ArrayList<>();
		boolean isAll = billCriteria.getTenants().stream().filter(tenant -> tenant.equalsIgnoreCase("ALL")).findAny().orElse(null) == null ? false : true;
		if(isAll) {
			log.info("Processing for all tenants");
			tenantIds = wSCalculationDao.getTenantId();
		} else {
			log.info("Processing for specific tenants");
			tenantIds = billCriteria.getTenants();
		}

		boolean isNone = billCriteria.getSkipTenants().stream().filter(tenant -> tenant.equalsIgnoreCase("NONE")).findAny().orElse(null) == null ? false : true;
		if(!isNone) {
			log.info("Skip tenants: " + billCriteria.getSkipTenants());
			tenantIds = tenantIds.stream().filter(tenant -> !billCriteria.getSkipTenants().contains(tenant)).collect(Collectors.toList());
		}
		if (tenantIds.isEmpty()) {
			log.info("Effective processing tenant Ids : " + tenantIds.toString());
			return;
		}

		log.info("Effective processing tenant Ids : " + tenantIds.toString());
		tenantIds.forEach(tenantId -> {
			demandService.generateDemandForTenantId(tenantId, requestInfo, billCriteria);
		});


	}

	private void ValidateRequest(BillSchedulerCriteria billCriteria) {

		if(billCriteria.getTenants()==null || billCriteria.getTenants().isEmpty()) {
			throw new CustomException("INVALID_REQUEST", "Tenants are missing or empty");
		}

		if(billCriteria.getSkipTenants()==null || billCriteria.getSkipTenants().isEmpty()) {
			throw new CustomException("INVALID_REQUEST", "Skip tenants are missing or empty");
		}

		if(billCriteria.isSpecificMonth()) {
			if(billCriteria.getDemandMonth() <= 0) {
				throw new CustomException("INVALID_REQUEST", "Demand month should be present with positive value");
			}

			if(billCriteria.getDemandYear() <= 0) {
				throw new CustomException("INVALID_REQUEST", "Demand year should be present with positive value");
			}
		}

		if(billCriteria.getWards() != null && !billCriteria.getWards().isEmpty()) {
			for (DemandWard ward : billCriteria.getWards()) {
				if(!StringUtils.hasText(ward.getTenant())) {
					throw new CustomException("INVALID_REQUEST", "Tenant is not present in specificWards section");
				}

				if(ward.getWards() == null ) {
					throw new CustomException("INVALID_REQUEST", "wards can not be empty");
				}
			}
		}
	}

	private void enrichRequest(BillSchedulerCriteria billCriteria) {
		billCriteria.setTenants(billCriteria.getTenants().stream().map(String::trim).collect(Collectors.toList()));
		billCriteria.setSkipTenants(billCriteria.getSkipTenants().stream().map(String::trim).collect(Collectors.toList()));
		billCriteria.setConnectionNos(new ArrayList<>());
	}

	private void enrichConfiguration(BillSchedulerCriteria billCriteria) {
		wsCalculationConfiguration.setDemandStartEndDateManuallyConfigurable(billCriteria.isSpecificMonth());
		wsCalculationConfiguration.setDemandManualMonthNo(billCriteria.getDemandMonth());
		wsCalculationConfiguration.setDemandManualYear(billCriteria.getDemandYear());
		if(billCriteria.getSpecialRebateMonths() != null) {
			wsCalculationConfiguration.setSpecialRebateMonths(billCriteria.getSpecialRebateMonths().stream().map(String::valueOf).collect(Collectors.joining(",")));
		} else {
			wsCalculationConfiguration.setSpecialRebateMonths(null);
		}

		wsCalculationConfiguration.setSpecialRebateYear(String.valueOf(billCriteria.getSpecialRebateYear()));
	}
	
	@Override
	public void generateConnectionDemandBasedOnTimePeriod(RequestInfo requestInfo, BillSchedulerCriteria billCriteria) {
		ValidateConnectionRequest(billCriteria);
		enrichConfiguration(billCriteria);

		DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
		LocalDateTime date = LocalDateTime.now();
		log.info("Time schedule start for water demand generation on : " + date.format(dateTimeFormatter));
		demandService.generateDemandForConnections(requestInfo, billCriteria);
	}

	private void ValidateConnectionRequest(BillSchedulerCriteria billCriteria) {
		if(billCriteria.getTenants()==null || billCriteria.getTenants().isEmpty()) {
			throw new CustomException("INVALID_REQUEST", "Tenants are missing or empty");
		}

		if(billCriteria.isSpecificMonth()) {
			if(billCriteria.getDemandMonth() <= 0) {
				throw new CustomException("INVALID_REQUEST", "Demand month should be present with positive value");
			}

			if(billCriteria.getDemandYear() <= 0) {
				throw new CustomException("INVALID_REQUEST", "Demand year should be present with positive value");
			}
		}

		if(billCriteria.getConnectionNos() == null || billCriteria.getConnectionNos().isEmpty()) {
			throw new CustomException("INVALID_REQUEST", "No connection specified for bill generation");
		}

	}
	
}
