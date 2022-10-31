package ru.vladimirsazonov.SiteSearchEngine.exceptions;

public class EmptySearchQueryException extends RuntimeException {
    public EmptySearchQueryException() {
        super("Поисковый запрост пуст!");
    }
}
