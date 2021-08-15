package org.egov.migration.reader.model;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import org.egov.migration.util.MigrationConst;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class Owner {

	private String salutation;
	
	@Pattern(regexp = MigrationConst.OWNER_NAME_PATTERN, message = "OwnerName not a valid name")
	@Size(max = 50, message = "Owner name can not be greater than 50 character")
	@NotEmpty(message = "Owner name can not be blank/empty")
	private String ownerName;
	
	private String gender;
	
	private String dob;
	
	private String mobileNumber;
	
	@Email(message = "Owner Email is not valid", regexp = "(?:[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*|\"(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21\\x23-\\x5b\\x5d-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])*\")@(?:(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?|\\[(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?|[a-z0-9-]*[a-z0-9]:(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21-\\x5a\\x53-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])+)\\])")
	private String email;
	
	@NotEmpty(message = "Owner creation date can not be blank/empty")
	private String createdDate;
	
	private String status;
	
	private String gurdianName;
	
	private String relationship;
	
	private String ulb;
	
	private String propertyId;
	
	private String primaryOwner;
	
	private String ownerType;
	
	private String ownerPercentage;
}
