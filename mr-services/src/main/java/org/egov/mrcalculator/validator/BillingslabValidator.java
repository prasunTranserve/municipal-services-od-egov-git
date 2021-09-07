package org.egov.mrcalculator.validator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.egov.mrcalculator.service.BillingslabService;
import org.egov.mrcalculator.utils.ErrorConstants;
import org.egov.mrcalculator.web.models.BillingSlab;
import org.egov.mrcalculator.web.models.BillingSlabReq;
import org.egov.mrcalculator.web.models.BillingSlabRes;
import org.egov.mrcalculator.web.models.BillingSlabSearchCriteria;
import org.egov.tracer.model.CustomException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class BillingslabValidator {
		
	@Autowired
	private BillingslabService service;
	
	/**
	 * Validates the create request for billing slabs. The validation involves:
	 * 1. Checking if all the billing slabs belong to a same tenant
	 * 2. Checking if the billing slab being created already exist in the system.
	 * 3. Checking if the slab is valid with respect to business rules.
	 * 4. Checking if all the provided MDMS codes are valid.
	 * @param billingSlabReq
	 */
	public void validateCreate(BillingSlabReq billingSlabReq) {
		Map<String, String> errorMap = new HashMap<>();
		tenantIdCheck(billingSlabReq, errorMap);
		duplicateCheck(billingSlabReq, errorMap);
		if(!CollectionUtils.isEmpty(errorMap.keySet())) {
			throw new CustomException(errorMap);
		}
		log.info("All validations passed.");
	}
	
	/**
	 * Validates the update request for billing slabs. The validation involves:
	 * 1. Checking if all the billing slabs belong to a same tenant
	 * 2. Checking if the billing slab being created are existing in the system.
	 * 3. Checking if the slab is valid with respect to business rules.
	 * 4. Checking if an existing slab is being updated to a slab that is duplicate.
	 * 5. Checking if all the provided MDMS codes are valid.
	 * 
	 * @param billingSlabReq
	 */
	public void validateUpdate(BillingSlabReq billingSlabReq) {
		Map<String, String> errorMap = new HashMap<>();
		tenantIdCheck(billingSlabReq, errorMap);
		areRecordsExisiting(billingSlabReq, errorMap);
		duplicateCheck(billingSlabReq, errorMap); //Suppose slab s is being updated to s'. If that s' is already available, the update shouldn't be allowed.
		if(!CollectionUtils.isEmpty(errorMap.keySet())) {
			throw new CustomException(errorMap);
		}
		log.info("All validations passed.");
	}
	
	/**
	 * Checks if all billing slabs belong to the same tenant
	 * @param billingSlabReq
	 * @param errorMap
	 */
	public void tenantIdCheck(BillingSlabReq billingSlabReq, Map<String, String> errorMap) {
		Set<String> tenantIds = billingSlabReq.getBillingSlab().parallelStream().map(BillingSlab::getTenantId).collect(Collectors.toSet());
		if(tenantIds.size() > 1) {
			errorMap.put(ErrorConstants.MULTIPLE_TENANT_CODE, ErrorConstants.MULTIPLE_TENANT_MSG);
			throw new CustomException(errorMap);
		}
	}
	
	/**
	 * Checks if the billing slabs being created are duplicate.
	 * @param billingSlabReq
	 * @param errorMap
	 */
	public void duplicateCheck(BillingSlabReq billingSlabReq, Map<String, String> errorMap) {
		List<BillingSlab> duplicateSlabs = new ArrayList<>();
		billingSlabReq.getBillingSlab().parallelStream().forEach(slab -> {
			BillingSlabSearchCriteria criteria = BillingSlabSearchCriteria.builder().tenantId(slab.getTenantId()).build();
			BillingSlabRes slabRes = service.searchSlabs(criteria, billingSlabReq.getRequestInfo());
			if(!CollectionUtils.isEmpty(slabRes.getBillingSlab())) {
				if(!(slabRes.getBillingSlab().size()==1 &&
						slabRes.getBillingSlab().get(0).getId().equalsIgnoreCase(slab.getId())))
				duplicateSlabs.add(slab);
			}
		});
		if(!CollectionUtils.isEmpty(duplicateSlabs)) {
			errorMap.put(ErrorConstants.DUPLICATE_SLABS_CODE, ErrorConstants.DUPLICATE_SLABS_MSG + ": "+duplicateSlabs);
			throw new CustomException(errorMap);	
		}
	}
	
	/**
	 * Verifies if the billing slabs being updated are there in the system.
	 * @param billingSlabReq
	 * @param errorMap
	 */
	public void areRecordsExisiting(BillingSlabReq billingSlabReq, Map<String, String> errorMap) {
		BillingSlabSearchCriteria criteria = BillingSlabSearchCriteria.builder().tenantId(billingSlabReq.getBillingSlab().get(0).getTenantId())
				.ids(billingSlabReq.getBillingSlab().parallelStream().map(BillingSlab :: getId).collect(Collectors.toList())).build();
		BillingSlabRes slabRes = service.searchSlabs(criteria, billingSlabReq.getRequestInfo());
		List<String> ids = new ArrayList<>();
		if(billingSlabReq.getBillingSlab().size() != slabRes.getBillingSlab().size()) {
			List<String> responseIds = slabRes.getBillingSlab().parallelStream().map(BillingSlab :: getId).collect(Collectors.toList());
			for(BillingSlab slab: billingSlabReq.getBillingSlab()) {
				if(!responseIds.contains(slab.getId()))
					ids.add(slab.getId());
			}
			errorMap.put(ErrorConstants.INVALID_IDS_CODE, ErrorConstants.INVALID_IDS_MSG + ": "+ids);
			throw new CustomException(errorMap);
		}
		
	}
	

	
	

}
