package org.egov.pt.migration.validation;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.egov.pt.migration.config.SystemProperties;
import org.egov.pt.migration.util.MigrationUtility;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class TaxHeadValidator implements ConstraintValidator<ValidTaxHead, String> {
	
	@Autowired
	SystemProperties properties;
	
	@Override
	public void initialize(ValidTaxHead constraintAnnotation) {
		properties = MigrationUtility.getSystemProperties();
	}

	@Override
	public boolean isValid(String taxHead, ConstraintValidatorContext context) {
		return taxHead != null && properties.getTaxhead().containsKey(taxHead.trim().toLowerCase().replace(" ", "_"));
	}

}
