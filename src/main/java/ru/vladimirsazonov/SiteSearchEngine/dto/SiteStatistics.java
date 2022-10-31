package ru.vladimirsazonov.SiteSearchEngine.dto;

import lombok.Data;

@Data
public class SiteStatistics {
    private final String url;
    private final String name;
    private final String status;
    private final String statusTime;
    private final int pages;
    private final int lemmas;
}
