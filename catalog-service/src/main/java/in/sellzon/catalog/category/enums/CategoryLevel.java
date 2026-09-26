package in.sellzon.catalog.category.enums;

public enum CategoryLevel {
    LEVEL_1,
    LEVEL_2,
    LEVEL_3,
    LEVEL_4;

    public int getLevelNumber() {
        return switch (this) {
            case LEVEL_1 -> 1;
            case LEVEL_2 -> 2;
            case LEVEL_3 -> 3;
            case LEVEL_4 -> 4;
        };
    }

    public static CategoryLevel fromLevelNumber(int level) {
        return switch (level) {

            case 1 -> LEVEL_1;
            case 2 -> LEVEL_2;
            case 3 -> LEVEL_3;
            case 4 -> LEVEL_4;
            default -> throw new IllegalArgumentException("Category level must be between 1 and 4");
        };
    }
}
