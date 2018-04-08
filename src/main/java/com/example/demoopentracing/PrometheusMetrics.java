package com.example.demoopentracing;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.micrometer.prometheus.PrometheusMeterRegistry;

/**
 * @author Yuri Shkuro
 */
@RestController
public class PrometheusMetrics {

    @Autowired
    private PrometheusMeterRegistry micrometerRegistry;

    @RequestMapping(value = "/metrics", produces = "application/json; charset=UTF-8")
    public String metrics() {
        return micrometerRegistry.scrape();
    }
}