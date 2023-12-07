package ru.vladimirsazonov.SiteSearchEngine.exceptions;

public class ServerStateException extends RuntimeException {
    public ServerStateException(String message) {
        super(message);
    }
}
