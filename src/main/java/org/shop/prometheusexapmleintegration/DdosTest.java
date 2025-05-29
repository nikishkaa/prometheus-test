package org.shop.prometheusexapmleintegration;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class DdosTest {
    private static final String TARGET_URL = "http://34.132.194.33:8080";
    private static final int NUM_THREADS = 50;
    private static final int DURATION_SECONDS = 60;
    private static final int REQUESTS_PER_THREAD = 1000;
    private static final Random random = new Random();

    public static void main(String[] args) {
        System.out.println("Начинаем тест нагрузки на " + TARGET_URL);
        System.out.println("Количество потоков: " + NUM_THREADS);
        System.out.println("Длительность теста: " + DURATION_SECONDS + " секунд");
        System.out.println("Запросов на поток: " + REQUESTS_PER_THREAD);

        ExecutorService executor = Executors.newFixedThreadPool(NUM_THREADS);
        List<Future<RequestStats>> futures = new ArrayList<>();
        long startTime = System.currentTimeMillis();

        // Запускаем потоки
        for (int i = 0; i < NUM_THREADS; i++) {
            futures.add(executor.submit(new RequestWorker()));
        }

        // Собираем результаты
        int totalRequests = 0;
        int successfulRequests = 0;
        for (Future<RequestStats> future : futures) {
            try {
                RequestStats stats = future.get();
                totalRequests += stats.getTotalRequests();
                successfulRequests += stats.getSuccessfulRequests();
            } catch (InterruptedException | ExecutionException e) {
                System.err.println("Ошибка при получении результатов: " + e.getMessage());
            }
        }

        long duration = (System.currentTimeMillis() - startTime) / 1000;
        System.out.println("\nРезультаты теста:");
        System.out.println("Общее время: " + duration + " секунд");
        System.out.println("Всего запросов: " + totalRequests);
        System.out.println("Успешных запросов: " + successfulRequests);
        System.out.println("Запросов в секунду: " + (totalRequests / duration));
        System.out.println("Процент успешных запросов: " + 
            (totalRequests > 0 ? (successfulRequests * 100.0 / totalRequests) : 0) + "%");

        executor.shutdown();
    }

    static class RequestWorker implements Callable<RequestStats> {
        private final HttpClient client;
        private final AtomicInteger totalRequests = new AtomicInteger(0);
        private final AtomicInteger successfulRequests = new AtomicInteger(0);

        public RequestWorker() {
            this.client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        }

        @Override
        public RequestStats call() {
            long endTime = System.currentTimeMillis() + (DURATION_SECONDS * 1000);

            while (System.currentTimeMillis() < endTime && 
                   totalRequests.get() < REQUESTS_PER_THREAD) {
                try {
                    // Добавляем случайные параметры для разнообразия запросов
                    String url = TARGET_URL + "?param1=" + random.nextInt(1000) + 
                                "&param2=" + random.nextInt(1000);
                    
                    HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .timeout(Duration.ofSeconds(5))
                        .GET()
                        .build();

                    HttpResponse<String> response = client.send(request, 
                        HttpResponse.BodyHandlers.ofString());

                    totalRequests.incrementAndGet();
                    if (response.statusCode() == 200) {
                        successfulRequests.incrementAndGet();
                    }

                    // Небольшая задержка между запросами
                    Thread.sleep(100);
                } catch (IOException | InterruptedException e) {
                    System.err.println("Ошибка запроса: " + e.getMessage());
                }
            }

            return new RequestStats(totalRequests.get(), successfulRequests.get());
        }
    }

    static class RequestStats {
        private final int totalRequests;
        private final int successfulRequests;

        public RequestStats(int totalRequests, int successfulRequests) {
            this.totalRequests = totalRequests;
            this.successfulRequests = successfulRequests;
        }

        public int getTotalRequests() {
            return totalRequests;
        }

        public int getSuccessfulRequests() {
            return successfulRequests;
        }
    }
} 