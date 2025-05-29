package org.shop.prometheusexapmleintegration;

import io.prometheus.metrics.exporter.httpserver.HTTPServer;
import io.prometheus.metrics.instrumentation.jvm.JvmMetrics;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.IOException;

@SpringBootApplication
public class PrometheusExapmleIntegrationApplication {

    public static void main(String[] args) throws InterruptedException, IOException {
        JvmMetrics.builder().register();

        HTTPServer server = HTTPServer.builder()
                .port(9400)
                .buildAndStart();



        System.out.println("HTTPServer listening on http://localhost:" + server.getPort() + "/metrics");

        Thread.currentThread().join();
    }

}
