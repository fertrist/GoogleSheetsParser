package app.report;

import app.entities.ColRow;

public class ReportRange
{
    private ColRow start;
    private ColRow end;

    public ReportRange(ColRow start, ColRow end) {
        this.start = start;
        this.end = end;
    }

    public ColRow getStart() {
        return start;
    }

    public void setStart(ColRow start) {
        this.start = start;
    }

    public ColRow getEnd() {
        return end;
    }

    public void setEnd(ColRow end) {
        this.end = end;
    }
}
