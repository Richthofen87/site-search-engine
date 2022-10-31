package ru.vladimirsazonov.SiteSearchEngine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import ru.vladimirsazonov.SiteSearchEngine.model.Site;

import java.util.Optional;

@Repository
public interface SiteRepository extends JpaRepository<Site, Integer> {

    Optional<Site> findByName(String name);

    Optional<Site> findByUrl(String url);

    @Query("UPDATE Site SET statusTime = NOW() WHERE id = ?1")
    @Modifying
    void setSiteStatusTimeBySiteId(int id);

    @Query("UPDATE Site SET statusTime = NOW(), status = 'INDEXED' WHERE id = ?1")
    @Modifying
    void setSiteStatusIndexed(int id);

    @Query("UPDATE Site SET status = 'FAILED', statusTime = NOW(), lastError = ?1 WHERE id = ?2")
    @Modifying
    void setSiteStatusFailed(String errorMessage, int id);

    @Query("UPDATE Site SET status = 'FAILED', statusTime = NOW(), lastError = 'Индексация отменена' WHERE status = 'INDEXING'")
    @Modifying
    void setAllSitesStatusFailed();
}
