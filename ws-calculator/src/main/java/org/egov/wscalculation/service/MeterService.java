package org.egov.wscalculation.service;

import java.util.List;

import javax.validation.Valid;

import org.egov.common.contract.request.RequestInfo;
import org.egov.wscalculation.web.models.BulkMeterConnectionRequest;
import org.egov.wscalculation.web.models.MeterConnectionRequest;
import org.egov.wscalculation.web.models.MeterReading;
import org.egov.wscalculation.web.models.MeterReadingSearchCriteria;


public interface MeterService {
	List<MeterReading> createMeterReading(MeterConnectionRequest meterConnectionRequest);
	
	List<MeterReading> searchMeterReadings(MeterReadingSearchCriteria criteria, RequestInfo requestInfo);
	
	List<MeterReading> updateMeterReading(MeterConnectionRequest meterConnectionRequest);

	List<MeterReading> bulkCreateMeterReading(@Valid BulkMeterConnectionRequest meterConnectionRequest);
}