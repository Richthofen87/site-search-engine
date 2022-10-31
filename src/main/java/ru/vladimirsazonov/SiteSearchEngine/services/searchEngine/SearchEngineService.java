package ru.vladimirsazonov.SiteSearchEngine.services.searchEngine;

import ru.vladimirsazonov.SiteSearchEngine.dto.SearchEngineResponse;

public interface SearchEngineService {

    SearchEngineResponse startIndexing();
    SearchEngineResponse stopIndexing();
    SearchEngineResponse indexPage(String url);
    SearchEngineResponse getStatistics();
    SearchEngineResponse startSearch(String query, String site, int offset, int limit);
}

