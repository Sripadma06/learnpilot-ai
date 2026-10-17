# LearnPilot AI — Adaptive Learning Intelligence Platform

A starter full-stack AI learning platform using React + Vite + TypeScript, Spring Boot + JWT, PostgreSQL, and a Python FastAPI ML service.

## What is included

- React dashboard with document upload, quiz, mastery, recommendations, and tutor screens (starter UI)
- Spring Boot REST API with registration/login, JWT authentication, user profile, topics, quiz attempts, mastery, recommendations, and document upload endpoints
- PostgreSQL support (install PostgreSQL directly on Windows, or optionally use Docker Compose)
- Python FastAPI ML prediction endpoint with a baseline scikit-learn model fallback

## Important scope note

This is a runnable starter scaffold, not a finished production AI platform. LLM generation, document extraction/storage, and RAG are integration points with safe starter responses. Do not represent these as production AI features until implemented.

## Requirements

- Java JDK 21 or later
- Eclipse IDE for Enterprise Java and Web Developers (or another IDE)
- Eclipse Maven Integration (m2e); Maven command-line installation is not required to run via Eclipse
- Node.js 20+ and npm (for the frontend)
- PostgreSQL 16+ (native Windows install is supported; Docker is optional)
- Docker Desktop only if you choose the optional Docker setup or want the ML service container

## Recommended setup: PostgreSQL installed on Windows (no Docker)

### 1. Install and start PostgreSQL

