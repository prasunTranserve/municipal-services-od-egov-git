package org.egov.mrcalculator.web.controllers;

import static org.egov.mrcalculator.utils.MRCalculatorConstants.businessService_MR;

import javax.validation.Valid;

import org.egov.common.contract.request.RequestInfo;
import org.egov.mr.util.ResponseInfoFactory;
import org.egov.mrcalculator.service.BillingslabService;
import org.egov.mrcalculator.validator.BillingslabValidator;
import org.egov.mrcalculator.web.models.BillingSlabReq;
import org.egov.mrcalculator.web.models.BillingSlabRes;
import org.egov.mrcalculator.web.models.BillingSlabSearchCriteria;
import org.egov.tracer.model.CustomException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;



@Controller
@RequestMapping("/billingslab")
public class BillingslabController {

	@Autowired
	private BillingslabValidator billingslabValidator;

	@Autowired
	private  BillingslabService service;


	@Autowired
	private ResponseInfoFactory factory;
	/**
	 * Creates Billing Slabs for MarriageRegistration
	 * @param billingSlabReq
	 * @return
	 */
	@RequestMapping(value = "/_create", method = RequestMethod.POST)
	public ResponseEntity<BillingSlabRes> billingslabCreatePost(@Valid @RequestBody BillingSlabReq billingSlabReq) {
		billingslabValidator.validateCreate(billingSlabReq);
		BillingSlabRes response = service.createSlabs(billingSlabReq);
		return new ResponseEntity<BillingSlabRes>(response, HttpStatus.OK);
	}

	/**
	 * Updates Billing Slabs of MarriageRegistration
	 * @param billingSlabReq
	 * @return
	 */
	@RequestMapping(value = "/_update", method = RequestMethod.POST)
	public ResponseEntity<BillingSlabRes> billingslabUpdatePost(@Valid @RequestBody BillingSlabReq billingSlabReq) {
		billingslabValidator.validateUpdate(billingSlabReq);
		BillingSlabRes response = service.updateSlabs(billingSlabReq);
		return new ResponseEntity<BillingSlabRes>(response, HttpStatus.OK);
	}

	/**
	 * Searches Billing Slabs belonging MarriageRegistration based on criteria
	 * @param billingSlabSearchCriteria
	 * @param requestInfo
	 * @return
	 */
	@RequestMapping(value = {"/{servicename}/_search", "/_search"}, method = RequestMethod.POST)
	public ResponseEntity<BillingSlabRes> billingslabSearchPost(@ModelAttribute @Valid BillingSlabSearchCriteria billingSlabSearchCriteria,
			@Valid @RequestBody RequestInfo requestInfo,@PathVariable(required = false) String servicename) {
		if(servicename==null)
			servicename = businessService_MR;

		BillingSlabRes response = null;
		switch(servicename)
		{
		case businessService_MR:
			response = service.searchSlabs(billingSlabSearchCriteria, requestInfo);
			break;

		default:
			throw new CustomException("UNKNOWN_BUSINESSSERVICE", " Business Service not supported");
		}
		return new ResponseEntity<BillingSlabRes>(response, HttpStatus.OK);
	}




}