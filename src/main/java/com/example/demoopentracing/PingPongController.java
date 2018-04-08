package com.example.demoopentracing;

import java.rmi.UnexpectedException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

/**
 * @author Yuri Shkuro
 */
@RestController
public class PingPongController {

    @Value("${server.port}")
    private String port;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private io.opentracing.Tracer tracer;

    @RequestMapping("/ping")
    public String ping() {
        appendCallPath("jaeger-demo::ping");

        ResponseEntity<String> response = restTemplate.getForEntity("http://localhost:" + port + "/pong", String.class);
        return "ping + " + response.getBody();
    }

    @RequestMapping(value = "/pong")
    public String pong() {
        appendCallPath("jaeger-demo::pong");
        maybeFail();

        return "pong!";
    }

    private void appendCallPath(String path) {
        io.opentracing.Span span = tracer.activeSpan();
        String currentPath = span.getBaggageItem("callpath");
        if (currentPath == null) {
            currentPath = path;
        } else {
            currentPath += "->" + path;
        }
        span.setBaggageItem("callpath", currentPath);
    }

    private void maybeFail() {
        io.opentracing.Span span = tracer.activeSpan();
        if (span.getBaggageItem("fail") != null) {
            throw new RuntimeException("simulated failure");
        }        
    }
}
