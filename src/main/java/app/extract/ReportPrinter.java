package app.extract;

import static app.conf.Configuration.REPORT_SPREADSHEET_URL;
import static app.conf.Configuration.REPORT_TITLE;
import static app.conf.Configuration.getProperty;
import static app.conf.Configuration.getReportSpreadSheetId;
import static app.conf.Configuration.getSheetGid;
import static app.extract.ReportUtil.MAX_ROWS;
import static app.extract.ReportUtil.getCell;
import static app.extract.ReportUtil.getCellWithBgColor;
import static app.extract.ReportUtil.getCellWithValue;
import static app.extract.ReportUtil.getColor;
import static app.extract.ReportUtil.join;
import static app.extract.ReportUtil.listToNote;
import static java.util.Collections.singletonList;

import app.conf.Configuration;
import app.entities.*;
import app.entities.Category;
import app.report.GroupReport;
import app.report.GroupWeeklyReport;
import app.report.RegionReport;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.BatchUpdateSpreadsheetRequest;
import com.google.api.services.sheets.v4.model.Border;
import com.google.api.services.sheets.v4.model.CellData;
import com.google.api.services.sheets.v4.model.CellFormat;
import com.google.api.services.sheets.v4.model.Color;
import com.google.api.services.sheets.v4.model.GridCoordinate;
import com.google.api.services.sheets.v4.model.GridProperties;
import com.google.api.services.sheets.v4.model.GridRange;
import com.google.api.services.sheets.v4.model.MergeCellsRequest;
import com.google.api.services.sheets.v4.model.RepeatCellRequest;
import com.google.api.services.sheets.v4.model.Request;
import com.google.api.services.sheets.v4.model.RowData;
import com.google.api.services.sheets.v4.model.SheetProperties;
import com.google.api.services.sheets.v4.model.UpdateBordersRequest;
import com.google.api.services.sheets.v4.model.UpdateCellsRequest;
import com.google.api.services.sheets.v4.model.UpdateSheetPropertiesRequest;
import com.google.common.collect.Lists;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class ReportPrinter
{
    /**
     * Reports all regionReports to report sheet.
     */
    private static void report(Sheets service, List<RegionReport> regionReports) throws IOException {
        Integer sheetGid = getSheetGid(getProperty(REPORT_SPREADSHEET_URL));

        UpdateCellsRequest updateHeaderRequest = new UpdateCellsRequest();
        updateHeaderRequest.setFields("*");

        GridCoordinate headerGridCoordinate = new GridCoordinate();
        headerGridCoordinate.setColumnIndex(0);
        headerGridCoordinate.setSheetId(sheetGid);
        headerGridCoordinate.setRowIndex(0);
        updateHeaderRequest.setStart(headerGridCoordinate);

        // add report main header (report summary)
        List<RowData> headers = new ArrayList<>();
        headers.add(getTitleHeader());
        headers.add(getReportHeader());
        updateHeaderRequest.setRows(headers);

        MutableInteger rowPointer = new MutableInteger(0);
        rowPointer.setValue(rowPointer.getValue() + 2);

        int totalNewCount = 0;
        int totalWhiteCount = 0;

        for (RegionReport regionReport : regionReports) {
            printRegion(service, rowPointer, regionReport);
            totalNewCount += regionReport.getTotalNewCount();
            totalWhiteCount += regionReport.getTotalWhiteCount();
        }

        // add report main footer (current summary)
        UpdateCellsRequest updateFooterRequest = new UpdateCellsRequest();
        updateFooterRequest.setFields("*");

        GridCoordinate footerGridCoordinate = new GridCoordinate();
        footerGridCoordinate.setColumnIndex(0);
        footerGridCoordinate.setSheetId(sheetGid);
        footerGridCoordinate.setRowIndex(rowPointer.getValue());
        updateFooterRequest.setStart(footerGridCoordinate);

        List<RowData> footers = new ArrayList<>();
        footers.add(getReportFooterRow(totalWhiteCount, totalNewCount));

        updateFooterRequest.setRows(footers);

        // make header frozen, create borders
        UpdateBordersRequest updateBordersRequest = new UpdateBordersRequest();
        GridRange borderRange = new GridRange().setStartColumnIndex(0).setEndColumnIndex(getReportColumns().length + 1)
                .setStartRowIndex(1).setEndRowIndex(rowPointer.getValue() + 1).setSheetId(sheetGid);
        updateBordersRequest.setRange(borderRange);
        Border border = new Border().setColor(getColor(0, 0, 0)).setStyle("SOLID");
        updateBordersRequest.setTop(border).setLeft(border).setBottom(border).setRight(border)
                .setInnerHorizontal(border).setInnerVertical(border);

        UpdateSheetPropertiesRequest freezeHeaderUpdateTitleRequest = new UpdateSheetPropertiesRequest().setFields("*");
        GridProperties gridProperties = new GridProperties().setFrozenRowCount(2).setRowCount(MAX_ROWS + 30)
                .setColumnCount(getReportColumns().length);

        SheetProperties sheetProperties = new SheetProperties().setGridProperties(gridProperties)
                .setTitle(getProperty(REPORT_TITLE)).setSheetId(sheetGid);
        freezeHeaderUpdateTitleRequest.setProperties(sheetProperties);

        // align all cells
        RepeatCellRequest alignAllCellsRequest = new RepeatCellRequest().setFields("userEnteredFormat.horizontal_alignment")
                .setRange(borderRange.setStartRowIndex(0))
                .setCell(new CellData().setUserEnteredFormat(new CellFormat().setHorizontalAlignment("CENTER")));

        MergeCellsRequest mergeTitleNameRequest = new MergeCellsRequest().setMergeType("MERGE_ALL")
                .setRange(new GridRange().setStartRowIndex(0).setEndRowIndex(1).setStartColumnIndex(0).setEndColumnIndex(2)
                        .setSheetId(sheetGid));
        // resize columns
        /*UpdateDimensionPropertiesRequest resizeRequest = new UpdateDimensionPropertiesRequest().setFields("")
                .setRange(new DimensionRange().setDimension("COLUMNS").setSheetId(sheetGid)
                        .setStartIndex(0).setEndIndex(getReportColumns().length + 1))
                .setProperties(new DimensionProperties().setPixelSize())*/

        List<Request> requests = new ArrayList<>();
        requests.add(new Request().setUpdateCells(updateHeaderRequest));
        requests.add(new Request().setUpdateCells(updateFooterRequest));
        requests.add(new Request().setUpdateBorders(updateBordersRequest));
        requests.add(new Request().setUpdateSheetProperties(freezeHeaderUpdateTitleRequest));
        requests.add(new Request().setRepeatCell(alignAllCellsRequest));
        requests.add(new Request().setMergeCells(mergeTitleNameRequest));
        //requests.add(new Request().setUpdateDimensionProperties(resizeRequest));

        BatchUpdateSpreadsheetRequest body =
                new BatchUpdateSpreadsheetRequest().setRequests(requests);
        service.spreadsheets().batchUpdate(getReportSpreadSheetId(), body).execute();
    }

    /**
     * Prints single regionReport to the report sheet.
     * @param rowPointer which counts offset of rows, to print next items correctly
     */
    private static void printRegion(Sheets service, MutableInteger rowPointer, RegionReport regionReport) throws IOException {
        // print regionReport header
        List<Request> requests = new ArrayList<>();

        UpdateCellsRequest updateCellsRequest = new UpdateCellsRequest().setFields("*");

        GridCoordinate gridCoordinate = new GridCoordinate().setColumnIndex(0)
                .setSheetId(getSheetGid(getProperty(REPORT_SPREADSHEET_URL))).setRowIndex(rowPointer.getValue());

        updateCellsRequest.setStart(gridCoordinate);

        updateCellsRequest.setRows(singletonList(new RowData().setValues(
                        Lists.asList(getCellWithBgColor(regionReport.getLeader(), GREY), Collections.nCopies(12, getCellWithBgColor(GREY)).toArray(new CellData[12])))
        ));

        MergeCellsRequest mergeCellsRequest = new MergeCellsRequest().setMergeType("MERGE_ALL");
        GridRange range = new GridRange().setSheetId(getSheetGid(getProperty(REPORT_SPREADSHEET_URL)))
                .setStartRowIndex(rowPointer.getValue()).setEndRowIndex(rowPointer.getValue() + 1)
                .setStartColumnIndex(0).setEndColumnIndex(getReportColumns().length);
        mergeCellsRequest.setRange(range);

        rowPointer.setValue(rowPointer.getValue() + 1);

        requests.add(new Request().setUpdateCells(updateCellsRequest));
        requests.add(new Request().setMergeCells(mergeCellsRequest));

        BatchUpdateSpreadsheetRequest body =
                new BatchUpdateSpreadsheetRequest().setRequests(requests);
        service.spreadsheets().batchUpdate(getReportSpreadSheetId(), body).execute();

        for (GroupReport groupReport : regionReport.getGroupReports()) {
            List<GroupWeeklyReport> groupGroupWeeklyReports = groupReport.getGroupWeeklyReports();
            printWeeks(service, groupReport.getGroup(), groupGroupWeeklyReports, rowPointer.getValue());
            rowPointer.setValue(rowPointer.getValue() + groupGroupWeeklyReports.size() + 1);
        }
    }

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

    public static void prettyPrintWeeks(List<GroupWeeklyReport> groupWeeklyReports) {
        String format = "%10s | %10s | %10s | %10s | %10s | %10s | %15s | %15s | %15s | %15s | %10s %n";
        System.out.printf(format, "Неделя", "По списку", "Было всего", "Cписочных", "Гости", "Новые люди",
                "Посещ.списки", "Встр. списки", "Посещ.новые", "Встр. новые", "Звонки");
        for (GroupWeeklyReport groupWeeklyReport : groupWeeklyReports) {
            System.out.printf(format, groupWeeklyReport.getStart(), groupWeeklyReport.getWhiteList().size(), groupWeeklyReport.getPresent().size(), groupWeeklyReport.getPresentByCategory(Category.WHITE).size(),
                    groupWeeklyReport.getPresentByCategory(Category.GUEST).size(), groupWeeklyReport.getPresentByCategory(Category.NEW).size(), groupWeeklyReport.getVisitWhite(),
                    groupWeeklyReport.getMeetingWhite(), groupWeeklyReport.getVisitNew(), groupWeeklyReport.getMeetingNew(), groupWeeklyReport.getCalls());
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
        footerCells.add(getCellWithBgColor(uniqueNewPeople.size(), YELLOW).setNote(join(uniqueNewPeople, "\n")));
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
     * @param groupWeeklyReport groupWeeklyReport to report
     * @param newPeople set of new people of current region (to collect summary)
     */
    public static RowData getWeekRow(GroupWeeklyReport groupWeeklyReport, Group group, Set<String> newPeople, boolean lastWeek) {
        RowData rowData = new RowData();

        CellData leader = getCellWithValue(group.getLeaderName());
        CellData weekName = getCellWithValue(ReportUtil.getDayMonth(groupWeeklyReport.getStart())
                + " - " + ReportUtil.getDayMonth(groupWeeklyReport.getEnd()));
        CellData presentTotal = getCellWithValue(groupWeeklyReport.getTotalCount());
        CellData listWhite = getCellWithValue(groupWeeklyReport.getWhiteList().size());
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
        CellData presentWhite = getCell(groupWeeklyReport.getPresentByCategory(Category.WHITE).size(), listToNote(groupWeeklyReport.getWhiteAbsent()));
        List<Person> guests = groupWeeklyReport.getPresentByCategory(Category.GUEST, Category.TRIAL);
        CellData presentGuests = getCell(guests.size(), listToNote(guests));
        List<Person> newGuys = groupWeeklyReport.getPresentByCategory(Category.NEW);
        CellData presentNew = getCell(newGuys.size(), listToNote(newGuys));
        newPeople.addAll(newGuys.stream().map(Person::getName).collect(Collectors.toSet()));
        CellData groupRate = getCellWithValue(
                groupWeeklyReport.getPercents() != null ? groupWeeklyReport.getPercents() : "-");
        groupRate.setNote(groupWeeklyReport.getGroupComments());
        CellData visitsWhite = getCellWithValue(groupWeeklyReport.getVisitWhite());
        CellData meetingsWhite = getCellWithValue(groupWeeklyReport.getMeetingWhite());
        CellData visitsNew = getCellWithValue(groupWeeklyReport.getVisitNew());
        CellData meetingsNew = getCellWithValue(groupWeeklyReport.getMeetingNew());
        CellData callsNew = getCellWithValue(groupWeeklyReport.getCalls());

        rowData.setValues(Arrays.asList(leader, weekName, listWhite, presentTotal, presentWhite, presentGuests,
                presentNew, groupRate, visitsWhite, meetingsWhite, visitsNew, meetingsNew, callsNew));
        return rowData;
    }

    public static void printWeeks(Sheets service, Group group, List<GroupWeeklyReport> groupWeeklyReports, int row) throws IOException {
        prettyPrintWeeks(groupWeeklyReports);

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
        for (int i = 0; i <= groupWeeklyReports.size() - 2; i++) {
            allRows.add(getWeekRow(groupWeeklyReports.get(i), group, uniqueNewPeople, false));
        }
        allRows.add(getWeekRow(groupWeeklyReports.get(groupWeeklyReports.size() - 1), group, uniqueNewPeople, true));

        int lastWhiteCount = groupWeeklyReports.get(groupWeeklyReports.size() - 1).getWhiteList().size();

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
