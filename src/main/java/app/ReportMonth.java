package app;

import app.utils.ReportUtil.Month;

public class ReportMonth
{
    private Month month;
    private int start;
    private int end;

    public ReportMonth(Month month, int start, int end) {
        this.month = month;
        this.start = start;
        this.end = end;
    }

    public Month getMonth() {
        return month;
    }

    public void setMonth(Month month) {
        this.month = month;
    }

    public int getStart() {
        return start;
    }

    public void setStart(int start) {
        this.start = start;
    }

    public int getEnd() {
        return end;
    }

    public void setEnd(int end) {
        this.end = end;
    }

    public int getMonthNumber()
    {
        return month.ordinal() + 1;
    }
}
