package org.egov.noc.web.model;

import java.util.List;

import org.egov.noc.thirdparty.nma.model.Document;
import org.springframework.validation.annotation.Validated;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Validated
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class NmaRequest {
	
	@JsonProperty("ApplicationUniqueNumebr")
	private String applicationUniqueNumebr;
	
	@JsonProperty("Comment")
	private String comment;
	
	@JsonProperty("Action")
	private String action;
	
	@JsonProperty("Documents")
	private List<Document> documents;
	

}
