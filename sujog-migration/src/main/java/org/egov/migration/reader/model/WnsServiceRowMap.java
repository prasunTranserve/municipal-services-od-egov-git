package org.egov.migration.reader.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class WnsServiceRowMap {
	
	private String connectionNo;
	
	private String connectionType;
	
	private String createdDate;
	
	private int rowNum;

}
