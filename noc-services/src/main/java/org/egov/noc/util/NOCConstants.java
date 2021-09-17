package org.egov.noc.util;

import org.springframework.stereotype.Component;

@Component
public class NOCConstants {

	public static final String SEARCH_MODULE = "rainmaker-nocsrv";
	
	public static final String NOC_MODULE = "NOC";
	
	public static final String NOC_TYPE = "NocType";
	
	// mdms path codes

    public static final String NOC_JSONPATH_CODE = "$.MdmsRes.NOC";

    // error constants

	public static final String INVALID_TENANT_ID_MDMS_KEY = "INVALID TENANTID";

	public static final String INVALID_TENANT_ID_MDMS_MSG = "No data found for this tenentID";

	public static final String APPROVED_STATE = "APPROVED";	
	
	public static final String AUTOAPPROVED_STATE = "AUTO_APPROVED";	
	
	public static final String ACTION_APPROVE = "APPROVE";	
	
	public static final String ACTION_SUBMIT = "SUBMIT";	
	
	public static final String ACTION_AUTO_APPROVE="AUTO_APPROVE";
	
	public static final String MODE = "mode";	
	
	public static final String ONLINE_MODE = "online";	
	
	public static final String OFFLINE_MODE = "offline";
	
	public static final String THIRD_PARTY_MODE = "thirdParty";
	
	public static final String ONLINE_WF = "onlineWF";	

	public static final String OFFLINE_WF = "offlineWF";

	public static final String THIRD_PARTY_WF = "thirdPartyWF";
	
	public static final String ACTION_REJECT = "REJECT";	
	
	public static final String WORKFLOWCODE = "workflowCode";	
	
    public static final String NOCTYPE_JSONPATH_CODE = "$.MdmsRes.NOC.NocType";
    
    public static final String NOC_DOC_TYPE_MAPPING = "DocumentTypeMapping";
    
	public static final String DOCUMENT_TYPE = "DocumentType";
	
	public static final String COMMON_MASTERS_MODULE = "common-masters";
	    
	public static final String COMMON_MASTER_JSONPATH_CODE = "$.MdmsRes.common-masters";
	
	 public static final String CREATED_STATUS = "CREATED";	
	    
	public static final String ACTION_VOID = "VOID";	
	
	public static final String VOIDED_STATUS = "VOIDED";	
	
	public static final String ACTION_INITIATE = "INITIATE";	

	public static final String ACTION_INPROGRESS = "INPROGRESS";	

	public static final String INITIATED_TIME = "SubmittedOn";	
	
	//sms notification

	public static final String ACTION_STATUS_CREATED = "null_CREATED";
	
	public static final String ACTION_STATUS_INITIATED = "INITIATE_INPROGRESS";
	
	public static final String ACTION_STATUS_REJECTED = "REJECT_REJECTED";
	
	public static final String ACTION_STATUS_APPROVED = "APPROVE_APPROVED";
	
	public static final String FIRE_NOC_TYPE = "FIRE_NOC";
	
	public static final String AIRPORT_NOC_TYPE = "AIRPORT_AUTHORITY";
	
	public static final String NMA_NOC_TYPE = "NMA_NOC";

	public static final String PARSING_ERROR = "PARSING ERROR";
	
	public static final String DOC_TYPE_APPL_SALEGIFTDEED="APPL.SALEGIFTDEED";
	
	public static final String DOC_TYPE_APPL_OWNERIDPROOF="APPL.OWNERIDPROOF";
	
	public static final String DOC_TYPE_BPD_BPL="BPD.BPL";
	
	public static final String DOC_TYPE_BPD_SITEPHOTO="BPD.SITEPHOTO";//
	
	public static final String DOC_TYPE_APPL_ROR="APPL.ROR";
	
	public static final String DOC_TYPE_NOC_NMA_ELEVATION="NOC.NMA.Elevation";
	
	public static final String DOC_TYPE_NOC_NMA_IN_CASE_OF_REPAIRS="NOC.NMA.InCaseOfRepairs";
	
	public static final String DOC_TYPE_NOC_NMA_SIGNATURE="NOC.NMA.Signature";
	
	public static final String DOC_TYPE_NOC_NMA_OTHER="NOC.NMA.Other";
	
	public static final String DOC_TYPE_NOC_NMA_SECTION="NOC.NMA.Section";
	
	public static final String DOC_TYPE_NOC_NMA_FIRM_FILES="NOC.NMA.FirmFiles";
	
	public static final String DOC_TYPE_NOC_NMA_GOOGLE_EARTH_IMAGE="NOC.NMA.GoogleEarthImage";
	
	public static final String EDCR_ERROR="EDCR_ERROR";
	
	public static final String NMA_ERROR="NMA_ERROR";
	
	public static final String FILE_STORE_ERROR="FILE_STORE_ERROR";
	
	public static final String NEW_CONSTRUCTION="NEW_CONSTRUCTION";
	
	public static final String ALTERATION="ALTERATION";
	
	public static final String NMA_STATUS_CREATED="Application Received";
	
	public static final String FIRE_NOC_ERROR="FIRE_NOC_ERROR";
}
