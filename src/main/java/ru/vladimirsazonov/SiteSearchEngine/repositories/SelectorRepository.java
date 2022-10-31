package ru.vladimirsazonov.SiteSearchEngine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.vladimirsazonov.SiteSearchEngine.model.Selector;

@Repository
public interface SelectorRepository extends JpaRepository<Selector, Integer> {
}
