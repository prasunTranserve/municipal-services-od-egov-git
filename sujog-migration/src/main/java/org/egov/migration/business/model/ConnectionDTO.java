package org.egov.migration.business.model;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@ToString
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ConnectionDTO {

	private WaterConnectionDTO water;
	
	private SewerageConnectionDTO sewerage;
	
	private List<DemandDTO> demands;
	
	
}
