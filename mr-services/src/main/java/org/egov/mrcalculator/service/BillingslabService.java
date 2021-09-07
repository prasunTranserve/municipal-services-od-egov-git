package org.egov.mrcalculator.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.egov.common.contract.request.RequestInfo;
import org.egov.mdms.model.MdmsCriteriaReq;
import org.egov.mr.producer.Producer;
import org.egov.mr.util.ResponseInfoFactory;
import org.egov.mr.web.models.AuditDetails;
import org.egov.mrcalculator.utils.BillingslabUtils;
import org.egov.mrcalculator.web.models.*;
import org.egov.mrcalculator.config.BillingSlabConfigs;
import org.egov.mrcalculator.repository.builder.BillingslabQueryBuilder;
import org.egov.mrcalculator.repository.BillingslabRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.jayway.jsonpath.JsonPath;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class BillingslabService {
	
	@Autowired
	private BillingslabUtils util;
	
	@Autowired
	private RestTemplate restTemplate;
	
	@Autowired
	private Producer producer;
	
	@Autowired
	private ResponseInfoFactory factory;
	
	@Autowired
	private BillingslabRepository repository;
	
	@Autowired
	private BillingslabQueryBuilder queryBuilder;
	
	@Autowired
	private BillingSlabConfigs billingSlabConfigs;
	
	/**
	 * Service layer for creating billing slabs
	 * @param billingSlabReq
	 * @return
	 */
	public BillingSlabRes createSlabs(BillingSlabReq billingSlabReq) {
		enrichSlabsForCreate(billingSlabReq);
		billingSlabReq.getBillingSlab().parallelStream().forEach(slab -> {
			List<BillingSlab> slabs = new ArrayList<>();
			slabs.add(slab);
			BillingSlabReq req = BillingSlabReq.builder().requestInfo(billingSlabReq.getRequestInfo()).billingSlab(slabs).build();
			producer.push(billingSlabConfigs.getPersisterSaveTopic(), req);
		});
		return BillingSlabRes.builder().responseInfo(factory.createResponseInfoFromRequestInfo(billingSlabReq.getRequestInfo(), true))
				.billingSlab(billingSlabReq.getBillingSlab()).build();
	}
	
	/**
	 * Service layer for updating billing slabs
	 * @param billingSlabReq
	 * @return
	 */
	public BillingSlabRes updateSlabs(BillingSlabReq billingSlabReq) {
		enrichSlabsForUpdate(billingSlabReq);
		billingSlabReq.getBillingSlab().parallelStream().forEach(slab -> {
			List<BillingSlab> slabs = new ArrayList<>();
			slabs.add(slab);
			BillingSlabReq req = BillingSlabReq.builder().requestInfo(billingSlabReq.getRequestInfo()).billingSlab(slabs).build();
			producer.push(billingSlabConfigs.getPersisterUpdateTopic(), req);
		});
		return BillingSlabRes.builder().responseInfo(factory.createResponseInfoFromRequestInfo(billingSlabReq.getRequestInfo(), true))
				.billingSlab(billingSlabReq.getBillingSlab()).build();
	}
	
	/**
	 * Service layer for searching billing slabs from the db
	 * @param criteria
	 * @param requestInfo
	 * @return
	 */
	public BillingSlabRes searchSlabs(BillingSlabSearchCriteria criteria, RequestInfo requestInfo) {
		List<Object> preparedStmtList = new ArrayList<>();
		String query = queryBuilder.getSearchQuery(criteria, preparedStmtList);
		return BillingSlabRes.builder().responseInfo(factory.createResponseInfoFromRequestInfo(requestInfo, true))
				.billingSlab(repository.getDataFromDB(query, preparedStmtList)).build();
	}
	
	/**
	 * Enriches the request for creating billing slabs. Enrichment includes:
	 * 1. Preparing audit information for the slab
	 * 2. Setting id to the billing slabs
	 * @param billingSlabReq
	 */
	public void enrichSlabsForCreate(BillingSlabReq billingSlabReq) {
		AuditDetails audit = AuditDetails.builder().createdBy(billingSlabReq.getRequestInfo().getUserInfo().getUuid())
				.createdTime(new Date().getTime()).lastModifiedBy(billingSlabReq.getRequestInfo().getUserInfo().getUuid()).lastModifiedTime(new Date().getTime()).build();
		for(BillingSlab slab: billingSlabReq.getBillingSlab()) {
			slab.setId(UUID.randomUUID().toString());
			slab.setAuditDetails(audit);
		}
	}
	
	/**
	 * Enriches the request for updating billing slabs. Enrichment includes:
	 * 1. Preparing audit information for the slab
	 * @param billingSlabReq
	 */
	public void enrichSlabsForUpdate(BillingSlabReq billingSlabReq) {
		AuditDetails audit = AuditDetails.builder().lastModifiedBy(billingSlabReq.getRequestInfo().getUserInfo().getUuid()).lastModifiedTime(new Date().getTime()).build();
		billingSlabReq.getBillingSlab().parallelStream().forEach(slab ->  slab.setAuditDetails(audit) );
	}
	
	


}
