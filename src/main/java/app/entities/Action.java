package app.entities;

public enum Action {
    GROUP(new String[]{"групп", "домашк"}),
    VISIT(new String[]{"посещени"}),
    MEETING(new String[]{"встреч"}),
    CALL(new String[]{"звон"});

    private String[] possibleValues;

    Action(String[] possibleValues) {
        this.possibleValues = possibleValues;
    }

    public String [] getPossibleValues() {
        return possibleValues;
    }

    public static Action getEnumFor(String value) {
        for (Action mark : values()) {
            for (String possibleValue : mark.getPossibleValues()) {
                if (value.toLowerCase().contains(possibleValue)) {
                    return mark;
                }
            }
        }
        return null;
    }
}
