package xyz.volcanobay.empiric.core.ui.bind;

public class UiException extends RuntimeException {

    public UiException(String message) {
        super(message);
    }

    public UiException(String message, Throwable cause) {
        super(message, cause);
    }
}