Install PostgreSQL for Windows from [postgresql.org/download/windows](https://www.postgresql.org/download/windows/). The installer normally includes pgAdmin. Keep the PostgreSQL service running and note its port (normally `5432`). The password chosen for the PostgreSQL administrator `postgres` is separate from the application account created below.

### 2. Create the app database and login

In pgAdmin, connect to your local PostgreSQL server, open **Tools → Query Tool** while connected to the default `postgres` database, and run the following SQL. Replace `REPLACE_WITH_A_PASSWORD` with a password you choose for the LearnPilot database user:

```sql
CREATE ROLE learnpilot WITH LOGIN PASSWORD 'REPLACE_WITH_A_PASSWORD';
CREATE DATABASE learnpilot OWNER learnpilot;
```

If the `learnpilot` role or database already exists, do not run the corresponding `CREATE` command again. In pgAdmin, use **Login/Group Roles → learnpilot → Properties → Definition** to set the role password, and keep the existing database.

### 3. Import the backend into Eclipse

1. Choose **File → Import… → Maven → Existing Maven Projects**.
2. Set the root directory to the project’s `backend` folder (for example `D:\LearnPilot_AI_Enhanced\backend`).
3. Select `pom.xml`, then click **Finish** and wait for dependencies to download.
4. Ensure Eclipse uses a JDK 21 or later: **Window → Preferences → Java → Installed JREs**.

### 4. Create the Eclipse Maven run configuration

1. Open **Run → Run Configurations…** and select **Maven Build** in the left list. Click the New icon if you need a new configuration.
2. On **Main**, set **Base directory** to the backend folder and **Goals** to `spring-boot:run`.
3. On **Environment**, add these as two separate rows:
   - `POSTGRES_USER` = `learnpilot`
   - `POSTGRES_PASSWORD` = the password you chose in step 2 (not necessarily the PostgreSQL `postgres` administrator password)
4. If PostgreSQL uses a port other than `5432`, add another environment row:
   - `DATABASE_URL` = `jdbc:postgresql://localhost:YOUR_PORT/learnpilot`
5. Click **Apply → Run**. Keep the Console open. A successful start includes a line similar to `Started LearnPilotApplication` and the API listens on `http://localhost:8080`.

Eclipse's Maven run configuration does not automatically import variables from the root `.env` file. Set the database variables in that configuration as above.

### 5. Start the frontend

Open a terminal in the `frontend` folder and run:

```powershell
npm install
npm run dev
```

Open the URL Vite prints, usually `http://localhost:5173`.

There is no default LearnPilot account. Register one in the UI. API documentation is at `http://localhost:8080/swagger-ui/index.html`.

## Optional: run PostgreSQL in Docker instead

Docker is not required if PostgreSQL is installed locally. To use Docker for PostgreSQL, from the project root copy `.env.example` to `.env`, choose a database password there, and run:

```powershell
docker compose up -d postgres
```

Use the same database username/password in the Eclipse Maven Build configuration. Important: for an already-initialized PostgreSQL Docker volume, changing `.env` does not change the password inside the existing database. Do not run `docker compose down -v` unless you intentionally want to delete that database's data.

The ML service is optional for starting the backend and frontend. To start it with Docker too, run `docker compose up -d ml-service`.

## Configuration reference

The backend reads these environment variables:

- `DATABASE_URL` — defaults to `jdbc:postgresql://localhost:5432/learnpilot`
- `POSTGRES_USER` — defaults to `learnpilot`
- `POSTGRES_PASSWORD` — defaults to `change_me_local_only`; set your own value in Eclipse before running
- `JWT_SECRET` — use a long random secret for deployments; the development fallback is not production-safe
- `CORS_ALLOWED_ORIGINS` — defaults to `http://localhost:5173`
- `ML_SERVICE_URL` — defaults to `http://localhost:8001`

The `.env.example` file is a template for Docker Compose and reference only. Spring Boot launched directly from Eclipse does not automatically load it.

## Troubleshooting

- **`password authentication failed for user "learnpilot"`**: set `POSTGRES_PASSWORD` in the Eclipse Maven Build configuration to the password of the `learnpilot` role. Do not assume it is the password of the PostgreSQL `postgres` admin account.
- **`No goals have been specified`**: set Maven Build → Main → Goals to `spring-boot:run`.
- **`mvn` is not recognized in PowerShell**: if using Eclipse, use Run Configurations → Maven Build; Eclipse can run Maven through m2e without a separate `mvn` shell command.
- **`invalid value for parameter TimeZone: Asia/Calcutta`**: in the Maven Build configuration’s JRE/VM arguments, add `-Duser.timezone=Asia/Kolkata`.
- **Port 5432 connection refused**: ensure PostgreSQL Windows service is running and that `DATABASE_URL` uses the actual server port.
- **Frontend API errors**: verify backend starts successfully and listens on port 8080.

## Local URLs

- Frontend: `http://localhost:5173`
- Backend: `http://localhost:8080`
- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- PostgreSQL: `localhost:5432` (default)
- Optional ML service: `http://localhost:8001`

## API endpoints included

- `POST /api/v1/auth/register`
- `POST /api/v1/auth/login`
- `GET /api/v1/users/me`
- `POST /api/v1/documents`
- `GET /api/v1/documents`
- `GET /api/v1/topics`
- `GET /api/v1/users/me/mastery`
- `GET /api/v1/users/me/recommendations`
- `GET /api/v1/analytics/overview`
- `POST /api/v1/quizzes/demo/attempts`
- `POST /api/v1/tutor/ask`
- `POST /internal/predict-correctness` (ML service; keep internal in production)

## Security and production checklist

This scaffold uses JWT authentication and BCrypt password hashing, but before production add refresh-token rotation or short-lived access-token strategy, stricter rate limiting, upload malware scanning, private object storage, robust CORS configuration, audit logging, migrations, and complete ownership checks for every resource. Do not expose the ML service publicly in production.

## Next implementation milestones

1. Add database migrations (Flyway recommended).
2. Add real PDF text extraction using Apache PDFBox.
3. Integrate Spring AI with a chosen provider and validate structured quiz output.
4. Add pgvector embeddings and user-scoped RAG retrieval.
5. Add persistent Quiz, Question, AttemptAnswer, DocumentChunk, and StudyTask entities.
6. Replace demo mastery/recommendations with persisted learning history.
7. Add unit/integration tests and evaluation reports.


## Using the app (after the feature update)
1. Start Postgres (`docker compose up -d postgres`), then the backend (`cd backend && mvn spring-boot:run`) and frontend (`cd frontend && npm install && npm run dev`).
2. Open http://localhost:5173 and click **Sign in / Create account** -> **Create an account**. Upload, quizzes and the tutor need you to be signed in.
3. **Upload Material** -> choose a text-based PDF -> **Generate quiz** or **Ask tutor**.
4. Optional: set `ANTHROPIC_API_KEY` before starting the backend for AI-written questions and tutor answers.
   - Windows PowerShell: `$env:ANTHROPIC_API_KEY="sk-ant-..."; mvn spring-boot:run`
   - Windows cmd: `set ANTHROPIC_API_KEY=sk-ant-...` then `mvn spring-boot:run`

Troubleshooting: if the UI says it cannot reach the backend, check that port 8080 is up. Any `localhost` port is accepted by CORS.

### Quiz and tutor quality: offline vs AI mode
- **Offline (no key):** works best on Q&A style material (interview sheets, FAQs, study guides). Each question in the PDF becomes a multiple-choice question whose correct option is the PDF's own answer, and the tutor returns the matching answer. For plain prose it falls back to fill-in-the-blank, which is much weaker.
- **AI mode (recommended for notes/textbooks):** set `GEMINI_API_KEY` (free key from https://aistudio.google.com/apikey) or `ANTHROPIC_API_KEY` before starting the backend. Questions and tutor answers are then written by a model, grounded in your PDF.
  - PowerShell: `$env:GEMINI_API_KEY="your_key"; mvn spring-boot:run`
