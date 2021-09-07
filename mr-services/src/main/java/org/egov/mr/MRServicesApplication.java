package org.egov.mr;

import org.egov.tracer.config.TracerConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@ComponentScan(basePackages = { "org.egov.mr", "org.egov.mrcalculator" })
@Import({ TracerConfiguration.class })
public class MRServicesApplication {

	public static void main(String[] args) {
		SpringApplication.run(MRServicesApplication.class, args);
	}

}
