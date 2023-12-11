package ru.vladimirsazonov.SiteSearchEngine.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import ru.vladimirsazonov.SiteSearchEngine.config.SitesList;
import ru.vladimirsazonov.SiteSearchEngine.dto.SearchEngineResponse;
import ru.vladimirsazonov.SiteSearchEngine.exceptions.RunApplicationException;
import ru.vladimirsazonov.SiteSearchEngine.exceptions.ServerStateException;
import ru.vladimirsazonov.SiteSearchEngine.model.Page;
import ru.vladimirsazonov.SiteSearchEngine.model.Site;
import ru.vladimirsazonov.SiteSearchEngine.services.indexing.impl.DAO;
import ru.vladimirsazonov.SiteSearchEngine.services.indexing.impl.IndexingServiceImpl;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IndexingServiceImplTest {
    @Mock
    private SitesList sitesList;
    @Mock
    private DAO dao;
    @InjectMocks
    private IndexingServiceImpl indexingServiceImpl;
    List<Site> sites;
    private final int PARALLELISM_LEVEL = Runtime.getRuntime().availableProcessors();

    @BeforeEach
    void setUp() {
        Site site1 = new Site("Playback", "http://www.playback.ru");
        Site site2 = new Site("Dimonvideo", "http://dimonvideo.ru");
        LocalDateTime statusTime = LocalDateTime.now();
        site1.setStatusTime(statusTime);
        site2.setStatusTime(statusTime);
        sites = Arrays.asList(site1, site2);
    }

    @Test
    void startIndexingTest_whenSiteListIsEmpty_throwsException() {
        doReturn(List.of()).when(sitesList).getSites();
        assertThrows(RunApplicationException.class,
                () -> indexingServiceImpl.startIndexing(), "В конфигурационом файле не указаны сайты");
    }

    @Test
    void startIndexingTest_whenIndexingRunningFlagIsTrue_throwsException() {
        doReturn(sites).when(sitesList).getSites();
        ReflectionTestUtils.setField(indexingServiceImpl, "indexingRunningFlag", true);
        assertThrows(ServerStateException.class,
                () -> indexingServiceImpl.startIndexing(), "Индексация уже запущена");
    }

    @Test
    void startIndexingTest_whenIsSinglePageModeIsTrue_returnValidSearchEngineResponse() {
        doReturn(sites).when(sitesList).getSites();
        Site site = sites.get(0);
        ReflectionTestUtils.setField(indexingServiceImpl, "isSinglePageMode", true);
        ReflectionTestUtils.setField(indexingServiceImpl, "urlForSinglePage", "http://www.playback.ru/dostavka.html");
        doReturn(new Page()).when(dao).findPageBySiteIdAndPath(site, "/dostavka.html");
        lenient().doReturn(site).when(dao).updateSiteData(site.getUrl(), site.getName(), true);
        lenient().doReturn(site).when(dao).findSiteById(0);
        assertEquals(new SearchEngineResponse(), indexingServiceImpl.startIndexing());
    }

    @Test
    void startIndexingTest_whenIsSinglePageModeIsTrueAndPageIsNotInIndex_throwsException() {
        doReturn(sites).when(sitesList).getSites();
        ReflectionTestUtils.setField(indexingServiceImpl, "isSinglePageMode", true);
        ReflectionTestUtils.setField(indexingServiceImpl, "urlForSinglePage", "http://www.playback.ru/dostavka.html");
        doReturn(null).when(dao).findPageBySiteIdAndPath(sites.get(0), "/dostavka.html");
        assertThrows(RunApplicationException.class,
                () -> indexingServiceImpl.startIndexing(), "Указанная страница не найдена");
    }

    @Test
    void startIndexingTest_whenIsSinglePageModeIsTrueAndSiteOptionalIsEmpty_throwsException() {
        ReflectionTestUtils.setField(indexingServiceImpl, "isSinglePageMode", true);
        ReflectionTestUtils.setField(indexingServiceImpl, "urlForSinglePage", "https://go.skillbox.ru/education");
        assertThrows(RunApplicationException.class,
                () -> indexingServiceImpl.startIndexing(), "Данная страница находится за пределами сайтов, " +
                        "указанных в конфигурационном файле");
    }

    @Test
    void startIndexingTest_whenIsSinglePageModeIsFalse_returnValidSearchEngineResponse() {
        doReturn(sites).when(sitesList).getSites();
        sites.forEach(s -> lenient().doReturn(s).when(dao).updateSiteData(s.getUrl(), s.getName(), false));
        assertEquals(new SearchEngineResponse(), indexingServiceImpl.startIndexing());
    }

    @Test
    void stopIndexingTest_whenIndexingRunningFlagIsFalse_throwsException() {
        assertThrows(ServerStateException.class,
                () -> indexingServiceImpl.stopIndexing(), "Индексация ещё не запущена");
    }

    @Test
    void stopIndexingTest_whenIndexingRunningFlagIsTrue_ReturnValidSearchEngineResponse() {
        ReflectionTestUtils.setField(indexingServiceImpl, "scheduledThreadPoolExecutor",
                Executors.newScheduledThreadPool(PARALLELISM_LEVEL));
        ReflectionTestUtils.setField(indexingServiceImpl, "threadPoolExecutor",
                Executors.newFixedThreadPool(PARALLELISM_LEVEL));
        ReflectionTestUtils.setField(indexingServiceImpl, "forkJoinPool", new ForkJoinPool());
        ReflectionTestUtils.setField(indexingServiceImpl, "indexingRunningFlag", true);
        doNothing().when(dao).setAllSitesStatusFailed();
        assertEquals(new SearchEngineResponse(), indexingServiceImpl.stopIndexing());
    }

    @Test
    void indexPageTest_whenPageUrlIsNotBlank_thenReturnValidSearchEngineResponse() {
        Site site = sites.get(0);
        doReturn(sites).when(sitesList).getSites();
        doReturn(new Page()).when(dao).findPageBySiteIdAndPath(any(Site.class), anyString());
        lenient().doReturn(site).when(dao).updateSiteData(site.getUrl(), site.getName(), true);
        assertEquals(new SearchEngineResponse(), indexingServiceImpl.indexPage(site.getUrl()));
    }

    @Test
    void indexPageTest_whenPageUrlIsBlank_thenThrowsException() {
        assertThrows(RunApplicationException.class,
                () -> indexingServiceImpl.indexPage("   "), "В запросе передана пустая страница");
    }
}