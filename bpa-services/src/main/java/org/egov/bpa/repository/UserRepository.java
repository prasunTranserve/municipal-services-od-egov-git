package org.egov.bpa.repository;

import java.util.ArrayList;
import java.util.List;

import javax.validation.Valid;

import org.egov.bpa.repository.querybuilder.UserQueryBuilder;
import org.egov.bpa.web.model.user.UserSearchCriteria;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class UserRepository {
	
	@Autowired
	private JdbcTemplate jdbcTemplate;
	
	@Autowired
	private UserQueryBuilder queryBuilder;

	public List<String> getIdByRoles(@Valid UserSearchCriteria criteria) {
		List<Object> preparedStmtList = new ArrayList<>();
		String query = queryBuilder.getUserSearchQuery(criteria, preparedStmtList);
		List<String> uuids = jdbcTemplate.queryForList(query, preparedStmtList.toArray(), String.class);
		return uuids;
	}
}
