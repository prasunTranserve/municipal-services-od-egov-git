ALTER TABLE eg_sw_service
ADD COLUMN createdBy character varying(64),
ADD COLUMN lastModifiedBy character varying(64),
ADD COLUMN createdTime bigint,
ADD COLUMN lastModifiedTime bigint;

ALTER TABLE eg_sw_plumberinfo
ADD COLUMN createdBy character varying(64),
ADD COLUMN lastModifiedBy character varying(64),
ADD COLUMN createdTime bigint,
ADD COLUMN lastModifiedTime bigint;