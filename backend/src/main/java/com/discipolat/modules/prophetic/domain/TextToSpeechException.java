package com.discipolat.modules.prophetic.domain;

/**
 * Exception levée lorsqu'une synthèse vocale échoue.
 */
public class TextToSpeechException extends RuntimeException {

    public TextToSpeechException(String message) {
        super(message);
    }

    public TextToSpeechException(String message, Throwable cause) {
        super(message, cause);
    }
}
