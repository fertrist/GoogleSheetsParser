package app.utils;

import app.entities.Group;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.LocalDate;
import java.time.Month;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import static app.utils.ReportUtil.isEmpty;

public class Configuration {

    public static final String REPORT_SPREADSHEET_URL = "report.spreadsheet.url";
    public static final String REGIONS = "report.regions";
    public static final String REPORT_START_DATE = "report.start.date";
    public static final String REPORT_END_DATE = "report.end.date";
    public static final String REPORT_TITLE = "report.title";
    public static final String PREVIOUS_WHITE_COUNT = "previous.white.count";
    public static final String PREVIOUS_NEW_COUNT = "previous.new.count";

    public static final String DEFAULT_ROW_WITH_MONTHS = "default.row.with.months";
    public static final String DEFAULT_GROUP_DAY = "default.group.day";
    public static final String DEFAULT_PEOPLE_COLUMN = "default.people.column";

    public static final String SPREADSHEET_URL = "spreadsheet.url";
    public static final String LEADER = "leader.name";
    public static final String ROW_WITH_MONTHS = "row.with.months";
    public static final String GROUP_DAY = "group.day";
    public static final String DATA_FIRST_ROW = "data.first.row";
    public static final String PEOPLE_COLUMN = "people.column";
    public static final String ADDED_PEOPLE = "added.people";
    public static final String REMOVED_PEOPLE = "removed.people";
    public static final String GROUPS = "groups";

    private static final String REGION_PREFIX = "region%s.";
    private static final String GROUP_PREFIX = "group%d.";
    private static final String CONFIGURATION_FILE = "CONFIGURATION_FILE";

    private static String reportSpreadSheetId;
    private static int reportStartDay;
    private static String reportStartMonth;
    private static String reportStartDate;
    private static int reportEndDay;
    private static String reportEndMonth;
    private static String reportEndDate;

    static Properties properties;
    static {
        String configurationFile = System.getProperty(CONFIGURATION_FILE);
        properties = new Properties();
        try {
            properties.load(new InputStreamReader(new FileInputStream(configurationFile)));
        } catch (IOException e) {
            e.printStackTrace();
        }

        reportStartDate = properties.getProperty(REPORT_START_DATE);
        LocalDate startDate = LocalDate.parse(reportStartDate);
        reportStartMonth = translateMonth(startDate.getMonth());
        reportStartDay = startDate.getDayOfMonth();

        reportEndDate = properties.getProperty(REPORT_END_DATE);
        LocalDate endDate = LocalDate.parse(reportEndDate);
        reportEndMonth = translateMonth(endDate.getMonth());
        reportEndDay = endDate.getDayOfMonth();

        reportSpreadSheetId = getSpreadsheetId(properties.getProperty(REPORT_SPREADSHEET_URL));
    }

    private static String translateMonth(Month month) {
        String translation = null;
        switch (month) {
            case JANUARY:
                translation = "январь";
                break;
            case FEBRUARY:
                translation = "февраль";
                break;
            case MARCH:
                translation = "март";
                break;
            case APRIL:
                translation = "апрель";
                break;
            case MAY:
                translation = "май";
                break;
            case JUNE:
                translation = "июнь";
                break;
            case JULY:
                translation = "июль";
                break;
            case AUGUST:
                translation = "август";
                break;
            case SEPTEMBER:
                translation = "сентябрь";
                break;
            case OCTOBER:
                translation = "октябрь";
                break;
            case NOVEMBER:
                translation = "ноябрь";
                break;
            case DECEMBER:
                translation = "декабрь";
                break;
        }
        return translation;
    }

    public static Group buildGroup(int groupNo) {
        String spreadsheetId = getSpreadsheetId(getGroupProperty(SPREADSHEET_URL, groupNo));
        String leaderName = getGroupProperty(LEADER, groupNo);
        String groupDay = getGroupProperty(GROUP_DAY, groupNo);
        groupDay = groupDay == null ? getProperty(DEFAULT_GROUP_DAY) : groupDay;

        String rowWithMonths = getGroupProperty(ROW_WITH_MONTHS, groupNo);
        rowWithMonths = rowWithMonths == null ? getProperty(DEFAULT_ROW_WITH_MONTHS) : rowWithMonths;

        String peopleColumn = getGroupProperty(PEOPLE_COLUMN, groupNo);
        peopleColumn = peopleColumn == null ? getProperty(DEFAULT_PEOPLE_COLUMN) : peopleColumn;

        String dataFirstRow = getGroupProperty(DATA_FIRST_ROW, groupNo);
        dataFirstRow = dataFirstRow != null ? dataFirstRow
                : String.valueOf(Integer.valueOf(rowWithMonths) + 2);

        String addedPeopleStr = getGroupProperty(ADDED_PEOPLE, groupNo);

        String removedPeopleStr = getGroupProperty(REMOVED_PEOPLE, groupNo);

        List<String> addedPeople = isEmpty(addedPeopleStr) ? new ArrayList<>()
                : Arrays.asList(addedPeopleStr.split(","));
        List<String> removedPeople = isEmpty(removedPeopleStr) ? new ArrayList<>()
                : Arrays.asList(removedPeopleStr.split(","));

        return Group.builder().groupNumber(groupNo).spreadSheetId(spreadsheetId)
                .leaderName(leaderName)
                .groupDay(groupDay)
                .monthsRow(rowWithMonths)
                .peopleColumn(peopleColumn)
                .dataStartRow(dataFirstRow)
                .addedPeople(addedPeople)
                .removedPeople(removedPeople)
                .build();
    }

    public static String getGroupProperty(String property, int groupNo) {
        return properties.getProperty(String.format(GROUP_PREFIX, groupNo) + property);
    }

    public static String getRegionProperty(String property, String regionNo) {
        return properties.getProperty(String.format(REGION_PREFIX, regionNo) + property);
    }

    public static String getProperty(String property) {
        return properties.getProperty(property);
    }

    public static String getSpreadsheetId(String spreadSheetUrl) {
        String d = "/d/";
        return spreadSheetUrl.substring(spreadSheetUrl.indexOf(d) + d.length(), spreadSheetUrl.indexOf("/edit"));
    }

    public static Integer getSheetGid(String spreadSheetUrl) {
        String edit = "/edit#gid=";
        return Integer.valueOf(spreadSheetUrl.substring(
                spreadSheetUrl.indexOf(edit) + edit.length(), spreadSheetUrl.length()));
    }

    public static String getReportEndMonth() {
        return reportEndMonth;
    }

    public static int getReportEndDay() {
        return reportEndDay;
    }

    public static String getReportStartMonth() {
        return reportStartMonth;
    }

    public static int getReportStartDay() {
        return reportStartDay;
    }

    public static String getReportSpreadSheetId() {
        return reportSpreadSheetId;
    }

    public static String getReportStartDate() {
        return reportStartDate;
    }

    public static String getReportEndDate() {
        return reportEndDate;
    }

}
