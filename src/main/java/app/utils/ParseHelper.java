package app.utils;

import app.entities.Group;
import app.entities.Person;
import app.enums.Actions;
import app.enums.Category;
import com.google.api.services.sheets.v4.model.*;
import javafx.util.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static app.utils.Configuration.*;
import static app.utils.ReportUtil.*;
import static app.utils.ReportUtil.findMonthForColumn;

/**
 * Does stuff related to parsing.
 */
public class ParseHelper {

    /**
     * Retrieves people by categories
     */
    public static List<Person> parsePeople(GridData peopleData, Group group) {
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

    public static Map<Actions, Color> parseColors(GridData gridData) {
        Map<Actions, Color> colors = new HashMap<>();
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
            Actions mark = Actions.getEnumFor(nameCell.getEffectiveValue().getStringValue());
            if (mark != null)
                colors.putIfAbsent(mark, backgroundColor);
        }
        return colors;
    }

    public static Map<String, List<Integer>> getColumnToMonthMap(List<CellData> monthsCells, List<CellData> datesCells) {
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
        }
        return columnToMonthMap;
    }

    public static Pair<Integer, Integer> getStartEndColumns(Map<String, List<Integer>> columnToMonthMap,
                                                            List<CellData> datesCells) {
        int startColumn = 0;
        int endColumn = 0;
        for (int i = 0; i < datesCells.size(); i++)
        {
            Double day = datesCells.get(i).getEffectiveValue().getNumberValue();
            String month = findMonthForColumn(i, columnToMonthMap);
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
        return new Pair<>(startColumn, endColumn);
    }
}
