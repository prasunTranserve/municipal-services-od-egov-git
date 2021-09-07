package org.egov.mr.repository;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.egov.common.contract.request.RequestInfo;
import org.egov.mr.config.MRConfiguration;
import org.egov.mr.producer.Producer;
import org.egov.mr.repository.builder.MRQueryBuilder;
import org.egov.mr.repository.rowmapper.MRRowMapper;
import org.egov.mr.web.models.MarriageRegistration;
import org.egov.mr.web.models.MarriageRegistrationRequest;
import org.egov.mr.web.models.MarriageRegistrationSearchCriteria;
import org.egov.mr.workflow.WorkflowService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.util.CollectionUtils;

import lombok.extern.slf4j.Slf4j;



@Slf4j
@Repository
public class MRRepository {

    private JdbcTemplate jdbcTemplate;

    private MRQueryBuilder queryBuilder;

    private MRRowMapper rowMapper;

    private Producer producer;

    private MRConfiguration config;

    private WorkflowService workflowService;


    @Autowired
    public MRRepository(JdbcTemplate jdbcTemplate, MRQueryBuilder queryBuilder, MRRowMapper rowMapper,
                        Producer producer, MRConfiguration config, WorkflowService workflowService) {
        this.jdbcTemplate = jdbcTemplate;
        this.queryBuilder = queryBuilder;
        this.rowMapper = rowMapper;
        this.producer = producer;
        this.config = config;
        this.workflowService = workflowService;
    }



    public List<MarriageRegistration> getMarriageRegistartions(MarriageRegistrationSearchCriteria criteria) {
        List<Object> preparedStmtList = new ArrayList<>();
        String query = queryBuilder.getMRSearchQuery(criteria, preparedStmtList);
        List<MarriageRegistration> registrations =  jdbcTemplate.query(query, preparedStmtList.toArray(), rowMapper);
        return registrations;
    }


    public void save(MarriageRegistrationRequest marriageRegistrationRequest) {
        producer.push(config.getSaveTopic(), marriageRegistrationRequest);
    }
    /**
     * Pushes the update request to update topic or on workflow topic depending on the status
     *
     * @param marriageRegistrationRequest The update requuest
     */
    public void update(MarriageRegistrationRequest marriageRegistrationRequest,Map<String,Boolean> idToIsStateUpdatableMap) {
        RequestInfo requestInfo = marriageRegistrationRequest.getRequestInfo();
        List<MarriageRegistration> marriageRegistrations = marriageRegistrationRequest.getMarriageRegistrations();

        List<MarriageRegistration> registrationsForStatusUpdate = new LinkedList<>();
        List<MarriageRegistration> registrationsForUpdate = new LinkedList<>();


        for (MarriageRegistration registrations : marriageRegistrations) {
            if (idToIsStateUpdatableMap.get(registrations.getId())) {
                registrationsForUpdate.add(registrations);
                
            }
            else {
                registrationsForStatusUpdate.add(registrations);
            }
        }

        if (!CollectionUtils.isEmpty(registrationsForUpdate))
            producer.push(config.getUpdateTopic(), new MarriageRegistrationRequest(requestInfo, registrationsForUpdate));

        if (!CollectionUtils.isEmpty(registrationsForStatusUpdate))
            producer.push(config.getUpdateWorkflowTopic(), new MarriageRegistrationRequest(requestInfo, registrationsForStatusUpdate));


    }









}
