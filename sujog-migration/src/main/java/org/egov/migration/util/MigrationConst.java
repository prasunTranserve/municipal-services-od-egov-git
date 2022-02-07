package org.egov.migration.util;

import java.util.Arrays;
import java.util.List;

public class MigrationConst {
	
	public static final String SOURCE_MUNICIPAL_RECORDS = "MUNICIPAL_RECORDS";
	public static final String CHANNEL_CFC_COUNTER = "CFC_COUNTER";
	public static final String MIGRATE = "MIGRATE";
	public static final String SHEET_PROPERTY = "PROPERTY";
	public static final String SHEET_OWNER = "OWNER";
	public static final String SHEET_PROPERTY_UNIT = "UNIT";
	public static final String SHEET_ADDRESS = "ADDRESS";
	public static final String SHEET_ASSESSMENT = "ASSESSMENT";
	public static final String SHEET_DEMAND = "DEMAND";
	public static final String SHEET_DEMAND_DETAIL = "DEMAND_DETAILS";
	
	public static final String SHEET_CONNECTION = "CONNECTION_APPLICATION";
	public static final String SHEET_CONNECTION_SERVICE = "CONNECTION_SERVICE";
	public static final String SHEET_CONNECTION_HOLDER = "HOLDER";
	public static final String SHEET_METER_READING = "METER_READING";
	
	public static final String COL_PROPERTY_ID = "PROPERTY_ID";
	public static final String COL_ULB = "ULB_NAME";
	public static final String COL_STATUS = "STATUS";
	public static final String COL_PROPERTY_TYPE = "PROPERTY_TYPE";
	public static final String COL_OWNERSHIP_CATEGORY = "OWNERSHIP_CATEGORY";
	public static final String COL_USAGE_CATEGORY = "USAGE_CATEGORY";
	public static final String COL_FLOOR_NO = "FLOOR_NO";
	public static final String COL_LAND_AREA = "LAND_AREA";
	public static final String COL_PLOT_SIZE_UNIT = "PLOT_SIZE_UNIT";
	public static final String COL_SUPER_BUILTUP_AREA = "SUPER_BUILTUP_AREA";
	public static final String COL_BUILTUP_AREA_UNIT = "BUILTUP_AREA_UNIT";
	public static final String COL_CREATED_DATE = "CREATED_DATE";
	
	public static final String COL_SALUTATION = "SALUTATION";
	public static final String COL_OWNER_NAME = "OWNER_NAME";
	public static final String COL_GENDER = "GENDER";
	public static final String COL_DOB = "DOB";
	public static final String COL_MOBILE_NUMBER = "MOBILE_NUMBER";
	public static final String COL_OWNER_EMAIL = "OWNER_EMAIL";
	public static final String COL_GUARDIAN_NAME = "GUARDIAN_NAME";
	public static final String COL_RELATIONSHIP_WITH_THE_GUARDIAN = "RELATIONSHIP_WITH_THE_GUARDIAN";
	public static final String COL_PRIMARY_OWNER = "PRIMARY_OWNER";
	public static final String COL_OWNER_TYPE = "OWNER_TYPE";
	public static final String COL_OWNERSHIP_PERCENTAGE = "OWNERSHIP_PERCENTAGE";
	
	public static final String COL_DOOR_NO = "DOOR_NO";
	public static final String COL_PLOT_NO = "PLOT_NO";
	public static final String COL_BUILDING_NAME = "BUILDING_NAME";
	public static final String COL_ADDRESS_LINE1 = "ADDRESS_LINE1";
	public static final String COL_ADDRESS_LINE2 = "ADDRESS_LINE2";
	public static final String COL_LANDMARK = "LANDMARK";
	public static final String COL_CITY = "CITY";
	public static final String COL_PIN_CODE = "PIN_CODE";
	public static final String COL_LOCALITY = "LOCALITY";
	public static final String COL_DISTRICT_NAME = "DISTRICT_NAME";
	public static final String COL_REGION = "REGION";
	public static final String COL_STATE = "STATE";
	public static final String COL_COUNTRY = "COUNTRY";
	public static final String COL_WARD = "WARD";
	public static final String COL_ADDITIONAL_DETAILS = "ADDITIONAL_DETAILS";
	
