package org.egov.pt.migration.validation;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.egov.pt.migration.config.SystemProperties;
import org.egov.pt.migration.util.MigrationUtility;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class TenantValidator implements ConstraintValidator<ValidTenant, String> {
	
	@Autowired
	SystemProperties properties;
	
	@Override
	public void initialize(ValidTenant constraintAnnotation) {
		properties = MigrationUtility.getSystemProperties();
	}

	@Override
	public boolean isValid(String tenant, ConstraintValidatorContext context) {
		return tenant != null && properties.getTenants().containsKey(tenant.toLowerCase());
	}

}
