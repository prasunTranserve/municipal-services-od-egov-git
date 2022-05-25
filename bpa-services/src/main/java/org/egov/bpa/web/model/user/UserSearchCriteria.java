package org.egov.bpa.web.model.user;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserSearchCriteria {

	@JsonProperty("tenantId")
	public String tenantId;
	
	@JsonProperty("roles")
    private List<String> roles;
	
	@JsonProperty("isActive")
	public Boolean isActive;
	
}
