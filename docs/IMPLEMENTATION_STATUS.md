# Implementation status

This is now a single Spring Boot application: the React UI is bundled in `src/main/resources/static` and served by Spring.


## Working now
- **Auth**: `POST /api/v1/auth/register` and `/login` (BCrypt + JWT). Unauthenticated calls now return `401 Please sign in to continue.`; the UI opens the sign-in dialog and discards expired tokens.
- **PDF upload**: `POST /api/v1/documents` validates the PDF signature, extracts text per page with Apache PDFBox, splits it into passages and stores them per user (`study_documents`, `document_chunks`). `GET /documents`, `DELETE /documents/{id}`.
- **Quizzes**: `POST /documents/{id}/quizzes?count=5` generates a quiz; `GET /quizzes`, `GET /quizzes/{id}`, `POST /quizzes/{id}/attempts` grades it server-side (answers are never sent to the browser before submitting).
  - With `ANTHROPIC_API_KEY`: AI-written multiple-choice questions grounded in the PDF.
  - Without a key: multiple-choice questions built from the question/answer pairs in the PDF (Q&A style documents), else fill-in-the-blank from sentences.
  - Free option: `GEMINI_API_KEY` (Google AI Studio) enables AI mode too.
- **AI tutor**: `POST /tutor/ask` retrieves the most relevant passages from the user's own PDFs (keyword/TF-IDF ranking) and returns citations. With a key, a model writes the answer from those passages; without one, the best matching sentences are quoted.
- **Progress**: mastery, streak, overview and recommendations are computed from real quiz attempts.

## Still to do
- Scanned (image-only) PDFs need OCR; they are rejected with a clear message.
- Retrieval is keyword based; swap for pgvector embeddings for better recall.
- Study plan tasks are derived from recommendations, not yet persisted/scheduled.
- Flyway migrations instead of `ddl-auto=update`.
- Rate limiting for upload and LLM endpoints.
