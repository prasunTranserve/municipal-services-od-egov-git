
CREATE TABLE eg_tl_tradelicense_migration (
	id character varying(64) NOT NULL,
    applicationdate bigint,
    applicationstatus character varying(64),
    applicationno character varying(64) ,
    tenantid character varying(64) NOT NULL,
	tradetype character varying(256),
	tradesubtype character varying(256),
	tradename character varying(256),
	tradeunitmeasurementname character varying(64),
	tradeunitofmeasurementvalue character varying(64),
	licensetype character varying(64),
	licensenumber character varying(64),	
	commencementdate bigint,
	issueddate bigint,
	finacialyear character varying(64),
	validfromdate bigint,
	validtodate bigint,
	traderaddress character varying(1024),
	ward character varying(64),
	tradevillage character varying(256),
	tradecity character varying(256),
	pincode character varying(64),
	tradecategory character varying(64),
	tradeprimaryownername character varying(256),
	tradesecondaryownername character varying(256),
	authorizedpersonname character varying(256),
	tradeinstitutionofficialcorrespondanceaddress character varying(1024),
	ownermobilenumber character varying(64),
	tradeinstitutionphonenumber character varying(64)	,
	createdtime bigint,
	
	CONSTRAINT uk_eg_tradelicense_migration UNIQUE (id)
);


