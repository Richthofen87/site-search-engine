package ru.vladimirsazonov.SiteSearchEngine.exceptions;

public class DisableSiteSearchException extends RuntimeException{

    public DisableSiteSearchException() {
        super("Данный сайт не указан в конфигурационном файле!");
    }
}
