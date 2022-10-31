package ru.vladimirsazonov.SiteSearchEngine.dto;

import lombok.Getter;

@Getter
public class ErrorSiteStatistics extends SiteStatistics {
    private final String error;

    public ErrorSiteStatistics(String url, String name, String status,
                               String statusTime, String error, int pages, int lemmas) {
        super(url, name, status, statusTime, pages, lemmas);
        this.error = error;
    }
}
