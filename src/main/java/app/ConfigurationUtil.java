package app;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Properties;

public class ConfigurationUtil {

    public static final String RESULT_SPREADSHEET_ID = "result.spreadSheetId";
    public static final String GROUP_COUNT = "group.count";
    public static final String SPREADSHEET_ID = "spreadSheetId";
    public static final String LEADER = "leaderName";
    public static final String ROW_WITH_MONTHS = "rowWithMonths";
    public static final String GROUP_DAY = "groupDay";
    public static final String COLORS_ROW = "colorsRow";
    public static final String DATA_FIRST_ROW = "dataFirstRow";
    public static final String DATA_LAST_ROW = "dataLastRow";

    private static final String GROUP_PREFIX = "group%d.";
    private static final String CONFIGURATION_FILE = "CONFIGURATION_FILE";

    static Properties properties;
    static {
        String configurationFile = System.getProperty(CONFIGURATION_FILE);
        properties = new Properties();
        try {
            properties.load(new InputStreamReader(new FileInputStream(configurationFile)));
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public static String propForGroup(int groupNumber, String property) {
        return String.format(GROUP_PREFIX, groupNumber) + property;
    }

    public static Group buildGroup(int groupNo) {
        return Group.builder().groupNumber(groupNo)
                .spreadSheetId(getGroupProperty(SPREADSHEET_ID, groupNo))
                .leaderName(getGroupProperty(LEADER, groupNo))
                .groupDay(getGroupProperty(GROUP_DAY, groupNo))
                .monthsRow(getGroupProperty(ROW_WITH_MONTHS, groupNo))
                .markingRow(getGroupProperty(COLORS_ROW, groupNo))
                .dataStartRow(getGroupProperty(DATA_FIRST_ROW, groupNo))
                .dataEndRow(getGroupProperty(DATA_LAST_ROW, groupNo))
                .build();
    }

    private static String getGroupProperty(String property, int groupNo) {
        return properties.getProperty(propForGroup(groupNo, property));
    }

    public static String getProperty(String property) {
        return properties.getProperty(property);
    }

}
