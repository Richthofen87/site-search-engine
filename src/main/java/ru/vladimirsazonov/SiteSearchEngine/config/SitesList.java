package ru.vladimirsazonov.SiteSearchEngine.config;

import lombok.*;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import ru.vladimirsazonov.SiteSearchEngine.model.Site;

import java.util.ArrayList;
import java.util.List;

@Component
@ConfigurationProperties(prefix = "indexing-settings")
@Data
public class SitesList {
    private List<Site> sites;
}
