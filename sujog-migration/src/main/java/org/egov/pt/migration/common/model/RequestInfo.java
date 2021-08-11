package org.egov.pt.migration.common.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@ToString
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RequestInfo {
	@JsonProperty("apiId")
	private String apiId;
	
	@JsonProperty("ver")
	private String ver;
	
	@JsonProperty("ts")
	private String ts;
	
	@JsonProperty("action")
	private String action;
	
	@JsonProperty("did")
	private String did;
	
	@JsonProperty("key")
	private String key;
	
	@JsonProperty("msgId")
	private String msgId;
	
	@JsonProperty("authToken")
	private String authToken;
}
