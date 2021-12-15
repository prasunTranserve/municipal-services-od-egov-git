ALTER TABLE eg_ws_service
ADD COLUMN IF NOT EXISTS connectionFacility  character varying(32),
ADD COLUMN IF NOT EXISTS noOfWaterClosets integer,
ADD COLUMN IF NOT EXISTS noOfToilets integer,
ADD COLUMN IF NOT EXISTS proposedWaterClosets integer,
ADD COLUMN IF NOT EXISTS proposedToilets integer;

ALTER TABLE eg_ws_service_audit
ADD COLUMN IF NOT EXISTS connectionFacility  character varying(32),
ADD COLUMN IF NOT EXISTS noOfWaterClosets integer,
ADD COLUMN IF NOT EXISTS noOfToilets integer,
ADD COLUMN IF NOT EXISTS proposedWaterClosets integer,
ADD COLUMN IF NOT EXISTS proposedToilets integer; 

