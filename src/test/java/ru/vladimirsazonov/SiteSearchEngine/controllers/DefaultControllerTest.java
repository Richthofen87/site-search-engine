package ru.vladimirsazonov.SiteSearchEngine.controllers;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class DefaultControllerTest {

    private final MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new DefaultController()).build();

    @Test
    void indexTest() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.request(HttpMethod.GET, "/"))
                .andExpectAll(status().isOk(), forwardedUrl("index"));
    }
}