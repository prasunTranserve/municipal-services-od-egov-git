package org.egov.mr.service;

import static org.egov.mr.util.MRConstants.ACTION_PAY;
import static org.egov.mr.util.MRConstants.STATUS_APPROVED;
import static org.egov.mr.util.MRConstants.businessService_MR;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.egov.common.contract.request.RequestInfo;
import org.egov.common.contract.request.Role;
import org.egov.mr.config.MRConfiguration;
import org.egov.mr.repository.MRRepository;
import org.egov.mr.util.MarriageRegistrationUtil;
import org.egov.mr.web.models.MarriageRegistration;
import org.egov.mr.web.models.MarriageRegistrationRequest;
import org.egov.mr.web.models.MarriageRegistrationSearchCriteria;
import org.egov.mr.web.models.collection.PaymentDetail;
import org.egov.mr.web.models.collection.PaymentRequest;
import org.egov.mr.web.models.workflow.BusinessService;
import org.egov.mr.workflow.WorkflowIntegrator;
import org.egov.mr.workflow.WorkflowService;
import org.egov.tracer.model.CustomException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.DocumentContext;

import lombok.extern.slf4j.Slf4j;


@Service
@Slf4j
public class PaymentUpdateService {

	private MarriageRegistrationsForPayment marriageRegistrationPaymentService;

	private MRConfiguration config;

	private MRRepository repository;

	private WorkflowIntegrator wfIntegrator;

	private EnrichmentService enrichmentService;

	private ObjectMapper mapper;

	private WorkflowService workflowService;

	private MarriageRegistrationUtil util;

	@Autowired
	public PaymentUpdateService(MarriageRegistrationsForPayment marriageRegistrationPaymentService, MRConfiguration config, MRRepository repository,
								WorkflowIntegrator wfIntegrator, EnrichmentService enrichmentService, ObjectMapper mapper,
								WorkflowService workflowService,MarriageRegistrationUtil util) {
		this.marriageRegistrationPaymentService = marriageRegistrationPaymentService;
		this.config = config;
		this.repository = repository;
		this.wfIntegrator = wfIntegrator;
		this.enrichmentService = enrichmentService;
		this.mapper = mapper;
		this.workflowService = workflowService;
		this.util = util;
	}




	final String tenantId = "tenantId";

	final String businessService = "businessService";

	final String consumerCode = "consumerCode";

	/**
	 * Process the message from kafka and updates the status to paid
	 * 
	 * @param record The incoming message from receipt create consumer
	 */
	public void process(HashMap<String, Object> record) {

		try {
			PaymentRequest paymentRequest = mapper.convertValue(record,PaymentRequest.class);
			RequestInfo requestInfo = paymentRequest.getRequestInfo();
			List<PaymentDetail> paymentDetails = paymentRequest.getPayment().getPaymentDetails();
			String tenantId = paymentRequest.getPayment().getTenantId();
			for(PaymentDetail paymentDetail : paymentDetails){
				if (paymentDetail.getBusinessService().equalsIgnoreCase(businessService_MR) ) {
					MarriageRegistrationSearchCriteria searchCriteria = new MarriageRegistrationSearchCriteria();
					searchCriteria.setTenantId(tenantId);
					searchCriteria.setApplicationNumber(paymentDetail.getBill().getConsumerCode());
					searchCriteria.setBusinessService(paymentDetail.getBusinessService());
					List<MarriageRegistration> licenses = marriageRegistrationPaymentService.getMarriageRegistrationsWithOwnerInfo(searchCriteria, requestInfo);
					String wfbusinessServiceName = null;
					switch (paymentDetail.getBusinessService()) {
						case businessService_MR:
							wfbusinessServiceName = config.getMrBusinessServiceValue();
							break;


					}
				BusinessService businessService = workflowService.getBusinessService(licenses.get(0).getTenantId(), requestInfo,wfbusinessServiceName);


					if (CollectionUtils.isEmpty(licenses))
						throw new CustomException("INVALID RECEIPT",
								"No tradeLicense found for the comsumerCode " + searchCriteria.getApplicationNumber());

					licenses.forEach(license -> license.setAction(ACTION_PAY));

					// FIXME check if the update call to repository can be avoided
					// FIXME check why aniket is not using request info from consumer
					// REMOVE SYSTEM HARDCODING AFTER ALTERING THE CONFIG IN WF FOR TL

					Role role = Role.builder().code("SYSTEM_PAYMENT").tenantId(licenses.get(0).getTenantId()).build();
					requestInfo.getUserInfo().getRoles().add(role);
					MarriageRegistrationRequest updateRequest = MarriageRegistrationRequest.builder().requestInfo(requestInfo)
							.MarriageRegistrations(licenses).build();

					/*
					 * calling workflow to update status
					 */
					wfIntegrator.callWorkFlow(updateRequest);

					updateRequest.getMarriageRegistrations()
							.forEach(obj -> log.info(" the status of the application is : " + obj.getStatus()));

					List<String> endStates = Collections.nCopies(updateRequest.getMarriageRegistrations().size(), STATUS_APPROVED);
					
					enrichmentService.postStatusEnrichment(updateRequest,endStates);

					/*
					 * calling repository to update the object in TL tables
					 */
					Map<String,Boolean> idToIsStateUpdatableMap = util.getIdToIsStateUpdatableMap(businessService,licenses);
					repository.update(updateRequest,idToIsStateUpdatableMap);
			}
		 }
		} catch (Exception e) {
			log.error("KAFKA_PROCESS_ERROR", e);
		}

	}

	/**
	 * Extracts the required fields as map
	 * 
	 * @param context The documentcontext of the incoming receipt
	 * @return Map containing values of required fields
	 */
	private Map<String, String> enrichValMap(DocumentContext context) {
		Map<String, String> valMap = new HashMap<>();
		try {
			valMap.put(businessService, context.read("$.Payments.*.paymentDetails[?(@.businessService=='TL')].businessService"));
			valMap.put(consumerCode, context.read("$.Payments.*.paymentDetails[?(@.businessService=='TL')].bill.consumerCode"));
			valMap.put(tenantId, context.read("$.Payments[0].tenantId"));
		} catch (Exception e) {
			throw new CustomException("PAYMENT ERROR", "Unable to fetch values from payment");
		}
		return valMap;
	}

}
