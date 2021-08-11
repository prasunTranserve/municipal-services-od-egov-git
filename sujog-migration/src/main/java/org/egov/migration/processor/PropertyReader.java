package org.egov.migration.processor;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.egov.migration.mapper.AddressRowMapper;
import org.egov.migration.mapper.AssessmentRowMapper;
import org.egov.migration.mapper.DemandDetailRowMapper;
import org.egov.migration.mapper.DemandRowMapper;
import org.egov.migration.mapper.OwnerRowMapper;
import org.egov.migration.mapper.PropertyRowMapper;
import org.egov.migration.mapper.PropertyUnitRowMapper;
import org.egov.migration.model.Address;
import org.egov.migration.model.Assessment;
import org.egov.migration.model.Demand;
import org.egov.migration.model.DemandDetail;
import org.egov.migration.model.Owner;
import org.egov.migration.model.Property;
import org.egov.migration.model.PropertyUnit;
import org.egov.migration.util.MigrationConst;
import org.egov.migration.util.MigrationUtility;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.NonTransientResourceException;
import org.springframework.batch.item.ParseException;
import org.springframework.batch.item.UnexpectedInputException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PropertyReader implements ItemReader<Property> {
	
	private String file;
	private String ulb;
	
	private int propertyRowIndex;
	private Iterator<Row> propertyRowIterator;
	private int skipRecord;
	
	private Map<String, Integer> propertyColMap;
	private Map<String, Integer> addressColMap;
	private Map<String, Integer> ownerColMap;
	private Map<String, Integer> propertyUnitColMap;
	private Map<String, Integer> assessmentColMap;
	private Map<String, Integer> demandColMap;
	private Map<String, Integer> demandDetailColMap;
	
	private Map<String, Integer> addressRowMap;
	private Map<String, List<Integer>> ownerRowMap;
	private Map<String, List<Integer>> propertyUnitRowMap;
	private Map<String, List<Integer>> assessmentRowMap;
	private Map<String, List<Integer>> demandRowMap;
	private Map<String, List<Integer>> demandDetailRowMap;
	
	private Sheet ownerSheet;
	private Sheet addressSheet;
	private Sheet propertyUnitSheet;
	private Sheet assessmentSheet;
	private Sheet demandSheet;
	private Sheet demandDetailSheet;
	
	@Autowired
	private PropertyRowMapper propertyRowMapper;
	
	@Autowired
	private OwnerRowMapper ownerRowMapper;
	
	@Autowired
	private AddressRowMapper addressRowMapper;
	
	@Autowired
	private PropertyUnitRowMapper propertyUnitRowMapper;
	
	@Autowired
	private AssessmentRowMapper assessmentRowMapper;
	
	@Autowired
	private DemandRowMapper demandRowMapper;
	
	@Autowired
	private DemandDetailRowMapper demandDetailRowMapper;
	
	public PropertyReader() throws EncryptedDocumentException, IOException, Exception {
		this.propertyRowIndex = 0;
	}
	
	public PropertyReader(String file) throws EncryptedDocumentException, IOException, Exception {
		this.file = file;
		this.propertyRowIndex = 0;
	}
	
	@Value("#{jobParameters['filePath']}")
	public void setFile(final String fileName) {
		this.file = fileName;
	}
	
	public String getFile() {
		return file;
	}

	public void setSkipRecord(int skipRecord) {
		this.skipRecord = skipRecord;
	}

	@Override
	public Property read()
			throws Exception, UnexpectedInputException, ParseException, NonTransientResourceException {
		if(this.propertyRowIndex==0) {
			readProperty();
			skipRecord();
		}
		
		if(this.propertyRowIterator.hasNext()) {
			this.propertyRowIndex++;
			return getProperty(this.propertyRowIterator.next());
		}
		
		return null;
	}
	
	private Property getProperty(Row propertyRow) {
		Property property = propertyRowMapper.mapRow(this.ulb, propertyRow, this.propertyColMap);
		log.info("Property: "+property.getPropertyId()+" reading...");
		Address address = getAddress(property);
		List<Owner> owners = getOwner(property.getPropertyId());
		List<PropertyUnit> prpertyUnits = getPropertyUnit(property.getPropertyId());
		List<Assessment> assessments = getAssessments(property.getPropertyId());
		List<DemandDetail> demanDetails = getDemandDetails(property.getPropertyId());
		List<Demand> demands = getDemands(property.getPropertyId());
		
		property.setAddress(address);
		property.setOwners(owners);
		property.setUnit(prpertyUnits);
		property.setAssessments(assessments);
		property.setDemands(demands);
		property.setDemandDetails(demanDetails);
		log.info("Property: "+property.getPropertyId()+" read successfully");
		return property;
	}

	private List<PropertyUnit> getPropertyUnit(String propertyId) {
		if(this.propertyUnitRowMap.get(propertyId) == null)
			return null;
		return this.propertyUnitRowMap.get(propertyId).stream().map(rowIndex -> propertyUnitRowMapper.mapRow(this.ulb, propertyUnitSheet.getRow(rowIndex), this.propertyUnitColMap)).collect(Collectors.toList());
	}

	private Address getAddress(Property property) {
		if(this.addressRowMap.get(property.getPropertyId()) == null)
			return null;
		return addressRowMapper.mapRow(this.ulb, this.addressSheet.getRow(this.addressRowMap.get(property.getPropertyId())), this.addressColMap);
	}
	
	private List<DemandDetail> getDemandDetails(String propertyId) {
		if(this.demandDetailRowMap.get(propertyId) == null)
			return null;
		return this.demandDetailRowMap.get(propertyId).stream().map(rowIndex -> demandDetailRowMapper.mapRow(this.ulb, demandDetailSheet.getRow(rowIndex), this.demandDetailColMap)).collect(Collectors.toList());
	}

	private List<Demand> getDemands(String propertyId) {
		if(this.demandRowMap.get(propertyId) == null) 
			return null;
		return this.demandRowMap.get(propertyId).stream().map(rowIndex -> demandRowMapper.mapRow(this.ulb, demandSheet.getRow(rowIndex), this.demandColMap)).collect(Collectors.toList());
	}

	private List<Assessment> getAssessments(String propertyId) {
		if(this.assessmentRowMap.get(propertyId) == null)
			return null;
		return this.assessmentRowMap.get(propertyId).stream().map(rowIndex -> assessmentRowMapper.mapRow(this.ulb, assessmentSheet.getRow(rowIndex), this.assessmentColMap)).collect(Collectors.toList());
	}

	private List<Owner> getOwner(String propertyId) {
		if(this.ownerRowMap.get(propertyId)==null)
			return null;
		return this.ownerRowMap.get(propertyId).stream().map(rowIndex -> ownerRowMapper.mapRow(this.ulb, ownerSheet.getRow(rowIndex), this.ownerColMap)).collect(Collectors.toList());
	}

	private void skipRecord() {
		for(int i=1; i<=this.skipRecord; i++) {
			if(propertyRowIterator.hasNext()) {
				this.propertyRowIterator.next();
				this.propertyRowIndex++;
			}
		}
	}

	private void readProperty() throws Exception,EncryptedDocumentException, IOException {
		File inputFile = new File(this.file);
		this.ulb = inputFile.getName().split("\\.")[0].toLowerCase();
		
		Workbook workbook = WorkbookFactory.create(inputFile);
		Sheet propertySheet = workbook.getSheet(MigrationConst.SHEET_PROPERTY);
		this.propertyRowIterator = propertySheet.iterator();
		updateAddressRowMap(workbook);
		updateOwnerRowMap(workbook);
		updatePropertyUnitRowMap(workbook);
		updateAssessmentRowMap(workbook);
		updateDemandRowMap(workbook);
		updateDemandDetailRowMap(workbook);
		
		updatePropertyColumnMap(propertySheet.getRow(0));
	}
	
	private void updatePropertyUnitRowMap(Workbook workbook) {
		this.propertyUnitRowMap = new HashMap<>();
		this.propertyUnitSheet = workbook.getSheet(MigrationConst.SHEET_PROPERTY_UNIT);
		this.propertyUnitSheet.rowIterator().forEachRemaining(row -> {
			if(row.getRowNum()==0) {
				updatePropertyUnitColumnMap(row);
			} else {
				String propertyId = MigrationUtility.readCellValue(row.getCell(this.propertyUnitColMap.get(MigrationConst.COL_PROPERTY_ID)), false);
				List<Integer> list = this.propertyUnitRowMap.get(propertyId);
				if(list == null) {
					list = new ArrayList<>();
				}
				list.add(row.getRowNum());
				this.propertyUnitRowMap.put(MigrationUtility.removeLeadingZeros(MigrationUtility.readCellValue(row.getCell(this.propertyUnitColMap.get(MigrationConst.COL_PROPERTY_ID)), false)), list);
			}
		});
	}

	private void updatePropertyUnitColumnMap(Row row) {
		this.propertyUnitColMap = new HashMap<>();
		Iterator<Cell> cellIterator = row.cellIterator();
		cellIterator.forEachRemaining(cell -> {
			this.propertyUnitColMap.put(MigrationUtility.readCellValue(cell, false).trim(), cell.getColumnIndex());
		});
	}

	private void updateAddressRowMap(Workbook workbook) throws EncryptedDocumentException, IOException {
		this.addressRowMap = new HashMap<>();
		this.addressSheet = workbook.getSheet(MigrationConst.SHEET_ADDRESS);
		this.addressSheet.rowIterator().forEachRemaining(row -> {
			if(row.getRowNum()==0) {
				updateAddressColumnMap(row);
			} else {
				this.addressRowMap.put(MigrationUtility.removeLeadingZeros(MigrationUtility.readCellValue(row.getCell(this.addressColMap.get(MigrationConst.COL_PROPERTY_ID)), false)), row.getRowNum());
			}
		});
		
		workbook.close();
	}

	private void updateOwnerRowMap(Workbook workbook) throws EncryptedDocumentException, IOException {
		this.ownerRowMap = new HashMap<>();
		this.ownerSheet = workbook.getSheet(MigrationConst.SHEET_OWNER);
		this.ownerSheet.rowIterator().forEachRemaining(row -> {
			if(row.getRowNum()==0) {
				updateOwnerColumnMap(row);
			} else {
				String propertyId = MigrationUtility.readCellValue(row.getCell(this.ownerColMap.get(MigrationConst.COL_PROPERTY_ID)), false);
				List<Integer> list = this.ownerRowMap.get(propertyId);
				if(list == null) {
					list = new ArrayList<>();
				}
				list.add(row.getRowNum());
				this.ownerRowMap.put(MigrationUtility.removeLeadingZeros(MigrationUtility.readCellValue(row.getCell(this.ownerColMap.get(MigrationConst.COL_PROPERTY_ID)), false)), list);
			}
		});
	}
	
	private void updateAssessmentRowMap(Workbook workbook) throws EncryptedDocumentException, IOException {
		this.assessmentRowMap = new HashMap<>();
		this.assessmentSheet = workbook.getSheet(MigrationConst.SHEET_ASSESSMENT);
		this.assessmentSheet.rowIterator().forEachRemaining(row -> {
			if(row.getRowNum()==0) {
				updateAssessmentColumnMap(row);
			} else {
				String propertyId = MigrationUtility.readCellValue(row.getCell(this.assessmentColMap.get(MigrationConst.COL_PROPERTY_ID)), false);
				List<Integer> list = this.assessmentRowMap.get(propertyId);
				if(list == null) {
					list = new ArrayList<>();
				}
				list.add(row.getRowNum());
				this.assessmentRowMap.put(MigrationUtility.removeLeadingZeros(MigrationUtility.readCellValue(row.getCell(this.assessmentColMap.get(MigrationConst.COL_PROPERTY_ID)), false)), list);
			}
		});
	}

	private void updateDemandRowMap(Workbook workbook) throws EncryptedDocumentException, IOException {
		this.demandRowMap = new HashMap<>();
		this.demandSheet = workbook.getSheet(MigrationConst.SHEET_DEMAND);
		this.demandSheet.rowIterator().forEachRemaining(row -> {
			if(row.getRowNum()==0) {
				updatedemandColumnMap(row);
			} else {
				String propertyId = MigrationUtility.readCellValue(row.getCell(this.demandColMap.get(MigrationConst.COL_PROPERTY_ID)), false);
				List<Integer> list = this.demandRowMap.get(propertyId);
				if(list == null) {
					list = new ArrayList<>();
				}
				list.add(row.getRowNum());
				this.demandRowMap.put(MigrationUtility.removeLeadingZeros(MigrationUtility.readCellValue(row.getCell(this.demandColMap.get(MigrationConst.COL_PROPERTY_ID)), false)), list);
			}
		});
	}

	private void updateDemandDetailRowMap(Workbook workbook) throws EncryptedDocumentException, IOException {
		this.demandDetailRowMap = new HashMap<>();
		this.demandDetailSheet = workbook.getSheet(MigrationConst.SHEET_DEMAND_DETAIL);
		this.demandDetailSheet.rowIterator().forEachRemaining(row -> {
			if(row.getRowNum()==0) {
				updatedemandDetailColumnMap(row);
			} else {
				String propertyId = MigrationUtility.readCellValue(row.getCell(this.demandDetailColMap.get(MigrationConst.COL_PROPERTY_ID)), false);
				List<Integer> list = this.demandDetailRowMap.get(propertyId);
				if(list == null) {
					list = new ArrayList<>();
				}
				list.add(row.getRowNum());
				this.demandDetailRowMap.put(MigrationUtility.removeLeadingZeros(MigrationUtility.readCellValue(row.getCell(this.demandDetailColMap.get(MigrationConst.COL_PROPERTY_ID)), false)), list);
			}
		});
	}
	
	private void updatedemandDetailColumnMap(Row row) {
		this.demandDetailColMap = new HashMap<>();
		Iterator<Cell> cellIterator = row.cellIterator();
		cellIterator.forEachRemaining(cell -> {
			this.demandDetailColMap.put(MigrationUtility.readCellValue(cell, false).trim(), cell.getColumnIndex());
		});
	}
	
	private void updatedemandColumnMap(Row row) {
		this.demandColMap = new HashMap<>();
		Iterator<Cell> cellIterator = row.cellIterator();
		cellIterator.forEachRemaining(cell -> {
			this.demandColMap.put(MigrationUtility.readCellValue(cell, false).trim(), cell.getColumnIndex());
		});
	}

	private void updateAssessmentColumnMap(Row row) {
		this.assessmentColMap = new HashMap<>();
		Iterator<Cell> cellIterator = row.cellIterator();
		cellIterator.forEachRemaining(cell -> {
			this.assessmentColMap.put(MigrationUtility.readCellValue(cell, false).trim(), cell.getColumnIndex());
		});
	}
	
	private void updateOwnerColumnMap(Row row) {
		this.ownerColMap = new HashMap<>();
		Iterator<Cell> cellIterator = row.cellIterator();
		cellIterator.forEachRemaining(cell -> {
			this.ownerColMap.put(MigrationUtility.readCellValue(cell, false).trim(), cell.getColumnIndex());
		});
	}

	private void updateAddressColumnMap(Row row) {
		this.addressColMap = new HashMap<>();
		Iterator<Cell> cellIterator = row.cellIterator();
		cellIterator.forEachRemaining(cell -> {
			this.addressColMap.put(MigrationUtility.readCellValue(cell, false).trim(), cell.getColumnIndex());
		});
	}

	private void updatePropertyColumnMap(Row row) {
		this.propertyColMap = new HashMap<>();
		Iterator<Cell> cellIterator = row.cellIterator();
		cellIterator.forEachRemaining(cell -> {
			this.propertyColMap.put(MigrationUtility.readCellValue(cell, false).trim(), cell.getColumnIndex());
		});
	}
	
}
