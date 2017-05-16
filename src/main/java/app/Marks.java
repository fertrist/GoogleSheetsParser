package app;

enum Marks {
    GROUP("группа"), VISIT("посещение"), MEETING("встреча"), CALL("звонок");

    private String name;

    Marks(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public static Marks getEnumFor(String value) {
        for (Marks mark : values()) {
            if (mark.getName().equals(value)) {
                return mark;
            }
        }
        return null;
    }
}
