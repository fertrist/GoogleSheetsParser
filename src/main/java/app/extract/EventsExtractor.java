package app.extract;

import static app.extract.ReportUtil.hasBackground;
import app.data.GroupTableData;
import app.entities.CellWrapper;
import app.entities.EventType;
import app.entities.Person;
import app.report.Event;
import com.google.api.services.sheets.v4.model.CellData;
import com.google.api.services.sheets.v4.model.Color;
import com.google.api.services.sheets.v4.model.RowData;
import javafx.util.Pair;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class EventsExtractor
{
    private GroupTableData groupTableData;

    public EventsExtractor(GroupTableData groupTableData)
    {
        this.groupTableData = groupTableData;
    }

    public List<Event> extractEvents()
    {
        List<Event> events = new ArrayList<>();

        List<RowData> dataRows = groupTableData.getData();
        List<Person> people = groupTableData.getPeople();

        for (Person person : people)
        {
            RowData personRow = getPersonRow(dataRows, person);
            if (!isRowEmpty(personRow))
            {
                events.addAll(extractPersonItems(personRow, person));
            }
        }
        return events;
    }

    private List<Event> extractPersonItems(RowData personRow, Person person)
    {
        List<CellData> personCells = personRow.getValues();

        return IntStream.range(0, personCells.size())
                .mapToObj(i -> new Pair<>(i, personCells.get(i)))
                .filter(pair -> hasBackground(pair.getValue()))
                .map(extractEventSetPerson(person))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }

    private Function<Pair<Integer, CellData>, Optional<Event>> extractEventSetPerson(Person person)
    {
        return pair ->
        {
            Optional<Event> itemOptional = extractEventFromCell(pair.getValue());
            itemOptional.ifPresent(item ->
            {
                item.setDate(getDateForColumn(pair.getKey()));
                item.setPerson(person);
            });
            return itemOptional;
        };
    }

    private Optional<Event> extractEventFromCell(CellData cell)
    {
        Color bgColor = new CellWrapper(cell).getBgColor().getColor();
        EventType eventType = getEventTypeByColor(bgColor);

        Event item = (eventType == null) ? null : new Event(eventType);
        return Optional.ofNullable(item);
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

    private LocalDate getDateForColumn(Integer column)
    {
        return groupTableData.getColumnDateMapper().dateForColumn(column);
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

    private EventType getEventTypeByColor(Color bgColor)
    {
        return groupTableData.getColorActionMapper().getActionByColor(bgColor);
    }
}
