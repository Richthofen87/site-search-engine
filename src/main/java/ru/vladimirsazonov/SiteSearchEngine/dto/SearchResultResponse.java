package ru.vladimirsazonov.SiteSearchEngine.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SearchResultResponse extends SearchEngineResponse {
    private final int count;
    private SearchResult[] data;

    public record SearchResult(String site, String siteName, String uri,
                               String title, String snippet, float relevance) implements Comparable<SearchResult> {

        @Override
        public int compareTo(SearchResult o) {
            return Float.compare(this.relevance, o.relevance);
        }
    }
}
