package org.egov.migration.business.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@Data
@NoArgsConstructor
public class ConnectionResponse {

	@JsonProperty("WaterConnection")
	private List<WaterConnectionDTO> waterConnection = null;
	
	@JsonProperty("SewerageConnections")
	private List<SewerageConnectionDTO> sewerageConnections = null;
	
	@JsonProperty("meterReadings")
	private List<MeterReadingDTO> meterReadings = null;
	
	@JsonProperty("Demands")
	private List<DemandDTO> demands = null;
}
