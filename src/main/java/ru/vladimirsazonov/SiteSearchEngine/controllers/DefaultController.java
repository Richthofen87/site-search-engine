package ru.vladimirsazonov.SiteSearchEngine.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DefaultController {

    @GetMapping
    public String index() {
        return "index";
    }
}