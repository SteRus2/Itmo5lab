package us.obviously.itmo.prog.gui.i18n;

import us.obviously.itmo.prog.gui.controllers.Translatable;

import java.util.ResourceBundle;

public class Internalization {
    private static Language currentLanguage;
    private static ResourceBundle currentBundle;
    private static Translatable currentController;

    static {
        setLanguage(Language.RUSSIAN); // Default language
    }

    public static void setLanguage(Language language) {
        currentLanguage = language;
        currentBundle = ResourceBundle.getBundle("us.obviously.itmo.prog.gui.i18n.ResourceBundle", currentLanguage.getLocale());
        requestTranslateCurrentController();
    }

    public static ResourceBundle getBundle() {
        return currentBundle;
    }

    public static String getTranslation(String key) {
        return currentBundle.getString(key);
    }

    public static Object getObject(String key) {
        return currentBundle.getObject(key);
    }

    public static Language getCurrentLanguage() {
        return currentLanguage;
    }

    public static void applyNewController(Translatable currentController) {
        Internalization.currentController = currentController;
        requestTranslateCurrentController();
    }

    private static void requestTranslateCurrentController() {
        if (currentController == null) return;
        currentController.setBundle(getCurrentLanguage());
    }
}
