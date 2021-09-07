package org.egov.mr.repository.rowmapper;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.egov.mr.web.models.AuditDetails;
import org.egov.mr.web.models.Boundary;
import org.egov.mr.web.models.Couple;
import org.egov.mr.web.models.CoupleAddress;
import org.egov.mr.web.models.Document;
import org.egov.mr.web.models.GuardianDetails;
import org.egov.mr.web.models.MarriagePlace;
import org.egov.mr.web.models.MarriageRegistration;
import org.egov.mr.web.models.Witness;
import org.egov.tracer.model.CustomException;
import org.postgresql.util.PGobject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;


@Component
public class MRRowMapper  implements ResultSetExtractor<List<MarriageRegistration>> {


    @Autowired
    private ObjectMapper mapper;



    public List<MarriageRegistration> extractData(ResultSet rs) throws SQLException, DataAccessException {
        Map<String, MarriageRegistration> marriageRegistrationMap = new LinkedHashMap<>();
        Map<String, Couple> coupleMap = new LinkedHashMap<>();
        Map<String, Witness> witnessMap = new LinkedHashMap<>();

        while (rs.next()) {
            String id = rs.getString("mr_originalId");
            MarriageRegistration currentMarriageRegistration = marriageRegistrationMap.get(id);
            String tenantId = rs.getString("mr_tenantId");

            if(currentMarriageRegistration == null){
                Long lastModifiedTime = rs.getLong("mr_lastModifiedTime");
                if(rs.wasNull()){lastModifiedTime = null;}

                Long commencementDate = (Long) rs.getObject("marriageDate");
                Long issuedDate = (Long) rs.getObject("issueddate");
                Long applicationDate = (Long) rs.getObject("applicationdate");

                AuditDetails auditdetails = AuditDetails.builder()
                        .createdBy(rs.getString("mr_createdBy"))
                        .createdTime(rs.getLong("mr_createdTime"))
                        .lastModifiedBy(rs.getString("mr_lastModifiedBy"))
                        .lastModifiedTime(lastModifiedTime)
                        .build();

                currentMarriageRegistration = MarriageRegistration.builder().auditDetails(auditdetails)
                        .workflowCode(rs.getString("workflowcode"))
                        .applicationDate(applicationDate)
                        .applicationNumber(rs.getString("applicationnumber"))
                        .mrNumber(rs.getString("mrnumber"))
                        .marriageDate(commencementDate)
                        .issuedDate(issuedDate)
                        .accountId(rs.getString("accountid"))
                        .action(rs.getString("action"))
                        .status(rs.getString("status"))
                        .tenantId(tenantId)
                        .businessService(rs.getString("businessservice"))
                        .id(id)
                        .build();

                marriageRegistrationMap.put(id,currentMarriageRegistration);
            }
            
            
            if(rs.getString("mrc_id")!=null ) {
            	
            	CoupleAddress coupleAddress = null ;
            	
            	if(rs.getString("mrca_id")!=null )
            	{
            		coupleAddress = CoupleAddress.builder()
            				.id(rs.getString("mrca_id"))
            				.tenantId(rs.getString("mr_tenantId"))
            				.addressLine1(rs.getString("mrca_addressLine1"))
            				.addressLine2(rs.getString("mrca_addressLine2"))
            				.addressLine3(rs.getString("mrca_addressLine3"))
            				.country(rs.getString("mrca_country"))
            				.state(rs.getString("mrca_state"))
            				.district(rs.getString("mrca_district"))
            				.pinCode(rs.getString("mrca_pincode"))
            				.locality(rs.getString("mrca_locality"))
            				.build();
            	}
            	
            	
            	
            	Couple couple = Couple.builder()
                        .id(rs.getString("mrc_id"))
                        .isDivyang(rs.getBoolean("isdivyang"))
                        .isGroom(rs.getBoolean("isgroom"))
                        .title(rs.getString("mrc_title"))
                        .firstName(rs.getString("mrc_firstName"))
                        .middleName(rs.getString("mrc_middleName"))
                        .lastName(rs.getString("mrc_lastName"))
                        .dateOfBirth(rs.getLong("dateofbirth"))
                        .fatherName(rs.getString("fathername"))
                        .motherName(rs.getString("mothername"))
                        .build();
            	
            	if(coupleAddress !=  null)
            	{
            		couple.setCoupleAddress(coupleAddress);
            	}
            	
            	
            	GuardianDetails guardianDetails = null ;
            	
            	if(rs.getString("mrgd_id")!=null )
            	{
            		guardianDetails = GuardianDetails.builder()
            				.id(rs.getString("mrgd_id"))
            				.tenantId(rs.getString("mr_tenantId"))
            				.addressLine1(rs.getString("mrgd_addressLine1"))
            				.addressLine2(rs.getString("mrgd_addressLine2"))
            				.addressLine3(rs.getString("mrgd_addressLine3"))
            				.country(rs.getString("mrgd_country"))
            				.state(rs.getString("mrgd_state"))
            				.district(rs.getString("mrgd_district"))
            				.pinCode(rs.getString("mrgd_pincode"))
            				.locality(rs.getString("mrgd_locality"))
            				.groomSideGuardian(rs.getBoolean("isgroomsideguardian"))
            				.relationship(rs.getString("relationship"))
            				.name(rs.getString("name"))
            				.contact(rs.getString("mrgd_contact"))
            				.emailAddress(rs.getString("emailaddress"))
            				.build();
            		
            	}
            	
            	if(guardianDetails !=  null)
            	{
            		couple.setGuardianDetails(guardianDetails);
            	}
            	
            	if(coupleMap.get(rs.getString("mrc_id")) == null)
            	{
            		coupleMap.put(rs.getString("mrc_id"), couple);
            		currentMarriageRegistration.addCoupleDetailsItem(couple);
            	}
            }
            
            
            if(rs.getString("mrw_id")!=null ) {
            	
            	Witness witness = Witness.builder()
            			.id(rs.getString("mrw_id"))
            			.tenantId(rs.getString("mr_tenantId"))
            			.title(rs.getString("mrw_title"))
            			.address(rs.getString("address"))
            			.firstName(rs.getString("mrw_firstName"))
                        .middleName(rs.getString("mrw_middleName"))
                        .lastName(rs.getString("mrw_lastName"))
                        .country(rs.getString("mrw_country"))
        				.state(rs.getString("mrw_state"))
        				.district(rs.getString("mrw_district"))
        				.pinCode(rs.getString("mrw_pincode"))
        				.contact(rs.getString("mrw_contact"))
            			.build();
            	
            	if(witnessMap.get(rs.getString("mrw_id")) == null)
            	{
            		witnessMap.put(rs.getString("mrw_id"), witness);
            		currentMarriageRegistration.addWitnessItem(witness);
            	}
            	
            	
            }
            
            
            
            
            if(rs.getString("mr_ap_doc_id")!=null && rs.getBoolean("mr_ap_doc_active")) {
                Document applicationDocument = Document.builder()
                        .documentType(rs.getString("mr_ap_doc_documenttype"))
                        .fileStoreId(rs.getString("mr_ap_doc_filestoreid"))
                        .id(rs.getString("mr_ap_doc_id"))
                        .tenantId(tenantId)
                        .active(rs.getBoolean("mr_ap_doc_active"))
                        .build();
                currentMarriageRegistration.addApplicationDocumentsItem(applicationDocument);
            }

            if(rs.getString("mr_ver_doc_id")!=null && rs.getBoolean("mr_ver_doc_active")) {
                Document verificationDocument = Document.builder()
                        .documentType(rs.getString("mr_ver_doc_documenttype"))
                        .fileStoreId(rs.getString("mr_ver_doc_filestoreid"))
                        .id(rs.getString("mr_ver_doc_id"))
                        .tenantId(tenantId)
                        .active(rs.getBoolean("mr_ver_doc_active"))
                        .build();
                currentMarriageRegistration.addVerificationDocumentsItem(verificationDocument);
            }
            
            
            
            
            
            addChildrenToProperty(rs, currentMarriageRegistration);

        }

        return new ArrayList<>(marriageRegistrationMap.values());

    }



