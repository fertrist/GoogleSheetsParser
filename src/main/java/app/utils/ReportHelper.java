package app.utils;

import app.entities.Group;
import app.entities.Person;
import app.entities.Week;
import app.enums.Category;
import app.enums.Actions;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.BatchUpdateSpreadsheetRequest;
import com.google.api.services.sheets.v4.model.CellData;
import com.google.api.services.sheets.v4.model.Color;
import com.google.api.services.sheets.v4.model.GridCoordinate;
import com.google.api.services.sheets.v4.model.GridData;
import com.google.api.services.sheets.v4.model.Request;
import com.google.api.services.sheets.v4.model.RowData;
import com.google.api.services.sheets.v4.model.UpdateCellsRequest;
import com.sun.deploy.util.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static app.utils.Configuration.REPORT_SPREADSHEET_URL;
import static app.utils.Configuration.getProperty;
import static app.utils.Configuration.getReportSpreadSheetId;
import static app.utils.Configuration.getSheetGid;
import static app.utils.ReportUtil.getCell;
import static app.utils.ReportUtil.getCellWithBgColor;
import static app.utils.ReportUtil.getCellWithValue;
import static app.utils.ReportUtil.getColor;
import static app.utils.ReportUtil.listToNote;

/**
 * Does stuff related to reporting computed results
 */
public class ReportHelper {

    private static String [] REPORT_COLUMNS = new String[]{"Лидер", "Неделя", "По списку", "Было всего", "Белый список", "Гости", "Новые люди",
            "Как прошла (%)", "Посещ.списки", "Встр. списки", "Посещ.новые", "Встр. новые", "Звонки"};
    public static final Color YELLOW = getColor(255, 214, 93);
    public static final Color BLUE = getColor(147, 176, 255);
    public static final Color GREY = getColor(134, 133, 135);

    public static RowData getTitleHeader() {
        RowData titleRow = new RowData();
        List<CellData> headerCells = new ArrayList<>();

        headerCells.add(getCellWithBgColor(String.format("Динамический отчет за: \n %s : %s",
                Configuration.getReportStartDate(), Configuration.getReportEndDate()), BLUE));
        headerCells.add(getCellWithBgColor(BLUE));
        headerCells.add(getCellWithBgColor(Configuration.getProperty(Configuration.PREVIOUS_WHITE_COUNT), BLUE));
        headerCells.add(getCellWithBgColor(BLUE));
        headerCells.add(getCellWithBgColor(BLUE));
        headerCells.add(getCellWithBgColor(BLUE));
        headerCells.add(getCellWithBgColor(Configuration.getProperty(Configuration.PREVIOUS_NEW_COUNT), BLUE));

        titleRow.setValues(headerCells);
        return titleRow;
    }

    public static RowData getReportHeader() {
        RowData headerRow = new RowData();
        List<CellData> headerCells = new ArrayList<>();
        for (String reportColumn : REPORT_COLUMNS) {
            CellData cellData = getCellWithBgColor(reportColumn, YELLOW);
            headerCells.add(cellData);
        }
        headerRow.setValues(headerCells);
        return headerRow;
    }

    public static void prettyPrintWeeks(List<Week> weeks) {
        String format = "%10s | %10s | %10s | %10s | %10s | %10s | %15s | %15s | %15s | %15s | %10s %n";
        System.out.printf(format, "Неделя", "По списку", "Было всего", "Cписочных", "Гости", "Новые люди",
                "Посещ.списки", "Встр. списки", "Посещ.новые", "Встр. новые", "Звонки");
        for (Week week : weeks) {
            System.out.printf(format, week.getStart(), week.getWhiteList().size(), week.getPresent().size(), week.getPresentByCategory(Category.WHITE).size(),
                    week.getPresentByCategory(Category.GUEST).size(), week.getPresentByCategory(Category.NEW).size(), week.getVisitWhite(),
                    week.getMeetingWhite(), week.getVisitNew(), week.getMeetingNew(), week.getCalls());
        }
    }

    public static RowData getWeekFooterRow(int lastWhiteCount, Set<String> uniqueNewPeople) {
        RowData footerRow = new RowData();
        List<CellData> footerCells = new ArrayList<>();
        footerCells.add(getCellWithBgColor("Итого:", YELLOW));
        footerCells.add(getCellWithValue(""));
        footerCells.add(getCellWithBgColor(lastWhiteCount, YELLOW));
        footerCells.add(getCellWithBgColor(YELLOW));
        footerCells.add(getCellWithValue(""));
        footerCells.add(getCellWithBgColor(YELLOW));
        footerCells.add(getCellWithBgColor(uniqueNewPeople.size(), YELLOW).setNote(StringUtils.join(uniqueNewPeople, "\n")));
        footerCells.add(getCellWithBgColor(YELLOW));
        footerRow.setValues(footerCells);
        return footerRow;
    }