	public static final String COL_FINANCIAL_YEAR = "FINANCIAL_YEAR";
	public static final String COL_ASSESSMENT_DATE = "ASSESSMENT_DATE";
	
	public static final String COL_PAYER_NAME = "PAYER_NAME";
	public static final String COL_TAX_PERIOD_FROM = "TAX_PERIOD_FROM";
	public static final String COL_TAX_PERIOD_TO = "TAX_PERIOD_TO";
	public static final String COL_MINIMUM_AMOUNT_PAYBLE = "MINIMUM_AMOUNT_PAYBLE";
	public static final String COL_IS_PAYMENT_COMPLETED = "IS_PAYMENT_COMPLETED";
	
	public static final String COL_TAX_HEAD = "TAX_HEAD";
	public static final String COL_TAX_AMOUNT = "TAX_AMOUNT";
	public static final String COL_COLLECTION_AMOUNT = "COLLECTION_AMOUNT";
	public static final String COL_UNIT_TYPE = "UNIT_TYPE";
	public static final String COL_OCCUPANCY_TYPE = "OCCUPANCY_TYPE";
	public static final String COL_BUILTUP_AREA = "BUILTUP_AREA";
	public static final String COL_RENT = "RENT";
	
	public static final String COL_CONNECTION_APPLICATION_NO = "CONNECTION_APPLICATION_NO";
	public static final String COL_CONNECTION_FACILTY = "CONNECTION_FACILTY";
	public static final String COL_CONNECTION_APPLICATION_STATUS = "CONNECTION_APPLICATION_STATUS";
	public static final String COL_CONNECTION_STATUS = "CONNECTION_STATUS";
	public static final String COL_CONNECTION_NO = "CONNECTION_NO";
	public static final String COL_APPLICATION_TYPE = "APPLICATION_TYPE";
	public static final String COL_CONNECTION_CATEGORY = "CONNECTION_CATEGORY";
	public static final String COL_CONNECTION_TYPE = "CONNECTION_TYPE";
	public static final String COL_WATER_SOURCE = "WATER_SOURCE";
	public static final String COL_METER_SERIAL_NO = "METER_SERIAL_NO";
	public static final String COL_METER_INSTALLATION_DATE = "METER_INSTALLATION_DATE";
	public static final String COL_ACTUAL_PIPE_SIZE = "ACTUAL_PIPE_SIZE";
	public static final String COL_NO_OF_TAPS = "NO_OF_TAPS";
	public static final String COL_CONNECTION_EXECUTION_DATE = "CONNECTION_EXECUTION_DATE";
	public static final String COL_PROPOSED_PIPE_SIZE = "PROPOSED_PIPE_SIZE";
	public static final String COL_PROPOSED_TAPS = "PROPOSED_TAPS";
	public static final String COL_LAST_METER_READING = "LAST_METER_READING";
	public static final String COL_NO_OF_FLATS = "NO_OF_FLATS";
	public static final String COL_NO_OF_CLOSETS = "NO_OF_CLOSETS";
	public static final String COL_NO_OF_TOILETS = "NO_OF_TOILETS";
	public static final String COL_PROPOSED_WATER_CLOSETS = "PROPOSED_WATER_CLOSETS";
	public static final String COL_PROPOSED_TOILETS = "PROPOSED_TOILETS";
	public static final String COL_HOLDER_NAME = "HOLDER_NAME";
	public static final String COL_HOLDER_ADDRESS ="HOLDER_ADDRESS";
	public static final String COL_CONNECTION_HOLDER_TYPE = "CONNECTION_HOLDER_TYPE";
	public static final String COL_BILLING_PERIOD = "BILLING_PERIOD";
	public static final String COL_PREVIOUS_READING = "PREVIOUS_READING";
	public static final String COL_PREVIOUS_READING_DATE = "PREVIOUS_READING_DATE";
	public static final String COL_CURRENT_READING = "CURRENT_READING";
	public static final String COL_CURRENT_READING_DATE = "CURRENT_READING_DATE";
	public static final String COL_BILLING_PERIOD_FROM = "BILLING_PERIOD_FROM";
	public static final String COL_BILLING_PERIOD_TO = "BILLING_PERIOD_TO";
	public static final String COL_WATER_CHARGES = "WATER_CHARGES";
	public static final String COL_SEWERAGE_FEE = "SEWERAGE_FEE";
	public static final String COL_GUARDIAN_RELATION = "GUARDIAN_RELATION";
	public static final String COL_CONSUMER_CATEGORY = "CONSUMER_CATEGORY";
	public static final String COL_METER_STATUS = "METER_STATUS";
	public static final String COL_METER_MAKE = "METER_MAKE";
	public static final String COL_METER_READING_RATIO = "METER_READING_RATIO";
	public static final String COL_LAST_MODIFIED_ON = "LAST_MODIFIED_ON";
	
