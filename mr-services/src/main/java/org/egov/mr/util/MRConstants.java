package org.egov.mr.util;

public class MRConstants {

	public static  final String businessService_MR = "MR";
	

    public static final String ACTION_INITIATE = "INITIATE";

    public static final String ACTION_APPLY  = "APPLY";

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

	    //TL types

	    public static final String APPLICATION_TYPE_CORRECTION = "CORRECTION";

	    public static final String APPLICATION_TYPE_NEW = "NEW";


}



