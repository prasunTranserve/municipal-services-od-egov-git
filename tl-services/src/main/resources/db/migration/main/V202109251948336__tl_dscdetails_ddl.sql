CREATE TABLE eg_tl_dscdetails(
    id character varying(64),
    tenantid character varying(64),
    documenttype character varying(64),
    documentid character varying(64),
    tradeLicenseDetailId character varying(64),
    applicationnumber character varying(64),
    approvedby character varying(64),
	additionalDetail JSONB,
    createdBy character varying(64),
    lastModifiedBy character varying(64),
    createdTime bigint,
    lastModifiedTime bigint,

    CONSTRAINT uk_eg_tl_dscdetails PRIMARY KEY (id),
    CONSTRAINT fk_eg_tl_dscdetails FOREIGN KEY (tradeLicenseDetailId) REFERENCES eg_tl_TradeLicenseDetail (id)
);

CREATE INDEX IF NOT EXISTS index_eg_tl_dscdetails_tenantid ON eg_tl_dscdetails (tenantid);

CREATE INDEX IF NOT EXISTS index_eg_tl_dscdetails_approvedby ON eg_tl_dscdetails (approvedby);

CREATE INDEX IF NOT EXISTS index_eg_tl_dscdetails_documentid ON eg_tl_dscdetails (documentid);