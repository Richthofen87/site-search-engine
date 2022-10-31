package ru.vladimirsazonov.SiteSearchEngine.services.searchEngine.impl;

import lombok.*;

import java.io.IOException;
import java.util.*;

@Data
public class LinkHandleTaskResult {
    private final String path;
    private final int statusCode;
    private final int siteId;
    private final String content;
    private IOException exception;
    private Map<String, Float> lemmasAndRanksMap;
}
