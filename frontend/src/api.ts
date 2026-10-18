/**
 * Builds the backend URL from VITE_API_BASE_URL, tolerating the usual mistakes: missing https://,
 * trailing slash, or a missing /api/v1 suffix (a scheme-less value would otherwise be treated as a
 * path on the frontend's own host, which answers POST with 405).
 */
export function normalizeBase(raw: string | undefined): string {
  let b = (raw || '').trim()
  if (!b) return 'http://localhost:8080/api/v1'
  if (!/^https?:\/\//i.test(b)) b = (/^(localhost|127\.)/i.test(b) ? 'http://' : 'https://') + b.replace(/^\/+/, '')
  b = b.replace(/\/+$/, '')
  if (!/\/api\/v1$/.test(b)) b += '/api/v1'
  return b
}
export const API_BASE = normalizeBase(import.meta.env.VITE_API_BASE_URL)

export type AuthResponse = { token: string; tokenType: string; email: string; role: string }
export type Me = { id: string; email: string; role: string; createdAt: string }
export type DocumentInfo = { id: string; title: string; status: string; pageCount: number; chunkCount: number; createdAt: string; message?: string }
export type QuizSummary = { id: string; title: string; topic: string; source: string; questionCount: number; createdAt: string; lastScore: number | null; attempts: number }
export type QuizQuestion = { id: string; text: string; options: string[]; page: number }
export type Quiz = { id: string; title: string; topic: string; source: string; questions: QuizQuestion[] }
export type QuestionResult = { questionId: string; selected: number; correctIndex: number; correct: boolean; explanation: string }
export type AttemptResult = { score: number; correct: number; total: number; results: QuestionResult[] }
export type TutorAnswer = { answer: string; citations: Array<{ documentTitle: string; pageNumber: number; excerpt: string }>; grounded: boolean; mode: string }

export const TOKEN_KEY = 'learnpilot_token'
export const UNAUTHORIZED_EVENT = 'learnpilot:unauthorized'

function token() {
  try { return localStorage.getItem(TOKEN_KEY) } catch { return null }
}

async function request<T>(path: string, options: RequestInit = {}, authenticated = true): Promise<T> {
  const headers = new Headers(options.headers)
  if (!(options.body instanceof FormData) && options.body) headers.set('Content-Type', 'application/json')
  // ngrok's free tier shows an HTML warning page to browsers unless this header is sent.
  if (/ngrok/i.test(API_BASE)) headers.set('ngrok-skip-browser-warning', 'true')
  const t = token()
  if (authenticated && t) headers.set('Authorization', `Bearer ${t}`)
  let response: Response
  try {
    response = await fetch(`${API_BASE}${path}`, { ...options, headers })
  } catch {
    throw new Error(`Cannot reach the backend at ${API_BASE}. Check that it is running and that CORS_ALLOWED_ORIGINS on the backend includes ${window.location.origin}.`)
  }
  const raw = await response.text()
  let data: unknown = undefined
  try { data = raw ? JSON.parse(raw) : undefined } catch { data = raw }
  if (!response.ok) {
    if (authenticated && (response.status === 401 || response.status === 403)) {
      // Missing/expired token: drop it and let the app prompt for sign-in.
      try { localStorage.removeItem(TOKEN_KEY) } catch { /* ignore */ }
      window.dispatchEvent(new Event(UNAUTHORIZED_EVENT))
      throw new Error('Please sign in to continue.')
    }
    const message = typeof data === 'object' && data && 'error' in data
      ? String((data as { error: unknown }).error)
      : `Request failed (${response.status}) for ${options.method ?? 'GET'} ${response.url || API_BASE + path}`
    throw new Error(message)
  }
  return data as T
}

export const api = {
  register: (email: string, password: string) =>
    request<AuthResponse>('/auth/register', { method: 'POST', body: JSON.stringify({ email, password }) }, false),
  login: (email: string, password: string) =>
    request<AuthResponse>('/auth/login', { method: 'POST', body: JSON.stringify({ email, password }) }, false),
  me: () => request<Me>('/users/me'),
  overview: () => request<{ overallMastery: number; quizzesCompleted: number; studyStreak: number; dueTasks: number }>('/analytics/overview'),
  mastery: () => request<Array<{ topic: string; mastery: number; attempts: number }>>('/users/me/mastery'),
  recommendations: () => request<Array<{ topic: string; action: string; reason: string; priority: string }>>('/users/me/recommendations'),
  upload: (file: File) => {
    const body = new FormData()
    body.append('file', file)
    return request<DocumentInfo>('/documents', { method: 'POST', body })
  },
  documents: () => request<DocumentInfo[]>('/documents'),
  deleteDocument: (id: string) => request<void>(`/documents/${id}`, { method: 'DELETE' }),
  generateQuiz: (documentId: string, count = 5) =>
    request<Quiz>(`/documents/${documentId}/quizzes?count=${count}`, { method: 'POST' }),
  quizzes: () => request<QuizSummary[]>('/quizzes'),
  quiz: (id: string) => request<Quiz>(`/quizzes/${id}`),
  submitAttempt: (id: string, answers: number[]) =>
    request<AttemptResult>(`/quizzes/${id}/attempts`, { method: 'POST', body: JSON.stringify({ answers }) }),
  askTutor: (question: string, documentId?: string) =>
    request<TutorAnswer>('/tutor/ask', { method: 'POST', body: JSON.stringify({ question, documentId: documentId || null }) })
}
