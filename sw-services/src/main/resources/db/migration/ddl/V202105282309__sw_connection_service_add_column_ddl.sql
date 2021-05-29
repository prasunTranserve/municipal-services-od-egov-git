ALTER TABLE eg_sw_service
ADD COLUMN IF NOT EXISTS usageCategory character varying(256),
ADD COLUMN IF NOT EXISTS connectionCategory character varying(256),
ADD COLUMN IF NOT EXISTS noOfFlats INTEGER;

ALTER TABLE eg_sw_service_audit
ADD COLUMN IF NOT EXISTS usageCategory character varying(256),
ADD COLUMN IF NOT EXISTS connectionCategory character varying(256),
ADD COLUMN IF NOT EXISTS noOfFlats INTEGER; 

ALTER TABLE eg_sw_connection 
ALTER COLUMN property_id DROP NOT NULL;

ALTER TABLE eg_sw_connection_audit 
ALTER COLUMN property_id DROP NOT NULL;