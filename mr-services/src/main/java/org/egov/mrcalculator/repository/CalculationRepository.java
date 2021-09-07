package org.egov.mrcalculator.repository;

import lombok.extern.slf4j.Slf4j;

import org.egov.mrcalculator.repository.rowmapper.BillingSlabRowMapper;
import org.egov.mrcalculator.repository.rowmapper.CalculationRowMapper;
import org.egov.mrcalculator.web.models.BillingSlab;
import org.egov.mrcalculator.web.models.BillingSlabIds;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;


@Slf4j
@Repository
public class CalculationRepository {


    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private CalculationRowMapper calculationRowMapper;

    /**
     * Executes the argument query on db
     * @param query The query to be executed
     * @param preparedStmtList The parameter values for the query
     * @return BillingSlabIds
     */
    public BillingSlabIds getDataFromDB(String query, List<Object> preparedStmtList){
        BillingSlabIds billingSlabIds = null;
        try {
            billingSlabIds = jdbcTemplate.query(query, preparedStmtList.toArray(), calculationRowMapper);
        }catch(Exception e) {
            log.error("Exception while fetching from DB: " + e);
            return billingSlabIds;
        }

        return billingSlabIds;
    }


}
