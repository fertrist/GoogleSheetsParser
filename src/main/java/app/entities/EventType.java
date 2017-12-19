package app.entities;

public enum EventType {
    GROUP(new String[]{"групп", "домашк"}),
    VISIT(new String[]{"посещени"}),
    MEETING(new String[]{"встреч"}),
    CALL(new String[]{"звон"});

    private String[] possibleValues;

    EventType(String[] possibleValues) {
        this.possibleValues = possibleValues;
    }

    public String [] getPossibleValues() {
        return possibleValues;
    }

    public static EventType getEnumFor(String value) {
        for (EventType mark : values()) {
            for (String possibleValue : mark.getPossibleValues()) {
                if (value.toLowerCase().contains(possibleValue)) {
                    return mark;
                }
            }
        }
        return null;
    }
}
