package org.egov.bpa.repository.querybuilder;

import java.util.Calendar;
import java.util.List;

import javax.validation.Valid;

import org.egov.bpa.config.BPAConfiguration;
import org.egov.bpa.web.model.BPASearchCriteria;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

@Component
public class BPAQueryBuilder {

	@Autowired
	private BPAConfiguration config;

	private static final String LEFT_OUTER_JOIN_STRING = " LEFT OUTER JOIN ";

	private static final String QUERY = "SELECT bpa.*,bpadoc.*,bpa.id as bpa_id,bpa.tenantid as bpa_tenantId,bpa.lastModifiedTime as "
			+ "bpa_lastModifiedTime,bpa.createdBy as bpa_createdBy,bpa.lastModifiedBy as bpa_lastModifiedBy,bpa.createdTime as "
			+ "bpa_createdTime,bpa.additionalDetails,bpa.reworkhistory as reWorkHistory,bpa.landId as bpa_landId, bpadoc.id as bpa_doc_id, bpadoc.additionalDetails as doc_details, bpadoc.documenttype as bpa_doc_documenttype,bpadoc.filestoreid as bpa_doc_filestore"
			+ ",bpadsc.id as dsc_id,bpadsc.additionaldetails as dsc_additionaldetails,bpadsc.documenttype as dsc_doctype,bpadsc.documentid as dsc_docid,bpadsc.approvedby as dsc_approvedby,bpadsc.applicationno as dsc_applicationno,bpadsc.buildingplanid as dsc_buildingplanid,bpadsc.createdBy as dsc_createdby,bpadsc.lastmodifiedby as dsc_lastmodifiedby,bpadsc.createdtime as dsc_createdtime,bpadsc.lastmodifiedtime as dsc_lastmodifiedtime "
			+ " FROM eg_bpa_buildingplan bpa"
			+ LEFT_OUTER_JOIN_STRING
			+"eg_bpa_dscdetails bpadsc ON bpadsc.buildingplanid = bpa.id"
			+ LEFT_OUTER_JOIN_STRING
			+ "eg_bpa_document bpadoc ON bpadoc.buildingplanid = bpa.id";;

	private final String paginationWrapper = "SELECT * FROM "
			+ "(SELECT *, DENSE_RANK() OVER (ORDER BY bpa_lastModifiedTime DESC) offset_ FROM " + "({})"
			+ " result) result_offset " + "WHERE offset_ > ? AND offset_ <= ?";
	
	private static final String DSC_PENDING_QUERY = "SELECT ebd.* FROM eg_bpa_dscdetails ebd inner join eg_bpa_buildingplan ebb on ebb.applicationno = ebd.applicationno ";
	private final String dscPaginationWrapper = "SELECT * FROM " +
            "(SELECT *, DENSE_RANK() OVER (ORDER BY lastModifiedTime DESC) offset_ FROM " +
            "({})" +
            " result) result_offset " +
            "WHERE offset_ > ? AND offset_ <= ?";
	
	private static final String BPA_QUERY = "SELECT bpa.*,bpa.id as bpa_id,bpa.tenantid as bpa_tenantId,bpa.lastModifiedTime as "
			+ "bpa_lastModifiedTime,bpa.createdBy as bpa_createdBy,bpa.lastModifiedBy as bpa_lastModifiedBy,bpa.createdTime as "
			+ "bpa_createdTime,bpa.additionalDetails,bpa.landId as bpa_landId from eg_bpa_buildingplan bpa";

	private final String countWrapper = "SELECT COUNT(DISTINCT(bpa_id)) FROM ({INTERNAL_QUERY}) as bpa_count";
	
	private static final String BPA_APPROVER_QUERY = "select distinct approvedby from eg_bpa_dscdetails ";
	 

