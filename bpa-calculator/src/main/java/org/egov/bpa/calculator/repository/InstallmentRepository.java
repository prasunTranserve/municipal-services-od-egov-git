package org.egov.bpa.calculator.repository;

import java.util.ArrayList;
import java.util.List;

import org.egov.bpa.calculator.repository.querybuilder.InstallmentQueryBuilder;
import org.egov.bpa.calculator.repository.rowmapper.InstallmentRowMapper;
import org.egov.bpa.calculator.web.models.Installment;
import org.egov.bpa.calculator.web.models.InstallmentSearchCriteria;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class InstallmentRepository {

	@Autowired
	private InstallmentQueryBuilder queryBuilder;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private InstallmentRowMapper rowMapper;

	/**
	 * Installment search in database
	 *
	 * @param criteria The Installment Search criteria
	 * @return List of installments from search
	 */
	public List<Installment> getInstallments(InstallmentSearchCriteria criteria) {
		List<Object> preparedStmtList = new ArrayList<>();
		String query = queryBuilder.getInstallmentSearchQuery(criteria, preparedStmtList);
		List<Installment> installments = jdbcTemplate.query(query, preparedStmtList.toArray(), rowMapper);
		return installments;
	}

}
