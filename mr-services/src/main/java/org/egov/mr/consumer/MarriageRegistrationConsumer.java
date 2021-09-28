package org.egov.mr.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import org.egov.mr.service.MarriageRegistrationService;
import org.egov.mr.service.notification.MRNotificationService;
import org.egov.mr.web.models.MarriageRegistrationRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import java.util.HashMap;



@Slf4j
@Component
public class MarriageRegistrationConsumer {

    private MRNotificationService notificationService;

    private MarriageRegistrationService marriageRegistrationService;

    @Autowired
    public MarriageRegistrationConsumer(MRNotificationService notificationService, MarriageRegistrationService marriageRegistrationService) {
        this.notificationService = notificationService;
        this.marriageRegistrationService = marriageRegistrationService;
    }

    @KafkaListener(topics = {"${persister.update.marriageregistration.topic}","${persister.save.marriageregistration.topic}","${persister.update.marriageregistration.workflow.topic}"})
    public void listen(final HashMap<String, Object> record, @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        ObjectMapper mapper = new ObjectMapper();
        MarriageRegistrationRequest marriageRegistrationRequest = new MarriageRegistrationRequest();
        try {
            marriageRegistrationRequest = mapper.convertValue(record, MarriageRegistrationRequest.class);
        } catch (final Exception e) {
            log.error("Error while listening to value: " + record + " on topic: " + topic + ": " + e);
        }
       
        notificationService.process(marriageRegistrationRequest);
    }
}
