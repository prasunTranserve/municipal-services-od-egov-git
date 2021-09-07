CREATE TABLE eg_mr_billingslab (
tenantid varchar,
id varchar,
rate numeric(12,2),
createdtime bigint,
createdby varchar,
lastmodifiedtime bigint,
lastmodifiedby varchar,

CONSTRAINT pk_mr_billingslab  PRIMARY KEY(id,tenantid) );