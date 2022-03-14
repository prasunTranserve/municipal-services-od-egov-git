package org.egov.wscalculation.service;

import java.util.ArrayList;
import java.util.List;

import org.egov.common.contract.request.RequestInfo;
import org.egov.wscalculation.producer.WSCalculationProducer;
import org.egov.wscalculation.repository.WSCalculationDao;
import org.egov.wscalculation.web.models.Demand;
import org.egov.wscalculation.web.models.Installments;
import org.egov.wscalculation.web.models.InstallmentsRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class InstallmentService {
	
	@Autowired
	private WSCalculationDao wsCalculationDao;
	
	@Autowired
	private WSCalculationProducer wsCalculationProducer;
	
	@Value("${kafka.waterservice.update.installment.topic}")
    private String wsUpdateInstallmentTopic;
	
	public void updateInstallmentsWithDemands(RequestInfo requestInfo, List<Demand> demands, boolean isForApplication) {
		List<Installments> installmentsToBeUpdated = new ArrayList<>();
		
		for (Demand demand : demands) {
			List<Installments> installments;
			if(isForApplication) {
				installments = wsCalculationDao.getApplicableInstallmentsByApplicationNo(demand.getTenantId(), demand.getConsumerCode());
			} else {
				installments = wsCalculationDao.getApplicableInstallmentsByConsumerNo(demand.getTenantId(), demand.getConsumerCode());
			}
			
			log.info(installments.size() + " installment found for consumer: "+ demand.getConsumerCode());
			
			installments.forEach(installment -> {
				installment.setDemandId(demand.getId());
				installment.getAuditDetails().setLastModifiedBy(requestInfo.getUserInfo().getUuid());
				installment.getAuditDetails().setLastModifiedTime(System.currentTimeMillis());
			});
			installmentsToBeUpdated.addAll(installments);
		}
		
		//Update installment table
		if(!installmentsToBeUpdated.isEmpty()) {
			wsCalculationProducer.push(wsUpdateInstallmentTopic, InstallmentsRequest
					.builder().requestInfo(requestInfo).installments(installmentsToBeUpdated).build());
		}
	}

}
