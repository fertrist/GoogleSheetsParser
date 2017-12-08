package app.extract;

import app.data.ColorActionMapper;
import app.data.ColumnDateMapper;
import app.data.GroupTableData;
import app.entities.Action;
import app.entities.CellWrapper;
import app.entities.Person;
import app.report.ReportItem;
import com.google.api.services.sheets.v4.model.CellData;
import com.google.api.services.sheets.v4.model.Color;
import com.google.api.services.sheets.v4.model.RowData;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ReportItemsExtractor
{
    private GroupTableData groupTableData;

    public ReportItemsExtractor(GroupTableData groupTableData)
    {
        this.groupTableData = groupTableData;
    }

    public List<ReportItem> extractItems()
    {
        List<ReportItem> reportItems = new ArrayList<>();

        List<RowData> dataRows = groupTableData.getData();
        List<Person> people = groupTableData.getPeople();

        for (Person person : people)
        {
            RowData personRow = getPersonRow(dataRows, person);
            if (!isRowEmpty(personRow))
            {
                reportItems.addAll(extractPersonItemsFromRow(personRow, person));
            }
        }
        return reportItems;
    }

    private List<ReportItem> extractPersonItemsFromRow(RowData personRow, Person person)
    {
        List<CellData> personCells = personRow.getValues();

        List<ReportItem> reportItems = new ArrayList<>();
        for (int cellColumn = 0; cellColumn < personCells.size(); cellColumn++)
        {
            CellData cell = personCells.get(cellColumn);

            ReportItem reportItem = getReportItemForCell(cell, cellColumn);
            if (reportItem != null)
            {
                reportItem.setPerson(person.clone());
                reportItems.add(reportItem);
            }
        }
        return reportItems;
    }

    private ReportItem getReportItemForCell(CellData cellData, int cellColumn)
    {
        LocalDate date = getDateForColumn(cellColumn);
        Action action = getActionByCellBackground(cellData);
        return action != null ? new ReportItem(action, date) : null;
    }

    private LocalDate getDateForColumn(int column)
    {
        ColumnDateMapper columnDateMapper = groupTableData.getColumnDateMapper();
        return columnDateMapper.dateForColumn(column);
    }

    private Action getActionByCellBackground(CellData cellData)
    {
        Color bgColor = new CellWrapper(cellData).getBgColor().getColor();
        ColorActionMapper colorActionMapper = groupTableData.getColorActionMapper();
        return bgColor != null ? colorActionMapper.getActionByColor(bgColor) : null;
    }

    private RowData getPersonRow(List<RowData> dataRows, Person person)
    {
        RowData personRow = null;

        if (!isPersonRowAbsent(dataRows, person))
        {
            personRow = dataRows.get(person.getIndex());
        }

        return personRow;
    }

    public boolean isRowEmpty(RowData rowData)
    {
        return rowData == null || rowData.getValues() == null;
    }

    private boolean isPersonRowAbsent(List<RowData> dataRows, Person person)
    {
        return dataRows.size() < toTableIndex(person.getIndex());
    }

    private int toTableIndex(int row)
    {
        return row + 1;
    }
}
