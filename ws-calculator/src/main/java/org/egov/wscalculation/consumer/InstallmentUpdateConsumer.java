package org.egov.wscalculation.consumer;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.egov.common.contract.request.RequestInfo;
import org.egov.wscalculation.service.InstallmentService;
import org.egov.wscalculation.service.WSCalculationService;
import org.egov.wscalculation.web.models.Demand;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class InstallmentUpdateConsumer {
	
	@Autowired
	private InstallmentService installmentService;
	
	@Autowired
	private ObjectMapper mapper;
	
	@Value("${kafka.topic.ws.installment.update}")
    private String wsInstallmentUpdateTopic;

	@KafkaListener(topics = { "${kafka.topic.ws.installment.update}" })
	public void processMessage(Map<String, Object> consumerRecord, @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
		log.info("WS Calculator received from topic: " + topic);
		try {
//			Map installmentUpdateRequest = consumerRecord;
			RequestInfo requestInfo = mapper.convertValue(consumerRecord.get("requestInfo"), RequestInfo.class);
//			List<Demand> demands = mapper.readValue(consumerRecord.get("demands"), new TypeReference<List<Demand>>(){});
			List<Demand> ObjDemands = mapper.convertValue(consumerRecord.get("demands"), new TypeReference<List<Demand>>(){});
			
			log.info("Passing demands to update installments.");
			installmentService.updateInstallmentsWithDemands(requestInfo, ObjDemands, false);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
