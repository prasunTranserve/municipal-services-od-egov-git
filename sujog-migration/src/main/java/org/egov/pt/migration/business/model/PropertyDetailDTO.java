package org.egov.pt.migration.business.model;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PropertyDetailDTO {
	
	private PropertyDTO property;
	
	private AssessmentDTO assessment;
	
	private List<DemandDTO> demands;

}
