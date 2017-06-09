package app.data;

import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.RowData;
import com.google.api.services.sheets.v4.model.Sheet;
import com.google.api.services.sheets.v4.model.Spreadsheet;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import static app.utils.ReportUtil.columnToLetter;

public class CustomSheetApi {

    private Sheets service;

    public CustomSheetApi(Sheets service) {
        this.service = service;
    }

    public Sheet getSheet(String spreadsheetId, int startRow, int endRow) throws IOException {
        String monthsRange = startRow + ":" + endRow;

        Spreadsheet spreadsheet = service.spreadsheets().get(spreadsheetId)
                .setRanges(Collections.singletonList(monthsRange)).setIncludeGridData(true).execute();

        return spreadsheet.getSheets().get(0);
    }

    public List<RowData> getRowsData(String spreadsheetId, int startRow, int endRow,
                                     String startColumn, String endColumn) throws IOException {
        String start = startColumn + startRow;

        String end = endColumn + endRow;
        String dataRange = start + ":" + end;

        System.out.println("Fetching data : " + dataRange);

        Spreadsheet spreadsheet = service.spreadsheets().get(spreadsheetId)
                .setRanges(Collections.singletonList(dataRange)).setIncludeGridData(true).execute();

        return spreadsheet.getSheets().get(0).getData().get(0).getRowData();
    }

    public List<RowData> getRowsData(String spreadsheetId, int startRow, int endRow,
                                     int startColumn, int endColumn) throws IOException {
        String start = columnToLetter(startColumn) + startRow;

        String end = columnToLetter(endColumn) + endRow;
        String dataRange = start + ":" + end;

        System.out.println("Fetching data : " + dataRange);

        Spreadsheet spreadsheet = service.spreadsheets().get(spreadsheetId)
                .setRanges(Collections.singletonList(dataRange)).setIncludeGridData(true).execute();

        return spreadsheet.getSheets().get(0).getData().get(0).getRowData();
    }

}
