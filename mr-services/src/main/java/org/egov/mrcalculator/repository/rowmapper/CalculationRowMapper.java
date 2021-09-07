package org.egov.mrcalculator.repository.rowmapper;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.egov.mrcalculator.web.models.BillingSlabIds;
import org.egov.mrcalculator.web.models.FeeAndBillingSlabIds;
import org.egov.tracer.model.CustomException;
import org.postgresql.util.PGobject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;


@Component
    public class CalculationRowMapper implements ResultSetExtractor<BillingSlabIds> {


    @Autowired
    private ObjectMapper mapper;

        /**
         * Rowmapper that maps every column of the search result set to a key in the model.
         */
        @Override
        public BillingSlabIds extractData(ResultSet rs) throws SQLException, DataAccessException {
           BillingSlabIds billingSlabIds = new BillingSlabIds();

            while (rs.next()) {

                String consumerCode = rs.getString("mr_consumercode");
                PGobject pgObjType = (PGobject) rs.getObject("billingslabids");

                try {
                    FeeAndBillingSlabIds feeBillingSlabIds = mapper.readValue(pgObjType.getValue(), FeeAndBillingSlabIds.class);

                    billingSlabIds.setConsumerCode(consumerCode);
                    billingSlabIds.setBillingSlabIds(feeBillingSlabIds.getBillingSlabIds());

                }
                catch (IOException e){
                    throw new CustomException("PARSING ERROR","Failed to parse json object");
                }

            }
            return billingSlabIds;
        }

}
