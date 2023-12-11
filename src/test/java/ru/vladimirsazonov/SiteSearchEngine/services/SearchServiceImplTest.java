package ru.vladimirsazonov.SiteSearchEngine.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.vladimirsazonov.SiteSearchEngine.dto.SearchEngineResponse;
import ru.vladimirsazonov.SiteSearchEngine.dto.SearchResultResponse;
import ru.vladimirsazonov.SiteSearchEngine.exceptions.RunApplicationException;
import ru.vladimirsazonov.SiteSearchEngine.model.Site;
import ru.vladimirsazonov.SiteSearchEngine.model.Status;
import ru.vladimirsazonov.SiteSearchEngine.services.indexing.impl.DAO;
import ru.vladimirsazonov.SiteSearchEngine.services.morphology.MorphologyService;
import ru.vladimirsazonov.SiteSearchEngine.services.search.SearchServiceImpl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SearchServiceImplTest {
    @Mock
    private MorphologyService morphologyService;
    @Mock
    private DAO dao;
    @InjectMocks
    private SearchServiceImpl searchServiceImpl;
    private Site site;

    @BeforeEach
    void setUp() {
        site = new Site("Playback", "http://www.playback.ru");
        site.setStatusTime(LocalDateTime.now());
    }

    @ParameterizedTest
    @NullAndEmptySource
    void startSearchTest_whenQueryIsNullOrIsBlank_thenThrowsException(String query) {
        Exception exception = assertThrows(RunApplicationException.class,
                () -> searchServiceImpl.startSearch(query, null, 0, 0));
        assertEquals("Задан пустой поисковый запрос", exception.getMessage());
    }

    @Test
    void startSearchTest_whenSiteUrlIsNullAndSitesIsNotIndexedYet_thenThrowsException() {
        Site site = new Site("Dimonvideo", "http://dimonvideo.ru");
        LocalDateTime statusTime = LocalDateTime.now();
        site.setStatusTime(statusTime);
        doReturn(List.of(site, this.site)).when(dao).findAllSites();
        Exception exception = assertThrows(RunApplicationException.class,
                () -> searchServiceImpl.startSearch("запрос", null, 0, 20));
        assertEquals("Сайты ещё не проиндексированы", exception.getMessage());
    }

    @Test
    void startSearchTest_whenSiteUrlIsNotNullAndPagesListIsEmpty_thenReturnValidSearchResultResponse() {
        doReturn(site).when(dao).findSiteByUrl(site.getUrl());
        site.setStatus(Status.INDEXED);
        doReturn(Set.of()).when(morphologyService).getLemmasSet(anyString());
        assertEquals(new SearchResultResponse(0, new SearchResultResponse.SearchResult[0]),
                searchServiceImpl.startSearch("запрос", site.getUrl(), 0, 20));
    }

    @Test
    void startSearchTest_whenSiteUrlIsNotNullAndSiteIsNull_thenThrowsException() {
        Exception exception = assertThrows(RunApplicationException.class,
                () -> searchServiceImpl.startSearch("запрос", site.getUrl(), 0, 20));
        assertEquals("Данный сайт не указан в конфигурационном файле", exception.getMessage());
    }

    @Test
    void startSearchTest_whenSiteUrlIsNotNullAndSiteIsNotIndexedYet_thenThrowsException() {
        doReturn(site).when(dao).findSiteByUrl(site.getUrl());
        Exception exception = assertThrows(RunApplicationException.class,
                () -> searchServiceImpl.startSearch("запрос", site.getUrl(), 0, 20));
        assertEquals("Сайт ещё не проиндексирован", exception.getMessage());
    }

    @Test
    void startSearchTest_whenSiteUrlIsNotNull_thenReturnValidSearchResultResponse() {
        doReturn(site).when(dao).findSiteByUrl(site.getUrl());
        site.setStatus(Status.INDEXED);
        assertEquals(new SearchEngineResponse(), searchServiceImpl.startSearch("запрос", site.getUrl(), 0, 20));
    }
}