package app.entities;

import app.conf.Constants;

public class GroupMeetingNotes
{
    private String percentage;
    private String comments;

    public static GroupMeetingNotes fromNote(String note)
    {
        String percentage = getPercentageFromNote(note);
        String comments = getCommentsFromNote(note);
        return new GroupMeetingNotes(percentage, comments);
    }

    private static String getPercentageFromNote(String note)
    {
        String percentage = note.split("\\n")[0];
        return percentage.matches(Constants.GROUP_PERCENT_LINE_REGEX) ? percentage : null;
    }

    private static String getCommentsFromNote(String note)
    {
        String percentage = getPercentageFromNote(note);
        return percentage == null ? null : skipLineFromNote(note, percentage);
    }

    private static String skipLineFromNote(String note, String firstLine)
    {
        return note.substring(note.indexOf(firstLine), note.length());
    }

    private GroupMeetingNotes(String percentage, String comments)
    {
        this.percentage = percentage;
        this.comments = comments;
    }

    public String getPercentage() {
        return percentage;
    }

    public void setPercentage(String percentage) {
        this.percentage = percentage;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }
}
