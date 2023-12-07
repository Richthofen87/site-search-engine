package ru.vladimirsazonov.SiteSearchEngine.services.indexing;

import ru.vladimirsazonov.SiteSearchEngine.dto.SearchEngineResponse;

public interface IndexingService {
    boolean isIndexingRunning();

    SearchEngineResponse startIndexing();

    SearchEngineResponse stopIndexing();

    SearchEngineResponse indexPage(String url);
}
