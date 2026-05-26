## Data Analyst

Data Analyst converts natural language questions into SQL over an uploaded CSV dataset and returns query results. It is designed for internal analytics workflows where data is short-lived and read-only.

## Features

- CSV upload for ad hoc datasets
- Natural language to SQL using Groq LLaMA 3.1
- Query results rendered as a table
- Dynamic schema detection from CSV
- SQL copy and keyboard submit
- Read-only SQL execution (SELECT only)

## Architecture

```
┌─────────────────────────────────────────────────────────┐
│                    FRONTEND (Local)                      │
│                                                         │
│   React + Vite + Tailwind CSS                           │
│   ┌───────────────────────────────────────────┐         │
│   │  Step 1: Upload CSV file                  │         │
│   │  Step 2: Ask question in plain English    │         │
│   └──────────┬────────────────┬───────────────┘         │
│              │ POST /upload   │ POST /query             │
└──────────────┼────────────────┼─────────────────────────┘
               │                │
               ▼                ▼
┌─────────────────────────────────────────────────────────┐
│                   BACKEND (Local)                        │
│                                                         │
│   Spring Boot + Java                                    │
│   ┌─────────────────┐    ┌──────────────────────┐       │
│   │  /upload API    │    │  /query API          │       │
│   │  CSV → SQLite   │    │  Question → Groq LLM │       │
│   │  via JDBC       │    │  → SQL → Execute     │       │
│   └────────┬────────┘    └──────────┬───────────┘       │
│            │                        │                   │
│            ▼                        ▼                   │
│   ┌─────────────────────────────────────────┐           │
│   │   SQLite Database (uploaded_data.db)    │           │
│   │   Dynamic table from uploaded CSV       │           │
│   └─────────────────────────────────────────┘           │
└─────────────────────────────────────────────────────────┘
```

### How It Works

1. Upload a CSV file. The backend stores it in SQLite using JDBC.
2. Column names and types are inferred from the CSV.
3. A question is converted into a `SELECT` query by Groq LLaMA 3.1.
4. The backend executes the SQL and returns results as JSON.

## Tech Stack

| Layer | Technology | Purpose |
|-------|-----------|---------|
| **Frontend** | React 19, Vite 7, Tailwind CSS 4 | UI & styling |
| **Backend** | Spring Boot, Java | REST API |
| **Model** | Groq Cloud, LLaMA 3.1-8B | NL → SQL conversion |
| **Database** | SQLite, JDBC, Commons CSV | Dynamic data storage |
| **Deployment** | Not deployed | TBD |

## Java Backend Dependencies

- Java 17
- Spring Boot 3.x
- Maven
- SQLite JDBC
- Apache Commons CSV

---

## Getting Started

### Prerequisites

- **Node.js** ≥ 18
- **Java** 17+
- **Maven**
- **Groq API Key** — Get one at [console.groq.com](https://console.groq.com)

### 1. Clone the Repository

```bash
git clone https://github.com/omsudhamsh/nl-to-sql-Java-.git
cd nl-sql
```

### 2. Run the Backend

```bash
cd backend

# Export API key (set this in your shell)
export GROQ_API_KEY=your_groq_api_key_here

# Run the backend
mvn spring-boot:run
```

The API server will start at `http://127.0.0.1:8000`

### 3. Run the Frontend

```bash
cd frontend/nl-sql

# Install dependencies
npm install

# Run dev server
npm run dev
```

The app will be available at `http://localhost:5173`

### 4. Use the App

1. Upload a CSV file.
2. Ask a question in natural language or paste a valid `SELECT` query.
3. Review the generated SQL and results.

## Project Structure

```
nl-sql/
├── backend/
│   ├── src/main/java/
│   │   └── com/nlsql/        # Spring Boot app, routes, services
│   ├── src/main/resources/
│   │   └── application.properties
│   └── pom.xml
│
├── frontend/nl-sql/
│   ├── src/
│   │   ├── App.jsx          # Main app (upload + query UI)
│   │   ├── index.css         # Premium dark theme styles
│   │   └── main.jsx         # React entry point
│   ├── index.html
│   ├── vite.config.js
│   └── package.json
│
├── assets/                  # Screenshots for README
└── README.md
```

---

## Safety

- Only `SELECT` queries are allowed — the backend rejects `INSERT`, `UPDATE`, `DELETE`, etc.
- SQL output is sanitized to strip markdown formatting from LLM responses
- CORS configured for secure cross-origin requests
- API key should be stored in environment variables and never committed

## License

This project is licensed under the MIT License — see the [LICENSE](LICENSE) file for details.