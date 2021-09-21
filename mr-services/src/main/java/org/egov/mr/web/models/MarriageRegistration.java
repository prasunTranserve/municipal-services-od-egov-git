package org.egov.mr.web.models;

import java.util.ArrayList;
import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.egov.mr.util.MRConstants;
import org.egov.mrcalculator.web.models.Calculation;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MarriageRegistration {
	
	@Size(max=64)
    @JsonProperty("id")
    private String id = null;

    @NotNull
    @Size(max=64)
    @JsonProperty("tenantId")
    private String tenantId = null;
    
    @Size(max=64)
    @JsonProperty("accountId")
    private String accountId = null;
    
    public enum ApplicationTypeEnum {
        NEW(MRConstants.APPLICATION_TYPE_NEW),

        CORRECTION(MRConstants.APPLICATION_TYPE_CORRECTION);

        private String value;

        ApplicationTypeEnum(String value) {
            this.value = value;
        }

        @Override
        @JsonValue
        public String toString() {
            return String.valueOf(value);
        }

        @JsonCreator
        public static ApplicationTypeEnum fromValue(String text) {
            for (ApplicationTypeEnum b : ApplicationTypeEnum.values()) {
                if (String.valueOf(b.value).equals(text)) {
                    return b;
                }
            }
            return null;
        }
    }
    
  //  @NotNull(message = "applicationType is mandatory ")
    @JsonProperty("applicationType")
    private ApplicationTypeEnum applicationType = null;
    
    @JsonProperty("businessService")
    private String businessService = "MR";
    
    @JsonProperty("workflowCode")
    private String workflowCode = null;
    
    @Size(max=64)
    @JsonProperty("mrNumber")
    private String mrNumber = null;

    @Size(max=64)
    @JsonProperty("applicationNumber")
    private String applicationNumber;
    
    
    @JsonProperty("applicationDate")
    private Long applicationDate = null;
    
    @JsonProperty("appointmentDate")
    private Long appointmentDate = null;

    @JsonProperty("marriageDate")
    private Long marriageDate = null;
    
    @JsonProperty("issuedDate")
    private Long issuedDate = null;
    
    @NotNull
    @Size(max=64)
    @JsonProperty("action")
    private String action = null;

    @Size(max=64)
    @JsonProperty("status")
    private String status = null;
    
    @Valid
    @JsonProperty("wfDocuments")
    private List<Document> wfDocuments;


    @JsonProperty("auditDetails")
    private AuditDetails auditDetails = null;
    
    
    @JsonProperty("marriagePlace")
    @Valid
    private MarriagePlace  marriagePlace ; 
    
    
	  @JsonProperty("applicationDocuments")
      @Valid
      private List<Document> applicationDocuments = null;
	  
      @JsonProperty("verificationDocuments")
      @Valid
      private List<Document> verificationDocuments = null;
	  
	  @JsonProperty("coupleDetails")
      @Valid
      private List<Couple> coupleDetails = null;
	  
	
	  
	  @JsonProperty("assignee")
      private List<String> assignee = null;
	  
	  @Size(max=128)
      private String comment;
	  
	  
      @JsonProperty("calculation")
      private Calculation calculation;
	  
	  
	  public MarriageRegistration addCoupleDetailsItem(CoupleDetails coupleDetailItem) {
          if (this.coupleDetails == null) {
          this.coupleDetails = new ArrayList<>();
          }
          if(!coupleDetailItem.getIsGroom())
          {
        	  if(this.coupleDetails.size()>0)
        	  {
              if(this.coupleDetails.get(0).getBride() != null &&   !this.coupleDetails.get(0).getBride().equals(coupleDetailItem))
              this.coupleDetails.get(0).setBride(coupleDetailItem);
              else if(this.coupleDetails.get(0).getBride() == null)
            	  this.coupleDetails.get(0).setBride(coupleDetailItem);
        	  }else
        	  {
        		  Couple  couple = new Couple();
        		  couple.setBride(coupleDetailItem);
        		  this.coupleDetails.add(couple);
        	  }
          }
          if(coupleDetailItem.getIsGroom())
          {
        	  if(this.coupleDetails.size()>0)
        	  {
        	  if(this.coupleDetails.get(0).getGroom()!= null &&  !this.coupleDetails.get(0).getGroom().equals(coupleDetailItem))
                  this.coupleDetails.get(0).setGroom(coupleDetailItem); 
        	  else if(this.coupleDetails.get(0).getGroom()  == null)
        		  this.coupleDetails.get(0).setGroom(coupleDetailItem); 
        	  }else
        	  {
        		  Couple  couple = new Couple();
        		  couple.setGroom(coupleDetailItem);
        		  this.coupleDetails.add(couple);
        	  }
          }
          
          return this;
      }
	  
	  public MarriageRegistration addApplicationDocumentsItem(Document applicationDocumentsItem) {
          if (this.applicationDocuments == null) {
          this.applicationDocuments = new ArrayList<>();
          }
          if(!this.applicationDocuments.contains(applicationDocumentsItem))
              this.applicationDocuments.add(applicationDocumentsItem);
          return this;
      }

      public MarriageRegistration addVerificationDocumentsItem(Document verificationDocumentsItem) {
          if (this.verificationDocuments == null) {
          this.verificationDocuments = new ArrayList<>();
          }
          if(!this.verificationDocuments.contains(verificationDocumentsItem))
              this.verificationDocuments.add(verificationDocumentsItem);
          return this;
      }
    
}
