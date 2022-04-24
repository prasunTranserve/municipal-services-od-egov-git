package org.egov.wscalculation.web.controller;

import javax.validation.Valid;

import org.egov.wscalculation.service.WSCalculationServiceImpl;
import org.egov.wscalculation.util.ResponseInfoFactory;
import org.egov.wscalculation.web.models.AnnualPaymentDetails;
import org.egov.wscalculation.web.models.AnnuapPaymentResponse;
import org.egov.wscalculation.web.models.CalculationReq;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
@RestController
@RequestMapping("/waterCalculator/annualAdvance")
public class AnnualAdvanceController {
	
	@Autowired
	private WSCalculationServiceImpl wSCalculationService;
	
	@Autowired
	private final ResponseInfoFactory responseInfoFactory;

	@PostMapping("/_estimate")
	public ResponseEntity<AnnuapPaymentResponse> getAnnualTaxEstimation(@RequestBody @Valid CalculationReq calculationReq) {
		AnnualPaymentDetails annualPaymentDetails = wSCalculationService.getAnnualPaymentEstimation(calculationReq);
		AnnuapPaymentResponse response = AnnuapPaymentResponse.builder().payment(annualPaymentDetails)
				.responseInfo(
						responseInfoFactory.createResponseInfoFromRequestInfo(calculationReq.getRequestInfo(), true))
				.build();
		return new ResponseEntity<>(response, HttpStatus.OK);
	}
}
