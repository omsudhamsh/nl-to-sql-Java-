package com.nlsql.service;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class CsvUploadService {
  private static final String DB_URL = "jdbc:sqlite:uploaded_data.db";
  private static final String TABLE_NAME = "uploaded_table";

  enum ColumnType {
    INTEGER,
    REAL,
    TEXT
  }

  public SchemaStore.Schema storeCsv(MultipartFile file) throws IOException, SQLException {
    List<String> columns = new ArrayList<>();
    List<CSVRecord> records;

    try (Reader reader = new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8);
        CSVParser parser = CSVFormat.DEFAULT.builder()
            .setHeader()
            .setSkipHeaderRecord(true)
            .build()
            .parse(reader)) {
      columns.addAll(parser.getHeaderMap().keySet());
      records = parser.getRecords();
    }

    Map<String, ColumnType> types = inferColumnTypes(columns, records);

    try (Connection conn = DriverManager.getConnection(DB_URL)) {
      recreateTable(conn, columns, types);
      insertRows(conn, columns, types, records);
    }

    return new SchemaStore.Schema(TABLE_NAME, List.copyOf(columns));
  }

  private Map<String, ColumnType> inferColumnTypes(List<String> columns, List<CSVRecord> records) {
    Map<String, ColumnType> types = new LinkedHashMap<>();
    for (String column : columns) {
      types.put(column, ColumnType.INTEGER);
    }

    for (CSVRecord record : records) {
      for (String column : columns) {
        String value = record.get(column);
        ColumnType current = types.get(column);
        ColumnType merged = mergeType(current, value);
        types.put(column, merged);
      }
    }

    return types;
  }

  private ColumnType mergeType(ColumnType current, String value) {
    if (value == null || value.isBlank()) {
      return current;
    }

    if (isLong(value)) {
      return current == ColumnType.TEXT ? ColumnType.TEXT : ColumnType.INTEGER;
    }

    if (isDouble(value)) {
      return current == ColumnType.TEXT ? ColumnType.TEXT : ColumnType.REAL;
    }

    return ColumnType.TEXT;
  }

  private boolean isLong(String value) {
    try {
      Long.parseLong(value.trim());
      return true;
    } catch (NumberFormatException ex) {
      return false;
    }
  }

  private boolean isDouble(String value) {
    try {
      Double.parseDouble(value.trim());
      return true;
    } catch (NumberFormatException ex) {
      return false;
    }
  }

  private void recreateTable(
      Connection conn,
      List<String> columns,
      Map<String, ColumnType> types) throws SQLException {
    try (Statement stmt = conn.createStatement()) {
      stmt.executeUpdate("DROP TABLE IF EXISTS " + quoteIdentifier(TABLE_NAME));
    }

    StringBuilder sql = new StringBuilder();
    sql.append("CREATE TABLE ").append(quoteIdentifier(TABLE_NAME)).append(" (");
    for (int i = 0; i < columns.size(); i++) {
      String column = columns.get(i);
      ColumnType type = types.get(column);
      sql.append(quoteIdentifier(column)).append(" ").append(type.name());
      if (type == ColumnType.TEXT) {
        sql.append(" COLLATE NOCASE");
      }
      if (i < columns.size() - 1) {
        sql.append(", ");
      }
    }
    sql.append(")");

    try (Statement stmt = conn.createStatement()) {
      stmt.executeUpdate(sql.toString());
    }
  }

  private void insertRows(
      Connection conn,
      List<String> columns,
      Map<String, ColumnType> types,
      List<CSVRecord> records) throws SQLException {
    StringBuilder placeholders = new StringBuilder();
    for (int i = 0; i < columns.size(); i++) {
      placeholders.append("?");
      if (i < columns.size() - 1) {
        placeholders.append(", ");
      }
    }

    StringBuilder sql = new StringBuilder();
    sql.append("INSERT INTO ").append(quoteIdentifier(TABLE_NAME)).append(" (");
    for (int i = 0; i < columns.size(); i++) {
      sql.append(quoteIdentifier(columns.get(i)));
      if (i < columns.size() - 1) {
        sql.append(", ");
      }
    }
    sql.append(") VALUES (").append(placeholders).append(")");

    try (PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
      for (CSVRecord record : records) {
        for (int i = 0; i < columns.size(); i++) {
          String column = columns.get(i);
          String value = record.get(column);
          bindValue(stmt, i + 1, value, types.get(column));
        }
        stmt.addBatch();
      }
      stmt.executeBatch();
    }
  }

  private void bindValue(PreparedStatement stmt, int index, String value, ColumnType type)
      throws SQLException {
    if (value == null || value.isBlank()) {
      stmt.setNull(index, Types.NULL);
      return;
    }

    String trimmed = value.trim();
    switch (type) {
      case INTEGER -> stmt.setLong(index, Long.parseLong(trimmed));
      case REAL -> stmt.setDouble(index, Double.parseDouble(trimmed));
      case TEXT -> stmt.setString(index, value);
      default -> stmt.setString(index, value);
    }
  }

  private String quoteIdentifier(String identifier) {
    String escaped = identifier.replace("\"", "\"\"");
    return "\"" + escaped + "\"";
  }
}