package org.egov.mr.repository.builder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.egov.mr.config.MRConfiguration;
import org.egov.mr.web.models.*;
import org.egov.tracer.model.CustomException;
import org.postgresql.util.PGobject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

@Slf4j
@Component
public class MRQueryBuilder {

    private MRConfiguration config;

    @Autowired
    public MRQueryBuilder(MRConfiguration config) {
        this.config = config;
    }

    private static final String INNER_JOIN_STRING = " INNER JOIN ";
    private static final String LEFT_OUTER_JOIN_STRING = " LEFT OUTER JOIN ";

    @Value("${egov.receipt.businessserviceMR}")
    private String businessServiceMR;




    private static final String QUERY = "SELECT mr.*,mrp.*,mrc.*,mrca.*," +
            "mrgd.*,mrapldoc.*,mrverdoc.*,mrw.*,mr.id as mr_originalId,mr.tenantid as mr_tenantId,mr.lastModifiedTime as " +
            "mr_lastModifiedTime,mr.createdBy as mr_createdBy,mr.lastModifiedBy as mr_lastModifiedBy,mr.createdTime as " +
            "mr_createdTime,mrp.id as mrp_id,mrp.locality as mrp_locality,mrc.id as mrc_id,mrc.title as mrc_title,mrc.firstname as mrc_firstName,mrc.middlename as mrc_middleName,mrc.lastname as mrc_lastName," +
            "mrca.id as mrca_id,mrca.addressline1 as mrca_addressLine1,mrca.addressline2 as mrca_addressLine2,mrca.addressline3 as mrca_addressLine3,mrca.country as mrca_country,mrca.state as mrca_state,mrca.district as mrca_district,mrca.pincode as mrca_pincode,mrca.locality as mrca_locality," +
            "mrgd.id as mrgd_id,mrgd.addressline1 as mrgd_addressLine1,mrgd.addressline2 as mrgd_addressLine2,mrgd.addressline3 as mrgd_addressLine3,mrgd.country as mrgd_country,mrgd.state as mrgd_state,mrgd.district as mrgd_district,mrgd.pincode as mrgd_pincode,mrgd.locality as mrgd_locality,mrgd.contact as mrgd_contact," +
            "mrw.id as mrw_id,mrw.title as mrw_title,mrw.firstname as mrw_firstName,mrw.middlename as mrw_middleName,mrw.lastname as mrw_lastName,mrw.country as mrw_country,mrw.state as mrw_state,mrw.district as mrw_district,mrw.pincode as mrw_pincode,mrw.contact as mrw_contact," +
            "mrapldoc.id as mr_ap_doc_id,mrapldoc.documenttype as mr_ap_doc_documenttype,mrapldoc.filestoreid as mr_ap_doc_filestoreid,mrapldoc.active as mr_ap_doc_active," +
            "mrverdoc.id as mr_ver_doc_id,mrverdoc.documenttype as mr_ver_doc_documenttype,mrverdoc.filestoreid as mr_ver_doc_filestoreid,mrverdoc.active as mr_ver_doc_active FROM eg_mr_application mr " 
            +INNER_JOIN_STRING
            +"eg_mr_marriageplace mrp ON mrp.mr_id = mr.id"
            +INNER_JOIN_STRING
            +"eg_mr_couple mrc ON mrc.mr_id = mr.id"
            +LEFT_OUTER_JOIN_STRING
            +"eg_mr_coupleaddress mrca ON mrca.mr_couple_id = mrc.id"
            +LEFT_OUTER_JOIN_STRING
            +"eg_mr_gaurdiandetails mrgd ON mrgd.mr_couple_id = mrc.id"
            +LEFT_OUTER_JOIN_STRING
            +"eg_mr_witness mrw ON mrw.mr_id = mr.id"
            +LEFT_OUTER_JOIN_STRING
            +"eg_mr_verificationdocument mrverdoc ON mrverdoc.mr_id = mr.id"
            +LEFT_OUTER_JOIN_STRING
            +"eg_mr_applicationdocument mrapldoc ON mrapldoc.mr_id = mr.id";


      private final String paginationWrapper = "SELECT * FROM " +
              "(SELECT *, DENSE_RANK() OVER (ORDER BY mr_lastModifiedTime DESC , mr_originalId) offset_ FROM " +
              "({})" +
              " result) result_offset " +
              "WHERE offset_ > ? AND offset_ <= ?";





