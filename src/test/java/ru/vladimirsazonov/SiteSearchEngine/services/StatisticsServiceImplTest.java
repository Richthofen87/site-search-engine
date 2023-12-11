package ru.vladimirsazonov.SiteSearchEngine.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.vladimirsazonov.SiteSearchEngine.config.SitesList;
import ru.vladimirsazonov.SiteSearchEngine.dto.StatisticsResponse;
import ru.vladimirsazonov.SiteSearchEngine.model.Site;
import ru.vladimirsazonov.SiteSearchEngine.services.indexing.IndexingService;
import ru.vladimirsazonov.SiteSearchEngine.services.indexing.impl.DAO;
import ru.vladimirsazonov.SiteSearchEngine.services.statistics.StatisticsServiceImpl;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StatisticsServiceImplTest {
    @Mock
    private DAO dao;
    @Mock
    private IndexingService indexingService;
    @Mock
    private SitesList sites;
    @InjectMocks
    private StatisticsServiceImpl statisticsService;
    private List<Site> siteList;
    private StatisticsResponse searchEngineResponse;

    @BeforeEach
    void setUp() {
        Site site1 = new Site("Playback", "http://www.playback.ru");
        Site site2 = new Site("Dimonvideo", "http://dimonvideo.ru");
        LocalDateTime statusTime = LocalDateTime.now();
        site1.setStatusTime(statusTime);
        site2.setStatusTime(statusTime);
        siteList = Arrays.asList(site1, site2);
        searchEngineResponse = new StatisticsResponse(
                new StatisticsResponse.Statistics(new StatisticsResponse.Total(2, 0, 0, false),
                        List.of(new StatisticsResponse.SiteStatistics("http://www.playback.ru", "Playback",
                                null, Timestamp.valueOf(statusTime).getTime(), null, 0, 0))));
    }

    @Test
    void getStatisticsTest_ifSiteListIsNotEmpty_returnValidSearchEngineResponse() {
        doReturn(siteList).when(dao).findAllSites();
        doReturn(0L).when(dao).getPagesTotalCount();
        doReturn(0L).when(dao).getLemmasTotalCount();
        doReturn(false).when(indexingService).isIndexingRunning();
        verifyNoMoreInteractions(dao);
        assertEquals(searchEngineResponse, statisticsService.getStatistics());
    }

    @Test
    void getStatisticsTest_ifSiteListIsEmpty_returnValidSearchEngineResponse() {
        doReturn(List.of()).when(dao).findAllSites();
        doReturn(siteList).when(sites).getSites();
        doReturn(siteList).when(dao).saveSites(siteList);
        doReturn(0L).when(dao).getPagesTotalCount();
        doReturn(0L).when(dao).getLemmasTotalCount();
        doReturn(false).when(indexingService).isIndexingRunning();
        assertEquals(searchEngineResponse, statisticsService.getStatistics());
    }
}