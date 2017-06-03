package app;

import app.entities.Group;
import app.entities.Person;
import app.entities.Region;
import app.entities.Week;
import app.enums.Category;
import app.enums.Actions;
import app.utils.Configuration;
import app.utils.SheetsApp;
import com.google.api.services.sheets.v4.model.*;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.Color;
import com.google.common.collect.Lists;
import javafx.util.Pair;
import sun.java2d.xr.MutableInteger;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static app.enums.Actions.GROUP;
import static app.utils.Configuration.*;
import static app.utils.ReportUtil.*;
import static app.utils.Configuration.getProperty;
import static app.utils.ReportHelper.*;
import static java.util.Collections.singletonList;

/**
 * Pass CONFIGURATION_FILE as vm option for example:
 * -DCONFIGURATION_FILE=/home/myUser/.../resources/report.configuration.properties
 */
public class ReportProcessor extends SheetsApp {

    public static void main(String[] args) throws IOException {
        Sheets service = getSheetsService();

        String[] regionNumbers = getProperty(REGIONS).split(",");
        List<Region> regions = new ArrayList<>();
        for (String regionNumber : regionNumbers){
            regions.add(processRegion(service, regionNumber));
        }
        report(service, regions);
    }

    private static Region processRegion(Sheets service, String regionNo) throws IOException {
        String regionLeader = getRegionProperty(LEADER, regionNo);
        System.out.println("Processing " + regionLeader + "'s region.");

        // get groups data from properties
        String [] groupNumbers = getRegionProperty(GROUPS, regionNo).split(",");
        List<Group> groups = Arrays.stream(groupNumbers)
                .map(Integer::valueOf)
                .map(Configuration::buildGroup)
                .collect(Collectors.toList());

        //print group properties
        groups.forEach(System.out::println);

        // loop through groups, collect and print data
        Region region = new Region(regionLeader);
        for (Group group : groups) {
            Map<Group, List<Week>> groupData = new HashMap<>();
            List<Week> weeks = processGroup(service, group);
            region.getGroups().put(group, weeks);
        }
        return region;
    }

    private static List<Week> processGroup(Sheets service, Group group) throws IOException {
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
        Pair<Integer, Integer> roughColumnsRange = getApproximateColumnsRange(sheet);
        System.out.printf("roughColumnsRange : [%d : %d] %n", roughColumnsRange.getKey(), roughColumnsRange.getValue());
        System.out.printf("roughColumnsRange : [%s : %s] %n", columnToLetter(roughColumnsRange.getKey()), columnToLetter(roughColumnsRange.getValue()));

        Map<Actions, Color> colors = parseColors(sheet.getData().get(1));
        List<Person> people = parsePeople(sheet.getData().get(2), group);

        //TODO get week columns and parse by rows by colored lists
        String dataRange = columnToLetter(roughColumnsRange.getKey() + 1) + 1 + ":" + columnToLetter(roughColumnsRange.getValue()) + group.getDataLastRow();
        spreadsheet = service.spreadsheets().get(spreadsheetId).setRanges(singletonList(dataRange)).setIncludeGridData(true).execute();

        List<RowData> dataRows = spreadsheet.getSheets().get(0).getData().get(0).getRowData();
        return processAllWeeks(dataRows, group, people, colors);
    }

    /**
     * Retrieves people by categories
     */
    private static List<Person> parsePeople(GridData peopleData, Group group) {
        List<Person> people = new ArrayList<>();

        for (int i = 0; i < peopleData.getRowData().size(); i++) {

            RowData r = peopleData.getRowData().get(i);
            if (r == null || r.getValues() == null)
            {
                continue;
            }

            CellData cellData = r.getValues().get(0);
            CellFormat effectiveFormat = cellData.getEffectiveFormat();

            if (!effectiveFormat.getTextFormat().getUnderline() && cellData.getEffectiveValue() != null) {

                String name = cellData.getEffectiveValue().getStringValue();
                Category category;
                boolean isAdded = containsIgnoreCase(group.getAddedPeople(), name);
                if (isWhite(effectiveFormat.getBackgroundColor()) && !isAdded)
                {
                    category = Category.WHITE;
                }
                else if (isGrey(effectiveFormat.getBackgroundColor())
                        || name.toLowerCase().contains("(и.с") || name.toLowerCase().contains("(исп.срок)"))
                {
                    category = Category.GUEST;
                }
                else
                {
                    category = Category.NEW;
                }
                people.add(new Person(category, cellData.getEffectiveValue().getStringValue(), i));
            }
        }
        return people;
    }

