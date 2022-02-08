package org.egov.wscalculation.service;

import java.util.List;

import javax.validation.Valid;

import org.egov.common.contract.request.RequestInfo;
import org.egov.wscalculation.web.models.BillSchedulerCriteria;
import org.egov.wscalculation.web.models.BulkBillCriteria;
import org.egov.wscalculation.web.models.Calculation;
import org.egov.wscalculation.web.models.CalculationReq;

public interface WSCalculationService {

	List<Calculation> getCalculation(CalculationReq calculationReq);

	void jobScheduler();

	void generateDemandBasedOnTimePeriod(RequestInfo requestInfo, BulkBillCriteria bulkBillCriteria);

	void generateDemandBasedOnTimePeriod(RequestInfo requestInfo, BillSchedulerCriteria billCriteria);

	void generateConnectionDemandBasedOnTimePeriod(RequestInfo requestInfo, BillSchedulerCriteria billCriteria);
}
