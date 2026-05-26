package com.nlsql.controller;

import com.nlsql.model.QuestionRequest;
import com.nlsql.service.CsvUploadService;
import com.nlsql.service.GroqClient;
import com.nlsql.service.SchemaStore;
import com.nlsql.service.SqlQueryService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class NlSqlController {
  private final SchemaStore schemaStore;
  private final CsvUploadService csvUploadService;
  private final SqlQueryService sqlQueryService;
  private final GroqClient groqClient;

  public NlSqlController(
      SchemaStore schemaStore,
      CsvUploadService csvUploadService,
      SqlQueryService sqlQueryService,
      GroqClient groqClient) {
    this.schemaStore = schemaStore;
    this.csvUploadService = csvUploadService;
    this.sqlQueryService = sqlQueryService;
    this.groqClient = groqClient;
  }

  @GetMapping("/")
  public Map<String, String> home() {
    return Map.of("message", "NL -> SQL backend running");
  }

  @PostMapping("/upload")
  public ResponseEntity<Map<String, Object>> upload(@RequestParam("file") MultipartFile file) {
    try {
      SchemaStore.Schema schema = csvUploadService.storeCsv(file);
      schemaStore.set(schema);

      Map<String, Object> response = new HashMap<>();
      response.put("message", "File uploaded successfully");
      response.put("schema", Map.of("table", schema.table(), "columns", schema.columns()));
      return ResponseEntity.ok(response);
    } catch (Exception ex) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(Map.of("error", "Upload failed. " + ex.getMessage()));
    }
  }

  @PostMapping("/query")
  public ResponseEntity<Map<String, Object>> query(@RequestBody QuestionRequest request) {
    SchemaStore.Schema schema = schemaStore.get();
    if (schema == null) {
      return ResponseEntity.badRequest()
          .body(Map.of("error", "Please upload a CSV file first."));
    }

    try {
      String question = request.question() == null ? "" : request.question().trim();
      String sql = question.toLowerCase().startsWith("select")
          ? question
          : groqClient.generateSql(question, schema);
      List<Map<String, Object>> result = sqlQueryService.runQuery(sql);

      Map<String, Object> response = new HashMap<>();
      response.put("generated_sql", sql);
      response.put("result", result);
      return ResponseEntity.ok(response);
    } catch (IllegalArgumentException ex) {
      return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
    } catch (Exception ex) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(Map.of("error", "Query failed. " + ex.getMessage()));
    }
  }
}