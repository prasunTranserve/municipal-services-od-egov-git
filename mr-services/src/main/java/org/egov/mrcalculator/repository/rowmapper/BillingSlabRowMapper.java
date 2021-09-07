package org.egov.mrcalculator.repository.rowmapper;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

import org.egov.mr.web.models.AuditDetails;
import org.egov.mrcalculator.web.models.BillingSlab;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.stereotype.Component;

@Component
public class BillingSlabRowMapper implements ResultSetExtractor<List<BillingSlab>> {

	/**
	 * Rowmapper that maps every column of the search result set to a key in the model.
	 */
	@Override
	public List<BillingSlab> extractData(ResultSet rs) throws SQLException, DataAccessException {
		Map<String, BillingSlab> billingSlabMap = new HashMap<>();
		while (rs.next()) {
			String currentId = rs.getString("id");
			BillingSlab currentBillingSlab = billingSlabMap.get(currentId);
			if (null == currentBillingSlab) {
				AuditDetails auditDetails = AuditDetails.builder().createdBy(rs.getString("createdby"))
						.createdTime(rs.getLong("createdtime")).lastModifiedBy(rs.getString("lastmodifiedby"))
						.lastModifiedTime(rs.getLong("lastmodifiedtime")).build();

				currentBillingSlab = BillingSlab.builder().id(rs.getString("id"))
						.rate(getBigDecimalValue(rs.getBigDecimal("rate")))
						.tenantId(rs.getString("tenantid"))
						.auditDetails(auditDetails).build();

				billingSlabMap.put(currentId, currentBillingSlab);
			}

		}

		return new ArrayList<>(billingSlabMap.values());

	}

	private BigDecimal getBigDecimalValue(BigDecimal amount){
		return Objects.isNull(amount) ? BigDecimal.ZERO : amount;
	}

}
