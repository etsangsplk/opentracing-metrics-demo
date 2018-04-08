package com.example.demoopentracing;

import com.uber.jaeger.Configuration;
import com.uber.jaeger.Tracer;
import com.uber.jaeger.metrics.Metrics;
import com.uber.jaeger.micrometer.MicrometerMetricsFactory;
import com.uber.jaeger.reporters.RemoteReporter;
import com.uber.jaeger.samplers.ConstSampler;
import com.uber.jaeger.senders.HttpSender;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

import io.micrometer.core.instrument.Clock;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import io.opentracing.contrib.metrics.prometheus.PrometheusMetricsReporter;
import io.prometheus.client.CollectorRegistry;

/**
 * @author Yuri Shkuro
 */
@SpringBootApplication
public class DemoOpentracingApplication {

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder restTemplateBuilder) {
        return restTemplateBuilder.build();
    }

    @Bean
    public CollectorRegistry prometheus() {
        return new CollectorRegistry();
    }

    @Bean
    public PrometheusMeterRegistry micrometerRegistry(CollectorRegistry collector) {
        PrometheusMeterRegistry registry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT, collector,
                Clock.SYSTEM);
        io.micrometer.core.instrument.Metrics.addRegistry(registry);
        return registry;
    }

    @Bean
    public io.opentracing.Tracer jaegerTracer(CollectorRegistry collector) {
        MicrometerMetricsFactory metricsReporter = new MicrometerMetricsFactory();
        Configuration configuration = new Configuration("jaeger-demo");
        Tracer jaegerTracer = configuration.getTracerBuilder() //
                .withMetricsFactory(metricsReporter)//
                .withSampler(new ConstSampler(true))//
                .withReporter(new RemoteReporter.Builder() //
                        .withMetrics(new Metrics(metricsReporter)) //
                        .withSender(new HttpSender("http://localhost:14268/api/traces")) //
                        .build()) //
                .build();

        PrometheusMetricsReporter reporter = PrometheusMetricsReporter //
                .newMetricsReporter() //
                .withCollectorRegistry(collector) //
                .withConstLabel("service", "jaeger-demo") //
                .withBaggageLabel("callpath", "") //
                .build();
        return io.opentracing.contrib.metrics.Metrics.decorate(jaegerTracer, reporter);
    }

    public static void main(String[] args) {
        SpringApplication.run(DemoOpentracingApplication.class, args);
    }
}
