package org.egov.bpa.repository.querybuilder;

import java.util.List;

import javax.validation.Valid;

import org.egov.bpa.config.BPAConfiguration;
import org.egov.bpa.web.model.NoticeSearchCriteria;
import org.egov.bpa.web.model.PreapprovedPlanSearchCriteria;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;


@Component
public class ScnQueryBuilder {
	
	@Autowired
	private BPAConfiguration config;
	
	private static final String QUERY = "SELECT id, businessid as businessid,letter_number as letterNumber,filestoreid as filestoreid,letter_type as letterType,tenantid as tenantid, createdby, lastmodifiedby, createdtime, lastmodifiedtime\r\n"
			+ "	FROM eg_bpa_notice";
	
	private final String paginationWrapper = "SELECT * FROM "
			+ "(SELECT *, DENSE_RANK() OVER (ORDER BY lastModifiedTime DESC) offset_ FROM " + "({})"
			+ " result) result_offset " + "WHERE offset_ > ? AND offset_ <= ?";

	public String getNoticeSearchQuery( @Valid NoticeSearchCriteria SearchCriteria, List<Object> preparedStmtList) {
		StringBuilder builder = new StringBuilder(QUERY);
		List<String> ids = SearchCriteria.getIds();
		//System.out.println(ids);
		if (!CollectionUtils.isEmpty(ids)) {
			addClauseIfRequired(preparedStmtList, builder);
			builder.append("id IN (").append(createQuery(ids)).append(")");
			addToPreparedStatement(preparedStmtList, ids);
		
	}
		if(SearchCriteria.getBusinessid()!=null) {
			addClauseIfRequired(preparedStmtList, builder);
			builder.append(" businessid=?");
			preparedStmtList.add(SearchCriteria.getBusinessid());
		}
		if(SearchCriteria.getLetterNo()!=null) {
			addClauseIfRequired(preparedStmtList, builder);
			builder.append("  letter_number=?");
			preparedStmtList.add(SearchCriteria.getLetterNo());
		}
		if(SearchCriteria.getFilestoreid()!=null) {
			addClauseIfRequired(preparedStmtList, builder);
			builder.append("  filestoreid=?");
			preparedStmtList.add(SearchCriteria.getFilestoreid());
		}
		if(SearchCriteria.getTenantid()!=null) {
			addClauseIfRequired(preparedStmtList, builder);
			builder.append("  tenantid=?");
			preparedStmtList.add(SearchCriteria.getTenantid());
		}
		
		if (SearchCriteria.getFromDate() != null && SearchCriteria.getToDate() != null) {
			addClauseIfRequired(preparedStmtList, builder);
			builder.append(" createdtime BETWEEN ").append(SearchCriteria.getFromDate()).append(" AND ")
					.append(SearchCriteria.getToDate());
		} else if (SearchCriteria.getFromDate() != null && SearchCriteria.getToDate() == null) {
			addClauseIfRequired(preparedStmtList, builder);
			builder.append(" createdtime >= ").append(SearchCriteria.getFromDate());
		}
		return addPaginationWrapper(builder.toString(), preparedStmtList, SearchCriteria);

	}

	private void addToPreparedStatement(List<Object> preparedStmtList, List<String> ids) {
		ids.forEach(id -> {
			preparedStmtList.add(id);
		});

	}

	private Object createQuery(List<String> ids) {
		StringBuilder builder = new StringBuilder();
		int length = ids.size();
		System.out.println("ids"+length);
		for (int i = 0; i < length; i++) {
			builder.append(" ?");
			if (i != length - 1)
				builder.append(",");
		}
		return builder.toString();
	}

	private void addClauseIfRequired(List<Object> values, StringBuilder builder) {
		if (values.isEmpty())
			builder.append(" WHERE ");
		else {
			builder.append(" AND");
		}
		
	}
	private String addPaginationWrapper(String query, List<Object> preparedStmtList,
			NoticeSearchCriteria criteria) {

		int limit = config.getDefaultLimit();
		int offset = config.getDefaultOffset();
		String finalQuery = paginationWrapper.replace("{}", query);

		if (criteria.getLimit() == null && criteria.getOffset() == null) {
			limit = config.getMaxSearchLimit();
		}

		if (criteria.getLimit() != null && criteria.getLimit() <= config.getMaxSearchLimit())
			limit = criteria.getLimit();

		if (criteria.getLimit() != null && criteria.getLimit() > config.getMaxSearchLimit()) {
			limit = config.getMaxSearchLimit();
		}

		if (criteria.getOffset() != null)
			offset = criteria.getOffset();

		if (limit == -1) {
			finalQuery = finalQuery.replace("WHERE offset_ > ? AND offset_ <= ?", "");
		} else {
			preparedStmtList.add(offset);
			preparedStmtList.add(limit + offset);
		}

		return finalQuery;

	}
	
	

	
}
