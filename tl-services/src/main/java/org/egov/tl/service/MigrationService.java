package org.egov.tl.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.egov.tl.config.TLConfiguration;
import org.egov.tl.repository.TradeLicenseExcelRepository;
import org.egov.tl.repository.rowmapper.LegacyExcelRowMapper;
import org.egov.tl.web.models.excel.LegacyRow;
import org.egov.tl.web.models.excel.RowExcel;
import org.egov.tl.web.models.excel.TradeLicense;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class MigrationService {

	@Autowired
	private ExcelService excelService;

	@Autowired
	private LegacyExcelRowMapper legacyExcelRowMapper;

	@Autowired
	private TradeLicenseExcelRepository tradeLicenseExcelRepository ;
	
	@Autowired
	private TLConfiguration config;
	
	final ClassLoader loader = MigrationService.class.getClassLoader();



	public void importTradeLicenses( Long skip, Long limit) throws Exception {


		  final InputStream excelFile = loader.getResourceAsStream(config.getMigrationFileName());


		AtomicInteger numOfSuccess = new AtomicInteger();
		AtomicInteger numOfErrors = new AtomicInteger();
		
		List<TradeLicense>  migartionList = new ArrayList<TradeLicense>();

		excelService.read(excelFile, skip, limit, (RowExcel row) -> {
			LegacyRow legacyRow = null;

			try {
				legacyRow = legacyExcelRowMapper.map(row);
				
				

				TradeLicense migratedTradeLicense= new 	TradeLicense();		

				migratedTradeLicense.setId(UUID.randomUUID().toString());


				if(legacyRow.getApplicationDate()!=null && !legacyRow.getApplicationDate().trim().isEmpty())
				{
					BigDecimal exponentialValue = new BigDecimal(legacyRow.getApplicationDate());
					migratedTradeLicense.setApplicationdate(Long.parseLong(exponentialValue.toPlainString()));
				}

				if(legacyRow.getCommencementDate()!=null && !legacyRow.getCommencementDate().trim().isEmpty())
				{
					BigDecimal exponentialValue = new BigDecimal(legacyRow.getCommencementDate());
					if(!exponentialValue.toPlainString().equals("0.0"))
					migratedTradeLicense.setCommencementdate(Long.parseLong(exponentialValue.toPlainString()));
					else
						migratedTradeLicense.setCommencementdate(Long.parseLong("0"));
				}

				if(legacyRow.getIssuedDate()!=null && !legacyRow.getIssuedDate().trim().isEmpty())
				{
					BigDecimal exponentialValue = new BigDecimal(legacyRow.getIssuedDate());
					migratedTradeLicense.setIssueddate(Long.parseLong(exponentialValue.toPlainString()));
				}

				if(legacyRow.getValidFromDate()!=null && !legacyRow.getValidFromDate().trim().isEmpty())
				{
					BigDecimal exponentialValue = new BigDecimal(legacyRow.getValidFromDate());
					migratedTradeLicense.setValidfromdate(Long.parseLong(exponentialValue.toPlainString()));
				}

				if(legacyRow.getValidToDate()!=null && !legacyRow.getValidToDate().trim().isEmpty())
				{
					BigDecimal exponentialValue = new BigDecimal(legacyRow.getValidToDate());
					migratedTradeLicense.setValidtodate(Long.parseLong(exponentialValue.toPlainString()));
				}

				if(legacyRow.getTradeUnitOfMeasurementValue()!= null )
				{
					migratedTradeLicense.setTradeunitofmeasurementvalue(legacyRow.getTradeUnitOfMeasurementValue().trim());
				}


				if(legacyRow.getTradeWard()!= null )
				{
					migratedTradeLicense.setWard(legacyRow.getTradeWard().trim());
				}

				if(legacyRow.getPincode()!= null )
				{
					migratedTradeLicense.setPincode(legacyRow.getPincode().trim());
				}

				if(legacyRow.getOwnerMobileNumber()!= null )
				{
					migratedTradeLicense.setOwnermobilenumber(legacyRow.getOwnerMobileNumber().trim());
				}
				
				if(legacyRow.getTradeInstitutionPhoneNumber()!= null )
				{
					migratedTradeLicense.setTradeinstitutionphonenumber(legacyRow.getTradeInstitutionPhoneNumber().trim());
				}

				if(legacyRow.getApplicationStatus()!= null )
				migratedTradeLicense.setApplicationstatus(legacyRow.getApplicationStatus().trim());

				if(legacyRow.getApplicationNo()!= null )
				migratedTradeLicense.setApplicationno(legacyRow.getApplicationNo().trim());

				if(legacyRow.getTenantId()!= null )
				migratedTradeLicense.setTenantid(legacyRow.getTenantId().trim());

				if(legacyRow.getTradeType()!= null )
				migratedTradeLicense.setTradetype(legacyRow.getTradeType().trim());

				if(legacyRow.getTradeSubType()!= null )
				migratedTradeLicense.setTradesubtype(legacyRow.getTradeSubType().trim());

				if(legacyRow.getTradeName()!= null )
				migratedTradeLicense.setTradename(legacyRow.getTradeName().trim());

				if(legacyRow.getTradeUnitMeasurementName()!= null )
				migratedTradeLicense.setTradeunitmeasurementname(legacyRow.getTradeUnitMeasurementName().trim());

				if(legacyRow.getLicenseType()!= null )
				migratedTradeLicense.setLicensetype(legacyRow.getLicenseType().trim());

				if(legacyRow.getLicenseNumber()!= null )
				migratedTradeLicense.setLicensenumber(legacyRow.getLicenseNumber().trim());

				if(legacyRow.getFinacialYear()!= null )
				migratedTradeLicense.setFinacialyear(legacyRow.getFinacialYear().trim());

				if(legacyRow.getTraderAddress()!= null )
				migratedTradeLicense.setTraderaddress(legacyRow.getTraderAddress().trim());

				if(legacyRow.getTradeVillage()!= null )
				migratedTradeLicense.setTradevillage(legacyRow.getTradeVillage().trim());

				if(legacyRow.getTradeCity()!= null )
				migratedTradeLicense.setTradecity(legacyRow.getTradeCity().trim());

				if(legacyRow.getTradecategory()!= null )
				migratedTradeLicense.setTradecategory(legacyRow.getTradecategory().trim());

				if(legacyRow.getTradePrimaryOwnerName()!= null )
				migratedTradeLicense.setTradeprimaryownername(legacyRow.getTradePrimaryOwnerName().trim());

				if(legacyRow.getTradeSecondaryOwnerName()!= null )
				migratedTradeLicense.setTradesecondaryownername(legacyRow.getTradeSecondaryOwnerName().trim());

				if(legacyRow.getAuthorizedPersonName()!= null )
				migratedTradeLicense.setAuthorizedpersonname(legacyRow.getAuthorizedPersonName().trim());

				if(legacyRow.getTradeInstitutionOfficialCorrespondanceAddress()!= null )
				migratedTradeLicense.setTradeinstitutionofficialcorrespondanceaddress(legacyRow.getTradeInstitutionOfficialCorrespondanceAddress().trim());

				migratedTradeLicense.setCreatedtime(new Date().getTime());
				Thread.sleep(1);
				migartionList.add(migratedTradeLicense);

				if(numOfSuccess.longValue() %1000 ==0)
				{
					tradeLicenseExcelRepository.saveAll(migartionList);
					migartionList.clear();
				}


			//	System.out.println(" saved to repoistory "+migratedTradeLicense.toString());

				numOfSuccess.getAndIncrement();

				log.info("  numOfSuccess  "+numOfSuccess);

			} catch (Exception e) {
				numOfErrors.getAndIncrement();
				
				log.info(" Failed to save to repoistory with line number "+row.getRowIndex());

				log.info("  numOfErrors  "+numOfErrors);

				e.printStackTrace();
			}

			return true;
		});
		
		if(!migartionList.isEmpty())
		{
			tradeLicenseExcelRepository.saveAll(migartionList);
			migartionList.clear();
		}

	}

}
