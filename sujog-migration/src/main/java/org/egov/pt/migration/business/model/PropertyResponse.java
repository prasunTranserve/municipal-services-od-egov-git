package org.egov.pt.migration.business.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@AllArgsConstructor
@Data
@NoArgsConstructor
public class PropertyResponse {

    @JsonProperty("Properties")
    List<PropertyDTO> properties;

}
