package app;

import com.google.api.services.sheets.v4.model.CellData;
import com.google.api.services.sheets.v4.model.Color;
import com.google.api.services.sheets.v4.model.RowData;
import com.sun.deploy.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static app.Util.getCellWithBgColor;
import static app.Util.getCellWithValue;
import static app.Util.getColor;

public class ReportTableHelper {

    private static String [] REPORT_COLUMNS = new String[]{"Лидер", "Неделя", "По списку", "Было всего", "Белый список", "Гости", "Новые люди",
            "Как прошла гр.(%)", "Посещ.списки", "Встр. списки", "Посещ.новые", "Встр. новые", "Звонки"};
    public static final Color YELLOW = getColor(255, 214, 93);
    public static final Color BLUE = getColor(147, 176, 255);
    public static final Color GREY = getColor(134, 133, 135);

    public static RowData getTitleHeader() {
        RowData titleRow = new RowData();
        List<CellData> headerCells = new ArrayList<>();

        headerCells.add(getCellWithBgColor(String.format("Динамический отчет за: \n %s : %s",
                ConfigurationUtil.getReportStartDate(), ConfigurationUtil.getReportEndDate()), BLUE));
        headerCells.add(getCellWithBgColor(BLUE));
        headerCells.add(getCellWithBgColor(ConfigurationUtil.getProperty(ConfigurationUtil.PREVIOUS_WHITE_COUNT), BLUE));
        headerCells.add(getCellWithBgColor(BLUE));
        headerCells.add(getCellWithBgColor(BLUE));
        headerCells.add(getCellWithBgColor(BLUE));
        headerCells.add(getCellWithBgColor(ConfigurationUtil.getProperty(ConfigurationUtil.PREVIOUS_NEW_COUNT), BLUE));

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

    public static void prettyPrintWeeks(List<Week> weeks) {
        String format = "%10s | %10s | %10s | %10s | %10s | %10s | %15s | %15s | %15s | %15s | %10s %n";
        System.out.printf(format, "Неделя", "По списку", "Было всего", "Cписочных", "Гости", "Новые люди",
                "Посещ.списки", "Встр. списки", "Посещ.новые", "Встр. новые", "Звонки");
        for (Week week : weeks) {
            System.out.printf(format, week.getWeekName(), week.getWhiteList().size(), week.getPresent().size(), week.getPresentByCategory(Category.WHITE).size(),
                    week.getPresentByCategory(Category.GUEST).size(), week.getPresentByCategory(Category.NEW).size(), week.getVisitWhite(),
                    week.getMeetingWhite(), week.getVisitNew(), week.getMeetingNew(), week.getCalls());
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
        footerCells.add(getCellWithBgColor(uniqueNewPeople.size(), YELLOW).setNote(StringUtils.join(uniqueNewPeople, "\n")));
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

}
