CREATE TABLE eg_mr_application_audit
(
    id character varying(64) ,
    accountid character varying(64) ,
    tenantid character varying(64) ,
    mrnumber character varying(64) ,
    applicationnumber character varying(64) ,
    applicationType character varying(64) ,
    applicationdate bigint,
    marriagedate bigint,
    issueddate bigint,
    action character varying(64) ,
    status character varying(64) ,
    createdby character varying(64) ,
    lastmodifiedby character varying(64) ,
    createdtime bigint,
    lastmodifiedtime bigint,
    businessservice character varying(64) ,
    workflowcode character varying(64)

);


CREATE TABLE eg_mr_marriageplace_audit(

  id character varying(64),
  ward character varying(64),
  pincode character varying(64),
  placeofmarriage character varying(64),
  locality character varying(64),
  additionalDetail JSONB,
  mr_id character varying(256),
  createdBy character varying(64),
  lastModifiedBy character varying(64),
  createdTime bigint,
  lastModifiedTime bigint


);


CREATE TABLE eg_mr_couple_audit(
  id character varying(64),
  tenantId character varying(256),
  mr_id character varying(64),
  isdivyang boolean,
  isgroom boolean,
  title character varying(64),
  firstname character varying(256),
  dateofbirth bigint,
  fathername character varying(64),
  mothername character varying(64), 
  createdby character varying(64),
  createdtime bigint,
  lastmodifiedby character varying(64),
  lastmodifiedtime bigint

);

CREATE TABLE eg_mr_address_audit(
  id character varying(64),
  tenantId character varying(256),
  mr_couple_id character varying(64),
  addressline1 character varying(256),
  country character varying(64),
  state character varying(256),
  district character varying(256),
  pincode character varying(256),
  contact character varying(256),
  emailaddress character varying(64),
  createdby character varying(64),
  createdtime bigint,
  lastmodifiedby character varying(64),
  lastmodifiedtime bigint

  );
  
  
  
CREATE TABLE eg_mr_gaurdiandetails_audit(
  id character varying(64),
  tenantId character varying(256),
  mr_couple_id character varying(64),
  addressId character varying(64),
  relationship character varying(64),
  name character varying(256),
  addressline1 character varying(256),
  country character varying(64),
  state character varying(256),
  district character varying(256),
  pincode character varying(256),
  contact character varying(256),
  emailaddress character varying(64),
  createdby character varying(64),
  createdtime bigint,
  lastmodifiedby character varying(64),
  lastmodifiedtime bigint

  );
  
  CREATE TABLE eg_mr_witness_audit(
  id character varying(64),
  tenantId character varying(256),
  mr_couple_id character varying(64),
  title character varying(64), 
  address character varying(256),
  firstname character varying(256),
  country character varying(64),
  state character varying(256),
  district character varying(256),
  pincode character varying(256),
  contact character varying(256),
  createdby character varying(64),
  createdtime bigint,
  lastmodifiedby character varying(64),
  lastmodifiedtime bigint
  );