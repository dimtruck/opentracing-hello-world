package domains;

public class HelloWorldRequest {

    private String longName;
    private String shortName;
    private String translation;

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

    public String getTranslation() {
        return translation;
    }

    public void setTranslation(String translation) {
        this.translation = translation;
    }

    @Override
    public String toString() {
        return "HelloWorldRequest{" +
                "longName='" + getLongName() + '\'' +
                "shortName='" + getShortName() + '\'' +
                "translation='" + getTranslation() + '\'' +
                '}';
    }
}
