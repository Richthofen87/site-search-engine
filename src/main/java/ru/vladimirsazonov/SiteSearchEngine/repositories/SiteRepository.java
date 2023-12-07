package ru.vladimirsazonov.SiteSearchEngine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import ru.vladimirsazonov.SiteSearchEngine.model.Site;
import ru.vladimirsazonov.SiteSearchEngine.model.Status;

import java.util.Optional;

@Repository
public interface SiteRepository extends JpaRepository<Site, Integer> {

    Optional<Site> findByName(String name);

    Optional<Site> findByUrl(String url);

    @Query("UPDATE Site SET statusTime = NOW() WHERE id = ?1")
    @Modifying
    void setStatusTimeBySiteId(int id);

    @Query("UPDATE Site SET status = ?1, statusTime = NOW() WHERE id = ?2")
    @Modifying
    void setStatusBySiteId(Status status, int id);

    @Query("UPDATE Site SET lastError = ?1 WHERE id = ?2")
    @Modifying
    void setErrorMessageBySiteId(String errorMessage, int id);

    @Query("UPDATE Site SET status = 'FAILED', statusTime = NOW(), lastError = 'Индексация отменена' WHERE status = 'INDEXING'")
    @Modifying
    void setAllSitesStatusFailed();
}
