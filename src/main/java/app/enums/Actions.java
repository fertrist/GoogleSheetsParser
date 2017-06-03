package app.enums;

public enum Actions {
    GROUP(new String[]{"групп", "домашк"}),
    VISIT(new String[]{"посещени"}),
    MEETING(new String[]{"встреч"}),
    CALL(new String[]{"звон"});

    private String[] possibleValues;

    Actions(String [] possibleValues) {
        this.possibleValues = possibleValues;
    }

    public String [] getPossibleValues() {
        return possibleValues;
    }

    public static Actions getEnumFor(String value) {
        for (Actions mark : values()) {
            for (String possibleValue : mark.getPossibleValues()) {
                if (value.toLowerCase().contains(possibleValue)) {
                    return mark;
                }
            }
        }
        return null;
    }
}