    /**
     * Retrieves people by categories
     */
    private static List<Week> processAllWeeks(
            List<RowData> dataRows, Group group, List<Person> people, Map<Actions, Color> colors)
    {
        int dataFirstRow = Integer.valueOf(group.getDataFirstRow());
        int groupDay = Integer.valueOf(group.getGroupDay());

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
        for (int i = 0; i < datesCells.size(); i++)
        {
            int monthIndex = Math.min(i, monthsCells.size() - 1);

            String newMonth = getMonthFromString(monthsCells.get(monthIndex).getEffectiveValue() != null
                    ? monthsCells.get(monthIndex).getEffectiveValue().getStringValue().toLowerCase() : month);

            if (!newMonth.equals(month))
            {
                month = newMonth;
                columnToMonthMap.put(month, new ArrayList<>());
            }
            columnToMonthMap.get(month).add(i);

            Double day = datesCells.get(i).getEffectiveValue().getNumberValue();

            if (month.equalsIgnoreCase(getReportStartMonth())
                    && day.equals((double) getReportStartDay()))
            {
                startColumn = i;
            }

            if (month.equalsIgnoreCase(getReportEndMonth())
                    && day.equals((double) getReportEndDay()))
            {
                endColumn = i;
            }
        }

        String groupWeekDay = getWeekDay(groupDay);
        List<Week> weeks = new ArrayList<>();
        Map<Week, List<Pair<Person, Actions>>> weekActionsMap = new HashMap<>();

        for (int weekIndex = startColumn; weekIndex <= endColumn; weekIndex++)
        {
            Week week = new Week();
            week.getWhiteList().addAll(whiteList);
            int monthNumber = findMonthForColumn(columnToMonthMap, weekIndex);
            int dayNumber = datesCells.get(weekIndex).getEffectiveValue().getNumberValue().intValue();
            week.setStart(monthNumber, dayNumber);

            // parse week for each guy
            int dayIndex = 0;
            int groupDayIndex = 0;

            List<Pair<Person, Actions>> actionsList = new ArrayList<>();
            for (Person person : people) {
                dayIndex = weekIndex - 1;
                RowData personRow = dataRows.get(dataFirstRow + person.getIndex());
                List<CellData> monthCells = personRow.getValues();
                String weekDay;
                do {
                    dayIndex++;
                    weekDay = daysCells.get(dayIndex).getEffectiveValue().getStringValue();
                    CellData cell = monthCells.get(dayIndex);
                    if (cell.getEffectiveFormat() == null
                            || cell.getEffectiveFormat().getBackgroundColor() == null){
                        continue;
                    }
                    Color bgColor = cell.getEffectiveFormat().getBackgroundColor();

                    if (weekDay.equalsIgnoreCase(groupWeekDay)) {
                        boolean wasPresentOnGroup = areColorsEqual(bgColor, colors.get(GROUP));
                        if (wasPresentOnGroup) {
                            actionsList.add(new Pair<>(person.clone(), GROUP));
                            //week.addPresent(person);
                            if (groupDayIndex == 0) {
                                groupDayIndex = dayIndex;
                            }
                            continue;
                        }
                    }
                    Actions action = getActionByColor(bgColor, colors);

                    if (action != null) {
                        //week.mergeAction(action, person.getCategory());
                        actionsList.add(new Pair<>(person.clone(), action));
                    }
                } while (!weekDay.equalsIgnoreCase("вс") && dayIndex < (monthCells.size() - 1));
            }

            weekActionsMap.put(week, actionsList);

            setGroupComments(week, daysCells.get(groupDayIndex), datesCells.get(groupDayIndex));

            weekIndex += (dayIndex - weekIndex);
            monthNumber = findMonthForColumn(columnToMonthMap, weekIndex);
            dayNumber = datesCells.get(weekIndex).getEffectiveValue().getNumberValue().intValue();
            week.setEnd(monthNumber, dayNumber);
            weeks.add(week);
        }

        // merge actions
        int lastIndex = weeks.size() - 1;
        for (int i = 0; i <= lastIndex; i++) {
            Week week = weeks.get(i);
            List<Pair<Person, Actions>> actions = weekActionsMap.get(week);
            for (Pair<Person, Actions> action : actions) {
                Person person = action.getKey();
                if (i == lastIndex && containsIgnoreCase(group.getAddedPeople(), person.getName())) {
                    person.setCategory(Category.WHITE);
                }
                if (i == lastIndex && containsIgnoreCase(group.getRemovedPeople(), person.getName())) {
                    person.setCategory(Category.NEW);
                }

                if (action.getValue() == GROUP) {
                    week.addPresent(person);
                } else {
                    week.mergeAction(action.getValue(), person.getCategory());
                }
            }
            if (i == lastIndex) {
                for (Person person : people) {
                    Person updated = person.clone();
                    if (containsIgnoreCase(group.getAddedPeople(), person.getName())) {
                        updated.setCategory(Category.WHITE);
                        week.getWhiteList().add(updated);
                    }
                    if (containsIgnoreCase(group.getRemovedPeople(), person.getName())) {
                        updated.setCategory(Category.NEW);
                        week.getWhiteList().remove(updated);
                    }
                }
            }
        }

        return weeks;
    }

