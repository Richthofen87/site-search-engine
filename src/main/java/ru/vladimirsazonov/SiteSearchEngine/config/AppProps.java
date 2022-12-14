package ru.vladimirsazonov.SiteSearchEngine.config;

import lombok.*;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@ConfigurationProperties
@Data
public class AppProps {
    private List<SiteData> sites = new ArrayList<>();
    private String userAgentName;
    private int resultPageMaxCount;

    @Component
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SiteData {
        private String name;
        private String url;
    }
}
