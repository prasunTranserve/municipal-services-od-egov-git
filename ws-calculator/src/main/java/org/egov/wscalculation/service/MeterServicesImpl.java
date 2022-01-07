package org.egov.wscalculation.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.validation.Valid;

import org.egov.common.contract.request.RequestInfo;
import org.egov.wscalculation.repository.WSCalculationDao;
import org.egov.wscalculation.validator.WSCalculationValidator;
import org.egov.wscalculation.validator.WSCalculationWorkflowValidator;
import org.egov.wscalculation.web.models.BulkMeterConnectionRequest;
import org.egov.wscalculation.web.models.CalculationCriteria;
import org.egov.wscalculation.web.models.CalculationReq;
import org.egov.wscalculation.web.models.MeterConnectionRequest;
import org.egov.wscalculation.web.models.MeterReading;
import org.egov.wscalculation.web.models.MeterReading.SuccessFail;
import org.egov.wscalculation.web.models.MeterReadingSearchCriteria;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

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
		List<MeterReading> meterReadingsList = new ArrayList<MeterReading>();
		if(meterConnectionRequest.getMeterReading().getGenerateDemand()){
			wsCalulationWorkflowValidator.applicationValidation(meterConnectionRequest.getRequestInfo(),meterConnectionRequest.getMeterReading().getTenantId(),meterConnectionRequest.getMeterReading().getConnectionNo(),genratedemand);
			wsCalculationValidator.validateMeterReading(meterConnectionRequest, true);
		}
		enrichmentService.enrichMeterReadingRequest(meterConnectionRequest, false);
		meterReadingsList.add(meterConnectionRequest.getMeterReading());
		wSCalculationDao.saveMeterReading(meterConnectionRequest);
//		if (meterConnectionRequest.getMeterReading().getGenerateDemand()) {
//			generateDemandForMeterReading(meterReadingsList, meterConnectionRequest.getRequestInfo());
//		}
		return meterReadingsList;
	}

	private void generateDemandForMeterReading(List<MeterReading> meterReadingsList, RequestInfo requestInfo) {
		List<CalculationCriteria> criteriaList = new ArrayList<>();
		meterReadingsList.forEach(reading -> {
			CalculationCriteria criteria = new CalculationCriteria();
			criteria.setTenantId(reading.getTenantId());
			criteria.setAssessmentYear(estimationService.getAssessmentYear());
			criteria.setCurrentReading(reading.getCurrentReading());
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
	
	/**
	 * 
	 * @param meterConnectionRequest MeterConnectionRequest contains meter reading connection to be updated
	 * @return List of MeterReading after create
	 */

	@Override
	public List<MeterReading> updateMeterReading(MeterConnectionRequest meterConnectionRequest) {
		Map<String, Object> masterMap = new HashMap<>();
		masterDataService.loadMeterReadingMasterData(meterConnectionRequest.getRequestInfo(),
				meterConnectionRequest.getMeterReading().getTenantId(), masterMap);
		Boolean genratedemand = true;
		List<MeterReading> meterReadingsList = new ArrayList<MeterReading>();
		if(meterConnectionRequest.getMeterReading().getGenerateDemand()){
			wsCalulationWorkflowValidator.applicationValidation(meterConnectionRequest.getRequestInfo(),meterConnectionRequest.getMeterReading().getTenantId(),meterConnectionRequest.getMeterReading().getConnectionNo(),genratedemand);
			wsCalculationValidator.validateUpdateMeterReading(meterConnectionRequest, true);
		}
		wsCalculationValidator.validateUpdate(meterConnectionRequest, masterMap);
		enrichmentService.enrichMeterReadingRequest(meterConnectionRequest, true);
		meterReadingsList.add(meterConnectionRequest.getMeterReading());
		wSCalculationDao.updateMeterReading(meterConnectionRequest);
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
}