	private static final String BPA_APPLICATION_QUERY ="select distinct on(bpa.applicationno)  bpa.applicationno as applicationno, bpa.id,bpa.tenantid as tenantId,bpa.lastmodifiedtime as bpa_lastModifiedTime,bpa.businessService as businessService,st.state as workflowstate,st.applicationstatus,asg.assignee as assigneeuuid,bpa.landId as landId,pi.businessservicesla,pi.statesla as statesla from eg_bpa_buildingplan bpa\r\n"
			+ "					  join eg_wf_processinstance_v2 pi on pi.businessid = bpa.applicationno left outer join eg_wf_assignee_v2 asg ON asg.processinstanceid = pi.id left outer join eg_wf_state_v2 st ON st.uuid = pi.status ";
			
	
	
	
	private static final String BPA_APPLICATION_APPROVEDBY_QUERY="select distinct on(dsc.applicationno) dsc.*,st.state as workflowstate,st.applicationstatus,ebb.additionaldetails as buildingadditionaldetails, bpadoc.id as bpa_doc_id,bpadoc.documentuid,\r\n"
			+ "bpadoc.additionalDetails as doc_details, bpadoc.documenttype as bpa_doc_documenttype,bpadoc.filestoreid as bpa_doc_filestore\r\n"
			+ "from eg_bpa_dscdetails dsc left outer join eg_wf_processinstance_v2 pi on pi.businessid = dsc.applicationno left outer join\r\n"
			+ " eg_wf_state_v2 st ON st.uuid = pi.status left outer join eg_bpa_buildingplan ebb on ebb.applicationno =dsc.applicationno \r\n"
			+ "left outer join eg_bpa_document bpadoc on bpadoc.buildingplanid = ebb.id ";
	
	
	
	/**
	 * To give the Search query based on the requirements.
	 * 
	 * @param criteria
	 *            BPA search criteria
	 * @param preparedStmtList
	 *            values to be replased on the query
	 * @return Final Search Query
	 */
	public String getBPASearchQuery(BPASearchCriteria criteria, List<Object> preparedStmtList, List<String> edcrNos) {

		StringBuilder builder = new StringBuilder(QUERY);

		if (criteria.getTenantId() != null) {
			if (criteria.getTenantId().split("\\.").length == 1) {

				addClauseIfRequired(preparedStmtList, builder);
				builder.append(" bpa.tenantid like ?");
				preparedStmtList.add('%' + criteria.getTenantId() + '%');
			} else {
				addClauseIfRequired(preparedStmtList, builder);
				builder.append(" bpa.tenantid=? ");
				preparedStmtList.add(criteria.getTenantId());
			}
		}

		List<String> ids = criteria.getIds();
		if (!CollectionUtils.isEmpty(ids)) {
			addClauseIfRequired(preparedStmtList, builder);
			builder.append(" bpa.id IN (").append(createQuery(ids)).append(")");
			addToPreparedStatement(preparedStmtList, ids);
		}

		String edcrNumbers = criteria.getEdcrNumber();
		if (edcrNumbers!=null) {
			addClauseIfRequired(preparedStmtList, builder);
			builder.append(" bpa.edcrNumber = ?");
			preparedStmtList.add(criteria.getEdcrNumber());
		}
		
		

		String applicationNo = criteria.getApplicationNo();
		if (applicationNo!=null) {
			addClauseIfRequired(preparedStmtList, builder);
			builder.append(" bpa.applicationNo =?");
			preparedStmtList.add(criteria.getApplicationNo());
		}
		
		String approvalNo = criteria.getApprovalNo();
		if (approvalNo!=null) {
			addClauseIfRequired(preparedStmtList, builder);
			builder.append(" bpa.approvalNo = ?");
			preparedStmtList.add(criteria.getApprovalNo());
		}
		
		String status = criteria.getStatus();
		if (status!=null) {
			addClauseIfRequired(preparedStmtList, builder);
			builder.append(" bpa.status = ?");
			preparedStmtList.add(criteria.getStatus());
			
		}
		Long permitDt = criteria.getApprovalDate();
		if ( permitDt != null) {
			
			Calendar permitDate = Calendar.getInstance();
			permitDate.setTimeInMillis(permitDt);
			
			int year = permitDate.get(Calendar.YEAR);
		    int month = permitDate.get(Calendar.MONTH);
		    int day = permitDate.get(Calendar.DATE);
			
			Calendar permitStrDate = Calendar.getInstance();
			permitStrDate.setTimeInMillis(0);
			permitStrDate.set(year, month, day, 0, 0, 0);
			
			Calendar permitEndDate = Calendar.getInstance();
			permitEndDate.setTimeInMillis(0);
			permitEndDate.set(year, month, day, 23, 59, 59);
			addClauseIfRequired(preparedStmtList, builder);
			builder.append(" bpa.approvalDate BETWEEN ").append(permitStrDate.getTimeInMillis()).append(" AND ")
			.append(permitEndDate.getTimeInMillis());	
		}
		if (criteria.getFromDate() != null && criteria.getToDate() != null) {
			addClauseIfRequired(preparedStmtList, builder);
			builder.append(" bpa.createdtime BETWEEN ").append(criteria.getFromDate()).append(" AND ")
					.append(criteria.getToDate());
		} else if (criteria.getFromDate() != null && criteria.getToDate() == null) {
			addClauseIfRequired(preparedStmtList, builder);
			builder.append(" bpa.createdtime >= ").append(criteria.getFromDate());
		}

		List<String> businessService = criteria.getBusinessService();
		if (!CollectionUtils.isEmpty(businessService)) {
			addClauseIfRequired(preparedStmtList, builder);
			builder.append(" bpa.businessService IN (").append(createQuery(businessService)).append(")");
			addToPreparedStatement(preparedStmtList, businessService);
		}
		List<String>landId  = criteria.getLandId();
		List<String> createdBy = criteria.getCreatedBy();
		if (!CollectionUtils.isEmpty(landId)) {
			addClauseIfRequired(preparedStmtList, builder);
			if(!CollectionUtils.isEmpty(createdBy)){
				builder.append("(");
			}
			builder.append(" bpa.landId IN (").append(createQuery(landId)).append(")");
			addToPreparedStatement(preparedStmtList, landId);
		}
		
		if (!CollectionUtils.isEmpty(createdBy)) {
			if (!CollectionUtils.isEmpty(landId)) {
				builder.append(" OR ");
			} else {
				addClauseIfRequired(preparedStmtList, builder);
			}
			builder.append(" bpa.createdby IN (").append(createQuery(createdBy)).append(")");
			if (!CollectionUtils.isEmpty(landId)) {
				builder.append(")");
			}
			addToPreparedStatement(preparedStmtList, createdBy);
		}
		return addPaginationWrapper(builder.toString(), preparedStmtList, criteria);

	}

