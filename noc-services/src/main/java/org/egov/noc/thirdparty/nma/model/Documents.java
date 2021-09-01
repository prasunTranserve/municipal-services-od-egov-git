package org.egov.noc.thirdparty.nma.model;

import java.util.List;

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
public class Documents {
	@JsonProperty("FirmFiles")
	public List<Document> firmFiles;
	@JsonProperty("ModernConstructionsImage")
	public List<Document> modernConstructionsImage;
	@JsonProperty("GoogleEarthImage")
	public List<Document> googleEarthImage;
	@JsonProperty("OwnershipDocuments")
	public List<Document> ownershipDocuments;
	@JsonProperty("TermAndCondition")
	public String termAndCondition;
	
	
}
