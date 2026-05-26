package com.nlsql.service;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class SqlQueryService {
  private static final String DB_URL = "jdbc:sqlite:uploaded_data.db";

  public List<Map<String, Object>> runQuery(String sql) throws SQLException {
    String cleaned = cleanSql(sql);
    if (!cleaned.toLowerCase().startsWith("select")) {
      throw new IllegalArgumentException("Only SELECT queries allowed");
    }

    try (Connection conn = DriverManager.getConnection(DB_URL);
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery(cleaned)) {
      List<Map<String, Object>> rows = new ArrayList<>();
      ResultSetMetaData meta = rs.getMetaData();
      int columnCount = meta.getColumnCount();

      while (rs.next()) {
        Map<String, Object> row = new LinkedHashMap<>();
        for (int i = 1; i <= columnCount; i++) {
          row.put(meta.getColumnLabel(i), rs.getObject(i));
        }
        rows.add(row);
      }
      return rows;
    }
  }

  private String cleanSql(String sql) {
    if (sql == null) {
      return "";
    }
    return sql.replace("```sql", "").replace("```", "").trim();
  }
}