    public String getMRSearchQuery(MarriageRegistrationSearchCriteria criteria, List<Object> preparedStmtList) {

        StringBuilder builder = new StringBuilder(QUERY);

        addBusinessServiceClause(criteria,preparedStmtList,builder);


        if(criteria.getAccountId()!=null){
            addClauseIfRequired(preparedStmtList,builder);
            builder.append(" mr.accountid = ? ");
            preparedStmtList.add(criteria.getAccountId());
   
        }
        else {

            if (criteria.getTenantId() != null) {
                addClauseIfRequired(preparedStmtList, builder);
                builder.append(" mr.tenantid=? ");
                preparedStmtList.add(criteria.getTenantId());
            }
            List<String> ids = criteria.getIds();
            if (!CollectionUtils.isEmpty(ids)) {
                addClauseIfRequired(preparedStmtList, builder);
                builder.append(" mr.id IN (").append(createQuery(ids)).append(")");
                addToPreparedStatement(preparedStmtList, ids);
            }



            if (criteria.getApplicationNumber() != null) {
                addClauseIfRequired(preparedStmtList, builder);
                builder.append("  LOWER(mr.applicationnumber) = LOWER(?) ");
                preparedStmtList.add(criteria.getApplicationNumber());
            }

            if (criteria.getStatus() != null) {
                addClauseIfRequired(preparedStmtList, builder);
                builder.append("  mr.status = ? ");
                preparedStmtList.add(criteria.getStatus());
            }


            List<String> mrNumbers = criteria.getMrNumbers();
            if (!CollectionUtils.isEmpty(mrNumbers)) {
                addClauseIfRequired(preparedStmtList, builder);
                builder.append(" LOWER(mr.mrnumber) IN (").append(createQuery(mrNumbers)).append(")");
                addToPreparedStatement(preparedStmtList, mrNumbers);
            }
            



            if (criteria.getFromDate() != null) {
                addClauseIfRequired(preparedStmtList, builder);
                builder.append("  mr.applicationDate >= ? ");
                preparedStmtList.add(criteria.getFromDate());
            }

            if (criteria.getToDate() != null) {
                addClauseIfRequired(preparedStmtList, builder);
                builder.append("  mr.applicationDate <= ? ");
                preparedStmtList.add(criteria.getToDate());
            }



        }

       // enrichCriteriaForUpdateSearch(builder,preparedStmtList,criteria);

        return addPaginationWrapper(builder.toString(),preparedStmtList,criteria);
    }


    private void addBusinessServiceClause(MarriageRegistrationSearchCriteria criteria,List<Object> preparedStmtList,StringBuilder builder){
        if ((criteria.getBusinessService() == null) || (businessServiceMR.equals(criteria.getBusinessService()))) {
            addClauseIfRequired(preparedStmtList, builder);
            builder.append(" (mr.businessservice=? or mr.businessservice isnull) ");
            preparedStmtList.add(businessServiceMR);
        }
    }

    private String createQuery(List<String> ids) {
        StringBuilder builder = new StringBuilder();
        int length = ids.size();
        for( int i = 0; i< length; i++){
            builder.append(" LOWER(?)");
            if(i != length -1) builder.append(",");
        }
        return builder.toString();
    }

    private void addToPreparedStatement(List<Object> preparedStmtList,List<String> ids)
    {
        ids.forEach(id ->{ preparedStmtList.add(id);});
    }


    private String addPaginationWrapper(String query,List<Object> preparedStmtList,MarriageRegistrationSearchCriteria criteria){
        int limit = config.getDefaultLimit();
        int offset = config.getDefaultOffset();
        String finalQuery = paginationWrapper.replace("{}",query);

        if(criteria.getLimit()!=null && criteria.getLimit()<=config.getMaxSearchLimit())
            limit = criteria.getLimit();

        if(criteria.getLimit()!=null && criteria.getLimit()>config.getMaxSearchLimit())
            limit = config.getMaxSearchLimit();

        if(criteria.getOffset()!=null)
            offset = criteria.getOffset();

        preparedStmtList.add(offset);
        preparedStmtList.add(limit+offset);

       log.info("finalQuery  :-  "+finalQuery);
        
       return finalQuery;
    }


    private static void addClauseIfRequired(List<Object> values, StringBuilder queryString) {
        if (values.isEmpty())
            queryString.append(" WHERE ");
        else {
            queryString.append(" AND");
        }
    }

    public String getTLPlainSearchQuery(MarriageRegistrationSearchCriteria criteria, List<Object> preparedStmtList) {
        StringBuilder builder = new StringBuilder(QUERY);

        List<String> ids = criteria.getIds();
        if (!CollectionUtils.isEmpty(ids)) {
            addClauseIfRequired(preparedStmtList,builder);
            builder.append(" mr.id IN (").append(createQuery(ids)).append(")");
            addToPreparedStatement(preparedStmtList, ids);
        }

        return addPaginationWrapper(builder.toString(), preparedStmtList, criteria);

    }




}
