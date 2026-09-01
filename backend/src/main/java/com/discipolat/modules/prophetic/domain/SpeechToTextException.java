package com.discipolat.modules.prophetic.domain;

public class SpeechToTextException extends RuntimeException {

    public SpeechToTextException(String message) {
        super(message);
    }

    public SpeechToTextException(String message, Throwable cause) {
        super(message, cause);
    }
}