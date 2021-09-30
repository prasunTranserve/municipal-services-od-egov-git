package org.egov.mr.util;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class MRConstants {

	public static  final String businessService_MR = "MR";
	
	public static  final String businessService_MR_CORRECTION = "MRCORRECTION";
	
	public static final String ROLE_CODE_COUNTER_EMPLOYEE = "MR_CEMP";
	

    public static final String ACTION_INITIATE = "INITIATE";

    public static final String ACTION_APPLY  = "APPLY";
    
    
    public static final String ACTION_SCHEDULE  = "SCHEDULE";
    
    public static final String ACTION_RESCHEDULE  = "RESCHEDULE";

    public static final String ACTION_APPROVE  = "APPROVE";

    public static final String ACTION_REJECT  = "REJECT";
	
    public static final String TRIGGER_NOWORKFLOW  = "NOWORKFLOW";

    public static final String ACTION_CANCEL  = "CANCEL";


    public static final String ACTION_PAY  = "PAY";
    
    
    public static final String STATUS_INITIATED = "INITIATED";

    public static final String STATUS_APPLIED  = "APPLIED";

    public static final String STATUS_APPROVED  = "APPROVED";

    public static final String STATUS_REJECTED  = "REJECTED";


    public static final String STATUS_CANCELLED  = "CANCELLED";

    public static final String STATUS_PAID  = "PAID";

    public static final String BILL_AMOUNT_JSONPATH = "$.Bill[0].totalAmount";
    
	public static final String CITIZEN_SENDBACK_ACTION = "SENDBACKTOCITIZEN";

	
	public static final String MDMS_MARRIAGE_REGISTRATION = "MarriageRegistration";
	
	public static final String MDMS_REGISTRATION_FEE = "RegistrationFee";

	 
	 public static final String MDMS_TREASURY_CHARGE = "treasuryCharge";
	 
	 public static final String MDMS_ESTABLISHMENT_COST = "establishmentCost";
	 
	 public static final String TENANT_ID = "tenantId";
	 
	 public static final String MDMS_COST = "cost";
	 
	 public static final String MDMS_WITHIN_ONE_MONTH_OF_MARRIAGE = "withinOneMonthOfMarriage"; 
	 
	 public static final String MDMS_AFTER_ONE_MONTH_OF_MARRIAGE = "afterOneMonthOfMarriage";  
	 
	 public static final String MDMS_MARRIAGE_REGISTRATION_REGISTRATION_FEE = "$.MdmsRes.MarriageRegistration.RegistrationFee";

	   

	    public static final String APPLICATION_TYPE_CORRECTION = "CORRECTION";

	    public static final String APPLICATION_TYPE_NEW = "NEW";

	    public static final String ROLE_CITIZEN = "CITIZEN";



	    // ACTION_STATUS combinations for notification

	    public static final String ACTION_STATUS_INITIATED = "INITIATE_INITIATED";
	    
	    public static final String ACTION_STATUS_PENDINGPAYMENT  = "FORWARD_PENDINGPAYMENT";

	    public static final String ACTION_STATUS_DOCVERIFICATION  = "APPLY_DOCVERIFICATION";
	    
	    public static final String ACTION_STATUS_FORWARD_PENDINGAPPROVAL  = "FORWARD_PENDINGAPPROVAL";
	    
	    public static final String ACTION_STATUS_FORWARD_DOCVERIFICATION  = "FORWARD_DOCVERIFICATION";
	    
	    public static final String ACTION_STATUS_SENDBACKTOCITIZEN_DOCVERIFICATION  = "SENDBACKTOCITIZEN_CITIZENACTIONPENDINGATDOCVERIFICATION";
	    
	    public static final String ACTION_STATUS_SENDBACKTOCITIZEN_PENDINGAPPROVAL  = "SENDBACKTOCITIZEN_CITIZENACTIONPENDINGATAPPROVER";
	    
	    public static final String ACTION_STATUS_SENDBACKTOCITIZEN_PENDINGSCHEDULE  = "SENDBACKTOCITIZEN_CITIZENACTIONPENDINGATSCHEDULE";
	    
	    public static final String ACTION_STATUS_PAY_PENDING_SCHEDULE  = "PAY_PENDINGSCHEDULE";
	    
	    public static final String ACTION_STATUS_FORWARD_PENDING_SCHEDULE  = "FORWARD_PENDINGSCHEDULE";
	    
	    public static final String ACTION_STATUS_APPROVED  = "APPROVE_APPROVED";

	    public static final String ACTION_STATUS_REJECTED  = "REJECT_REJECTED";
	    
	    public static final String VARIABLE_ACTIVE = "active";

	    public static final String VARIABLE_USERACTIVE = "userActive";

	    public static final String ACTION_STATUS_PENDINGAPPROVAL  = "SCHEDULE_PENDINGAPPROVAL";
	    
	    public static final String ACTION_STATUS_RESCHEDULE  = "RESCHEDULE_PENDINGAPPROVAL";

	    public static final String ACTION_CANCEL_CANCELLED  = "CANCEL_CANCELLED";
	    
	    public static final String VARIABLE_ACTION = "action";

	    public static final String VARIABLE_STATUS = "status";

	    public static final String VARIABLE_ISSUED_DATE = "issuedDate";

	    public static final String VARIABLE_WFDOCUMENTS = "wfDocuments";

	    public static final String VARIABLE_CREATEDBY = "createdBy";

	    public static final String VARIABLE_LASTMODIFIEDBY = "lastModifiedBy";

	    public static final String VARIABLE_CREATEDTIME = "createdTime";

	    public static final String VARIABLE_LASTMODIFIEDTIME = "lastModifiedTime";

	    public static final String VARIABLE_LASTMODIFIEDDATE = "lastModifiedDate";

	    public static final String VARIABLE_COMMENT = "comment";
	    
	    public static final String VARIABLE_APPOINTMENT_DETAILS = "appointmentDetails";
	    
	    public static final List<String> FIELDS_TO_IGNORE = Collections.unmodifiableList(Arrays.asList(VARIABLE_ACTION,VARIABLE_WFDOCUMENTS,
	            VARIABLE_CREATEDBY,VARIABLE_LASTMODIFIEDBY,VARIABLE_APPOINTMENT_DETAILS,VARIABLE_CREATEDTIME,VARIABLE_LASTMODIFIEDTIME,VARIABLE_STATUS,VARIABLE_LASTMODIFIEDDATE,VARIABLE_ISSUED_DATE,VARIABLE_COMMENT));
	    
	    public static final String MODULE = "rainmaker-mr";

	    public static final String NOTIFICATION_LOCALE = "en_IN";

	    public static final String CORRECTION_NOTIFICATION_INITIATED = "mr.correction.en.counter.initiate";

	    public static final String CORRECTION_NOTIFICATION_APPLIED = "mr.correction.en.counter.submit";

	    public static final String CORRECTION_NOTIFICATION_PENDINGAPPROVAL = "mr.correction.en.pending.approval";

	    public static final String NOTIFICATION_PENDING_APPROVAL = "mr.en.pending.approval";
	    
	    public static final String NOTIFICATION_RESCHEDULE_PENDING_APPROVAL = "mr.en.reschedule.pending.approval";
	    
	    public static final String NOTIFICATION_PENDING_SCHEDULE = "mr.en.counter.pending.schedule";
	    
	    public static final String CORRECTION_NOTIFICATION_REJECTED = "mr.correction.en.counter.rejected";
	    
	    public static final String CORRECTION_NOTIFICATION_APPROVED = "mr.correction.en.counter.approved";
	    
	    public static final String CORRECTION_NOTIFICATION_DOC_VERIFICATION = "mr.correction.en.counter.doc.verification";
	    
	    public static final String CORRECTION_NOTIFICATION_FORWARD_DOC_VERIFICATION = "mr.correction.en.counter.forward.doc.verification";
	    
	    public static final String CORRECTION_NOTIFICATION_FORWARD_PENDINGAPPROVAL = "mr.correction.en.counter.forward.pending.approval";

	    public static final String NOTIFICATION_INITIATED = "mr.en.counter.initiate";

	    public static final String NOTIFICATION_APPLIED = "mr.en.counter.submit";
	    
	    public static final String NOTIFICATION_PENDINDG_PAYMENT = "mr.en.counter.pending.payment";
	    
	    public static final String NOTIFICATION_DOC_VERIFICATION = "mr.en.counter.doc.verification";
	    
	    public static final String NOTIFICATION_FORWARD_DOC_VERIFICATION = "mr.en.counter.forward.doc.verification";
	    
	    public static final String NOTIFICATION_FORWARD_PENDINGAPPROVAL = "mr.en.counter.forward.pending.approval";

	    public static final String NOTIFICATION_PAYMENT_OWNER = "mr.en.counter.payment.successful.owner";

	    public static final String NOTIFICATION_PAYMENT_PAYER = "mr.en.counter.payment.successful.payer";

	    public static final String NOTIFICATION_PAID = "mr.en.counter.pending.approval";

	    public static final String NOTIFICATION_APPROVED = "mr.en.counter.approved";

	    public static final String NOTIFICATION_REJECTED = "mr.en.counter.rejected";

	    public static final String NOTIFICATION_CANCELLED = "mr.en.counter.cancelled";

	    public static final String NOTIFICATION_FIELD_CHANGED = "mr.en.edit.field.change";

	    public static final String NOTIFICATION_OBJECT_ADDED = "mr.en.edit.object.added";

	    public static final String NOTIFICATION_OBJECT_REMOVED = "mr.en.edit.object.removed";

	    public static final String NOTIFICATION_OBJECT_MODIFIED = "mr.en.edit.object.modified";

	    public static final String NOTIFICATION_OBJECT_CORRECTION_MODIFIED = "mr.en.edit.object.correction.modified";

	    public static final String NOTIFICATION_SENDBACK_CITIZEN= "mr.en.sendback.citizen";
	    
	    public static final String CORRECTION_NOTIFICATION_SENDBACK_CITIZEN= "mr.correction.en.sendback.citizen";

	    public static final String NOTIFICATION_FORWARD_CITIZEN = "mr.en.forward.citizen";
	    
		public static final String  USREVENTS_EVENT_TYPE = "SYSTEMGENERATED";
		public static final String  USREVENTS_EVENT_NAME = "Marriage Registration";
		public static final String  USREVENTS_EVENT_POSTEDBY = "SYSTEM-MR";
		
		public static final String NOTIF_OWNER_NAME_KEY = "{OWNER_NAME}";
		
		
	    public static final String MARRIAGE_REGISTRATION_MODULE = "MarriageRegistration";

	    public static final String MARRIAGE_REGISTRATION_MODULE_CODE = "MR";
	    
	    public static final String PAYMENT_LINK_PLACEHOLDER="{PAYMENT_LINK}";

	    public static final String COMMON_MASTERS_MODULE = "common-masters";
	    
	    public static final String DEFAULT_OBJECT_MODIFIED_MSG = "Dear <1>, Your Marriage Registration with application number <APPLICATION_NUMBER> was modified.Thank You. Govt. of Odisha";

	    public static final String DEFAULT_OBJECT_CORRECTION_MODIFIED_MSG = "Dear <1>, Your Marriage Registration Correction with application number <APPLICATION_NUMBER> was modified.Thank You. Govt. of Odisha";

	    
	    public static final List<String> NOTIFICATION_CODES = Collections.unmodifiableList(Arrays.asList(
	    		CORRECTION_NOTIFICATION_DOC_VERIFICATION,CORRECTION_NOTIFICATION_FORWARD_DOC_VERIFICATION,NOTIFICATION_PENDINDG_PAYMENT,CORRECTION_NOTIFICATION_INITIATED, CORRECTION_NOTIFICATION_APPLIED , CORRECTION_NOTIFICATION_PENDINGAPPROVAL,
	            CORRECTION_NOTIFICATION_REJECTED,CORRECTION_NOTIFICATION_APPROVED,CORRECTION_NOTIFICATION_SENDBACK_CITIZEN,NOTIFICATION_INITIATED,NOTIFICATION_APPLIED ,NOTIFICATION_PENDING_APPROVAL,
	            NOTIFICATION_PAYMENT_OWNER,NOTIFICATION_FORWARD_DOC_VERIFICATION,NOTIFICATION_PAYMENT_PAYER,NOTIFICATION_PAID,NOTIFICATION_APPROVED,NOTIFICATION_REJECTED,NOTIFICATION_CANCELLED,NOTIFICATION_FIELD_CHANGED,NOTIFICATION_OBJECT_ADDED,
	            NOTIFICATION_OBJECT_REMOVED,NOTIFICATION_OBJECT_MODIFIED,NOTIFICATION_DOC_VERIFICATION ,NOTIFICATION_RESCHEDULE_PENDING_APPROVAL,NOTIFICATION_OBJECT_CORRECTION_MODIFIED,NOTIFICATION_SENDBACK_CITIZEN,NOTIFICATION_PENDING_SCHEDULE,
	            NOTIFICATION_FORWARD_CITIZEN,NOTIFICATION_FORWARD_PENDINGAPPROVAL,CORRECTION_NOTIFICATION_FORWARD_PENDINGAPPROVAL));
	    
	    public MRConstants() {}
	    
}



