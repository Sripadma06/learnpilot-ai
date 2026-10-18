# LearnPilot AI

One Spring Boot project: the REST API **and** the React web app are served together on
**http://localhost:8080**. No separate frontend server, no CORS, no second deployment.

```
LearnPilot_AI/
  pom.xml
  src/main/java/...                 Spring Boot backend (auth, PDF upload, quizzes, tutor, progress)
  src/main/resources/application.yml
  src/main/resources/static/        the BUILT React app (served by Spring at /)
  src/main/frontend/                React + TypeScript source (only needed if you edit the UI)
  run.bat                           Windows: start the app
  infra/  docs/  optional/ml-service/
```

## Run it (Windows)

1. Install Java 21+ and Maven, and PostgreSQL (or `docker compose up -d postgres`).
2. Create the database once (pgAdmin Query Tool or psql):
   ```sql
   CREATE USER learnpilot WITH PASSWORD 'change_me_local_only';
   CREATE DATABASE learnpilot OWNER learnpilot;
   ```
3. Start everything:
   ```
   mvn spring-boot:run
   ```
   (or double-click `run.bat`). Open **http://localhost:8080**, click *Sign in / Create account*,
   create an account, then upload a PDF.

Tables are created automatically on first start. Node.js is NOT needed to run the app.

### Settings (environment variables, all optional)
| Variable | Purpose |
|---|---|
| `POSTGRES_USER`, `POSTGRES_PASSWORD`, `DATABASE_URL` | database login (defaults: `learnpilot` / `change_me_local_only` / `jdbc:postgresql://localhost:5432/learnpilot`) |
| `JWT_SECRET` | login-token signing key. **Set a long random value before exposing the app to the internet.** |
| `GEMINI_API_KEY` | free key from https://aistudio.google.com/apikey: AI-written quizzes and tutor answers |
| `ANTHROPIC_API_KEY` | alternative to Gemini |

PowerShell example:
```
$env:JWT_SECRET="a-long-random-string-of-40-or-more-characters"
$env:GEMINI_API_KEY="your_key"
mvn spring-boot:run
```

## Share it from your PC (ngrok)
Because the page and the API are one app, you only need one tunnel:
```
ngrok http 8080
```
Open the https URL it prints. (On a free ngrok account click *Visit Site* once on the warning page.)
Vercel is no longer needed. Set `JWT_SECRET` first, since the tunnel makes your app public.

## Changing the UI
- Hot-reload while editing: in `src/main/frontend` run `npm install` then `npm run dev`
  (http://localhost:5173, API calls are proxied to Spring on 8080).
- Rebuild the bundled UI: `mvn -Pfrontend spring-boot:run` (Maven downloads Node and builds into
  `src/main/resources/static`), or `npm run build` inside `src/main/frontend`.

## Quiz and tutor quality
- **Without an AI key:** works best on Q&A style PDFs (interview sheets, FAQs). Each question in the PDF
  becomes a multiple-choice question using the PDF's own answer, and the tutor returns the matching answer.
  For plain prose it falls back to fill-in-the-blank, which is weak.
- **With `GEMINI_API_KEY` or `ANTHROPIC_API_KEY`:** questions and answers are written by a model from your PDF.

See `docs/IMPLEMENTATION_STATUS.md` for what is done and what is left.
