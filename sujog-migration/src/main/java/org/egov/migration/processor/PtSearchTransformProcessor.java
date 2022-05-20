package org.egov.migration.processor;

import org.egov.migration.business.model.PropertyDTO;
import org.egov.migration.business.model.PropertyDetailDTO;
import org.egov.migration.config.SystemProperties;
import org.egov.migration.reader.model.Property;
import org.egov.migration.util.MigrationUtility;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Autowired;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PtSearchTransformProcessor  implements ItemProcessor<Property, PropertyDetailDTO> {

	private String tenant;

	private String localityCode;
	
	@Autowired
	private SystemProperties properties;

	@Override
	public PropertyDetailDTO process(Property property) throws Exception {
		try {
			return transformProperty(property);
		} catch (Exception e) {
			MigrationUtility.addError(property.getPropertyId(), e.getMessage());
		}
		return null;
	}

	private PropertyDetailDTO transformProperty(Property property) {
		try {
			PropertyDTO propertyDTO = new PropertyDTO();
			transformProperty(propertyDTO, property);

			return PropertyDetailDTO.builder().property(propertyDTO).build();
		} catch (Exception e) {
			log.error(String.format("Some exception generated while reading property %s, Message: ",
					property.getPropertyId(), e.getMessage()));
			MigrationUtility.addError(property.getPropertyId(), e.getMessage());
			return null;
		}

	}

	private void transformProperty(PropertyDTO propertyDTO, Property property) {
		String ulb = property.getUlb().trim().toLowerCase();
		this.tenant = properties.getTenants().get(ulb);
		this.localityCode = this.properties.getLocalitycode().get(ulb);

		propertyDTO.setTenantId(this.tenant);
		propertyDTO.setPropertyId(property.getPropertyId());
	}

}
