package org.egov.tl.repository.rowmapper;

import java.lang.reflect.Field;
import java.util.Map;

import org.apache.commons.beanutils.PropertyUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.egov.tl.web.models.excel.CellAnnotation;
import org.egov.tl.web.models.excel.CellReplaceAnnotation;
import org.egov.tl.web.models.excel.LegacyRow;
import org.egov.tl.web.models.excel.RowExcel;
import org.springframework.stereotype.Component;

@Component
public class LegacyExcelRowMapper {


	public LegacyRow map(RowExcel source) throws Exception {
		LegacyRow row = new LegacyRow();
		Map<Integer, Cell> cells = source.getCells();
		for (Field f: LegacyRow.class.getDeclaredFields()) {
			CellAnnotation column = f.getAnnotation(CellAnnotation.class);
			CellReplaceAnnotation replaceAnnotation = f.getAnnotation(CellReplaceAnnotation.class);
			Cell cell  = cells.get(column.index());
			if(cell != null && cell.getCellType().equals(CellType.STRING)){
				String value = cell.getStringCellValue();
				if(replaceAnnotation != null) value = value.replaceAll(replaceAnnotation.target(),replaceAnnotation.source());
				PropertyUtils.setProperty(row, f.getName(), value);
			} else if(cell != null && cell.getCellType().equals(CellType.NUMERIC)){
				String value = String.valueOf(cell.getNumericCellValue());
				if(replaceAnnotation != null) value = value.replaceAll(replaceAnnotation.target(),replaceAnnotation.source());
				PropertyUtils.setProperty(row, f.getName(), value);
			}

		}
		return row;
	}

}
