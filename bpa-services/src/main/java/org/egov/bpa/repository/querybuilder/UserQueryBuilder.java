package org.egov.bpa.repository.querybuilder;

import java.util.List;

import org.egov.bpa.web.model.user.UserSearchCriteria;
import org.springframework.stereotype.Component;

@Component
public class UserQueryBuilder {

	private static final String QUERY_GET_UUID = "select distinct uuid from eg_user ur inner join eg_userrole_v1 u_role on ur.id = u_role.user_id ";
	
	public String getUserSearchQuery(UserSearchCriteria criteria, List<Object> preparedStmtList) {
		StringBuilder queryBuilder = new StringBuilder(QUERY_GET_UUID);
		
		addClauseIfRequired(preparedStmtList, queryBuilder);
		queryBuilder.append(" u_role.role_code IN (").append(createQuery(criteria.getRoles())).append(")");
		addToPreparedStatement(preparedStmtList, criteria.getRoles());
		
		addClauseIfRequired(preparedStmtList, queryBuilder);
		queryBuilder.append(" ur.active = ?");
		preparedStmtList.add(criteria.getIsActive());
		
		return queryBuilder.toString();
	}
	
	/**
	 * add if clause to the Statement if required or elese AND
	 * @param values
	 * @param queryString
	 */
	private void addClauseIfRequired(List<Object> values, StringBuilder queryString) {
		if (values.isEmpty())
			queryString.append(" WHERE ");
		else {
			queryString.append(" AND");
		}
	}
	
	/**
	 * produce a query input for the multiple values
	 * @param ids
	 * @return
	 */
	private Object createQuery(List<String> ids) {
		StringBuilder builder = new StringBuilder();
		int length = ids.size();
		for (int i = 0; i < length; i++) {
			builder.append(" ?");
			if (i != length - 1)
				builder.append(",");
		}
		return builder.toString();
	}
	
	/**
	 * add values to the preparedStatment List
	 * @param preparedStmtList
	 * @param ids
	 */
	private void addToPreparedStatement(List<Object> preparedStmtList, List<String> ids) {
		ids.forEach(id -> {
			preparedStmtList.add(id);
		});

	}
}