    public static RowData getReportFooterRow(int totalWhiteCount, int totalNewCount) {
        RowData footerRow = new RowData();
        List<CellData> footerCells = new ArrayList<>();
        footerCells.add(getCellWithBgColor("Итого:", YELLOW));
        footerCells.add(getCellWithValue(""));
        footerCells.add(getCellWithBgColor(totalWhiteCount, YELLOW));
        footerCells.add(getCellWithValue(""));
        footerCells.add(getCellWithValue(""));
        footerCells.add(getCellWithValue(""));
        footerCells.add(getCellWithBgColor(totalNewCount, YELLOW));
        footerCells.add(getCellWithValue(""));
        footerRow.setValues(footerCells);
        return footerRow;
    }

    public static String[] getReportColumns() {
        return REPORT_COLUMNS;
    }

    /**
     * @param week week to report
     * @param newPeople set of new people of current region (to collect summary)
     */
    public static RowData getWeekRow(Week week, Group group, Set<String> newPeople, boolean lastWeek) {
        RowData rowData = new RowData();

        CellData leader = getCellWithValue(group.getLeaderName());
        CellData weekName = getCellWithValue(ReportUtil.getDayMonth(week.getStart())
                + " - " + ReportUtil.getDayMonth(week.getEnd()));
        CellData presentTotal = getCellWithValue(week.getTotalCount());
        CellData listWhite = getCellWithValue(week.getWhiteList().size());
        if (lastWeek) {
            String note = "";
            for (String name : group.getAddedPeople()) {
                note += "+ " + name + "\n";
            }
            for (String name : group.getRemovedPeople()) {
                note += "- " + name + "\n";
            }
            listWhite.setNote(note);
        }
        CellData presentWhite = getCell(week.getPresentByCategory(Category.WHITE).size(), listToNote(week.getWhiteAbsent()));
        CellData presentGuests = getCell(week.getPresentByCategory(Category.GUEST).size(), listToNote(week.getPresentByCategory(Category.GUEST)));
        CellData presentNew = getCell(week.getPresentByCategory(Category.NEW).size(), listToNote(week.getPresentByCategory(Category.NEW)));
        newPeople.addAll(week.getPresentByCategory(Category.NEW).stream().map(Person::getName).collect(Collectors.toSet()));
        CellData groupRate = getCellWithValue(
                week.getPercents() != null ? week.getPercents() : "-");
        groupRate.setNote(week.getGroupComments());
        CellData visitsWhite = getCellWithValue(week.getVisitWhite());
        CellData meetingsWhite = getCellWithValue(week.getMeetingWhite());
        CellData visitsNew = getCellWithValue(week.getVisitNew());
        CellData meetingsNew = getCellWithValue(week.getMeetingNew());
        CellData callsNew = getCellWithValue(week.getCalls());

        rowData.setValues(Arrays.asList(leader, weekName, listWhite, presentTotal, presentWhite, presentGuests,
                presentNew, groupRate, visitsWhite, meetingsWhite, visitsNew, meetingsNew, callsNew));
        return rowData;
    }

    public static void printWeeks(Sheets service, Group group, List<Week> weeks, int row) throws IOException {
        prettyPrintWeeks(weeks);

        List<Request> requests = new ArrayList<>();

        UpdateCellsRequest updateCellsRequest = new UpdateCellsRequest();
        updateCellsRequest.setFields("*");

        GridCoordinate gridCoordinate = new GridCoordinate();
        gridCoordinate.setColumnIndex(0);
        gridCoordinate.setSheetId(getSheetGid(getProperty(REPORT_SPREADSHEET_URL)));
        gridCoordinate.setRowIndex(row);
        updateCellsRequest.setStart(gridCoordinate);

        //set header
        List<RowData> allRows = new ArrayList<>();

        // set each row
        Set<String> uniqueNewPeople = new HashSet<>();
        for (int i = 0; i <= weeks.size() - 2; i++) {
            allRows.add(getWeekRow(weeks.get(i), group, uniqueNewPeople, false));
        }
        allRows.add(getWeekRow(weeks.get(weeks.size() - 1), group, uniqueNewPeople, true));

        int lastWhiteCount = weeks.get(weeks.size() - 1).getWhiteList().size();

        //add footer
        allRows.add(getWeekFooterRow(lastWhiteCount, uniqueNewPeople));

        //execute all
        updateCellsRequest.setRows(allRows);
        Request request = new Request();
        request.setUpdateCells(updateCellsRequest);
        requests.add(request);
        BatchUpdateSpreadsheetRequest body =
                new BatchUpdateSpreadsheetRequest().setRequests(requests);
        service.spreadsheets().batchUpdate(getReportSpreadSheetId(), body).execute();
    }

}
