package domains;

public class Language {

    public Language(String longName, String shortName) {
        this.shortName = shortName;
        this.longName = longName;
    }

    public Language() {

    }

    private String longName;

    public String getLongName() {
        return longName;
    }

    public void setLongName(String longName) {
        this.longName = longName;
    }

    public String getShortName() {
        return shortName;
    }

    public void setShortName(String shortName) {
        this.shortName = shortName;
    }

    private String shortName;


    @Override
    public String toString() {
        return "Language{" +
                "longName='" + longName + '\'' +
                ", shortName='" + shortName + '\'' +
                '}';
    }
}