	public static final String DEFAULT_OWNER_TYPE = "NONE";
	
	public static final String AMT_DUE = "DUE";
	public static final String AMT_COLLECTED = "PAID";
	public static final String OCCUPANCY_TYPE_SELFOCCUPIED = "SELFOCCUPIED";
	public static final String OCCUPANCY_TYPE_RENTED = "RENTED";
	
	public static final String CONNECTION_WATER = "Water";
	public static final String CONNECTION_SEWERAGE = "Sewerage";
	public static final String CONNECTION_WATER_SEWERAGE = "water_sewerage";
	
	public static final String CONNECTION_CATEGORY_PERMANENT = "PERMANENT";
	public static final String CONNECTION_CATEGORY_TEMPORARY = "TEMPORARY";
	
	public static final String CONNECTION_METERED = "Metered";
	public static final String CONNECTION_NON_METERED = "Non Metered";
	public static final String METER_WORKING = "Working";
	
	public static final String PROPERTY_ID = "PROPERTY_ID";
	public static final String ASSESSMENT_NUMBER = "ASSESSMENT_NUMBER";
	public static final String FETCH_BILL_STATUS = "FETCH_BILL_STATUS";
	
	public static final String WATER_CONNECTION_NO = "WATER_CONNECTION_NO";
	public static final String METER_READING = "METER_READING";
	public static final String DEMAND_WATER = "DEMAND_WATER";
	public static final String DEMAND_SEWERAGE = "DEMAND_SEWERAGE";
	public static final String SEWERAGE_CONNECTION_NO = "SEWERAGE_CONNECTION_NO";
	
	public static final String APPLICATION_APPROVED = "Application Approved";
	public static final String STATUS_ACTIVE = "Active";
	
	public static final String TAXHEAD_HOLDING_TAX_INPUT = "Holding Tax";
	public static final String TAXHEAD_HOLDING_TAX = "PT_HOLDING_TAX";
	
	public static final String OWNER_NAME_PATTERN = "^[a-zA-Z0-9 \\-'`\\.]*$";
	
	public static final String TAX_PERIOD_PATTERN = "\\d{4}-\\d{2}/Q\\d{1}";
	
	public static final String COL_LATEST_BILLING_PERIOD_FROM = "LATEST_BILLING_PERIOD_FROM";
	public static final String COL_LATEST_BILLING_PERIOD_TO = "LATEST_BILLING_PERIOD_TO";
	public static final String COL_LATEST_BILLING_WATER_CHARGES = "LATEST_BILLING_WATER_CHARGES";
	public static final String COL_LATEST_BILLING_SEWERAGE_FEE = "LATEST_BILLING_SEWERAGE_FEE";
	public static final String COL_ARREAR = "ARREAR";
	public static final String COL_COLLECTION_AMOUNT_AGAINST_LATEST_BILL = "COLLECTION_AMOUNT_AGAINST_LATEST_BILL";
	
	public static final List<String> METERED_VOLUMETRIC_CONNECTION = Arrays.asList("commercial", "industrial", "institutional", "apartment");
	
	public static final String USAGE_CATEGORY_OTHERS = "Others";

	public static final String SERVICE_WATER = "WATER";
	public static final String SERVICE_SEWERAGE = "SEWERAGE";
	public static final String SERVICE_WATER_SEWERAGE = "WATER-SEWERAGE";
	
}
