package ru.vladimirsazonov.SiteSearchEngine.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.vladimirsazonov.SiteSearchEngine.dto.SearchEngineResponse;
import ru.vladimirsazonov.SiteSearchEngine.services.indexing.IndexingService;
import ru.vladimirsazonov.SiteSearchEngine.services.search.SearchService;
import ru.vladimirsazonov.SiteSearchEngine.services.statistics.StatisticsService;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class SearchEngineController {

    private final IndexingService indexingService;
    private final StatisticsService statisticsService;
    final SearchService searchService;

    @GetMapping("/startIndexing")
    public ResponseEntity<SearchEngineResponse> startIndexing() {
        return ResponseEntity.ok(indexingService.startIndexing());
    }

    @GetMapping("/stopIndexing")
    public ResponseEntity<SearchEngineResponse> stopIndexing() {
        return ResponseEntity.ok(indexingService.stopIndexing());
    }

    @PostMapping("/indexPage")
    public ResponseEntity<SearchEngineResponse> indexPage(@RequestParam String url) {
        return ResponseEntity.ok(indexingService.indexPage(url));
    }

    @GetMapping("/statistics")
    public ResponseEntity<SearchEngineResponse> getStatistics() {
        return ResponseEntity.ok(statisticsService.getStatistics());
    }

    @GetMapping("/search")
    public ResponseEntity<SearchEngineResponse> getSearchResult(@RequestParam(required = false) String query,
                                                @RequestParam(required = false) String site,
                                                @RequestParam(defaultValue = "0") int offset,
                                                @RequestParam(defaultValue = "20") int limit) {
        return ResponseEntity.ok(searchService.startSearch(query, site, offset, limit));
    }
}
