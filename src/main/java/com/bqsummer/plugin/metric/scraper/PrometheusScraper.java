package com.bqsummer.plugin.metric.scraper;

import com.bqsummer.framework.http.HttpClientTemplate;

import java.net.URL;

public class PrometheusScraper {

    private HttpClientTemplate httpClientTemplate;

    private PrometheusScraper() {
    }

    public PrometheusScraper(HttpClientTemplate httpClientTemplate) {
        this.httpClientTemplate = httpClientTemplate;
    }

    public String scrape(URL url) {
        return httpClientTemplate.doGet(url.toString());
    }
}
