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
public class WnsConnectionHolderRowMap {
	
	private String connectionNo;
	
	private String createdDate;
	
	private int rowNum;
}
