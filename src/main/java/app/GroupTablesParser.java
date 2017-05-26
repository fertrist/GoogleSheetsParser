package app;

import com.google.api.services.sheets.v4.model.*;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.Color;
import com.google.common.collect.Lists;
import javafx.util.Pair;
import sun.java2d.xr.MutableInteger;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static app.ConfigurationUtil.*;
import static app.Util.*;
import static app.ConfigurationUtil.getProperty;
import static app.ReportTableHelper.*;
import static java.util.Collections.singletonList;

/**
 * Pass CONFIGURATION_FILE as vm option for example:
 * -DCONFIGURATION_FILE=/home/myUser/.../resources/report.configuration.properties
 */
public class GroupTablesParser extends GoogleSheetsApp {

    public static void main(String[] args) throws IOException {
        Sheets service = getSheetsService();

        int regionCount = Integer.valueOf(getProperty(REGION_COUNT));
        List<RegionData> regions = new ArrayList<>();
        for (int i = 1; i <= regionCount; i++) {
            regions.add(processRegion(service, i));
        }
        report(service, regions);
    }

    private static void report(Sheets service, List<RegionData> regions) throws IOException {
        Integer sheetGid = getSheetGid(getProperty(REPORT_SPREADSHEET_URL));

        UpdateCellsRequest updateHeaderRequest = new UpdateCellsRequest();
        updateHeaderRequest.setFields("*");

        GridCoordinate headerGridCoordinate = new GridCoordinate();
        headerGridCoordinate.setColumnIndex(0);
        headerGridCoordinate.setSheetId(sheetGid);
        headerGridCoordinate.setRowIndex(0);
        updateHeaderRequest.setStart(headerGridCoordinate);

        // TODO add report main header (report summary)
        List<RowData> headers = new ArrayList<>();
        headers.add(getTitleHeader());
        headers.add(getReportHeader());
        updateHeaderRequest.setRows(headers);

        MutableInteger rowPointer = new MutableInteger(0);
        rowPointer.setValue(rowPointer.getValue() + 2);
        int totalNewCount = 0;
        int totalWhiteCount = 0;
        for (RegionData region : regions) {
            printRegion(service, rowPointer, region);
            totalNewCount += region.getTotalNewCount();
            totalWhiteCount += region.getTotalWhiteCount();
        }
        // TODO add report main footer (current summary)
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

        // TODO make header frozen, create borders
        UpdateBordersRequest updateBordersRequest = new UpdateBordersRequest();
        GridRange borderRange = new GridRange().setStartColumnIndex(0).setEndColumnIndex(getReportColumns().length + 1)
                .setStartRowIndex(1).setEndRowIndex(rowPointer.getValue() + 1).setSheetId(sheetGid);
        updateBordersRequest.setRange(borderRange);
        Border border = new Border().setColor(getColor(0, 0, 0)).setStyle("SOLID");
        updateBordersRequest.setTop(border).setLeft(border).setBottom(border).setRight(border)
                .setInnerHorizontal(border).setInnerVertical(border);

        UpdateSheetPropertiesRequest freezeHeaderUpdateTitleRequest = new UpdateSheetPropertiesRequest().setFields("*");
        GridProperties gridProperties = new GridProperties().setFrozenRowCount(2).setRowCount(100)
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

    private static void printRegion(Sheets service, MutableInteger rowPointer, RegionData region) throws IOException {
        // TODO print region header
        List<Request> requests = new ArrayList<>();

        UpdateCellsRequest updateCellsRequest = new UpdateCellsRequest().setFields("*");

        GridCoordinate gridCoordinate = new GridCoordinate().setColumnIndex(0)
            .setSheetId(getSheetGid(getProperty(REPORT_SPREADSHEET_URL))).setRowIndex(rowPointer.getValue());

        updateCellsRequest.setStart(gridCoordinate);

        updateCellsRequest.setRows(singletonList(new RowData().setValues(
                        Lists.asList(getCellWithBgColor(region.getLeader(), GREY), Collections.nCopies(12, getCellWithBgColor(GREY)).toArray(new CellData[12])))
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

        for (Map<String, List<Week>> group : region.getGroups()) {
            Map.Entry<String, List<Week>> groupEntry = group.entrySet().iterator().next();
            String groupLeader = groupEntry.getKey();
            List<Week> groupWeeks = groupEntry.getValue();
            reportWeeks(service, groupLeader, groupWeeks, rowPointer.getValue());
            rowPointer.setValue(rowPointer.getValue() + groupWeeks.size() + 1);
        }
    }

    private static RegionData processRegion(Sheets service, int regionNo) throws IOException {
        String regionLeader = getRegionProperty(LEADER, regionNo);
        System.out.println("Processing " + regionLeader + "'s region.");

        // get groups data from properties
        String [] groupNumbers = getRegionProperty(GROUPS, regionNo).split(",");
        List<Group> groups = Arrays.stream(groupNumbers)
                .map(Integer::valueOf)
                .map(ConfigurationUtil::buildGroup)
                .collect(Collectors.toList());

        //print group properties
        groups.forEach(System.out::println);

        // loop through groups, collect and print data
        RegionData regionData = new RegionData(regionLeader);
        for (Group group : groups) {
            Map<String, List<Week>> groupData = new HashMap<>();
            groupData.put(group.getLeaderName(), parseGroup(service, group));
            regionData.getGroups().add(groupData);
        }
        return regionData;
    }

    private static List<Week> parseGroup(Sheets service, Group group) throws IOException {
        String spreadsheetId = group.getSpreadSheetId();
        String peopleColumn = group.getPeopleColumn();

        String monthsRange = group.getRowWithMonths() + ":" + group.getRowWithMonths();
        String peopleListRange = peopleColumn + group.getDataFirstRow() + ":" + peopleColumn + group.getDataLastRow();
        String colorStartColumn = Character.valueOf((char) (peopleColumn.toCharArray()[0] - 1)).toString();
        String colorsRange = colorStartColumn + group.getColorsRow() + ":" + peopleColumn + (Integer.valueOf(group.getColorsRow()) + 10);

        Spreadsheet spreadsheet = service.spreadsheets().get(spreadsheetId)
                .setRanges(Arrays.asList(monthsRange, colorsRange, peopleListRange)).setIncludeGridData(true).execute();

        //get moths and rough start end columns
        Sheet sheet = spreadsheet.getSheets().get(0); //sheet object required to get merged cells
        Pair<Integer, Integer> roughColumnsRange = getRoughColumnsRange(sheet);
        System.out.printf("roughColumnsRange : [%d : %d] %n", roughColumnsRange.getKey(), roughColumnsRange.getValue());
        System.out.printf("roughColumnsRange : [%s : %s] %n", columnToLetter(roughColumnsRange.getKey()), columnToLetter(roughColumnsRange.getValue()));

        Map<Marks, Color> colors = parseColors(sheet.getData().get(1));

        //TODO get white list rows range
        List<Person> people = new ArrayList<>();
        GridData peopleData = sheet.getData().get(2);
        for (int i = 0; i < peopleData.getRowData().size(); i++) {
            RowData r = peopleData.getRowData().get(i);
            if (r == null || r.getValues() == null) {
                continue;
            }
            CellData cellData = r.getValues().get(0);
            CellFormat effectiveFormat = cellData.getEffectiveFormat();
            if (!effectiveFormat.getTextFormat().getBold() && cellData.getEffectiveValue() != null) {
                Category category;
                if (isWhite(effectiveFormat.getBackgroundColor())) {
                    category = Category.WHITE;
                } else if (isGrey(effectiveFormat.getBackgroundColor())) {
                    category = Category.GUEST;
                } else {
                    category = Category.NEW;
                }
                people.add(new Person(category, cellData.getEffectiveValue().getStringValue(), i));
            }
        }

        //TODO get week columns and parse by rows by colored lists
        String dataRange = columnToLetter(roughColumnsRange.getKey() + 1) + 1 + ":" + columnToLetter(roughColumnsRange.getValue()) + group.getDataLastRow();
        spreadsheet = service.spreadsheets().get(spreadsheetId).setRanges(singletonList(dataRange)).setIncludeGridData(true).execute();

        List<RowData> dataRows = spreadsheet.getSheets().get(0).getData().get(0).getRowData();
        return parseWeeks(dataRows, Integer.valueOf(group.getDataFirstRow()), people, Integer.valueOf(group.getGroupDay()), colors);
    }

    private static List<Week> parseWeeks(List<RowData> dataRows, int dataFirstRow, List<Person> people,
                                         Integer groupDay, Map<Marks, Color> colors) {
        dataFirstRow--; // offset because of indexing
        List<Person> whiteList = people.stream().filter(p -> p.getCategory() == Category.WHITE).collect(Collectors.toList());

        // define exact start end
        RowData monthsRow = dataRows.get(0);
        List<CellData> monthsCells = monthsRow.getValues();
        RowData daysRow = dataRows.get(1);
        List<CellData> daysCells = daysRow.getValues();
        RowData datesRow = dataRows.get(2);
        List<CellData> datesCells = datesRow.getValues();
        int startColumn = 0, endColumn = 0;
        String month = "";
        Map<String, List<Integer>> columnToMonthMap = new HashMap<>();
        for (int i = 0; i < datesCells.size(); i++) {
            int monthIndex = Math.min(i, monthsCells.size() - 1);

            String newMonth = getMonthFromString(monthsCells.get(monthIndex).getEffectiveValue() != null
                    ? monthsCells.get(monthIndex).getEffectiveValue().getStringValue().toLowerCase() : month);
            if (!newMonth.equals(month)) {
                month = newMonth;
                columnToMonthMap.put(month, new ArrayList<>());
            }
            columnToMonthMap.get(month).add(i);

            Double day = datesCells.get(i).getEffectiveValue().getNumberValue();
            if (month.equalsIgnoreCase(getReportStartMonth())
                    && day.equals((double) getReportStartDay())) {
                startColumn = i;
            }
            if (month.equalsIgnoreCase(getReportEndMonth())
                    && day.equals((double) getReportEndDay())) {
                endColumn = i;
            }
        }

        String groupWeekDay = getWeekDay(groupDay);
        List<Week> weeks = new ArrayList<>();
        for (int weekIndex = startColumn; weekIndex <= endColumn; weekIndex++) {
            Week week = new Week();
            week.setWhiteList(whiteList);
            int monthNumber = findMonthForColumn(columnToMonthMap, weekIndex);
            int dayNumber = datesCells.get(weekIndex).getEffectiveValue().getNumberValue().intValue();
            week.setStart(monthNumber, dayNumber);

            // parse week for each guy
            int dayIndex = 0;
            for (Person person : people) {
                dayIndex = weekIndex - 1;
                RowData personRow = dataRows.get(dataFirstRow + person.getIndex());
                List<CellData> monthCells = personRow.getValues();
                String weekDay;
                do {
                    dayIndex++;
                    weekDay = daysCells.get(dayIndex).getEffectiveValue().getStringValue();
                    CellData cell = monthCells.get(dayIndex);
                    if (cell.getEffectiveFormat() == null || cell.getEffectiveFormat().getBackgroundColor() == null) continue;
                    if (weekDay.equalsIgnoreCase(groupWeekDay)) {
                        boolean wasPresentOnGroup = areColorsEqual(cell.getEffectiveFormat().getBackgroundColor(), colors.get(Marks.GROUP));
                        if (wasPresentOnGroup) {
                            week.addPresent(person);
                        }
                    }
                    Color bgColor = cell.getEffectiveFormat().getBackgroundColor();
                    Marks action = getActionByColor(bgColor, colors);
                    if (action != null) {
                        week.mergeAction(action, person.getCategory());
                    }
                } while (!weekDay.equalsIgnoreCase("вс") && dayIndex < (monthCells.size() - 1));
            }
            weekIndex += (dayIndex - weekIndex);
            monthNumber = findMonthForColumn(columnToMonthMap, weekIndex);
            dayNumber = datesCells.get(weekIndex).getEffectiveValue().getNumberValue().intValue();
            week.setEnd(monthNumber, dayNumber);
            weeks.add(week);
        }
        return weeks;
    }

    private static void reportWeeks(Sheets service, String groupLeader, List<Week> weeks, int row) throws IOException {
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
        for (Week week : weeks) {
            allRows.add(getWeekRow(week, groupLeader, uniqueNewPeople));
        }
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

    private static RowData getWeekRow(Week week, String groupLeader, Set<String> newPeople) {
        RowData rowData = new RowData();

        CellData leader = getCellWithValue(groupLeader);
        CellData weekName = getCellWithValue(week.getWeekName());
        CellData presentTotal = getCellWithValue(week.getTotalCount());
        CellData listWhite = getCellWithValue(week.getWhiteList().size());
        CellData presentWhite = getCell(week.getPresentByCategory(Category.WHITE).size(), listToNote(week.getWhiteAbsent()));
        CellData presentGuests = getCell(week.getPresentByCategory(Category.GUEST).size(), listToNote(week.getPresentByCategory(Category.GUEST)));
        CellData presentNew = getCell(week.getPresentByCategory(Category.NEW).size(), listToNote(week.getPresentByCategory(Category.NEW)));
        newPeople.addAll(week.getPresentByCategory(Category.NEW).stream().map(Person::getName).collect(Collectors.toSet()));
        CellData groupRate = getCellWithValue("-||-");
        CellData visitsWhite = getCellWithValue(week.getVisitWhite());
        CellData meetingsWhite = getCellWithValue(week.getMeetingWhite());
        CellData visitsNew = getCellWithValue(week.getVisitNew());
        CellData meetingsNew = getCellWithValue(week.getMeetingNew());
        CellData callsNew = getCellWithValue(week.getCalls());

        rowData.setValues(Arrays.asList(leader, weekName, listWhite, presentTotal, presentWhite, presentGuests,
                presentNew, groupRate, visitsWhite, meetingsWhite, visitsNew, meetingsNew, callsNew));
        return rowData;
    }

    private static Marks getActionByColor(Color color, Map<Marks, Color> colors) {
        for (Map.Entry<Marks, Color> e : colors.entrySet()) {
            if (areColorsEqual(e.getValue(), color)) {
                return e.getKey();
            }
        }
        return null;
    }

    private static Pair<Integer, Integer> getRoughColumnsRange(Sheet monthsSheet) {
        RowData monthsRow = monthsSheet.getData().get(0).getRowData().get(0);
        List<CellData> cellDatas = monthsRow.getValues();
        List<GridRange> merges = monthsSheet.getMerges();
        merges.sort((Comparator.comparing(GridRange::getStartColumnIndex)));

        int initialIndex = merges.size() > 12 ? merges.size() - 12 : 0;
        merges = merges.subList(initialIndex, merges.size());

        int startColumn = 0;
        int endColumn = 0;
        for (GridRange merge : merges) {
            int startIndex = merge.getStartColumnIndex();
            String monthName = cellDatas.get(startIndex).getEffectiveValue().getStringValue().toLowerCase();
            if (monthName.contains(getReportStartMonth())) {
                startColumn = merge.getStartColumnIndex();
            }
            if (monthName.contains(getReportEndMonth())) {
                endColumn = merge.getEndColumnIndex();
            }
        }
        return new Pair<>(startColumn, endColumn);
    }

    private static Map<Marks, Color> parseColors(GridData gridData) {
        Map<Marks, Color> colors = new HashMap<>();
        for (RowData r : gridData.getRowData()) {
            if (r == null || r.getValues() == null) {
                continue;
            }
            CellData colorCell = r.getValues().get(0);
            CellData nameCell = r.getValues().get(1);
            if (nameCell.getEffectiveValue() == null || colorCell.getEffectiveFormat() == null
                    || colorCell.getEffectiveFormat().getBackgroundColor() == null) {
                continue;
            }
            Color backgroundColor = colorCell.getEffectiveFormat().getBackgroundColor();
            Marks mark = Marks.getEnumFor(nameCell.getEffectiveValue().getStringValue());
            if (mark != null)
                colors.putIfAbsent(mark, backgroundColor);
        }
        return colors;
    }

}