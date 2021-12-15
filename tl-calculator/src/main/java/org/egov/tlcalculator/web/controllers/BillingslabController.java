package org.egov.tlcalculator.web.controllers;

import static org.egov.tlcalculator.utils.TLCalculatorConstants.businessService_BPA;
import static org.egov.tlcalculator.utils.TLCalculatorConstants.businessService_TL;

import java.util.Collections;

import javax.validation.Valid;

import org.egov.common.contract.request.RequestInfo;
import org.egov.tlcalculator.service.BPABillingSlabService;
import org.egov.tlcalculator.service.BillingslabService;
import org.egov.tlcalculator.utils.ResponseInfoFactory;
import org.egov.tlcalculator.validator.BillingslabValidator;
import org.egov.tlcalculator.web.models.BillingSlab;
import org.egov.tlcalculator.web.models.BillingSlabReq;
import org.egov.tlcalculator.web.models.BillingSlabRes;
import org.egov.tlcalculator.web.models.BillingSlabSearchCriteria;
import org.egov.tlcalculator.web.models.ModifyBillingSlabReq;
import org.egov.tlcalculator.web.models.ModifyBillingSlabRes;
import org.egov.tracer.model.CustomException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.CollectionUtils;
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
	private  BPABillingSlabService bpaBillingSlabService;

	@Autowired
	private ResponseInfoFactory factory;
	/**
	 * Creates Billing Slabs for TradeLicense
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
	 * Updates Billing Slabs of TradeLicense
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
	 * Searches Billing Slabs belonging TradeLicense based on criteria from eg_tl_billingslab and eg_tl_billingslab_audit
	 * If the search parameter searchIdsInAudit is true then First search is made in eg_tl_billingslab and if appropriate results are not returned then it searches in eg_tl_billingslab_audit table
	 * @param billingSlabSearchCriteria
	 * @param requestInfo
	 * @return
	 */
	@RequestMapping(value = {"/{servicename}/_searchAllBillingSlabs", "/_searchAllBillingSlabs"}, method = RequestMethod.POST)
	public ResponseEntity<BillingSlabRes> billingslabSearchAuditAlso(@ModelAttribute @Valid BillingSlabSearchCriteria billingSlabSearchCriteria,
			@Valid @RequestBody RequestInfo requestInfo,@PathVariable(required = false) String servicename) {
		if(servicename==null)
			servicename = businessService_TL;

		BillingSlabRes response = null;
		BillingSlabRes responseAudit = null;
		switch(servicename)
		{
		case businessService_TL:
			response = service.searchSlabs(billingSlabSearchCriteria, requestInfo);
			if(billingSlabSearchCriteria.getSearchIdsInAudit()!=null && billingSlabSearchCriteria.getSearchIdsInAudit() == true)
			{
				if(!CollectionUtils.isEmpty(billingSlabSearchCriteria.getIds()))
				{
					int searchIdsCount = billingSlabSearchCriteria.getIds().size();
					if(searchIdsCount!=response.getBillingSlab().size())
						responseAudit = service.searchSlabsAudit(billingSlabSearchCriteria, requestInfo);
				}
			}else
				responseAudit = service.searchSlabsAudit(billingSlabSearchCriteria, requestInfo);
			if(responseAudit!=null && !CollectionUtils.isEmpty(responseAudit.getBillingSlab()))
				response.getBillingSlab().addAll(responseAudit.getBillingSlab());
			break;

		default:
			throw new CustomException("UNKNOWN_BUSINESSSERVICE", " Business Service not supported");
		}
		return new ResponseEntity<BillingSlabRes>(response, HttpStatus.OK);
	}

	
	@RequestMapping(value = "/_modify", method = RequestMethod.POST)
	public ResponseEntity<ModifyBillingSlabRes> billingslabModifyPost(@Valid @RequestBody ModifyBillingSlabReq billingSlabReq) {
		
		BillingSlabReq billingSlabRequestWithDeleteBillingSlabs =  new BillingSlabReq();
		BillingSlabReq billingSlabRequestWithCreateBillingSlabs =  new BillingSlabReq();
		
		billingSlabRequestWithDeleteBillingSlabs.setRequestInfo(billingSlabReq.getRequestInfo());
		billingSlabRequestWithDeleteBillingSlabs.setBillingSlab(billingSlabReq.getDeleteBillingSlabs());
		billingSlabRequestWithCreateBillingSlabs.setRequestInfo(billingSlabReq.getRequestInfo());
		billingSlabRequestWithCreateBillingSlabs.setBillingSlab(billingSlabReq.getCreateBillingSlabs());
		
		billingslabValidator.validateModify(billingSlabRequestWithDeleteBillingSlabs,billingSlabRequestWithCreateBillingSlabs);
		ModifyBillingSlabRes response = service.modifySlabs(billingSlabRequestWithDeleteBillingSlabs,billingSlabRequestWithCreateBillingSlabs);
		return new ResponseEntity<ModifyBillingSlabRes>(response, HttpStatus.OK);
	}
	

	/**
	 * Searches Billing Slabs belonging TradeLicense based on criteria
	 * @param billingSlabSearchCriteria
	 * @param requestInfo
	 * @return
	 */
	@RequestMapping(value = {"/{servicename}/_search", "/_search"}, method = RequestMethod.POST)
	public ResponseEntity<BillingSlabRes> billingslabSearchPost(@ModelAttribute @Valid BillingSlabSearchCriteria billingSlabSearchCriteria,
																@Valid @RequestBody RequestInfo requestInfo,@PathVariable(required = false) String servicename) {
		if(servicename==null)
			servicename = businessService_TL;

		BillingSlabRes response = null;
		switch(servicename)
		{
		case businessService_TL:
			response = service.searchSlabs(billingSlabSearchCriteria, requestInfo);
			break;

		case businessService_BPA:
			BillingSlab billingSlab = bpaBillingSlabService.search(billingSlabSearchCriteria, requestInfo);
			response = BillingSlabRes.builder().responseInfo(factory.createResponseInfoFromRequestInfo(requestInfo, true))
					.billingSlab(Collections.singletonList(billingSlab)).build();
			break;

		default:
			throw new CustomException("UNKNOWN_BUSINESSSERVICE", " Business Service not supported");
		}
		return new ResponseEntity<BillingSlabRes>(response, HttpStatus.OK);
	}
	
	



}