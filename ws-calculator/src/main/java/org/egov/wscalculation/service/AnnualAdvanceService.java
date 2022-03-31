package org.egov.wscalculation.service;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import javax.validation.Valid;

import org.egov.common.contract.request.RequestInfo;
import org.egov.wscalculation.constants.WSCalculationConstant;
import org.egov.wscalculation.util.CalculatorUtil;
import org.egov.wscalculation.validator.WSCalculationValidator;
import org.egov.wscalculation.validator.WSCalculationWorkflowValidator;
import org.egov.wscalculation.web.models.AnnualAdvance.AnnualAdvanceStatus;
import org.egov.wscalculation.web.models.AnnualAdvanceRequest;
import org.egov.wscalculation.web.models.AnnualPaymentDetails;
import org.egov.wscalculation.web.models.AuditDetails;
import org.egov.wscalculation.web.models.CalculationCriteria;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.jayway.jsonpath.Criteria;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class AnnualAdvanceService {
	
	@Autowired
	private CalculatorUtil calculatorUtil;
	
	@Autowired
	private WSCalculationWorkflowValidator wsCalulationWorkflowValidator;
	
	@Autowired
	private WSCalculationValidator wsCalculationValidator;
	
	public void enrichRequest(@Valid AnnualAdvanceRequest annualAdvanceRequests) {
		annualAdvanceRequests.getAnnualAdvance().setId(UUID.randomUUID().toString());
		annualAdvanceRequests.getAnnualAdvance().setFinancialYear(calculatorUtil.getFinancialYear());
		annualAdvanceRequests.getAnnualAdvance().setStatus(AnnualAdvanceStatus.ACTIVE);
		
		if(StringUtils.isEmpty(annualAdvanceRequests.getAnnualAdvance().getChannel())) {
			annualAdvanceRequests.getAnnualAdvance().setChannel(WSCalculationConstant.COMMON_CHANNEL);
		}
		
		long currentTime = System.currentTimeMillis();
		AuditDetails auditDetails = AuditDetails.builder()
				.createdBy(annualAdvanceRequests.getRequestInfo().getUserInfo().getUuid())
				.createdTime(currentTime)
				.lastModifiedBy(annualAdvanceRequests.getRequestInfo().getUserInfo().getUuid())
				.lastModifiedTime(currentTime).build();
		annualAdvanceRequests.getAnnualAdvance().setAuditDetails(auditDetails);
	}
	
	public void applicationValidation(RequestInfo requestInfo, List<CalculationCriteria> calculationCriteria) {
		CalculationCriteria criteria = calculationCriteria.get(0);
		wsCalulationWorkflowValidator.waterApplicationValidationForAnnualAdvance(requestInfo, criteria.getTenantId(), criteria.getConnectionNo());
		wsCalculationValidator.validateAnnualAdvance(requestInfo, criteria.getTenantId(), criteria.getConnectionNo());
	}

	public Boolean validatePayment(@Valid AnnualAdvanceRequest annualAdvanceRequests) {
		// TODO Auto-generated method stub
		
		return true;
	}

	public void enrichAnnualAdvanceDetails(@Valid AnnualAdvanceRequest annualAdvanceRequests,
			AnnualPaymentDetails annualPaymentDetails) {
		HashMap<String, Object> additionalDetail = new HashMap<>();
		additionalDetail.put(WSCalculationConstant.ADVANCE_WATER_CHARGE, annualPaymentDetails.getTotalWaterCharge());
		additionalDetail.put(WSCalculationConstant.ADVANCE_SEWERAGE_CHARGE, annualPaymentDetails.getTotalSewerageCharge());
		additionalDetail.put(WSCalculationConstant.ADVANCE_REBATE, annualPaymentDetails.getTotalRebate());
		
		annualAdvanceRequests.getAnnualAdvance().setAdditionalDetails(additionalDetail);
	}

}
