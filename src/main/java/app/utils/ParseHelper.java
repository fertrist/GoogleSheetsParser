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
    public static List<Person> parsePeople(List<RowData> peopleData, Group group) {
        List<Person> people = new ArrayList<>();

        int offset = group.getDataFirstRow() - 1;

        for (int i = 0; i < peopleData.size(); i++) {

            RowData r = peopleData.get(i);

            if (isRowEmpty(r)) continue;
            if (r.getValues().size() < 2) continue; // should be at least 2 columns

            CellData cellData = r.getValues().get(1); // 2nd column is column with people names

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

    public static Map<Actions, Color> parseColors(List<RowData> rows, int colorsRow) {
        Map<Actions, Color> colors = new HashMap<>();
        for (int j = colorsRow + 1; j < Math.min(colorsRow + DATA_OVERLAP, rows.size()); j++) {
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

    public static Pair<Integer, Integer> getLastDataAndColorsRow(List<RowData> rows, int offsetFromStart) {
        int lastDataRow = 0;
        int colorsRow = 0;

        for (int i = offsetFromStart; i < rows.size(); i++) {

            RowData r = rows.get(i);

            if (isRowEmpty(r)) continue;

            CellData cell = r.getValues().get(Math.min(r.getValues().size() - 1, 1));

            if (isCellEmpty(cell)) continue;

            if (isColorsTitle(cell)) {
                colorsRow = i;
                break;
            }

            lastDataRow = i;
        }
        return new Pair<>(lastDataRow + offsetFromStart, colorsRow + offsetFromStart);
    }

    public static Map<Integer, LocalDate> getColumnToDateMap(Map<String, Pair<Integer, Integer>> monthLimits,
                                                             Pair<Integer, Integer> reportLimits,
                                                             List<CellData> datesCells) {

        Map<Integer, LocalDate> columnToDateMap = new HashMap<>();
        int currentYear = LocalDate.now().getYear();

        for (String month : monthLimits.keySet()) {

            Pair<Integer, Integer> limit = monthLimits.get(month);
            if (limit.getKey() > reportLimits.getValue()
                    || limit.getValue() < reportLimits.getKey()) continue;

            for (int i = limit.getKey(); i <= limit.getValue(); i++) {
                if (i < reportLimits.getKey() || i > reportLimits.getValue()) continue;

                int dayOfMonth = datesCells.get(i-1).getEffectiveValue().getNumberValue().intValue();
                columnToDateMap.put(i - reportLimits.getKey(), LocalDate.of(currentYear, getMonthNumber(month), dayOfMonth));
            }
        }

        return columnToDateMap;
    }

}
