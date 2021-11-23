CREATE TABLE eg_tl_billingslab_audit (
tenantid varchar,
id varchar,
licensetype varchar,
structuretype  varchar,
tradetype varchar,
accessorycategory varchar,
type varchar,
uom varchar,
fromUom FLOAT8,
toUom FLOAT8,
rate numeric(12,2),
createdtime bigint,
createdby varchar,
lastmodifiedtime bigint,
lastmodifiedby varchar,
applicationType character varying(64),

CONSTRAINT pk_tl_billingslab_audit  PRIMARY KEY(id,tenantid) );

CREATE INDEX IF NOT EXISTS index_eg_tl_billingslab_audit_tenantid ON eg_tl_billingslab_audit (tenantid);

CREATE INDEX IF NOT EXISTS index_eg_tl_billingslab_audit_accessorycategory ON eg_tl_billingslab_audit (accessorycategory);

CREATE INDEX IF NOT EXISTS index_eg_tl_billingslab_audit_tradetype ON eg_tl_billingslab_audit (tradetype);

CREATE INDEX IF NOT EXISTS index_eg_tl_billingslab_audit_fromUom ON eg_tl_billingslab_audit (fromUom);

CREATE INDEX IF NOT EXISTS index_eg_tl_billingslab_audit_toUom ON eg_tl_billingslab_audit (toUom);

CREATE INDEX IF NOT EXISTS index_eg_tl_billingslab_audit_uom ON eg_tl_billingslab_audit (uom);