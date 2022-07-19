package org.egov.bpa.repository.rowmapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.egov.bpa.web.model.BpaApplicationSearch;
import org.egov.bpa.web.model.BpaApprovedByApplicationSearch;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.stereotype.Component;

@Component
public class BPAApprovedByRowMapper implements ResultSetExtractor<List<BpaApprovedByApplicationSearch>> {

	@Override
	public List<BpaApprovedByApplicationSearch> extractData(ResultSet rs) throws SQLException, DataAccessException {
		Map<String, BpaApprovedByApplicationSearch>  approvalMap = new LinkedHashMap<String, BpaApprovedByApplicationSearch>();
		while (rs.next()) {
			String id = rs.getString("id");
			BpaApprovedByApplicationSearch bpas = approvalMap.get(id);
			if(bpas==null) {
				
				bpas = BpaApprovedByApplicationSearch.builder().applicationNo(rs.getString("applicationNo")).applicationstatus(rs.getString("applicationstatus"))
						.workflowstate(rs.getString("workflowstate")).tenantId(rs.getString("tenantid")).build();
				
				approvalMap.put(id, bpas);
			}
			
		}
		return new ArrayList<>(approvalMap.values());
	}

}
