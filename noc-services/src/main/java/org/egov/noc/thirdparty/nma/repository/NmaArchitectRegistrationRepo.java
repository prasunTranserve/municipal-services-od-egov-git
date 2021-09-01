package org.egov.noc.thirdparty.nma.repository;

import java.util.Optional;

import org.egov.noc.thirdparty.nma.model.NmaArchitectRegistration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Repository
public class NmaArchitectRegistrationRepo {

	private static final String SAVE_ARCH = "INSERT INTO public.eg_noc_nma_architect_registration "
			+ "(tenantid, userid, token, uniqueid) VALUES(?, ?, ?, ?)";

	private static final String SEARCH_ARCH = "SELECT * FROM public.eg_noc_nma_architect_registration WHERE userid=? AND tenantid=?";

	@Autowired
	private JdbcTemplate jdbcTemplate;

	public void save(NmaArchitectRegistration nmaArchitectRegistration) {
		jdbcTemplate.update(SAVE_ARCH, nmaArchitectRegistration.getTenantid(),nmaArchitectRegistration.getUserid(),
				nmaArchitectRegistration.getToken(), nmaArchitectRegistration.getUniqueid());
	}

	public NmaArchitectRegistration search(Long userid, String department) {
		try {
			return jdbcTemplate.queryForObject(SEARCH_ARCH, new Object[] {userid, department},
					(rs, rowNum) -> {
						return new NmaArchitectRegistration(rs.getInt("id"), rs.getString("tenantid"),
								rs.getLong("userid"), rs.getString("token"),
								rs.getString("uniqueid"), rs.getTimestamp("createdDate"),
								rs.getTimestamp("lastmodifiedtime"));
					});
		} catch (EmptyResultDataAccessException e) {
			log.debug("No record found in database for " + userid+" "+department);
			return null;
		}
	}
}
