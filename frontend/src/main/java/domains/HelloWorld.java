package domains;

public class HelloWorld {

    public HelloWorld() {}
    public HelloWorld(Language language, String translation) {
        this.language = language;
        this.translation = translation;
    }

    private Language language;

    public Language getLanguage() {
        return language;
    }

    public void setLanguage(Language language) {
        this.language = language;
    }

    public String getTranslation() {
        return translation;
    }

    public void setTranslation(String translation) {
        this.translation = translation;
    }

    private String translation;

    @Override
    public String toString() {
        return "HelloWorld{" +
                "language='" + language.getLongName() + '\'' +
                ", translation='" + translation + '\'' +
                '}';
    }
}
