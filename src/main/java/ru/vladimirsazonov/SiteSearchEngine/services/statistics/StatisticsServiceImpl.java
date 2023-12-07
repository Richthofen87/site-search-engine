package ru.vladimirsazonov.SiteSearchEngine.services.statistics;

import lombok.Data;
import org.springframework.stereotype.Service;
import ru.vladimirsazonov.SiteSearchEngine.config.SitesList;
import ru.vladimirsazonov.SiteSearchEngine.dto.SearchEngineResponse;
import ru.vladimirsazonov.SiteSearchEngine.dto.StatisticsResponse;
import ru.vladimirsazonov.SiteSearchEngine.model.Site;
import ru.vladimirsazonov.SiteSearchEngine.services.indexing.IndexingService;
import ru.vladimirsazonov.SiteSearchEngine.services.indexing.impl.DAO;

import java.util.List;

@Service
@Data
public class StatisticsServiceImpl implements StatisticsService {
    private final SitesList sites;
    private final DAO dao;
    private final IndexingService indexingService;

    @Override
    public SearchEngineResponse getStatistics() {
        List<Site> siteList = dao.findAllSites();
        if (siteList.isEmpty()) siteList = dao.saveSites(sites.getSites());
        List<StatisticsResponse.SiteStatistics> detailed = siteList
                .stream()
                .map(site -> new StatisticsResponse.SiteStatistics(site.getUrl(), site.getName(),
                        site.getStatus() == null ? null : site.getStatus().name(),
                        System.currentTimeMillis(), site.getLastError(),
                        site.getPages() == null ? 0 : site.getPages().size(),
                        site.getLemmas() == null ? 0 : site.getLemmas().size()))
                .toList();
        return new StatisticsResponse(new StatisticsResponse.Statistics(
                new StatisticsResponse.Total(siteList.size(), dao.getPagesTotalCount(),
                        dao.getLemmasTotalCount(), indexingService.isIndexingRunning()), detailed));
    }
}