	/**
	 * 
	 * @param query
	 *            prepared Query
	 * @param preparedStmtList
	 *            values to be replased on the query
	 * @param criteria
	 *            bpa search criteria
	 * @return the query by replacing the placeholders with preparedStmtList
	 */
	private String addPaginationWrapper(String query, List<Object> preparedStmtList, BPASearchCriteria criteria) {

		int limit = config.getDefaultLimit();
		int offset = config.getDefaultOffset();
		String finalQuery = paginationWrapper.replace("{}", query);
		
		if(criteria.getLimit() == null && criteria.getOffset() == null) {
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
	 * add values to the preparedStatment List
	 * @param preparedStmtList
	 * @param ids
	 */
	private void addToPreparedStatement(List<Object> preparedStmtList, List<String> ids) {
		ids.forEach(id -> {
			preparedStmtList.add(id);
		});

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
	
	public String getBPADscDetailsQuery(BPASearchCriteria criteria, List<Object> preparedStmtList) {
		StringBuilder builder = new StringBuilder(DSC_PENDING_QUERY);
		if (criteria.getTenantId() != null) {
			addClauseIfRequired(preparedStmtList, builder);
			builder.append(" ebd.tenantid = ? ");
			preparedStmtList.add(criteria.getTenantId());
		}
    	if (criteria.getApprovedBy() != null) {
            addClauseIfRequired(preparedStmtList, builder);
            builder.append(" ebd.approvedby = ? ");
            preparedStmtList.add(criteria.getApprovedBy());
        }
    	
    	addClauseIfRequired(preparedStmtList, builder);
    	builder.append(" ebb.status = ? ");
    	preparedStmtList.add("APPROVED");
    	
		builder.append(" AND  ebd.documentid is null ");
		return addDscPaginationWrapper(builder.toString(), preparedStmtList, criteria);
	}
	
	private String addDscPaginationWrapper(String query, List<Object> preparedStmtList, BPASearchCriteria criteria) {
		int limit = config.getDefaultLimit();
		int offset = config.getDefaultOffset();
		String finalQuery = dscPaginationWrapper.replace("{}", query);

		if (criteria.getLimit() != null && criteria.getLimit() <= config.getMaxSearchLimit())
			limit = criteria.getLimit();

		if (criteria.getLimit() != null && criteria.getLimit() > config.getMaxSearchLimit())
			limit = config.getMaxSearchLimit();

		if (criteria.getOffset() != null)
			offset = criteria.getOffset();

		if (limit == -1) {
			finalQuery = finalQuery.replace("WHERE offset_ > ? AND offset_ <= ?", "");
		}else  {
		preparedStmtList.add(offset);
		preparedStmtList.add(limit + offset);
		}

		return finalQuery;
	}

	public String getBPAsSearchQuery(List<Object> preparedStmtList) {
		StringBuilder builder = new StringBuilder(BPA_QUERY);
		return builder.toString();
	}
	
	private String addCountWrapper(String query) {
        return countWrapper.replace("{INTERNAL_QUERY}", query);
    }
	
	public String getBPASearchQueryForPlainSearch(BPASearchCriteria criteria, List<Object> preparedStmtList, List<String> edcrNos, boolean isCount) {

        StringBuilder builder = new StringBuilder(QUERY);

        if (criteria.getTenantId() != null) {
            if (criteria.getTenantId().split("\\.").length == 1) {

                addClauseIfRequired(preparedStmtList, builder);
                builder.append(" bpa.tenantid like ?");
                preparedStmtList.add('%' + criteria.getTenantId() + '%');
            } else {
                addClauseIfRequired(preparedStmtList, builder);
                builder.append(" bpa.tenantid=? ");
                preparedStmtList.add(criteria.getTenantId());
            }
        }


        if(isCount)
            return addCountWrapper(builder.toString());

        return addPaginationWrapper(builder.toString(), preparedStmtList, criteria);

    }

	public String getApplicationApprover(String tenantId, String applicationNo, List<Object> preparedStmtList) {
		StringBuilder queryBuilder = new StringBuilder(BPA_APPROVER_QUERY);
		
		addClauseIfRequired(preparedStmtList, queryBuilder);
		queryBuilder.append(" tenantid = ? ");
		preparedStmtList.add(tenantId);
		
		addClauseIfRequired(preparedStmtList, queryBuilder);
		queryBuilder.append(" applicationno = ? ");
		preparedStmtList.add(applicationNo);
		
		return queryBuilder.toString();
	}

	public String getBPAApplicationSearchQuery(@Valid BPASearchCriteria criteria, List<Object> preparedStmtList,
			List<String> edcrNos) {
		StringBuilder builder = new StringBuilder(BPA_APPLICATION_QUERY);

		if (criteria.getTenantId() != null) {
			if (criteria.getTenantId().split("\\.").length == 1) {

				addClauseIfRequired(preparedStmtList, builder);
				builder.append(" bpa.tenantid like ?");
				preparedStmtList.add('%' + criteria.getTenantId() + '%');
			} else {
				addClauseIfRequired(preparedStmtList, builder);
				builder.append(" bpa.tenantid=? ");
				preparedStmtList.add(criteria.getTenantId());
			}
		}

		List<String> ids = criteria.getIds();
		if (!CollectionUtils.isEmpty(ids)) {
			addClauseIfRequired(preparedStmtList, builder);
			builder.append(" bpa.id IN (").append(createQuery(ids)).append(")");
			addToPreparedStatement(preparedStmtList, ids);
		}

		String edcrNumbers = criteria.getEdcrNumber();
		if (edcrNumbers!=null) {
			addClauseIfRequired(preparedStmtList, builder);
			builder.append(" bpa.edcrNumber = ?");
			preparedStmtList.add(criteria.getEdcrNumber());
		}
		
		

		String applicationNo = criteria.getApplicationNo();
		if (applicationNo!=null) {
			addClauseIfRequired(preparedStmtList, builder);
			builder.append(" bpa.applicationNo =?");
			preparedStmtList.add(criteria.getApplicationNo());
		}
		
		String approvalNo = criteria.getApprovalNo();
		if (approvalNo!=null) {
			addClauseIfRequired(preparedStmtList, builder);
			builder.append(" bpa.approvalNo = ?");
			preparedStmtList.add(criteria.getApprovalNo());
		}
		
		String status = criteria.getStatus();
		if (status!=null) {
			addClauseIfRequired(preparedStmtList, builder);
			builder.append(" bpa.status = ?");
			preparedStmtList.add(criteria.getStatus());
			
		}
		Long permitDt = criteria.getApprovalDate();
		if ( permitDt != null) {
			
			Calendar permitDate = Calendar.getInstance();
			permitDate.setTimeInMillis(permitDt);
			
			int year = permitDate.get(Calendar.YEAR);
		    int month = permitDate.get(Calendar.MONTH);
		    int day = permitDate.get(Calendar.DATE);
			
			Calendar permitStrDate = Calendar.getInstance();
			permitStrDate.setTimeInMillis(0);
			permitStrDate.set(year, month, day, 0, 0, 0);
			
			Calendar permitEndDate = Calendar.getInstance();
			permitEndDate.setTimeInMillis(0);
			permitEndDate.set(year, month, day, 23, 59, 59);
			addClauseIfRequired(preparedStmtList, builder);
			builder.append(" bpa.approvalDate BETWEEN ").append(permitStrDate.getTimeInMillis()).append(" AND ")
			.append(permitEndDate.getTimeInMillis());	
		}
		if (criteria.getFromDate() != null && criteria.getToDate() != null) {
			addClauseIfRequired(preparedStmtList, builder);
			builder.append(" bpa.createdtime BETWEEN ").append(criteria.getFromDate()).append(" AND ")
					.append(criteria.getToDate());
		} else if (criteria.getFromDate() != null && criteria.getToDate() == null) {
			addClauseIfRequired(preparedStmtList, builder);
			builder.append(" bpa.createdtime >= ").append(criteria.getFromDate());
		}

		List<String> businessService = criteria.getBusinessService();
		if (!CollectionUtils.isEmpty(businessService)) {
			addClauseIfRequired(preparedStmtList, builder);
			builder.append(" bpa.businessService IN (").append(createQuery(businessService)).append(")");
			addToPreparedStatement(preparedStmtList, businessService);
		}
		List<String>landId  = criteria.getLandId();
		List<String> createdBy = criteria.getCreatedBy();
		if (!CollectionUtils.isEmpty(landId)) {
			addClauseIfRequired(preparedStmtList, builder);
			if(!CollectionUtils.isEmpty(createdBy)){
				builder.append("(");
			}
			builder.append(" bpa.landId IN (").append(createQuery(landId)).append(")");
			addToPreparedStatement(preparedStmtList, landId);
		}
		
		if (!CollectionUtils.isEmpty(createdBy)) {
			if (!CollectionUtils.isEmpty(landId)) {
				builder.append(" OR ");
			} else {
				addClauseIfRequired(preparedStmtList, builder);
			}
			builder.append(" bpa.createdby IN (").append(createQuery(createdBy)).append(")");
			if (!CollectionUtils.isEmpty(landId)) {
				builder.append(")");
			}
			addToPreparedStatement(preparedStmtList, createdBy);
		}
		builder.append("  order by bpa.applicationno,pi.createdtime desc");
		return addPaginationWrapper(builder.toString(), preparedStmtList, criteria);
	}

	public String getApplicationAprovedBy(String uuid, List<Object> preparedStmtList,
			@Valid BPASearchCriteria criteria) {
		StringBuilder builder = new StringBuilder(BPA_APPLICATION_APPROVEDBY_QUERY);
		addClauseIfRequired(preparedStmtList, builder);
		
		if (uuid != null) {
			
			builder.append(" dsc.approvedby = ? ");
			preparedStmtList.add(uuid);			
		}
	builder.append("  order by dsc.applicationno, pi.createdtime desc");
		return addDscPaginationWrapper(builder.toString(), preparedStmtList, criteria);
	
		}
		
	}

