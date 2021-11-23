package org.egov.tlcalculator.validator;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.egov.tlcalculator.service.BillingslabService;
import org.egov.tlcalculator.utils.BillingslabConstants;
import org.egov.tlcalculator.utils.BillingslabUtils;
import org.egov.tlcalculator.utils.ErrorConstants;
import org.egov.tlcalculator.web.models.BillingSlab;
import org.egov.tlcalculator.web.models.BillingSlabReq;
import org.egov.tlcalculator.web.models.BillingSlabRes;
import org.egov.tlcalculator.web.models.BillingSlabSearchCriteria;
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
		dataIntegrityCheck(billingSlabReq, errorMap);
		Map<String, List<String>> mdmsDataMap = service.getMDMSDataForValidation(billingSlabReq);
		billingSlabReq.getBillingSlab().parallelStream().forEach(slab -> validateMDMSCodes(slab, mdmsDataMap, errorMap));
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
		dataIntegrityCheck(billingSlabReq, errorMap);
		duplicateCheck(billingSlabReq, errorMap); //Suppose slab s is being updated to s'. If that s' is already available, the update shouldn't be allowed.
		Map<String, List<String>> mdmsDataMap = service.getMDMSDataForValidation(billingSlabReq);
		billingSlabReq.getBillingSlab().parallelStream().forEach(slab -> validateMDMSCodes(slab, mdmsDataMap, errorMap));
		if(!CollectionUtils.isEmpty(errorMap.keySet())) {
			throw new CustomException(errorMap);
		}
		log.info("All validations passed.");
	}
	
	
	public void validateModify(BillingSlabReq billingSlabRequestWithDeleteBillingSlabs ,BillingSlabReq billingSlabRequestWithCreateBillingSlabs) {

		Map<String, String> errorMap = new HashMap<>();
		List<String> deleteBillingSlabids = new ArrayList<>();
		
		if(!CollectionUtils.isEmpty(billingSlabRequestWithDeleteBillingSlabs.getBillingSlab()))
		{
			deleteBillingSlabids = billingSlabRequestWithDeleteBillingSlabs.getBillingSlab().parallelStream().map(BillingSlab :: getId).collect(Collectors.toList());
			tenantIdCheck(billingSlabRequestWithDeleteBillingSlabs, errorMap);
			areRecordsExisiting(billingSlabRequestWithDeleteBillingSlabs, errorMap);
		}
		
		if(!CollectionUtils.isEmpty(billingSlabRequestWithCreateBillingSlabs.getBillingSlab()))
		{
			tenantIdCheck(billingSlabRequestWithCreateBillingSlabs, errorMap);
			validateMandatoryFields(billingSlabRequestWithCreateBillingSlabs, errorMap);
			duplicateCheck(billingSlabRequestWithCreateBillingSlabs, errorMap);
			fromToUomCheck(billingSlabRequestWithCreateBillingSlabs, errorMap , deleteBillingSlabids);
			dataIntegrityCheck(billingSlabRequestWithCreateBillingSlabs, errorMap);
			Map<String, List<String>> mdmsDataMap = service.getMDMSDataForValidation(billingSlabRequestWithCreateBillingSlabs);
			billingSlabRequestWithCreateBillingSlabs.getBillingSlab().parallelStream().forEach(slab -> validateMDMSCodes(slab, mdmsDataMap, errorMap));
		
		}
		
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
			BillingSlabSearchCriteria criteria = BillingSlabSearchCriteria.builder().tenantId(slab.getTenantId()).accessoryCategory(slab.getAccessoryCategory())
					.tradeType(slab.getTradeType())
					.applicationType(slab.getApplicationType())
					.licenseType(null == slab.getLicenseType() ? null : slab.getLicenseType().toString())
					.structureType(slab.getStructureType()).uom(slab.getUom())
					.type(null == slab.getType() ? null : slab.getType().toString())
					.from(slab.getFromUom()).to(slab.getToUom()).build();
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
	
	
	
	public void validateMandatoryFields(BillingSlabReq createBillingSlabReq, Map<String, String> errorMap ) {
		createBillingSlabReq.getBillingSlab().parallelStream().forEach(slab -> {
			if(slab.getUom()!=null && slab.getUom().trim().isEmpty())
				errorMap.put(ErrorConstants.UOM_ERROR_CODE, ErrorConstants.UOM_ERROR_MESSAGE );
			if(slab.getUom()==null)
			{
				if(slab.getFromUom()!=null || slab.getToUom()!=null)
				{
					errorMap.put(ErrorConstants.FROMUOM_TOUOM_NULL_ERROR_CODE, ErrorConstants.FROMUOM_TOUOM_NULL_ERROR_MSG );
				}
				
				if(slab.getType()!=null && slab.getType().toString().equalsIgnoreCase(BillingSlab.TypeEnum.RATE.toString()))
				{
					errorMap.put(ErrorConstants.TYPE_RATE_ERROR_CODE, ErrorConstants.TYPE_RATE_ERROR_MESSAGE );
				}
				
			}else
			{
				if(slab.getFromUom()==null)
					errorMap.put(ErrorConstants.FROMUOM_ERROR_CODE, ErrorConstants.FROMUOM_ERROR_MSG );
				if(slab.getToUom()==null)
					errorMap.put(ErrorConstants.TOUOM_ERROR_CODE, ErrorConstants.TOUOM_ERROR_MSG );
				
				if(slab.getFromUom()<0)
				{
					errorMap.put(ErrorConstants.FROMUOM_NEGAVTIVE_ERROR_CODE, ErrorConstants.FROMUOM_NEGAVTIVE_ERROR_MSG );
				}
				
				if(slab.getToUom()<0)
				{
					errorMap.put(ErrorConstants.TOUOM_NEGAVTIVE_ERROR_CODE, ErrorConstants.TOUOM_NEGAVTIVE_ERROR_MSG );
				}
				
				
				if(slab.getFromUom()!=null && slab.getToUom()!=null && !(slab.getFromUom()<slab.getToUom()))
				{
					errorMap.put(ErrorConstants.FROMUOM_LESSTHAN_TOUOM_ERROR_CODE, ErrorConstants.FROMUOM_LESSTHAN_TOUOM_ERROR_MSG );
				}
			}
			if(BillingslabUtils.isEmpty(slab.getTenantId()))
				errorMap.put(ErrorConstants.TENANTID_ERROR_CODE, ErrorConstants.TENANTID_ERROR_MESSAGE );
			if(BillingslabUtils.isEmpty(slab.getApplicationType()))
				errorMap.put(ErrorConstants.APPLICATION_TYPE_ERROR_CODE, ErrorConstants.APPLICATION_TYPE_ERROR_MESSAGE );
			if(slab.getLicenseType()==null || BillingslabUtils.isEmpty(slab.getLicenseType().toString()))
				errorMap.put(ErrorConstants.LICENSE_TYPE_ERROR_CODE, ErrorConstants.LICENSE_TYPE_ERROR_MESSAGE );
			if(slab.getType()==null || BillingslabUtils.isEmpty(slab.getType().toString()))
				errorMap.put(ErrorConstants.TYPE_ERROR_CODE, ErrorConstants.TYPE_ERROR_MESSAGE );
			if(slab.getRate()==null)
				errorMap.put(ErrorConstants.RATE_ERROR_CODE, ErrorConstants.RATE_ERROR_MESSAGE );
			if(slab.getRate()!=null && slab.getRate().compareTo(BigDecimal.ZERO) < 0)
				errorMap.put(ErrorConstants.RATE_NEGATIVE_ERROR_CODE, ErrorConstants.RATE_NEGATIVE_ERROR_MESSAGE );
		});
	}
	
	public void fromToUomCheck(BillingSlabReq createBillingSlabReq, Map<String, String> errorMap ,List<String> deleteBillingSlabids) {
		List<BillingSlab> uomBillingSlabs = new ArrayList<>();
		Map<String, Set<Double>> tradeTypeDetailsMap = new HashMap<>();
		Map<String, String> tradeTypeUomDetailsMap = new HashMap<>();
		createBillingSlabReq.getBillingSlab().forEach(slab -> {
			
			
			if(slab.getFromUom()!=null && slab.getToUom()!=null)
			{
				BillingSlabSearchCriteria fromCriteria = BillingSlabSearchCriteria.builder().tenantId(slab.getTenantId()).accessoryCategory(slab.getAccessoryCategory())
						.tradeType(slab.getTradeType())
						.applicationType(slab.getApplicationType())
						.licenseType(null == slab.getLicenseType() ? null : slab.getLicenseType().toString())
						.structureType(slab.getStructureType()).uom(slab.getUom())
						.type(null == slab.getType() ? null : slab.getType().toString())
						.uomValue(slab.getFromUom()).build();
				BillingSlabRes fromSlabRes = service.searchSlabs(fromCriteria, createBillingSlabReq.getRequestInfo());
				if(!CollectionUtils.isEmpty(fromSlabRes.getBillingSlab())) {
					if(!(fromSlabRes.getBillingSlab().size()==1 &&
							fromSlabRes.getBillingSlab().get(0).getId().equalsIgnoreCase(slab.getId())))
						uomBillingSlabs.add(fromSlabRes.getBillingSlab().get(0));
					
					if(fromSlabRes.getBillingSlab().size()==1 &&!deleteBillingSlabids.contains(slab.getId()))
						errorMap.put(ErrorConstants.DELETE_BILLING_SLAB_ERROR_CODE, ErrorConstants.DELETE_BILLING_SLAB_ERROR_MSG + ": "+slab.getId());
					
				}
				
				BillingSlabSearchCriteria toCriteria = BillingSlabSearchCriteria.builder().tenantId(slab.getTenantId()).accessoryCategory(slab.getAccessoryCategory())
						.tradeType(slab.getTradeType())
						.applicationType(slab.getApplicationType())
						.licenseType(null == slab.getLicenseType() ? null : slab.getLicenseType().toString())
						.structureType(slab.getStructureType()).uom(slab.getUom())
						.type(null == slab.getType() ? null : slab.getType().toString())
						.uomValue(slab.getToUom()).build();
				BillingSlabRes toSlabRes = service.searchSlabs(toCriteria, createBillingSlabReq.getRequestInfo());
				if(!CollectionUtils.isEmpty(toSlabRes.getBillingSlab())) {
					if(!(toSlabRes.getBillingSlab().size()==1 &&
							toSlabRes.getBillingSlab().get(0).getId().equalsIgnoreCase(slab.getId())))
						uomBillingSlabs.add(toSlabRes.getBillingSlab().get(0));
					
					if(toSlabRes.getBillingSlab().size()==1 &&!deleteBillingSlabids.contains(slab.getId()))
						errorMap.put(ErrorConstants.DELETE_BILLING_SLAB_ERROR_CODE, ErrorConstants.DELETE_BILLING_SLAB_ERROR_MSG + ": "+slab.getId());
				}
				
				if(!tradeTypeDetailsMap.containsKey(slab.getTenantId()+slab.getApplicationType()+slab.getLicenseType()+slab.getTradeType()))
				{
					Set<Double> uomsList = new HashSet<Double>();
					tradeTypeDetailsMap.put(slab.getTenantId()+slab.getApplicationType()+slab.getLicenseType()+slab.getTradeType(), uomsList);
					if(!uomsList.add(slab.getFromUom()))
						errorMap.put(ErrorConstants.DUPLICATE_UOM_ERROR_CODE, ErrorConstants.DUPLICATE_UOM_ERROR_MSG + ": "+slab.getFromUom());
					if(!uomsList.add(slab.getToUom()))
						errorMap.put(ErrorConstants.DUPLICATE_UOM_ERROR_CODE, ErrorConstants.DUPLICATE_UOM_ERROR_MSG + ": "+slab.getToUom());
					else
						tradeTypeUomDetailsMap.put(slab.getTenantId()+slab.getApplicationType()+slab.getLicenseType()+slab.getTradeType()+slab.getFromUom(), slab.getTenantId()+slab.getApplicationType()+slab.getLicenseType()+slab.getTradeType()+slab.getToUom());

				}
				else
				{
					Set<Double> uomsList = tradeTypeDetailsMap.get(slab.getTenantId()+slab.getApplicationType()+slab.getLicenseType()+slab.getTradeType());
					if(!uomsList.add(slab.getFromUom()))
						errorMap.put(ErrorConstants.DUPLICATE_UOM_ERROR_CODE, ErrorConstants.DUPLICATE_UOM_ERROR_MSG + ": "+slab.getFromUom());
					if(!uomsList.add(slab.getToUom()))
						errorMap.put(ErrorConstants.DUPLICATE_UOM_ERROR_CODE, ErrorConstants.DUPLICATE_UOM_ERROR_MSG + ": "+slab.getToUom());
					else
						tradeTypeUomDetailsMap.put(slab.getTenantId()+slab.getApplicationType()+slab.getLicenseType()+slab.getTradeType()+slab.getFromUom(), slab.getTenantId()+slab.getApplicationType()+slab.getLicenseType()+slab.getTradeType()+slab.getToUom());
				}
				
				
				
			}
		});
		
		
		if(errorMap.isEmpty())
		{
		main : for (Map.Entry<String, Set<Double>> entry : tradeTypeDetailsMap.entrySet()) {
			TreeSet<Double> treeSet = new TreeSet<Double>(entry.getValue());
			Double[]   uomArray = treeSet.toArray(new Double[treeSet.size()]);
			for(int i= 0 ; i<uomArray.length-1 ; i = i+2)
			{
				if(tradeTypeUomDetailsMap.get(entry.getKey()+uomArray[i])!=null && !tradeTypeUomDetailsMap.get(entry.getKey()+uomArray[i]).equalsIgnoreCase(entry.getKey()+uomArray[i+1]))
				{
					errorMap.put(ErrorConstants.UOM_MISMATCH_ERROR_CODE, ErrorConstants.UOM_MISMATCH_ERROR_MESSAGE + entry.getKey());
					break main;
					
				}
			}
		}
		}
		
		if(!CollectionUtils.isEmpty(uomBillingSlabs)) {
			errorMap.put(ErrorConstants.FROMUOM_TOUOM_ERROR_CODE, ErrorConstants.FROMUOM_TOUOM_ERROR_MSG + ": "+uomBillingSlabs);
		}
		
		if(!CollectionUtils.isEmpty(errorMap.keySet())) {
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
	
	
	
	
	
	
	/**
	 * Checking if the slab is valid with respect to business rules.
	 * @param billingSlabReq
	 * @param errorMap
	 */
	public void dataIntegrityCheck(BillingSlabReq billingSlabReq, Map<String, String> errorMap) {
		billingSlabReq.getBillingSlab().parallelStream().forEach(slab -> {
			if(!StringUtils.isEmpty(slab.getAccessoryCategory()) && !StringUtils.isEmpty(slab.getTradeType())) {
				errorMap.put(ErrorConstants.INVALID_SLAB_CODE, ErrorConstants.INVALID_SLAB_MSG);
			}
		});
		if(!CollectionUtils.isEmpty(errorMap.keySet())) {
			throw new CustomException(errorMap);
		}
	}
	
	
	
	
	
	/**
	 * Validates MDMS codes present in the create/update request 
	 * @param billingSlab
	 * @param mdmsDataMap
	 * @param errorMap
	 */
	public void validateMDMSCodes(BillingSlab billingSlab, Map<String, List<String>> mdmsDataMap, Map<String, String> errorMap) {
		if(!StringUtils.isEmpty(billingSlab.getTradeType())) {
			
			if(billingSlab.getLicenseType().equals(billingSlab.getLicenseType().PERMANENT))
			{
			if(!mdmsDataMap.get(BillingslabConstants.TL_MDMS_TRADETYPE).contains(billingSlab.getTradeType()))
				errorMap.put(ErrorConstants.INVALID_TRADETYPE_CODE, ErrorConstants.INVALID_TRADETYPE_MSG + ": "+billingSlab.getTradeType());
			}else if(billingSlab.getLicenseType().equals(billingSlab.getLicenseType().TEMPORARY))
			{
				if(!mdmsDataMap.get(BillingslabConstants.TL_MDMS_TEMP_TRADETYPE).contains(billingSlab.getTradeType()))
					errorMap.put(ErrorConstants.INVALID_TRADETYPE_CODE, ErrorConstants.INVALID_TRADETYPE_MSG + ": "+billingSlab.getTradeType());
			}
			}
		if(!StringUtils.isEmpty(billingSlab.getAccessoryCategory())) {
			if(!mdmsDataMap.get(BillingslabConstants.TL_MDMS_ACCESSORIESCATEGORY).contains(billingSlab.getAccessoryCategory()))
				errorMap.put(ErrorConstants.INVALID_ACCESSORIESCATEGORY_CODE, ErrorConstants.INVALID_ACCESSORIESCATEGORY_MSG + ": "+billingSlab.getAccessoryCategory());
		}
		if(!StringUtils.isEmpty(billingSlab.getStructureType())) {
			if(!mdmsDataMap.get(BillingslabConstants.TL_MDMS_STRUCTURETYPE).contains(billingSlab.getStructureType()))
				errorMap.put(ErrorConstants.INVALID_STRUCTURETYPE_CODE, ErrorConstants.INVALID_STRUCTURETYPE_MSG + ": "+billingSlab.getStructureType());
		}
		if(!StringUtils.isEmpty(billingSlab.getUom())) {
			if(!mdmsDataMap.get(BillingslabConstants.TL_MDMS_UOM).contains(billingSlab.getUom()))
				errorMap.put(ErrorConstants.INVALID_UOM_CODE, ErrorConstants.INVALID_UOM_MSG + ": "+billingSlab.getUom());
		}
	}

}
