package com.nlsql.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class GroqClient {
  private static final URI GROQ_URI = URI.create("https://api.groq.com/openai/v1/chat/completions");
  private static final String MODEL = "llama-3.1-8b-instant";

  private final ObjectMapper objectMapper;
  private final HttpClient httpClient = HttpClient.newHttpClient();

  public GroqClient(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  public String generateSql(String question, SchemaStore.Schema schema)
      throws IOException, InterruptedException {
    String apiKey = System.getenv("GROQ_API_KEY");
    if (apiKey == null || apiKey.isBlank()) {
      throw new IllegalStateException("GROQ_API_KEY is not set");
    }

    String prompt = buildPrompt(question, schema);
    Map<String, Object> payload = Map.of(
        "model", MODEL,
        "messages", List.of(Map.of("role", "user", "content", prompt))
    );

    String body = objectMapper.writeValueAsString(payload);
    HttpRequest request = HttpRequest.newBuilder(GROQ_URI)
        .header("Authorization", "Bearer " + apiKey)
        .header("Content-Type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(body))
        .build();

    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    if (response.statusCode() >= 400) {
      throw new IOException("Groq API error: " + response.body());
    }

    JsonNode root = objectMapper.readTree(response.body());
    JsonNode content = root.path("choices").path(0).path("message").path("content");
    if (content.isMissingNode()) {
      throw new IOException("Groq API response missing content");
    }

    return content.asText().trim();
  }

  private String buildPrompt(String question, SchemaStore.Schema schema) {
    return "Convert natural language to SQL.\n\n"
        + "Database schema:\n"
        + "Table name: " + schema.table() + "\n"
        + "Columns: " + String.join(", ", schema.columns()) + "\n\n"
        + "Rules:\n"
        + "- Only generate SELECT queries.\n"
        + "- No explanation.\n"
        + "- Output only SQL.\n"
        + "- Use exact column names provided.\n\n"
        + "- For string filters, use case-insensitive matching like LOWER(column) = 'value'.\n\n"
        + "Question:\n"
        + question;
  }
}