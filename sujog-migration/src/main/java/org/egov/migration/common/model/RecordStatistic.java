package org.egov.migration.common.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class RecordStatistic {
	
	private Map<String, List<String>> errorRecords = new HashMap<String, List<String>>();
	
	private Map<String, Map<String, String>> successRecords = new HashMap<>();
	
	private Map<String, String> fileNotProcessed = new HashMap<>();
	
	private String successFile;
	
	private String errorFile;

	private String startTime;
	
	private String endTime;
}
