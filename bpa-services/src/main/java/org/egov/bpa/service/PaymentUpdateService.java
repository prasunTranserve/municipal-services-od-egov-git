package org.egov.bpa.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.egov.bpa.config.BPAConfiguration;
import org.egov.bpa.producer.Producer;
import org.egov.bpa.repository.BPARepository;
import org.egov.bpa.util.BPAConstants;
import org.egov.bpa.util.BPAErrorConstants;
import org.egov.bpa.web.model.BPA;
import org.egov.bpa.web.model.BPARequest;
import org.egov.bpa.web.model.BPASearchCriteria;
import org.egov.bpa.web.model.Installment;
import org.egov.bpa.web.model.InstallmentRequest;
import org.egov.bpa.web.model.InstallmentSearchCriteria;
import org.egov.bpa.web.model.InstallmentSearchResponse;
import org.egov.bpa.web.model.Workflow;
import org.egov.bpa.web.model.collection.PaymentDetail;
import org.egov.bpa.web.model.collection.PaymentRequest;
import org.egov.bpa.workflow.WorkflowIntegrator;
import org.egov.common.contract.request.RequestInfo;
import org.egov.common.contract.request.Role;
import org.egov.tracer.model.CustomException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class PaymentUpdateService {

	private BPAConfiguration config;

	private BPARepository repository;

	private WorkflowIntegrator wfIntegrator;

	private EnrichmentService enrichmentService;

	private ObjectMapper mapper;
	
	private CalculationService calculationService;
	
	private Producer producer; 

	@Autowired
	public PaymentUpdateService(BPAConfiguration config, BPARepository repository,
			WorkflowIntegrator wfIntegrator, EnrichmentService enrichmentService, ObjectMapper mapper,
			CalculationService calculationService, Producer producer) {
		this.config = config;
		this.repository = repository;
		this.wfIntegrator = wfIntegrator;
		this.enrichmentService = enrichmentService;
		this.mapper = mapper;
		this.calculationService = calculationService;
		this.producer = producer;

	}

	final String tenantId = "tenantId";

	final String businessService = "businessService";

	final String consumerCode = "consumerCode";

	/**
	 * Process the message from kafka and updates the status to paid
	 * 
	 * @param record
	 *            The incoming message from receipt create consumer
	 */
	public void process(HashMap<String, Object> record) {

		try {
			PaymentRequest paymentRequest = mapper.convertValue(record, PaymentRequest.class);
			RequestInfo requestInfo = paymentRequest.getRequestInfo();
			List<PaymentDetail> paymentDetails = paymentRequest.getPayment().getPaymentDetails();
			String tenantId = paymentRequest.getPayment().getTenantId();

			for (PaymentDetail paymentDetail : paymentDetails) {

				List<String> businessServices = new ArrayList<String>(
						Arrays.asList(config.getBusinessService().split(",")));
				if (businessServices.contains(paymentDetail.getBusinessService())) {
					BPASearchCriteria searchCriteria = new BPASearchCriteria();
					searchCriteria.setTenantId(tenantId);
//					List<String> codes = Arrays.asList(paymentDetail.getBill().getConsumerCode());
					searchCriteria.setApplicationNo(paymentDetail.getBill().getConsumerCode());
					List<BPA> bpas = repository.getBPAData(searchCriteria, null);
					if (CollectionUtils.isEmpty(bpas)) {
						throw new CustomException(BPAErrorConstants.INVALID_RECEIPT,
								"No Building Plan Application found for the comsumerCode "
										+ searchCriteria.getApplicationNo());
					}
					//installment update for payment status-
					try {
						//assumption: payment for only one bpa application is done at a time.
						BPA bpaApplication=bpas.get(0);
						InstallmentSearchResponse installmentResponse = fetchAllInstallments(
								bpaApplication.getApplicationNo(), requestInfo);
						if (isPaymentForInstallment(installmentResponse)) {
							findAndUpdateInstallments(paymentDetail, installmentResponse);
							//
							//TODO remove later as added for local testing of payment of 1+ installments-
							//bpaApplication.setStatus("APPROVED");
							
							// return if second time payment by installment-
							if (!StringUtils.isEmpty(bpaApplication.getApprovalNo())
									&& bpaApplication.getStatus().equalsIgnoreCase(BPAConstants.APPROVED_STATE))
								return;
						}
					}catch(Exception ex) {
						log.error("error inside method process method while checking for installment",ex);
					}
					
					Workflow workflow = Workflow.builder().action("PAY").build();
					bpas.forEach(bpa -> bpa.setWorkflow(workflow));
					
					// FIXME check if the update call to repository can be avoided
					// FIXME check why aniket is not using request info from consumer
					// REMOVE SYSTEM HARDCODING AFTER ALTERING THE CONFIG IN WF FOR TL

					Role role = Role.builder().code("SYSTEM_PAYMENT").tenantId(bpas.get(0).getTenantId()).build();
					requestInfo.getUserInfo().getRoles().add(role);
					role = Role.builder().code("CITIZEN").tenantId(bpas.get(0).getTenantId()).build();
					requestInfo.getUserInfo().getRoles().add(role);
					BPARequest updateRequest = BPARequest.builder().requestInfo(requestInfo).BPA(bpas.get(0)).build();

					/*
					 * calling workflow to update status
					 */
					wfIntegrator.callWorkFlow(updateRequest);

					log.debug(" the status of the application is : " + updateRequest.getBPA().getStatus());

					/*
					 * calling repository to update the object in eg_bpa_buildingpaln tables
					 */
					enrichmentService.postStatusEnrichment(updateRequest);

					repository.update(updateRequest, false);

				}
			}
		} catch (Exception e) {
			log.error("KAFKA_PROCESS_ERROR:", e);
		}
	}
	
	private void findAndUpdateInstallments(PaymentDetail paymentDetail,
			InstallmentSearchResponse installmentResponse) throws JsonProcessingException {
		log.info("inside method findDemandIdAndUpdateInstallment");
		List<List<Installment>> installments = installmentResponse.getInstallments();
		List<Installment> installmentsToUpdate = new ArrayList<>();
		for (List<Installment> installmentsInOneInstallment : installments) {
			installmentsToUpdate.addAll(findInstallmentsToUpdatePerInstallment(installmentsInOneInstallment));
		}
		installmentsToUpdate.addAll(findInstallmentsToUpdatePerInstallment(installmentResponse.getFullPayment()));
		// expecting only one demandId to be present in unpaid installments-
		Set<String> demandIdsFromInstallments = installmentsToUpdate.stream()
				.map(installment -> installment.getDemandId()).collect(Collectors.toSet());
		if (demandIdsFromInstallments.size() > 1)
			throw new CustomException("multiple demandIds found for unpaid installments",
					"multiple demandIds found for unpaid installments");

		Map<String, List<Installment>> persisterMap = new HashMap<>();
		log.info("size of installments to update with payment status:" + installmentsToUpdate.size());
		persisterMap.put(BPAConstants.INSTALLMENTS_FIELD, installmentsToUpdate);
		producer.push(config.getUpdateInstallmentTopic(), persisterMap);
	}
	
	private List<Installment> findInstallmentsToUpdatePerInstallment(List<Installment> installmentsInOneInstallment) {
		return installmentsInOneInstallment.stream()
				.filter(installment -> !StringUtils.isEmpty(installment.getDemandId())
						&& !installment.isPaymentCompletedInDemand())
				.map(installment -> {
					installment.setPaymentCompletedInDemand(true);
					return installment;
				}).collect(Collectors.toList());
	}
	
	private InstallmentSearchResponse fetchAllInstallments(String consumerCode, RequestInfo requestInfo) {
		log.info("inside method fetchAllInstallments");
		InstallmentSearchCriteria criteria = new InstallmentSearchCriteria().builder().consumerCode(consumerCode)
				.build();
		InstallmentRequest installmentRequest = InstallmentRequest.builder().installmentSearchCriteria(criteria)
				.requestInfo(requestInfo).build();
		Object installmentResponse = calculationService.getAllInstallments(installmentRequest);
		InstallmentSearchResponse installmentSearchResponse = mapper.convertValue(installmentResponse,
				InstallmentSearchResponse.class);
		return installmentSearchResponse;
	}
	
	private boolean isPaymentForInstallment(InstallmentSearchResponse installmentResponse) {
		log.info("inside method checkIfPaymentDoneForInstallment");
		if (Objects.nonNull(installmentResponse) 
				&& !CollectionUtils.isEmpty(installmentResponse.getInstallments())) {
			List<List<Installment>> installments = installmentResponse.getInstallments();
			for (List<Installment> installmentsInOneInstallment : installments) {
				for (Installment installment : installmentsInOneInstallment) {
					// if any installment is found with demand has been generated ,it means payment
					// is for installment-
					if (!StringUtils.isEmpty(installment.getDemandId()))
						return true;
				}
			}
			log.info("installments exist but no such installment found among non -1 installments");
		} if (Objects.nonNull(installmentResponse)
				&& !CollectionUtils.isEmpty(installmentResponse.getFullPayment())) {
			log.info("checking now for full payment");
			List<Installment> fullPaymentInstallments = installmentResponse.getFullPayment();
			for (Installment installment : fullPaymentInstallments) {
				// if any installment is found with demand has been generated ,it means payment
				// is for installment-
				if (!StringUtils.isEmpty(installment.getDemandId()))
					return true;
			}
		}
		log.info("returning false from method checkIfPaymentDoneForInstallment for consumercode:" + consumerCode);
		return false;
	}
}
