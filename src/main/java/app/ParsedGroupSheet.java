package app;

import com.google.api.services.sheets.v4.model.RowData;

import java.util.List;

public class ParsedGroupSheet
{
    private RowData monthsRow;

    private RowData datesRow;

    private List<RowData> data;

    public static Builder builder()
    {
        return new Builder();
    }

    private static class Builder
    {
        private RowData monthsRow;

        private RowData datesRow;

        private List<RowData> data;



        public Builder from(RowData monthsRow)
        {
            this.monthsRow = monthsRow;
            return this;
        }

        public Builder withMonthsRow(RowData monthsRow)
        {
            this.monthsRow = monthsRow;
            return this;
        }

        public ParsedGroupSheet build()
        {
            return new ParsedGroupSheet();
        }
    }
}
