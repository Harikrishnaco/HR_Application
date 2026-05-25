package com.example.demo.attendance;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.ArrayList;
import org.json.JSONObject;
import org.json.JSONArray;

@Service
public class ActiveWorkerCacheService {

    @Value("${UPSTASH_REDIS_REST_TOKEN}")
    private String restToken;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final String baseUrl = "https://hot-trout-136037.upstash.io";

    public void cacheActiveWorker(Long workerId, String workerName, String siteName, String clockInTime) {
        try {
            String trackingPayload = String.format("Worker ID: %d | Name: %s | Site: %s | Arrived: %s",
                    workerId, workerName, siteName, clockInTime);

            // Using Upstash REST API convention: POST /HSET/key/field value
            // We use standard JSON payload body passing to bypass raw URL character limits
            JSONArray command = new JSONArray();
            command.put("HSET");
            command.put("ACTIVE_WORKERS_TRACKER");
            command.put(String.valueOf(workerId));
            command.put(trackingPayload);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl))
                    .header("Authorization", "Bearer " + restToken)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(command.toString()))
                    .build();

            httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            System.err.println("REST Cache write failed: " + e.getMessage());
        }
    }

    public void removeActiveWorker(Long workerId) {
        try {
            JSONArray command = new JSONArray();
            command.put("HDEL");
            command.put("ACTIVE_WORKERS_TRACKER");
            command.put(String.valueOf(workerId));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl))
                    .header("Authorization", "Bearer " + restToken)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(command.toString()))
                    .build();

            httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            System.err.println("REST Cache eviction failed: " + e.getMessage());
        }
    }

    public List<Object> getAllActiveWorkers() {
        List<Object> activeWorkers = new ArrayList<>();
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/hvals/ACTIVE_WORKERS_TRACKER"))
                    .header("Authorization", "Bearer " + restToken)
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            String responseBody = response.body().trim();

            System.out.println(">>>> RAW UPSTASH RESPONSE: " + responseBody + " <<<<");

            if (responseBody.startsWith("{")) {
                // If Upstash returns a wrapped JSON object: {"result":["...", "..."]}
                org.json.JSONObject jsonResponse = new org.json.JSONObject(responseBody);
                if (jsonResponse.has("result") && !jsonResponse.isNull("result")) {
                    org.json.JSONArray results = jsonResponse.getJSONArray("result");
                    for (int i = 0; i < results.length(); i++) {
                        activeWorkers.add(results.getString(i));
                    }
                }
            } else if (responseBody.startsWith("[")) {
                // If Upstash returns a raw JSON array string directly: ["...", "..."]
                org.json.JSONArray results = new org.json.JSONArray(responseBody);
                for (int i = 0; i < results.length(); i++) {
                    activeWorkers.add(results.getString(i));
                }
            }
        } catch (Exception e) {
            System.err.println("REST Cache read parsing failed: " + e.getMessage());
        }
        return activeWorkers;
    }
}