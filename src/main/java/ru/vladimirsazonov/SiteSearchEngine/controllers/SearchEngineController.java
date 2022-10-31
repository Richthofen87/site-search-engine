package ru.vladimirsazonov.SiteSearchEngine.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.vladimirsazonov.SiteSearchEngine.dto.SearchEngineResponse;
import ru.vladimirsazonov.SiteSearchEngine.services.searchEngine.SearchEngineService;

@RestController
@RequestMapping("/api")
public class SearchEngineController {

    SearchEngineService searchEngineService;

    @Autowired
    public SearchEngineController(SearchEngineService searchEngineService) {
        this.searchEngineService = searchEngineService;
    }

    @GetMapping("/startIndexing")
    @ResponseStatus(HttpStatus.OK)
    public SearchEngineResponse startIndexing() {
        return searchEngineService.startIndexing();
    }

    @GetMapping("/stopIndexing")
    @ResponseStatus(HttpStatus.OK)
    public SearchEngineResponse stopIndexing() {
        return searchEngineService.stopIndexing();
    }

    @PostMapping("/indexPage")
    @ResponseStatus(HttpStatus.OK)
    public SearchEngineResponse indexPage(@RequestParam String url) {
        return searchEngineService.indexPage(url);
    }

    @GetMapping("/statistics")
    @ResponseStatus(HttpStatus.OK)
    public SearchEngineResponse getStatistics() {
        return searchEngineService.getStatistics();
    }

    @GetMapping("/search")
    @ResponseStatus(HttpStatus.OK)
    public SearchEngineResponse getSearchResult(@RequestParam(required = false) String query,
                                                @RequestParam(required = false) String site,
                                                @RequestParam(defaultValue = "0") int offset,
                                                @RequestParam(defaultValue = "20") int limit) {
        return searchEngineService.startSearch(query, site, offset, limit);
    }
}
