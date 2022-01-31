package org.egov.wscalculation.repository.rowmapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.egov.wscalculation.web.models.WaterConnection;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.stereotype.Component;

@Component
public class DemandSchedulerRowMapper implements ResultSetExtractor<List<WaterConnection>> {

	@Override
	public List<WaterConnection> extractData(ResultSet rs) throws SQLException, DataAccessException {
//		List<String> connectionLists = new ArrayList<>();
//		while (rs.next()) {
//			connectionLists.add(rs.getString("connectionno"));
//		}
//		return connectionLists;
		
		List<WaterConnection> connectionLists = new ArrayList<>();
		while (rs.next()) {
			WaterConnection waterConnection = new WaterConnection();
			waterConnection.setConnectionNo(rs.getString("connectionno"));
			waterConnection.setConnectionExecutionDate(rs.getLong("connectionexecutiondate"));
			connectionLists.add(waterConnection);
		}
		return connectionLists;
	}
}