CREATE TABLE eg_mr_application
(
    id character varying(64) ,
    accountid character varying(64) ,
    tenantid character varying(64) ,
    mrnumber character varying(64) ,
    applicationnumber character varying(64) ,
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
    workflowcode character varying(64),

    CONSTRAINT pk_eg_mr_application PRIMARY KEY (id)   
);


CREATE TABLE eg_mr_marriageplace(

  id character varying(64),
  ward character varying(64),
  placeofmarriage character varying(64),
  locality character varying(64),
  additionalDetail JSONB,
  mr_id character varying(256),
  createdBy character varying(64),
  lastModifiedBy character varying(64),
  createdTime bigint,
  lastModifiedTime bigint,

  CONSTRAINT pk_eg_mr_marriageplace PRIMARY KEY (id),
  CONSTRAINT fk_eg_mr_marriageplace FOREIGN KEY (mr_id) REFERENCES eg_mr_application (id)
);



CREATE TABLE eg_mr_couple(
  id character varying(64),
  tenantId character varying(256),
  mr_id character varying(64),
  isdivyang boolean,
  isgroom boolean,
  title character varying(64),
  firstname character varying(64),
  middlename character varying(64),
  lastname character varying(64),
  dateofbirth bigint,
  fathername character varying(64),
  mothername character varying(64), 
  createdby character varying(64),
  createdtime bigint,
  lastmodifiedby character varying(64),
  lastmodifiedtime bigint,
  CONSTRAINT pk_eg_mr_coupledetails PRIMARY KEY (id),
  CONSTRAINT fk_eg_mr_coupledetails FOREIGN KEY (mr_id) REFERENCES eg_mr_application (id)
);

CREATE TABLE eg_mr_coupleaddress(
  id character varying(64),
  tenantId character varying(256),
  mr_couple_id character varying(64),
  addressline1 character varying(256),
  addressline2 character varying(256),
  addressline3 character varying(256),
  country character varying(64),
  state character varying(256),
  district character varying(256),
  pincode character varying(256),
  locality character varying(256),
  createdby character varying(64),
  createdtime bigint,
  lastmodifiedby character varying(64),
  lastmodifiedtime bigint,
  CONSTRAINT pk_eg_mr_coupleaddress PRIMARY KEY (id),
  CONSTRAINT fk_eg_mr_coupleaddress FOREIGN KEY (mr_couple_id) REFERENCES eg_mr_couple (id)
  );

CREATE TABLE eg_mr_gaurdiandetails(
  id character varying(64),
  tenantId character varying(256),
  mr_couple_id character varying(64),
  addressId character varying(64),
  isgroomsideguardian boolean,
  relationship character varying(64),
  name character varying(256),
  addressline1 character varying(256),
  addressline2 character varying(256),
  addressline3 character varying(256),
  country character varying(64),
  state character varying(256),
  district character varying(256),
  pincode character varying(256),
  locality character varying(64),
  contact character varying(256),
  emailaddress character varying(64),
  createdby character varying(64),
  createdtime bigint,
  lastmodifiedby character varying(64),
  lastmodifiedtime bigint,
  CONSTRAINT pk_eg_mr_gaurdiandetails PRIMARY KEY (id),
  CONSTRAINT fk_eg_mr_gaurdiandetails FOREIGN KEY (mr_couple_id) REFERENCES eg_mr_couple (id)
  );
  
  CREATE TABLE eg_mr_witness(
  id character varying(64),
  tenantId character varying(256),
  mr_id character varying(64),
  title character varying(64), 
  address character varying(256),
  firstname character varying(64),
  middlename character varying(64),
  lastname character varying(64),
  country character varying(64),
  state character varying(256),
  district character varying(256),
  pincode character varying(256),
  contact character varying(256),
  createdby character varying(64),
  createdtime bigint,
  lastmodifiedby character varying(64),
  lastmodifiedtime bigint,
  CONSTRAINT pk_eg_mr_witness PRIMARY KEY (id),
  CONSTRAINT fk_eg_mr_witness FOREIGN KEY (mr_id) REFERENCES eg_mr_application (id)
  );
  
  CREATE TABLE eg_mr_applicationdocument(
    id character varying(64),
    tenantId character varying(64),
    documentType character varying(64),
    filestoreid character varying(64),
    mr_id character varying(64),
    active boolean,
    createdBy character varying(64),
    lastModifiedBy character varying(64),
    createdTime bigint,
    lastModifiedTime bigint,

    CONSTRAINT pk_eg_mr_applicationdocument PRIMARY KEY (id),
    CONSTRAINT fk_eg_mr_applicationdocument FOREIGN KEY (mr_id) REFERENCES eg_mr_application (id)
);

CREATE TABLE eg_mr_verificationDocument(
    id character varying(64),
    tenantId character varying(64),
    documentType character varying(64),
    filestoreid character varying(64),
    mr_id character varying(64),
    active boolean,
    createdBy character varying(64),
    lastModifiedBy character varying(64),
    createdTime bigint,
    lastModifiedTime bigint,

    CONSTRAINT pk_eg_tl_VerificationDocument PRIMARY KEY (id),
    CONSTRAINT fk_eg_tl_VerificationDocument FOREIGN KEY (mr_id) REFERENCES eg_mr_application (id)
);

DROP SEQUENCE IF EXISTS SEQ_EG_MR_APL ;

DROP SEQUENCE IF EXISTS SEQ_EG_MR_MRN;

CREATE SEQUENCE SEQ_EG_MR_APL;
CREATE SEQUENCE SEQ_EG_MR_MRN;