    private void addChildrenToProperty(ResultSet rs, MarriageRegistration marriageRegistration) throws SQLException {

        String marriagePlaceId = rs.getString("mrp_id");

        if(marriageRegistration.getMarriagePlace()==null){
        	
        

            Boundary locality = Boundary.builder().code(rs.getString("mrp_locality"))
                    .build();
            
            PGobject pgObj = (PGobject) rs.getObject("additionaldetail");
            
            AuditDetails auditdetails = AuditDetails.builder()
                    .createdBy(rs.getString("mr_createdBy"))
                    .createdTime(rs.getLong("mr_createdTime"))
                    .lastModifiedBy(rs.getString("mr_lastModifiedBy"))
                    .lastModifiedTime(rs.getLong("mr_lastModifiedTime"))
                    .build();
            
            try {
        	MarriagePlace marriagePalce = MarriagePlace.builder().locality(locality)
        			.id(marriagePlaceId)
        			.auditDetails(auditdetails)
        			.ward(rs.getString("ward"))
        			.placeOfMarriage(rs.getString("placeofmarriage"))
        			.build();

        			
        			  if(pgObj!=null){
                          JsonNode additionalDetail = mapper.readTree(pgObj.getValue());
                          marriagePalce.setAdditionalDetail(additionalDetail);
                      }

                      marriageRegistration.setMarriagePlace(marriagePalce);
                  }
                  catch (IOException e){
                      throw new CustomException("PARSING ERROR","The additionalDetail json cannot be parsed");
                      
                  }


      
    }


    }

}
