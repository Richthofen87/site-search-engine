package ru.vladimirsazonov.SiteSearchEngine.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class StatResponse extends SearchEngineResponse {
    private final Statistics statistics;
    public record Statistics(Total total, List<SiteStatistics> detailed) {}
    public record Total(int sites, long pages, long lemmas, boolean isIndexing) {}
}
