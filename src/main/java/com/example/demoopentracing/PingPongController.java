package com.example.demoopentracing;

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

    @RequestMapping("/ping")
    public String ping() {
        ResponseEntity<String> response = restTemplate.getForEntity("http://localhost:" + port + "/pong", String.class);
        return "ping + " + response.getBody();
    }

    @RequestMapping(value = "/pong")
    public String pong() {
        return "pong!";
    }
}
