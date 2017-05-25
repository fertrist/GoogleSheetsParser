package app;

enum Marks {
    GROUP(new String[]{"групп", "домашк"}),
    VISIT(new String[]{"посещени"}),
    MEETING(new String[]{"встреч"}),
    CALL(new String[]{"звон"});

    private String[] possibleValues;

    Marks(String [] possibleValues) {
        this.possibleValues = possibleValues;
    }

    public String [] getPossibleValues() {
        return possibleValues;
    }

    public static Marks getEnumFor(String value) {
        for (Marks mark : values()) {
            for (String possibleValue : mark.getPossibleValues()) {
                if (value.toLowerCase().contains(possibleValue)) {
                    return mark;
                }
            }
        }
        return null;
    }
}
