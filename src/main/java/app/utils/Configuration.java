package app.utils;

import app.entities.Group;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.LocalDate;
import java.time.Month;
import java.util.Properties;

public class Configuration {

    public static final String REPORT_SPREADSHEET_URL = "report.spreadsheet.url";
    public static final String REGIONS = "report.regions";
    public static final String REPORT_START_DATE = "report.start.date";
    public static final String REPORT_END_DATE = "report.end.date";
    public static final String REPORT_TITLE = "report.title";
    public static final String PREVIOUS_WHITE_COUNT = "previous.white.count";
    public static final String PREVIOUS_NEW_COUNT = "previous.new.count";

    public static final String SPREADSHEET_URL = "spreadsheet.url";
    public static final String LEADER = "leader.name";
    public static final String ROW_WITH_MONTHS = "row.with.months";
    public static final String GROUP_DAY = "group.day";
    public static final String COLORS_ROW = "colors.row";
    public static final String DATA_FIRST_ROW = "data.first.row";
    public static final String DATA_LAST_ROW = "data.last.row";
    public static final String PEOPLE_COLUMN = "people.column";
    public static final String GROUPS = "groups";

    private static final String REGION_PREFIX = "region%s.";
    private static final String GROUP_PREFIX = "group%d.";
    private static final String CONFIGURATION_FILE = "CONFIGURATION_FILE";

    private static String reportSpreadSheetId;
    private static String previousWhiteCount;
    private static String previousNewCount;

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
        return Group.builder().groupNumber(groupNo)
                .spreadSheetId(getSpreadsheetId(getGroupProperty(SPREADSHEET_URL, groupNo)))
                .leaderName(getGroupProperty(LEADER, groupNo))
                .groupDay(getGroupProperty(GROUP_DAY, groupNo))
                .monthsRow(getGroupProperty(ROW_WITH_MONTHS, groupNo))
                .markingRow(getGroupProperty(COLORS_ROW, groupNo))
                .dataStartRow(getGroupProperty(DATA_FIRST_ROW, groupNo))
                .dataEndRow(getGroupProperty(DATA_LAST_ROW, groupNo))
                .peopleColumn(getGroupProperty(PEOPLE_COLUMN, groupNo))
                .build();
    }

    private static String getGroupProperty(String property, int groupNo) {
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

    public static void setReportStartDate(String reportStartDate) {
        Configuration.reportStartDate = reportStartDate;
    }

    public static String getPreviousWhiteCount() {
        return previousWhiteCount;
    }

    public static String getPreviousNewCount() {
        return previousNewCount;
    }
}
