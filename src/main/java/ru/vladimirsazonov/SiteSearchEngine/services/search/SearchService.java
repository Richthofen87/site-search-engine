package ru.vladimirsazonov.SiteSearchEngine.services.search;

import ru.vladimirsazonov.SiteSearchEngine.dto.SearchEngineResponse;

public interface SearchService {
    SearchEngineResponse startSearch(String query, String site, int offset, int limit);

}
