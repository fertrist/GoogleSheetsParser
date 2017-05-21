package app;

enum Marks {
    GROUP("групп"), VISIT("посещени"), MEETING("встреч"), CALL("звон");

    private String name;

    Marks(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public static Marks getEnumFor(String value) {
        for (Marks mark : values()) {
            if (value.toLowerCase().contains(mark.getName())) {
                return mark;
            }
        }
        return null;
    }
}
