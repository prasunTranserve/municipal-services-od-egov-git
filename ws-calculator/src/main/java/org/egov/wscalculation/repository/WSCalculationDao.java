package org.egov.wscalculation.repository;

import java.util.ArrayList;
import java.util.List;

import org.egov.wscalculation.web.models.AnnualAdvance;
import org.egov.wscalculation.web.models.AnnualAdvanceRequest;
import org.egov.wscalculation.web.models.BillSchedulerCriteria;
import org.egov.wscalculation.web.models.Installments;
import org.egov.wscalculation.web.models.MeterConnectionRequest;
import org.egov.wscalculation.web.models.MeterReading;
import org.egov.wscalculation.web.models.MeterReadingSearchCriteria;
import org.egov.wscalculation.web.models.WaterConnection;

public interface WSCalculationDao {

	void saveMeterReading(MeterConnectionRequest meterConnectionRequest);
	
	List<MeterReading> searchMeterReadings(MeterReadingSearchCriteria criteria);
	
	ArrayList<String> searchTenantIds();

	ArrayList<WaterConnection> searchConnectionNos(String connectionType, String tenantId);
	
	List<MeterReading> searchCurrentMeterReadings(MeterReadingSearchCriteria criteria);
	
	int isMeterReadingConnectionExist(List<String> ids);
	
	List<WaterConnection> getConnectionsNoList(String tenantId, String connectionType, BillSchedulerCriteria billCriteria);
	
	List<String> getTenantId();
	
	int isBillingPeriodExists(String connectionNo, String billingPeriod);
	
	void updateMeterReading(MeterConnectionRequest meterConnectionRequest);

	long getConnectionCount(String tenantId, Long fromDate, Long toDate, boolean connectionWise, List<String> connectionNos);

	List<WaterConnection> getConnectionsNoList(String tenantId, String connectionType, Integer batchOffset,
			Integer batchsize, Long fromDate, Long toDate);
	
	List<WaterConnection> getConnectionsNoList(String tenantId, String nonmeterdconnection, Long fromDate, Long toDate, List<String> connectionNos);
	
	List<Installments> getApplicableInstallmentsByConsumerNo(String tenantId, String consumerNo);
	
	int getInstallmentCountByApplicationNoAndFeeType(String tenantId, String applicationNo, String feeType);

	List<Installments> getApplicableInstallmentsByApplicationNo(String tenantId, String consumerNo);
	
	void saveAnnualAdvance(AnnualAdvanceRequest annualAdvanceRequests);

	List<AnnualAdvance> getAnnualAdvance(String tenantId, String connectionNo, String assessYear);

}
