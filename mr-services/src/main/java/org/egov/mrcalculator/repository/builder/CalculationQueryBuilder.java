package org.egov.mrcalculator.repository.builder;

import org.egov.mrcalculator.web.models.BillingSlabSearchCriteria;
import org.egov.mrcalculator.web.models.CalculationSearchCriteria;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CalculationQueryBuilder {


    private static final String INNER_JOIN_STRING = " INNER JOIN ";
    private static final String LEFT_OUTER_JOIN_STRING = " LEFT OUTER JOIN ";

    private static final String QUERY = "SELECT mr.*,mr.consumercode as mr_consumercode FROM eg_mr_calculator mr " +
            " WHERE ";


    /**
     * Creates query to search billingSlabs based on tenantId and consumerCode ordered by lastModifiedTime
     * @param criteria The Search criteria
     * @param preparedStmtList The list of object containing the query parameter values
     * @return Search query for billingSlabs
     */
    public String getSearchQuery(CalculationSearchCriteria criteria, List<Object> preparedStmtList){
        StringBuilder builder = new StringBuilder(QUERY);

        builder.append(" mr.tenantid=? ");
        preparedStmtList.add(criteria.getTenantId());

        builder.append(" AND mr.consumercode=? ");
        preparedStmtList.add(criteria.getAplicationNumber());

        builder.append("ORDER BY mr.lastmodifiedtime DESC,acc.lastmodifiedtime DESC LIMIT 1");

        return builder.toString();
    }


}
