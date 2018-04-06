package app.conf;

import static java.lang.String.format;
import app.entities.Group;
import org.apache.commons.lang3.StringUtils;

import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

public class GroupBuilder {
    public static final String SPREADSHEET_URL = "spreadsheet.url";
    public static final String ROW_WITH_MONTHS = "row.with.months";
    public static final String GROUP_DAY = "group.day";
    public static final String DATA_FIRST_ROW = "data.first.row";
    public static final String PEOPLE_COLUMN = "people.column";
    public static final String ADDED_PEOPLE = "added.people";
    public static final String HAS_STAGES_ROW = "has.stages";
    public static final String REMOVED_PEOPLE = "removed.people";
    public static final String DEFAULT_ROW_WITH_MONTHS = "default.row.with.months";
    public static final String DEFAULT_GROUP_DAY = "default.group.day";
    public static final String DEFAULT_PEOPLE_COLUMN = "default.people.column";
    private static final String GROUP_PREFIX = "group%d.";
    public static final String LEADER = "leader.name";

    private Properties properties;
    private int groupOrdinalNumber;

    public GroupBuilder(Properties properties, int groupOrdinalNumber) {
        this.properties = properties;
        this.groupOrdinalNumber = groupOrdinalNumber;
    }

    public Group buildGroup()
    {
        String spreadsheetId = getSpreadsheetId(getGroupProperty(SPREADSHEET_URL));
        String leaderName = getGroupProperty(LEADER);
        DayOfWeek groupWeekDay = getGroupDay();

        int rowWithMonths = getRowWithMonth();
        int rowWithDays = getRowWithDays(rowWithMonths);
        int rowWithDates = rowWithDays + 1;
        int dataFirstRow = getFirstDataRow(rowWithDates);
        String peopleColumn = getPeopleColumn();

        String addedPeopleStr = getGroupProperty(ADDED_PEOPLE);
        List<String> addedPeople = getListOfTrimmedNames(addedPeopleStr);

        String removedPeopleStr = getGroupProperty(REMOVED_PEOPLE);
        List<String> removedPeople = getListOfTrimmedNames(removedPeopleStr);

        return Group.builder().groupNumber(groupOrdinalNumber).spreadSheetId(spreadsheetId)
                .leaderName(leaderName)
                .groupDay(groupWeekDay)
                .monthsRow(rowWithMonths)
                .rowWithDates(rowWithDates)
                .rowWithDays(rowWithDays)
                .peopleColumn(peopleColumn)
                .dataStartRow(dataFirstRow)
                .addedPeople(addedPeople)
                .removedPeople(removedPeople)
                .build();
    }

    private List<String> getListOfTrimmedNames(String stringList)
    {
        if (StringUtils.isBlank(stringList))
        {
            return new ArrayList<>();
        }
        return Arrays.stream(stringList.split(","))
                .filter(StringUtils::isNotBlank)
                .map(String::trim)
                .collect(Collectors.toList());
    }

    private String getPeopleColumn()
    {
        String peopleColumn = getGroupProperty(PEOPLE_COLUMN);
        return StringUtils.isBlank(peopleColumn) ? getProperty(DEFAULT_PEOPLE_COLUMN) : peopleColumn;
    }

    private int getFirstDataRow(int rowWithDates)
    {
        int dataFirstRow = rowWithDates + 1;
        String dataFirstRowStr = getGroupProperty(DATA_FIRST_ROW);
        if (StringUtils.isNotBlank(dataFirstRowStr))
        {
            dataFirstRow = Integer.valueOf(dataFirstRowStr);
        }
        return dataFirstRow;
    }

    private int getRowWithDays(int rowWithMonths)
    {
        boolean hasStagesRow = Boolean.valueOf(getGroupProperty(HAS_STAGES_ROW));
        int stagesOffset = hasStagesRow ? 1 : 0;

        return rowWithMonths + stagesOffset + 1;
    }

    private int getRowWithMonth()
    {
        String rowWithMonthsStr = getGroupProperty(ROW_WITH_MONTHS);
        rowWithMonthsStr = StringUtils.isBlank(rowWithMonthsStr) ? getProperty(DEFAULT_ROW_WITH_MONTHS) : rowWithMonthsStr;
        return Integer.valueOf(rowWithMonthsStr);
    }

    private DayOfWeek getGroupDay()
    {
        String groupDay = getGroupProperty(GROUP_DAY);
        if (StringUtils.isBlank(groupDay))
        {
            groupDay = getProperty(DEFAULT_GROUP_DAY);
        }
        Integer groupDayInt = Integer.valueOf(groupDay.trim());
        return getDayOfWeek(groupDayInt);
    }

    public static DayOfWeek getDayOfWeek(int d) {
        return DayOfWeek.values()[d-1];
    }

    public String getGroupProperty(String property) {
        return properties.getProperty(format(GROUP_PREFIX, groupOrdinalNumber) + property);
    }

    public String getProperty(String property) {
        return properties.getProperty(property);
    }

    public static String getSpreadsheetId(String spreadSheetUrl) {
        String d = "/d/";
        return spreadSheetUrl.substring(spreadSheetUrl.indexOf(d) + d.length(), spreadSheetUrl.indexOf("/edit"));
    }
}
