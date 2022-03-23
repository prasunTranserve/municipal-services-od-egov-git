package org.egov.wscalculation.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.validation.Valid;

import org.egov.common.contract.request.RequestInfo;
import org.egov.tracer.model.CustomException;
import org.egov.wscalculation.constants.WSCalculationConstant;
import org.egov.wscalculation.repository.WSCalculationDao;
import org.egov.wscalculation.util.CalculatorUtil;
import org.egov.wscalculation.validator.WSCalculationValidator;
import org.egov.wscalculation.validator.WSCalculationWorkflowValidator;
import org.egov.wscalculation.web.models.BulkMeterConnectionRequest;
import org.egov.wscalculation.web.models.CalculationCriteria;
import org.egov.wscalculation.web.models.CalculationReq;
import org.egov.wscalculation.web.models.MeterConnectionRequest;
import org.egov.wscalculation.web.models.MeterReading;
import org.egov.wscalculation.web.models.MeterReading.MeterStatusEnum;
import org.egov.wscalculation.web.models.MeterReading.SuccessFail;
import org.egov.wscalculation.web.models.MeterReadingSearchCriteria;
import org.egov.wscalculation.web.models.SearchCriteria;
import org.egov.wscalculation.web.models.WaterConnection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class MeterServicesImpl implements MeterService {

	@Autowired
	private WSCalculationDao wSCalculationDao;

	@Autowired
	private WSCalculationValidator wsCalculationValidator;
	
	@Autowired
	private WSCalculationService wSCalculationService;
	
	@Autowired
	private EstimationService estimationService;

	private EnrichmentService enrichmentService;
	
	@Autowired
	private WSCalculationWorkflowValidator wsCalulationWorkflowValidator;
	
	@Autowired
	private MasterDataService masterDataService; 

	@Autowired
	private CalculatorUtil util;

	@Autowired
	private ObjectMapper mapper;

	@Autowired
	public MeterServicesImpl(EnrichmentService enrichmentService) {
		this.enrichmentService = enrichmentService;
	}

	/**
	 * 
	 * @param meterConnectionRequest MeterConnectionRequest contains meter reading connection to be created
	 * @return List of MeterReading after create
	 */

	@Override
	public List<MeterReading> createMeterReading(MeterConnectionRequest meterConnectionRequest) {
		Boolean genratedemand = true;
		int maxMeterReading = 0;
		List<MeterReading> meterReadingsList = new ArrayList<MeterReading>();
		if(meterConnectionRequest.getMeterReading().getGenerateDemand()){
			wsCalulationWorkflowValidator.applicationValidation(meterConnectionRequest.getRequestInfo(),meterConnectionRequest.getMeterReading().getTenantId(),meterConnectionRequest.getMeterReading().getConnectionNo(),genratedemand);
			wsCalculationValidator.validateMeterReading(meterConnectionRequest, true);
			if(meterConnectionRequest.getMeterReading().getMeterStatus().equals(MeterStatusEnum.RESET)) {
					maxMeterReading = maxMeterReading(meterConnectionRequest);
			}
		}
		enrichmentService.enrichMeterReadingRequest(meterConnectionRequest, false);
		meterReadingsList.add(meterConnectionRequest.getMeterReading());
		wSCalculationDao.saveMeterReading(meterConnectionRequest);
		if (meterConnectionRequest.getMeterReading().getGenerateDemand()) {
			generateDemandForMeterReading(meterReadingsList, meterConnectionRequest.getRequestInfo(),maxMeterReading);
		}
		return meterReadingsList;
	}

	private void generateDemandForMeterReading(List<MeterReading> meterReadingsList, RequestInfo requestInfo, int maxMeterReading) {
		List<CalculationCriteria> criteriaList = new ArrayList<>();
		meterReadingsList.forEach(reading -> {
			CalculationCriteria criteria = new CalculationCriteria();
			criteria.setTenantId(reading.getTenantId());
			criteria.setAssessmentYear(estimationService.getAssessmentYear());
			if(reading.getMeterStatus() == MeterStatusEnum.RESET) {
				criteria.setCurrentReading(maxMeterReading + reading.getCurrentReading());
			}
			else {
			criteria.setCurrentReading(reading.getCurrentReading());
			}
			criteria.setLastReading(reading.getLastReading());
			criteria.setConnectionNo(reading.getConnectionNo());
			criteria.setFrom(reading.getLastReadingDate());
			criteria.setTo(reading.getCurrentReadingDate());
			criteriaList.add(criteria);
		});
		CalculationReq calculationRequest = CalculationReq.builder().requestInfo(requestInfo)
				.calculationCriteria(criteriaList).isconnectionCalculation(true).build();
		wSCalculationService.getCalculation(calculationRequest);
	}
	
	/**
	 * 
	 * @param criteria
	 *            MeterConnectionSearchCriteria contains meter reading
	 *            connection criteria to be searched for in the meter
	 *            connection table
	 * @return List of MeterReading after search
	 */
	@Override
	public List<MeterReading> searchMeterReadings(MeterReadingSearchCriteria criteria, RequestInfo requestInfo) {
		return wSCalculationDao.searchMeterReadings(criteria);
	}
	
	@Override
	public List<MeterReading> migrateMeterReading(@Valid MeterConnectionRequest meterConnectionRequest) {
		List<MeterReading> meterReadingsList = new ArrayList<MeterReading>();
		enrichmentService.enrichMeterReadingRequest(meterConnectionRequest, false);
		meterReadingsList.add(meterConnectionRequest.getMeterReading());
		wSCalculationDao.saveMeterReading(meterConnectionRequest);
		return meterReadingsList;
	}
	
	/**
	 * 
	 * @param meterConnectionRequest MeterConnectionRequest contains meter reading connection to be updated
	 * @return List of MeterReading after create
	 */

	@Override
	public List<MeterReading> updateMeterReading(MeterConnectionRequest meterConnectionRequest) {
		Boolean genratedemand = true;
		int maxMeterReading = 0;
		List<MeterReading> meterReadingsList = new ArrayList<MeterReading>();
		if(meterConnectionRequest.getMeterReading().getGenerateDemand()){
			wsCalulationWorkflowValidator.applicationValidation(meterConnectionRequest.getRequestInfo(),meterConnectionRequest.getMeterReading().getTenantId(),meterConnectionRequest.getMeterReading().getConnectionNo(),genratedemand);
			wsCalculationValidator.validateUpdateMeterReading(meterConnectionRequest, true);
			if(meterConnectionRequest.getMeterReading().getMeterStatus().equals(MeterStatusEnum.RESET)) {
				maxMeterReading = maxMeterReading(meterConnectionRequest);
		}
		}
		wsCalculationValidator.validateUpdate(meterConnectionRequest);
		enrichmentService.enrichMeterReadingRequest(meterConnectionRequest, true);
		meterReadingsList.add(meterConnectionRequest.getMeterReading());
		wSCalculationDao.updateMeterReading(meterConnectionRequest);
		if (meterConnectionRequest.getMeterReading().getGenerateDemand()) {
			generateDemandForMeterReading(meterReadingsList, meterConnectionRequest.getRequestInfo(),maxMeterReading);
		}
		return meterReadingsList;
	}

	@Override
	public List<MeterReading> bulkCreateMeterReading(@Valid BulkMeterConnectionRequest bulkMeterConnectionRequest) {
		List<MeterReading> meterReadingsList = new ArrayList<MeterReading>();
		for (MeterReading meterReading : bulkMeterConnectionRequest.getMeterReading()) {
			MeterConnectionRequest connectionRequest = MeterConnectionRequest.builder().requestInfo(bulkMeterConnectionRequest.getRequestInfo())
					.meterReading(meterReading).build();
			List<MeterReading> meterReadings = new ArrayList<>();
			try {
				meterReadings = createMeterReading(connectionRequest);
				meterReading = meterReadings.get(0);
				meterReading.setStatus(SuccessFail.SUCCESS);
			} catch (Exception e) {
				meterReading.setStatus(SuccessFail.FAIL);
				meterReadings.add(meterReading);
			}
			meterReadingsList.add(meterReading);
		}
		return meterReadingsList;
	}

	/*Returns Max Meter Reading for a connection using Max Meter Digits*/
	private int maxMeterReading(MeterConnectionRequest meterConnectionRequest) {
		List<WaterConnection> waterConnectionList = util.getWaterConnection(meterConnectionRequest.getRequestInfo(),meterConnectionRequest.getMeterReading().getConnectionNo(),meterConnectionRequest.getMeterReading().getTenantId());
		WaterConnection waterConnection = waterConnectionList.get(0);
		HashMap<String, Object> addDetail = mapper
				.convertValue(waterConnection.getAdditionalDetails(), HashMap.class);
		Integer maxMeterDigits = Integer.parseInt((String) addDetail.get(WSCalculationConstant.MAX_METER_DIGITS_CONST)); 
		int maxMeterReading = findLargestNumber(maxMeterDigits);
		return maxMeterReading;
	}

	/*Returns the largest number possible for meter digits ex:- meter digit 4 will give 9999 i.e largest 4 digit number*/
	private int findLargestNumber(int maxMeterDigit) {
		int largestNumber = (int) ((Math.pow(10, maxMeterDigit))-1);
		return largestNumber;
	}

}
