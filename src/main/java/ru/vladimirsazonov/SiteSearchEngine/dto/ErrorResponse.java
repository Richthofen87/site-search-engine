package ru.vladimirsazonov.SiteSearchEngine.dto;

import lombok.Getter;

@Getter
public class ErrorResponse extends SearchEngineResponse {
    private final String error;

    public ErrorResponse(String errorMessage) {
        error = errorMessage;
        setResult(false);
    }
}
