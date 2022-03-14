package org.egov.waterconnection.repository.rowmapper;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.egov.tracer.model.CustomException;
import org.egov.waterconnection.web.models.AuditDetails;
import org.egov.waterconnection.web.models.Installments;
import org.postgresql.util.PGobject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class InstallmentRowMapper implements ResultSetExtractor<List<Installments>> {

	@Autowired
	private ObjectMapper mapper;
	
	@Override
	public List<Installments> extractData(ResultSet rs) throws SQLException, DataAccessException {
		
		List<Installments> installmentsLists = new ArrayList<>();
		while (rs.next()) {
			Installments installments = new Installments();
			installments.setId(rs.getString("id"));
			installments.setApplicationNo(rs.getString("applicationno"));
			installments.setConsumerNo(rs.getString("consumerno"));
			installments.setFeeType(rs.getString("feetype"));
			installments.setInstallmentNo(rs.getInt("installmentno"));
			installments.setAmount(Objects.isNull(rs.getDouble("installmentamount"))?null:BigDecimal.valueOf(rs.getDouble("installmentamount")));
			installments.setDemandId(rs.getString("demandid"));
			
			PGobject obj = (PGobject) rs.getObject("additionaldetails");
			
			installments.setAdditionalDetails(getJsonValue(obj));
			
			AuditDetails auditdetails = AuditDetails.builder().createdBy(rs.getString("createdBy"))
					.createdTime(rs.getLong("createdTime")).lastModifiedBy(rs.getString("lastModifiedBy"))
					.lastModifiedTime(rs.getLong("lastModifiedTime")).build();
			installments.setAuditDetails(auditdetails);
			
			installmentsLists.add(installments);
		}
		return installmentsLists;
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
