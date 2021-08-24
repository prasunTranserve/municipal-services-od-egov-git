package org.egov.migration.business.model;

import java.util.ArrayList;
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
	
	private boolean water;
	
	private boolean sewerage;

	private WaterConnectionDTO waterConnection;
	
	private SewerageConnectionDTO sewerageConnection;
	
	private MeterReadingDTO meterReading;
	
	private List<DemandDTO> waterDemands;
	
	private List<DemandDTO> sewerageDemands;
	
//	public void addDemands(List<DemandDTO> demandList) {
//		if(demands == null) {
//			demands = new ArrayList<>();
//		}
//		demands.addAll(demandList);
//	}
	
}
