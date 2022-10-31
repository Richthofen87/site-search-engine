package ru.vladimirsazonov.SiteSearchEngine.exceptions;

public class IndexPageException extends RuntimeException{

    public IndexPageException() {
        super("Данная страница находится за пределами сайтов," +
                " указанных в конфигурационном файле!");
    }
}
