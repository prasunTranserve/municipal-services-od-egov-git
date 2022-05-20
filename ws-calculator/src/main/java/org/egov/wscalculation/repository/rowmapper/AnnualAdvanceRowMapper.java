package org.egov.wscalculation.repository.rowmapper;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.egov.tracer.model.CustomException;
import org.egov.wscalculation.web.models.AnnualAdvance;
import org.egov.wscalculation.web.models.AnnualAdvance.AnnualAdvanceStatus;
import org.egov.wscalculation.web.models.AuditDetails;
import org.postgresql.util.PGobject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class AnnualAdvanceRowMapper implements ResultSetExtractor<List<AnnualAdvance>> {

	@Autowired
	private ObjectMapper mapper;
	
	@Override
	public List<AnnualAdvance> extractData(ResultSet rs) throws SQLException, DataAccessException {
		
		List<AnnualAdvance> annualAdvanceList = new ArrayList<>();
		while (rs.next()) {
			AnnualAdvance annualAdvance = new AnnualAdvance();
			annualAdvance.setId(rs.getString("id"));
			annualAdvance.setConnectionNo(rs.getString("connectionno"));
			annualAdvance.setFinancialYear(rs.getString("finYear"));
			annualAdvance.setTenantId(rs.getString("tenantid"));
			annualAdvance.setChannel(rs.getString("channel"));
			
			annualAdvance.setStatus(AnnualAdvanceStatus.fromValue(rs.getString("status")));
			
			PGobject obj = (PGobject) rs.getObject("additionaldetails");
			annualAdvance.setAdditionalDetails(getJsonValue(obj));
			
			AuditDetails auditdetails = AuditDetails.builder().createdBy(rs.getString("createdBy"))
					.createdTime(rs.getLong("createdTime")).lastModifiedBy(rs.getString("lastModifiedBy"))
					.lastModifiedTime(rs.getLong("lastModifiedTime")).build();
			annualAdvance.setAuditDetails(auditdetails);
			
			annualAdvanceList.add(annualAdvance);
		}
		return annualAdvanceList;
	}
	
	private JsonNode getJsonValue(PGobject pGobject) {
		try {
			if (Objects.isNull(pGobject) || Objects.isNull(pGobject.getValue()))
				return null;
			else
				return mapper.readTree(pGobject.getValue());
		} catch (IOException e) {
			throw new CustomException("SERVER_ERROR", "Exception occurred while parsing the additionalDetail json : " + e
					.getMessage());
		}
	}
}