    private static void setGroupComments(Week week, CellData dayCell, CellData dateCell) {
        String groupNote = dayCell.getNote();
        groupNote = groupNote != null ? groupNote : dateCell.getNote();
        if (groupNote != null && !groupNote.isEmpty()) {
            String firstString = groupNote.split("\\n")[0];
            if (firstString.matches("[0-9]+[%]")) {
                week.setPercents(firstString);
                String comment = groupNote
                        .substring(groupNote.indexOf(firstString), groupNote.length());
                week.setGroupComments(comment.trim());
            } else {
                week.setGroupComments(groupNote);
            }
        }
    }

    /**
     * Get raw, approximate, rough range of columns to work with (to avoid parsing old columns)
     */
    private static Pair<Integer, Integer> getApproximateColumnsRange(Sheet monthsSheet) {
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
            String monthName = cellDatas.get(startIndex).getEffectiveValue().getStringValue();
            if (monthName == null) continue;
            monthName = monthName.toLowerCase();
            if (monthName.contains(getReportStartMonth())) {
                startColumn = merge.getStartColumnIndex();
            }
            if (monthName.contains(getReportEndMonth())) {
                endColumn = merge.getEndColumnIndex();
            }
        }
        return new Pair<>(startColumn, endColumn);
    }

    /**
     * Reports all regions to report sheet.
     */
    private static void report(Sheets service, List<Region> regions) throws IOException {
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
        for (Region region : regions) {
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

    /**
     * Prints single region to the report sheet.
     * @param rowPointer which counts offset of rows, to print next items correctly
     */
    private static void printRegion(Sheets service, MutableInteger rowPointer, Region region) throws IOException {
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

        for (Map.Entry<Group, List<Week>> groupEntry : region.getGroups().entrySet()) {
            List<Week> groupWeeks = groupEntry.getValue();
            reportWeeks(service, groupEntry.getKey(), groupWeeks, rowPointer.getValue());
            rowPointer.setValue(rowPointer.getValue() + groupWeeks.size() + 1);
        }
    }
}