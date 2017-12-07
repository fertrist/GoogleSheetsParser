package app.extract;

import static app.extract.ReportUtil.hasBackground;
import app.data.ColorActionMapper;
import app.data.ColumnDateMapper;
import app.data.GroupTableData;
import app.entities.Action;
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
        ColumnDateMapper columnDateMapper = groupTableData.getColumnDateMapper();
        ColorActionMapper colorActionMapper = groupTableData.getColorActionMapper();

        List<CellData> personCells = personRow.getValues();

        List<ReportItem> reportItems = new ArrayList<>();

        for (int i = 0; i < personCells.size(); i++)
        {
            CellData cell = personCells.get(i);

            if (!hasBackground(cell)) continue;

            Color bgColor = cell.getEffectiveFormat().getBackgroundColor();


            Action action = colorActionMapper.getActionByColor(bgColor);

            if (action != null)
            {
                LocalDate date = columnDateMapper.dateForColumn(i);
                reportItems.add(new ReportItem(person.clone(), action, date));
            }
        }
        return reportItems;
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
