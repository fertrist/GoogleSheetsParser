package app.dao;

import static app.extract.ReportUtil.columnToLetter;
import app.report.ReportRange;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.RowData;
import com.google.api.services.sheets.v4.model.Sheet;
import com.google.api.services.sheets.v4.model.Spreadsheet;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

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

    public List<RowData> getRowsData(String spreadsheetId, String rangeExpression) throws IOException {
        System.out.println("Fetching data : " + rangeExpression);

        Spreadsheet spreadsheet = service.spreadsheets().get(spreadsheetId)
                .setRanges(Collections.singletonList(rangeExpression)).setIncludeGridData(true).execute();

        return spreadsheet.getSheets().get(0).getData().get(0).getRowData();
    }

    public List<RowData> getRowsData(String spreadsheetId, int startRow, int endRow,
                                     String startColumn, String endColumn) throws IOException {
        String start = getRangePoint(startColumn, startRow);
        String end = getRangePoint(endColumn, endRow);
        String dataRange = start + ":" + end;

        System.out.println("Fetching data : " + dataRange);

        Spreadsheet spreadsheet = service.spreadsheets().get(spreadsheetId)
                .setRanges(Collections.singletonList(dataRange)).setIncludeGridData(true).execute();

        return spreadsheet.getSheets().get(0).getData().get(0).getRowData();
    }

    public String getRangeExpression(int startRow, int endRow, String startColumn, String endColumn)
    {
        String start = getRangePoint(startColumn, startRow);
        String end = getRangePoint(endColumn, endRow);
        return start + ":" + end;
    }

    private String getRangePoint(String column, int row)
    {
        return column + row;
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

    public List<RowData> getRowsData(String spreadsheetId, ReportRange reportRange) throws IOException {
        String start = columnToLetter(reportRange.getStart().getColumn()) + reportRange.getStart().getRow();

        String end = columnToLetter(reportRange.getEnd().getColumn()) + reportRange.getEnd().getRow();
        String dataRange = start + ":" + end;

        System.out.println("Fetching data : " + dataRange);

        Spreadsheet spreadsheet = service.spreadsheets().get(spreadsheetId)
                .setRanges(Collections.singletonList(dataRange)).setIncludeGridData(true).execute();

        return spreadsheet.getSheets().get(0).getData().get(0).getRowData();
    }

}
