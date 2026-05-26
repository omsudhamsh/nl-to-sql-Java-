package com.nlsql.service;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.stereotype.Component;

@Component
public class SchemaStore {
  public record Schema(String table, List<String> columns) {}

  private final AtomicReference<Schema> schemaRef = new AtomicReference<>();

  public void set(Schema schema) {
    schemaRef.set(schema);
  }

  public Schema get() {
    return schemaRef.get();
  }
}