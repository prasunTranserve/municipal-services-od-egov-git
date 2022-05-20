package org.egov.tl.web.models.excel;

import java.util.Map;

import org.apache.poi.ss.usermodel.Cell;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RowExcel {
    Map<Integer, String> header;
    Map<Integer, Cell> cells;
    int rowIndex;
}
