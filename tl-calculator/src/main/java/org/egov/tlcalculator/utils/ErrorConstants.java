package org.egov.tlcalculator.utils;

import org.springframework.stereotype.Component;

@Component
public class ErrorConstants {
	
	public static final String MULTIPLE_TENANT_CODE = "MULTIPLE_TENANTS";
	public static final String MULTIPLE_TENANT_MSG = "All Billingslabs being created must belong to one single tenant";
	
	public static final String INVALID_TRADETYPE_CODE = "INVALID_TRADETYPE";
	public static final String INVALID_TRADETYPE_MSG = "The following TradeType is invalid";
	
	public static final String INVALID_ACCESSORIESCATEGORY_CODE = "INVALID_ACCESSORIESCATEGORY";
	public static final String INVALID_ACCESSORIESCATEGORY_MSG = "The following AccessoriesCategory is invalid";
	
	public static final String INVALID_STRUCTURETYPE_CODE = "INVALID_STRUCTURETYPE";
	public static final String INVALID_STRUCTURETYPE_MSG = "The following StructureType is invalid";
	
	public static final String INVALID_UOM_CODE = "INVALID_UOM";
	public static final String INVALID_UOM_MSG = "The following UOM is invalid";
	
	public static final String DUPLICATE_SLABS_CODE = "DUPLICATE_BILLINGSLABS";
	public static final String DUPLICATE_SLABS_MSG = "The following Billing slabs are already available in the system";
	
	public static final String FROMUOM_TOUOM_ERROR_CODE = "FROMUOM_TOUOM_ERROR";
	public static final String FROMUOM_TOUOM_ERROR_MSG = "There are already billing slabs present between these fromUom and toUom values";
	
	public static final String DUPLICATE_UOM_ERROR_CODE = "DUPLICATE_UOM_ERROR";
	public static final String DUPLICATE_UOM_ERROR_MSG = "The Uom value is already present in the create billing slab request for this trade type , uom value :";
	
	public static final String DELETE_BILLING_SLAB_ERROR_CODE = "DELETE_BILLING_SLAB_ERROR";
	public static final String DELETE_BILLING_SLAB_ERROR_MSG = "The Create Billing slab is already present in DB , To update it send that Billing slab in delete json and create json , id of billing slab ";
	
	public static final String INVALID_IDS_CODE = "INVALID_IDS_UPDATE";
	public static final String INVALID_IDS_MSG = "The following Billing slabs are not available in the system, IDS";
	
	public static final String INVALID_SLAB_CODE = "INVALID_SLAB";
	public static final String INVALID_SLAB_MSG = "Billing slab must contain either TradeType OR AccesoriesCategory, not both.";
	
	public static final String  UOM_MISMATCH_ERROR_CODE = "UOM_MISMATCH_ERROR";
	public static final String  UOM_MISMATCH_ERROR_MESSAGE = "There is mismatch in uom values in this tradetype ";
	
	public static final String  UOM_ERROR_CODE = "UOM_ERROR";
	public static final String  UOM_ERROR_MESSAGE = "UOM should be null in case of UOM is YEAR and not empty";
	
	public static final String FROMUOM_TOUOM_NULL_ERROR_CODE = "FROMUOM_TOUOM_NULL_ERROR";
	public static final String FROMUOM_TOUOM_NULL_ERROR_MSG = "If UOM is null then fromUom and toUom should also be null";
	
	public static final String FROMUOM_LESSTHAN_TOUOM_ERROR_CODE = "FROMUOM_LESSTHAN_TOUOM_ERROR";
	public static final String FROMUOM_LESSTHAN_TOUOM_ERROR_MSG = "FromUom should be less than ToUom";
	
	public static final String FROMUOM_NEGAVTIVE_ERROR_CODE = "FROMUOM_NEGAVTIVE_ERROR";
	public static final String FROMUOM_NEGAVTIVE_ERROR_MSG = "From Uom should not be less than 0";
	
	public static final String TOUOM_NEGAVTIVE_ERROR_CODE = "TOUOM_NEGAVTIVE_ERROR";
	public static final String TOUOM_NEGAVTIVE_ERROR_MSG = "To Uom should not be less than 0";
	
	public static final String FROMUOM_ERROR_CODE = "FROMUOM_ERROR";
	public static final String FROMUOM_ERROR_MSG = "If UOM is not null then FROMUOM Should not be null";
	
	public static final String TOUOM_ERROR_CODE = "TOUOM_ERROR";
	public static final String TOUOM_ERROR_MSG = "If UOM is not null then TOUOM Should not be null";
	
	public static final String  TENANTID_ERROR_CODE = "TENANTID_ERROR";
	public static final String  TENANTID_ERROR_MESSAGE = "Tenant ID is mandatory";
	
	public static final String  APPLICATION_TYPE_ERROR_CODE = "APPLICATION_TYPE_ERROR";
	public static final String  APPLICATION_TYPE_ERROR_MESSAGE = "Application type is mandatory";
	
	public static final String  LICENSE_TYPE_ERROR_CODE = "LICENSE_TYPE_ERROR";
	public static final String  LICENSE_TYPE_ERROR_MESSAGE = "License Type is mandatory";
	
	public static final String  TYPE_ERROR_CODE = "TYPE_ERROR";
	public static final String  TYPE_ERROR_MESSAGE = "Type is mandatory";
	
	public static final String  TYPE_RATE_ERROR_CODE = "TYPE_RATE_ERROR";
	public static final String  TYPE_RATE_ERROR_MESSAGE = "When UOM is null then type should be FLAT";
	
	public static final String  RATE_ERROR_CODE = "RATE_ERROR";
	public static final String  RATE_ERROR_MESSAGE = "Rate is mandatory";
	
	public static final String  RATE_NEGATIVE_ERROR_CODE = "RATE_NEGATIVE_ERROR";
	public static final String  RATE_NEGATIVE_ERROR_MESSAGE = "Rate Should be positive value";

}
