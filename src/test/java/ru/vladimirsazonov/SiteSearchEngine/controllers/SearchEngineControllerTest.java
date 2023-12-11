package ru.vladimirsazonov.SiteSearchEngine.controllers;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import ru.vladimirsazonov.SiteSearchEngine.dto.SearchEngineResponse;
import ru.vladimirsazonov.SiteSearchEngine.services.indexing.IndexingService;
import ru.vladimirsazonov.SiteSearchEngine.services.search.SearchService;
import ru.vladimirsazonov.SiteSearchEngine.services.statistics.StatisticsService;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SearchEngineControllerTest {
    @Mock
    private IndexingService indexingService;
    @Mock
    private StatisticsService statisticsService;
    @Mock
    private SearchService searchService;
    @InjectMocks
    private SearchEngineController searchEngineController;

    @Test
    void startIndexingTest() {
        when(indexingService.startIndexing()).thenReturn(new SearchEngineResponse());
        ResponseEntity<SearchEngineResponse> response = searchEngineController.startIndexing();
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(MediaType.APPLICATION_JSON, response.getHeaders().getContentType());
        assertEquals(new SearchEngineResponse(), response.getBody());
    }

    @Test
    void stopIndexingTest() {
        when(indexingService.stopIndexing()).thenReturn(new SearchEngineResponse());
        ResponseEntity<SearchEngineResponse> response = searchEngineController.stopIndexing();
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(MediaType.APPLICATION_JSON, response.getHeaders().getContentType());
        assertEquals(new SearchEngineResponse(), response.getBody());
    }

    @Test
    void indexPageTest_whenParamIsPresent_returnValidEntityResponse() {
        when(indexingService.indexPage("pageUrl")).thenReturn(new SearchEngineResponse());
        ResponseEntity<SearchEngineResponse> response = searchEngineController.indexPage("pageUrl");
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(MediaType.APPLICATION_JSON, response.getHeaders().getContentType());
        assertEquals(new SearchEngineResponse(), response.getBody());
    }

    @Test
    void getStatisticsTest() {
        when(statisticsService.getStatistics()).thenReturn(new SearchEngineResponse());
        ResponseEntity<SearchEngineResponse> response = searchEngineController.getStatistics();
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(MediaType.APPLICATION_JSON, response.getHeaders().getContentType());
        assertEquals(new SearchEngineResponse(), response.getBody());
    }

    @Test
    void getSearchResultTest() {
        when(searchService.startSearch("query", "site", 0, 20)).thenReturn(new SearchEngineResponse());
        ResponseEntity<SearchEngineResponse> response = searchEngineController.getSearchResult("query", "site", 0, 20);
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(MediaType.APPLICATION_JSON, response.getHeaders().getContentType());
        assertEquals(new SearchEngineResponse(), response.getBody());
    }
}