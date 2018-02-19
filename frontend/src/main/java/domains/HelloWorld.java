package domains;

public class HelloWorld {

    private Language language;

    public Language getLanguage() {
        return language;
    }

    public void setLanguage(Language language) {
        this.language = language;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    private String text;

    @Override
    public String toString() {
        return "HelloWorld{" +
                "language='" + language.getLongName() + '\'' +
                ", test='" + text + '\'' +
                '}';
    }
}
