package app.data;

import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.GridData;
import com.google.api.services.sheets.v4.model.RowData;
import com.google.api.services.sheets.v4.model.Spreadsheet;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static app.utils.ReportUtil.columnToLetter;

public class DataRetriever {

    private Sheets service;

    public DataRetriever(Sheets service) {
        this.service = service;
    }

    public Map<String, List<RowData>> getData(String spreadsheetId, String... ranges) throws IOException {
        Spreadsheet spreadsheet = service.spreadsheets().get(spreadsheetId)
                .setRanges(Arrays.asList(ranges)).setIncludeGridData(true).execute();

        List<GridData> datas = spreadsheet.getSheets().get(0).getData();

        Map<String, List<RowData>> map = new HashMap<>();
        for (int i = 0; i < ranges.length; i++) {
            map.put(ranges[i], datas.get(i).getRowData());
        }
        return map;
    }

    public List<RowData> getData(String spreadsheetId, int rows, int startColumn, int endColumn) throws IOException {
        //A1 notation
        String a1Start = columnToLetter(startColumn) + "1";
        String a1End = columnToLetter(endColumn) + String.valueOf(rows);

        String dataRange = a1Start + ":" + a1End;

        Spreadsheet spreadsheet = service.spreadsheets().get(spreadsheetId)
                .setRanges(Collections.singletonList(dataRange)).setIncludeGridData(true).execute();

        return spreadsheet.getSheets().get(0).getData().get(0).getRowData();
    }

}
