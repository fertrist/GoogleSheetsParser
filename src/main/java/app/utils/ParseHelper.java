package app.utils;

import app.entities.Group;
import app.entities.Person;
import app.enums.Actions;
import app.enums.Category;
import com.google.api.services.sheets.v4.model.*;
import javafx.util.Pair;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static app.utils.Configuration.*;
import static app.utils.ReportUtil.*;

/**
 * Does stuff related to parsing.
 */
public class ParseHelper {

    private static final String COLORS_TITLE = "условные обозначения";
    public static final Integer DATA_OVERLAP = 30;

    /**
     * Retrieves people by categories
     */
    public static List<Person> parsePeople(GridData peopleData, Group group) {
        List<Person> people = new ArrayList<>();

        int offset = Integer.valueOf(group.getDataFirstRow()) - 1;

        for (int i = 0; i < peopleData.getRowData().size(); i++) {

            RowData r = peopleData.getRowData().get(i);

            if (isRowEmpty(r)) continue;

            CellData cellData = r.getValues().get(1); // column with people names

            if (isColorsTitle(cellData)) return people;

            if (!isUnderline(cellData) && !isCellEmpty(cellData)) {
                Category category = defineCategory(cellData, group);
                people.add(new Person(category, cellData.getEffectiveValue().getStringValue(), offset + i));
            }
        }

        return people;
    }

    private static Category defineCategory(CellData cellData, Group group) {

        CellFormat effectiveFormat = cellData.getEffectiveFormat();

        String name = cellData.getEffectiveValue().getStringValue();
        Category category;

        // handle added/removed people (they first are considered as if they weren't added/removed)
        boolean isAdded = containsIgnoreCase(group.getAddedPeople(), name);
        boolean isRemoved = containsIgnoreCase(group.getRemovedPeople(), name);
        boolean onTrial = name.toLowerCase().contains("(и.с") || name.toLowerCase().contains("(исп.срок)");

        if ((isWhite(effectiveFormat.getBackgroundColor()) && !isAdded) || isRemoved) {
            category = Category.WHITE;
        }
        else if (isGrey(effectiveFormat.getBackgroundColor()) || onTrial) {
            category = Category.GUEST;
        }
        else {
            category = Category.NEW;
        }

        return category;
    }

    private static boolean isRowEmpty(RowData r) {
        return r == null || r.getValues() == null;
    }

    private static boolean isColorsTitle(CellData cellData) {
        return isUnderline(cellData) && cellData.getEffectiveValue() != null
                && cellData.getEffectiveValue().getStringValue().equalsIgnoreCase(COLORS_TITLE);
    }

    private static boolean isUnderline(CellData cellData) {
        return cellData.getEffectiveFormat() != null && cellData.getEffectiveFormat().getTextFormat().getUnderline();
    }

    private static boolean isCellEmpty(CellData cellData) {
        return cellData.getEffectiveValue() == null || cellData.getEffectiveValue().getStringValue() == null
                || cellData.getEffectiveValue().getStringValue().isEmpty();
    }

    public static Map<Actions, Color> parseColors(GridData gridData, int colorsRow) {
        Map<Actions, Color> colors = new HashMap<>();
        List<RowData> rows = gridData.getRowData();
        for (int j = colorsRow + 1; j < colorsRow + DATA_OVERLAP; j++) {
            RowData r = rows.get(j);

            if (isRowEmpty(r)) continue;

            CellData colorCell = r.getValues().get(0);
            CellData nameCell = r.getValues().get(1);

            if (isCellEmpty(nameCell) ||
                    (colorCell.getEffectiveFormat() == null
                    || colorCell.getEffectiveFormat().getBackgroundColor() == null)) {
                break;
            }

            Color backgroundColor = colorCell.getEffectiveFormat().getBackgroundColor();
            Actions mark = Actions.getEnumFor(nameCell.getEffectiveValue().getStringValue());
            if (mark != null)
                colors.putIfAbsent(mark, backgroundColor);
        }
        return colors;
    }

    public static Pair<Integer, Integer> getLastDataAndColorsRow(GridData gridData, int fromIndex, int offsetFromStart) {
        int lastDataRow = 0;
        int colorsRow = 0;

        for (int i = fromIndex - 5; i < gridData.getRowData().size(); i++) {

            RowData r = gridData.getRowData().get(i);

            if (isRowEmpty(r) || isCellEmpty(r.getValues().get(1))) continue;

            if (isColorsTitle(r.getValues().get(1))) {
                colorsRow = i;
                break;
            }

            lastDataRow = i;
        }
        return new Pair<>(lastDataRow + offsetFromStart, colorsRow + offsetFromStart);
    }

    public static Map<Integer, LocalDate> getColumnToDateMap(List<CellData> monthsCells, List<CellData> datesCells) {
        String month = "";

        Map<Integer, LocalDate> columnToDateMap = new HashMap<>();
        int currentYear = LocalDate.now().getYear();
        for (int i = 0; i < datesCells.size(); i++)
        {
            int monthIndex = Math.min(i, monthsCells.size() - 1);
            int dayOfMonth = datesCells.get(i).getEffectiveValue().getNumberValue().intValue();

            String newMonth = getMonthFromString(monthsCells.get(monthIndex).getEffectiveValue() != null
                    ? monthsCells.get(monthIndex).getEffectiveValue().getStringValue().toLowerCase() : month);

            if (!newMonth.equals(month))
            {
                month = newMonth;
            }
            columnToDateMap.put(i, LocalDate.of(currentYear, getMonthNumber(month), dayOfMonth));
        }
        return columnToDateMap;
    }

    public static Pair<Integer, Integer> getStartEndColumns(Map<Integer, LocalDate> columnToDate,
                                                            List<CellData> datesCells) {
        int startColumn = 0;
        int endColumn = 0;
        for (int i = 0; i < datesCells.size(); i++)
        {
            int day = datesCells.get(i).getEffectiveValue().getNumberValue().intValue();
            String month = findMonthForColumnStr(columnToDate, i);
            if (month.equalsIgnoreCase(getReportStartMonth())
                    && day == getReportStartDay())
            {
                startColumn = i;
            }

            if (month.equalsIgnoreCase(getReportEndMonth())
                    && day == getReportEndDay())
            {
                endColumn = i;
            }
        }
        if (endColumn == 0) {
            endColumn = datesCells.size();
        }
        return new Pair<>(startColumn, endColumn);
    }
}
