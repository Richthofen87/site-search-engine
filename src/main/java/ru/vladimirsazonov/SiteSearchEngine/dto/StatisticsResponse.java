package ru.vladimirsazonov.SiteSearchEngine.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Getter
@RequiredArgsConstructor
public class StatisticsResponse extends SearchEngineResponse {
    private final Statistics statistics;

    public record Statistics(Total total, List<SiteStatistics> detailed) {
    }

    public record Total(int sites, long pages, long lemmas, boolean isIndexing) {
    }

    public record SiteStatistics(String url, String name, String status, long statusTime, String error, int pages,
                                 int lemmas) {
    }
}
