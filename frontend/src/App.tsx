import { useEffect, useState } from 'react'
import {
  Activity, BookOpen, BrainCircuit, CalendarDays, CheckCircle2, ChevronRight,
  CircleHelp, FileUp, Flame, GraduationCap, LayoutDashboard, LogOut,
  Menu, MessageSquareText, PanelLeftClose, Plus, Search, ShieldCheck,
  Sparkles, Target, Trash2, UploadCloud, X
} from 'lucide-react'
import { Bar, BarChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import { api, UNAUTHORIZED_EVENT, TOKEN_KEY, type Me, type DocumentInfo, type QuizSummary, type Quiz, type AttemptResult, type TutorAnswer } from './api'

type Overview = { overallMastery: number; quizzesCompleted: number; studyStreak: number; dueTasks: number }
type Mastery = { topic: string; mastery: number; attempts: number }
type Recommendation = { topic: string; action: string; reason: string; priority: string }
type NavKey = 'Overview' | 'My Learning' | 'Upload Material' | 'Quizzes' | 'AI Tutor' | 'Study Plan'

const demoOverview: Overview = { overallMastery: 64, quizzesCompleted: 24, studyStreak: 5, dueTasks: 3 }
const demoMastery: Mastery[] = [
  { topic: 'Java OOP', mastery: 72, attempts: 8 },
  { topic: 'Collections', mastery: 41, attempts: 5 },
  { topic: 'SQL & Joins', mastery: 84, attempts: 7 },
  { topic: 'Spring Boot', mastery: 58, attempts: 4 },
]
const demoRecommendations: Recommendation[] = [
  { topic: 'Java Collections', action: 'Review concept + easy quiz', reason: 'Lowest current mastery in the sample dashboard', priority: 'High' },
  { topic: 'Spring Boot', action: 'Practice medium questions', reason: 'Build confidence with recent concepts', priority: 'Medium' },
  { topic: 'SQL & Joins', action: 'Schedule a retention quiz', reason: 'Strong mastery; maintain retention', priority: 'Low' },
]

export default function App() {
  const [page, setPage] = useState<NavKey>('Overview')
  const [user, setUser] = useState<Me | null>(null)
  const [overview, setOverview] = useState<Overview>(demoOverview)
  const [mastery, setMastery] = useState<Mastery[]>(demoMastery)
  const [recommendations, setRecommendations] = useState<Recommendation[]>(demoRecommendations)
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [authMode, setAuthMode] = useState<'login' | 'register'>('login')
  const [authOpen, setAuthOpen] = useState(false)
  const [authError, setAuthError] = useState('')
  const [busy, setBusy] = useState(false)
  const [notice, setNotice] = useState('')
  const [file, setFile] = useState<File | null>(null)
  const [documents, setDocuments] = useState<DocumentInfo[]>([])
  const [quizzes, setQuizzes] = useState<QuizSummary[]>([])
  const [activeQuiz, setActiveQuiz] = useState<Quiz | null>(null)
  const [question, setQuestion] = useState('')
  const [tutorDoc, setTutorDoc] = useState('')
  const [tutorAnswer, setTutorAnswer] = useState<TutorAnswer | null>(null)
  const [sidebarOpen, setSidebarOpen] = useState(false)

  function resetToDemo() {
    setUser(null); setOverview(demoOverview); setMastery(demoMastery); setRecommendations(demoRecommendations)
    setDocuments([]); setQuizzes([]); setActiveQuiz(null); setTutorAnswer(null)
  }

  async function refreshProgress() {
    const [ov, mas, recs] = await Promise.all([api.overview(), api.mastery(), api.recommendations()])
    setOverview(ov); setMastery(mas); setRecommendations(recs)
  }

  async function loadAll() {
    try {
      const me = await api.me()
      setUser(me)
      const [ov, mas, recs, docs, qz] = await Promise.all([
        api.overview(), api.mastery(), api.recommendations(), api.documents(), api.quizzes()])
      setOverview(ov); setMastery(mas); setRecommendations(recs); setDocuments(docs); setQuizzes(qz)
    } catch (error) {
      if (localStorage.getItem(TOKEN_KEY)) setNotice(error instanceof Error ? error.message : 'Could not load your data.')
    }
  }

  useEffect(() => {
    if (localStorage.getItem(TOKEN_KEY)) void loadAll()
    const onUnauthorized = () => { resetToDemo(); setNotice('Please sign in to continue.'); setAuthOpen(true) }
    window.addEventListener(UNAUTHORIZED_EVENT, onUnauthorized)
    return () => window.removeEventListener(UNAUTHORIZED_EVENT, onUnauthorized)
  }, [])

  async function submitAuth(event: React.FormEvent) {
    event.preventDefault()
    setBusy(true); setAuthError('')
    try {
      const result = authMode === 'login' ? await api.login(email, password) : await api.register(email, password)
      localStorage.setItem(TOKEN_KEY, result.token)
      setAuthOpen(false); setPassword('')
      await loadAll()
      setNotice(`Signed in as ${result.email}`)
    } catch (error) {
      setAuthError(error instanceof Error ? error.message : 'Authentication failed')
    } finally { setBusy(false) }
  }

  function logout() {
    localStorage.removeItem(TOKEN_KEY)
    resetToDemo(); setNotice('Signed out')
  }

  function requireSignIn() { setAuthOpen(true) }

  async function uploadFile() {
    if (!file) { setNotice('Choose a PDF first.'); return }
    setBusy(true); setNotice('')
    try {
      const result = await api.upload(file)
      setDocuments(await api.documents())
      setFile(null)
      setNotice(`${result.title}: ${result.message ?? 'uploaded'}`)
    } catch (error) {
      setNotice(error instanceof Error ? error.message : 'Upload failed')
    } finally { setBusy(false) }
  }

  async function removeDocument(id: string) {
    try { await api.deleteDocument(id); setDocuments(await api.documents()) }
    catch (error) { setNotice(error instanceof Error ? error.message : 'Delete failed') }
  }

  async function makeQuiz(documentId: string) {
    setBusy(true); setNotice('')
    try {
      const quiz = await api.generateQuiz(documentId, 5)
      setActiveQuiz(quiz); setQuizzes(await api.quizzes()); setPage('Quizzes')
      if (quiz.source === 'OFFLINE') setNotice('Quiz built offline from the questions and answers in your PDF. For AI-written questions on any kind of document, set GEMINI_API_KEY (free) or ANTHROPIC_API_KEY on the backend.')
    } catch (error) {
      setNotice(error instanceof Error ? error.message : 'Could not generate a quiz')
    } finally { setBusy(false) }
  }

  async function openQuiz(id: string) {
    try { setActiveQuiz(await api.quiz(id)) }
    catch (error) { setNotice(error instanceof Error ? error.message : 'Could not open quiz') }
  }

  async function submitQuiz(answers: number[]): Promise<AttemptResult | null> {
    if (!activeQuiz) return null
    try {
      const result = await api.submitAttempt(activeQuiz.id, answers)
      void api.quizzes().then(setQuizzes).catch(() => undefined)
      void refreshProgress().catch(() => undefined)
      return result
    } catch (error) {
      setNotice(error instanceof Error ? error.message : 'Could not submit answers')
      return null
    }
  }

  async function askTutor(event: React.FormEvent) {
    event.preventDefault()
    if (!question.trim()) return
    setBusy(true); setTutorAnswer(null)
    try {
      setTutorAnswer(await api.askTutor(question, tutorDoc))
    } catch (error) {
      setNotice(error instanceof Error ? error.message : 'Tutor request failed')
    } finally { setBusy(false) }
  }

  const nav: { label: NavKey; icon: typeof LayoutDashboard }[] = [
    { label: 'Overview', icon: LayoutDashboard },
    { label: 'My Learning', icon: BookOpen },
    { label: 'Upload Material', icon: FileUp },
    { label: 'Quizzes', icon: CheckCircle2 },
    { label: 'AI Tutor', icon: MessageSquareText },
    { label: 'Study Plan', icon: CalendarDays },
  ]

  return (
    <div className="app-shell">
      <aside className={`sidebar ${sidebarOpen ? 'sidebar-open' : ''}`}>
        <div className="brand">
          <div className="brand-mark"><BrainCircuit size={22} /></div>
          <div><strong>LearnPilot <span>AI</span></strong><small>Adaptive learning</small></div>
          <button className="icon-button mobile-close" onClick={() => setSidebarOpen(false)} aria-label="Close menu"><X size={18}/></button>
        </div>
        <div className="workspace-label">WORKSPACE</div>
        <nav>
          {nav.map(item => {
            const Icon = item.icon
            return <button key={item.label} className={`nav-item ${page === item.label ? 'active' : ''}`} onClick={() => { setPage(item.label); setSidebarOpen(false) }}>
              <Icon size={18}/><span>{item.label}</span>{page === item.label && <ChevronRight className="nav-chevron" size={15}/>}
            </button>
          })}
        </nav>
        <div className="sidebar-bottom">
          <div className="plan-card"><div className="plan-icon"><Sparkles size={16}/></div><strong>Your next level</strong><p>Small daily sessions build lasting mastery.</p><button onClick={() => setPage('Study Plan')}>View study plan <ChevronRight size={14}/></button></div>
          <div className="profile">
            <div className="avatar">{(user?.email || 'S').slice(0,1).toUpperCase()}</div>
            <div className="profile-text"><strong>{user?.email || 'Student preview'}</strong><small>{user ? 'Student account' : 'Not signed in · demo data'}</small></div>
            {user && <button className="icon-button" onClick={logout} aria-label="Log out"><LogOut size={16}/></button>}
          </div>
        </div>
      </aside>
      {sidebarOpen && <button className="mobile-scrim" onClick={() => setSidebarOpen(false)} aria-label="Close sidebar"/>}
      <main className="main-area">
        <header className="topbar">
          <button className="icon-button menu-toggle" onClick={() => setSidebarOpen(true)} aria-label="Open menu"><Menu size={20}/></button>
          <div className="breadcrumbs">Workspace <span>/</span> <strong>{page}</strong></div>
          <div className="top-actions"><div className="searchbox"><Search size={16}/><input placeholder="Search topics..." aria-label="Search topics"/></div><div className="top-avatar">{(user?.email || 'S').slice(0,1).toUpperCase()}</div></div>
        </header>
        <div className="content">
          {notice && <div className="notice"><CircleHelp size={16}/><span>{notice}</span><button onClick={() => setNotice('')} aria-label="Dismiss"><X size={14}/></button></div>}
          {page === 'Overview' && <OverviewPage overview={overview} mastery={mastery} recommendations={recommendations} setPage={setPage} />}
          {page === 'My Learning' && <LearningPage mastery={mastery} signedIn={!!user} />}
          {page === 'Upload Material' && (user
            ? <UploadPage file={file} setFile={setFile} uploadFile={uploadFile} busy={busy} documents={documents} makeQuiz={makeQuiz} removeDocument={removeDocument} goTutor={(id) => { setTutorDoc(id); setPage('AI Tutor') }} />
            : <SignInGate what="upload study material" onSignIn={requireSignIn} />)}
          {page === 'Quizzes' && (user
            ? <QuizPage quizzes={quizzes} active={activeQuiz} openQuiz={openQuiz} closeQuiz={() => setActiveQuiz(null)} submitQuiz={submitQuiz} goUpload={() => setPage('Upload Material')} />
            : <SignInGate what="take quizzes" onSignIn={requireSignIn} />)}
          {page === 'AI Tutor' && (user
            ? <TutorPage question={question} setQuestion={setQuestion} answer={tutorAnswer} askTutor={askTutor} busy={busy} documents={documents} docId={tutorDoc} setDocId={setTutorDoc} />
            : <SignInGate what="ask the tutor" onSignIn={requireSignIn} />)}
          {page === 'Study Plan' && <StudyPlanPage recommendations={recommendations} />}
          <footer>LearnPilot AI <span>·</span> Learn smarter, one concept at a time.</footer>
        </div>
      </main>
      {!user && <div className="auth-fab"><ShieldCheck size={16}/><button onClick={() => setAuthOpen(true)}>Sign in / Create account</button></div>}
      <AuthDialog open={authOpen} close={() => setAuthOpen(false)} email={email} setEmail={setEmail} password={password} setPassword={setPassword} mode={authMode} setMode={setAuthMode} submit={submitAuth} error={authError} busy={busy} />
    </div>
  )
}

function OverviewPage({ overview, mastery, recommendations, setPage }: {
  overview: Overview; mastery: Mastery[]; recommendations: Recommendation[]; setPage: (page: NavKey) => void
}) {
  return <>
    <div className="welcome-row"><div><div className="eyebrow"><Sparkles size={14}/> YOUR PERSONAL LEARNING SPACE</div><h1>Good to see you <span>again.</span></h1><p>Build understanding with a plan that adapts as you learn.</p></div><button className="primary-button" onClick={() => setPage('Upload Material')}><Plus size={17}/> New learning material</button></div>
    <div className="stat-grid">
      <StatCard label="Overall mastery" value={`${overview.overallMastery}%`} note="Across tracked topics" icon={<Target/>} color="purple" />
      <StatCard label="Quizzes completed" value={String(overview.quizzesCompleted)} note="Practice makes progress" icon={<CheckCircle2/>} color="blue" />
      <StatCard label="Study streak" value={`${overview.studyStreak} days`} note="Keep the rhythm going" icon={<Flame/>} color="orange" />
      <StatCard label="Due revisions" value={String(overview.dueTasks)} note="Small sessions, big gains" icon={<CalendarDays/>} color="teal" />
    </div>
    <div className="dashboard-grid">
      <section className="panel chart-panel"><div className="panel-heading"><div><h2>Topic mastery</h2><p>Where your understanding stands</p></div><button className="text-button" onClick={() => setPage('My Learning')}>View details <ChevronRight size={15}/></button></div>
        {mastery.length === 0 ? <div className="empty-chart">No quiz results yet. Upload material and take a quiz to see your mastery here.</div> : <div className="chart-wrap"><ResponsiveContainer width="100%" height="100%"><BarChart data={mastery} margin={{ top: 12, right: 8, left: -22, bottom: 0 }}><CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#e9ebf3"/><XAxis dataKey="topic" tick={{ fontSize: 11, fill: '#737b91' }} axisLine={false} tickLine={false}/><YAxis domain={[0,100]} tick={{ fontSize: 11, fill: '#737b91' }} axisLine={false} tickLine={false}/><Tooltip cursor={{ fill: '#f4f2ff' }}/><Bar dataKey="mastery" name="Mastery %" fill="#7861f6" radius={[6,6,0,0]} maxBarSize={38}/></BarChart></ResponsiveContainer></div>}
      </section>
      <section className="panel recommendation-panel"><div className="panel-heading"><div><h2>Recommended for you</h2><p>Your next best learning steps</p></div><div className="sparkle-icon"><Sparkles size={17}/></div></div>
        <div className="recommendations">{recommendations.length === 0 && <p className="empty-note">Recommendations appear after you upload material.</p>}{recommendations.map((rec, i) => <div className="recommendation" key={rec.topic}><div className={`rec-number rec-${i}`}>{String(i+1).padStart(2,'0')}</div><div className="rec-content"><strong>{rec.topic}</strong><span>{rec.action}</span><small>{rec.reason}</small></div><span className={`priority priority-${rec.priority.toLowerCase()}`}>{rec.priority}</span></div>)}</div>
        <button className="secondary-button full-width" onClick={() => setPage('Study Plan')}>Open my study plan <ChevronRight size={15}/></button>
      </section>
    </div>
    <section className="panel bottom-panel"><div className="bottom-copy"><div className="mini-icon"><GraduationCap size={20}/></div><div><h2>Progress is built in small steps</h2><p>Upload material, practice a concept, and let your results guide the next session.</p></div></div><button className="primary-button" onClick={() => setPage('Quizzes')}>Practice now <ChevronRight size={16}/></button></section>
  </>
}

function StatCard({ label, value, note, icon, color }: { label: string; value: string; note: string; icon: React.ReactNode; color: string }) {
  return <section className="stat-card"><div className={`stat-icon ${color}`}>{icon}</div><div className="stat-label">{label}</div><div className="stat-value">{value}</div><div className="stat-note">{note}</div></section>
}

function SignInGate({ what, onSignIn }: { what: string; onSignIn: () => void }) {
  return <section className="panel gate-panel"><div className="upload-icon"><ShieldCheck size={28}/></div><h2>Sign in to {what}</h2><p>Your materials, quizzes and progress are saved to your own account. Creating one takes a few seconds.</p><button className="primary-button" onClick={onSignIn}>Sign in or create account <ChevronRight size={16}/></button></section>
}

function LearningPage({ mastery, signedIn }: { mastery: Mastery[]; signedIn: boolean }) {
  return <><PageHeading eyebrow="YOUR PROGRESS" title="My learning" description="Track your mastery by topic and see where to focus next."/>
    {signedIn && mastery.length === 0 && <div className="callout"><BrainCircuit size={20}/><div><strong>Nothing to show yet</strong><p>Upload a PDF, generate a quiz and submit it. Your topic mastery appears here after your first quiz.</p></div></div>}
    <div className="topic-list">{mastery.map(topic => <div className="topic-row" key={topic.topic}><div className="topic-dot"/><div className="topic-info"><strong>{topic.topic}</strong><small>{topic.attempts} quiz attempt{topic.attempts === 1 ? '' : 's'}</small></div><div className="progress-track"><div style={{ width: `${topic.mastery}%` }}/></div><strong className="topic-percent">{topic.mastery}%</strong></div>)}</div>
    <div className="callout"><BrainCircuit size={20}/><div><strong>How mastery works</strong><p>{signedIn ? 'Mastery is a recency-weighted average of your latest five quiz scores per topic, so recent results count most.' : 'You are viewing sample demo values. Sign in to see mastery calculated from your own quizzes.'}</p></div></div>
  </>
}

function UploadPage({ file, setFile, uploadFile, busy, documents, makeQuiz, removeDocument, goTutor }: {
  file: File | null; setFile: (file: File | null) => void; uploadFile: () => void; busy: boolean
  documents: DocumentInfo[]; makeQuiz: (id: string) => void; removeDocument: (id: string) => void; goTutor: (id: string) => void
}) {
  return <><PageHeading eyebrow="LEARNING MATERIAL" title="Upload material" description="Bring your notes into LearnPilot and turn them into active practice."/>
    <section className="panel upload-panel"><label className="dropzone"><input type="file" accept="application/pdf,.pdf" onChange={e => setFile(e.target.files?.[0] || null)}/><div className="upload-icon"><UploadCloud size={28}/></div><h2>{file ? file.name : 'Drop your PDF here'}</h2><p>{file ? `${(file.size / 1024 / 1024).toFixed(2)} MB selected` : 'or click to browse your files'}</p><span>Text-based PDF files · up to 10 MB</span></label><div className="upload-actions"><div><strong>What happens next?</strong><p>Text is extracted page by page, split into passages, and stored privately so you can generate quizzes and ask the tutor about it.</p></div><button className="primary-button" disabled={!file || busy} onClick={uploadFile}>{busy ? 'Working...' : 'Upload material'} <ChevronRight size={16}/></button></div></section>
    <section className="panel doc-panel"><div className="panel-heading"><div><h2>Your materials</h2><p>{documents.length === 0 ? 'Nothing uploaded yet' : `${documents.length} document${documents.length === 1 ? '' : 's'} ready`}</p></div></div>
      <div className="doc-list">{documents.map(d => <div className="doc-row" key={d.id}><div className="doc-info"><strong>{d.title}</strong><small>{d.pageCount} page{d.pageCount === 1 ? '' : 's'} · {d.chunkCount} passages · {new Date(d.createdAt).toLocaleDateString()}</small></div><div className="doc-actions"><button className="primary-button small" disabled={busy} onClick={() => makeQuiz(d.id)}>{busy ? 'Generating...' : 'Generate quiz'}</button><button className="secondary-button small" onClick={() => goTutor(d.id)}>Ask tutor</button><button className="icon-button" aria-label={`Delete ${d.title}`} onClick={() => removeDocument(d.id)}><Trash2 size={15}/></button></div></div>)}</div>
    </section>
  </>
}

function QuizPage({ quizzes, active, openQuiz, closeQuiz, submitQuiz, goUpload }: {
  quizzes: QuizSummary[]; active: Quiz | null; openQuiz: (id: string) => void; closeQuiz: () => void
  submitQuiz: (answers: number[]) => Promise<AttemptResult | null>; goUpload: () => void
}) {
  const [selected, setSelected] = useState<Record<number, number>>({})
  const [result, setResult] = useState<AttemptResult | null>(null)
  const [submitting, setSubmitting] = useState(false)
  useEffect(() => { setSelected({}); setResult(null) }, [active?.id])

  if (!active) {
    return <><PageHeading eyebrow="ACTIVE PRACTICE" title="Practice quizzes" description="Quizzes are generated from the material you upload."/>
      {quizzes.length === 0
        ? <section className="panel gate-panel"><div className="upload-icon"><FileUp size={28}/></div><h2>No quizzes yet</h2><p>Upload a PDF, then choose "Generate quiz" to create one.</p><button className="primary-button" onClick={goUpload}>Upload material <ChevronRight size={16}/></button></section>
        : <div className="doc-list">{quizzes.map(q => <div className="doc-row panel" key={q.id}><div className="doc-info"><strong>{q.title}</strong><small>{q.questionCount} questions · {q.source === 'AI' ? 'AI-written' : 'Offline generator'} · {q.attempts === 0 ? 'not attempted' : `last score ${q.lastScore}%`}</small></div><div className="doc-actions"><button className="primary-button small" onClick={() => openQuiz(q.id)}>{q.attempts === 0 ? 'Start' : 'Retake'} <ChevronRight size={14}/></button></div></div>)}</div>}
    </>
  }

  const questions = active.questions
  const answered = Object.keys(selected).length
  async function submit() {
    setSubmitting(true)
    const res = await submitQuiz(questions.map((_, i) => selected[i] ?? -1))
    setSubmitting(false)
    if (res) setResult(res)
  }
  return <><PageHeading eyebrow="ACTIVE PRACTICE" title={active.title} description="Pick one answer for each question, then submit to see explanations from your material."/>
    <section className="panel quiz-panel"><div className="quiz-top"><span className="pill">{active.source === 'AI' ? 'AI-WRITTEN' : 'FROM YOUR MATERIAL'}</span><span>{answered} of {questions.length} answered</span></div>
      {questions.map((q, i) => { const r = result?.results[i]; return <div className="question-block" key={q.id}><div className="question-number">QUESTION {i + 1}{q.page > 0 ? ` · PAGE ${q.page}` : ''}</div><h3>{q.text}</h3>
        <div className="options">{q.options.map((option, oi) => <button key={oi} disabled={!!result} className={`option ${selected[i] === oi ? 'selected' : ''} ${r && oi === r.correctIndex ? 'correct' : ''} ${r && selected[i] === oi && !r.correct ? 'incorrect' : ''}`} onClick={() => setSelected(old => ({ ...old, [i]: oi }))}><span className="option-letter">{String.fromCharCode(65 + oi)}</span>{option}</button>)}</div>
        {r && r.explanation && <p className="explanation">{r.explanation}</p>}</div> })}
      {result && <div className="result-banner"><CheckCircle2 size={20}/><div><strong>Your score: {result.correct}/{result.total} ({result.score}%)</strong><p>Saved to your progress. Your topic mastery has been updated.</p></div></div>}
      <div className="quiz-actions">
        {!result && <button className="primary-button" disabled={submitting || answered !== questions.length} onClick={submit}>{submitting ? 'Submitting...' : 'Submit answers'} <ChevronRight size={16}/></button>}
        {result && <button className="primary-button" onClick={() => { setSelected({}); setResult(null) }}>Retake <ChevronRight size={16}/></button>}
        <button className="secondary-button" onClick={closeQuiz}>Back to quizzes</button>
      </div></section>
  </>
}

function TutorPage({ question, setQuestion, answer, askTutor, busy, documents, docId, setDocId }: {
  question: string; setQuestion: (v: string) => void; answer: TutorAnswer | null; askTutor: (e: React.FormEvent) => void; busy: boolean
  documents: DocumentInfo[]; docId: string; setDocId: (v: string) => void
}) {
  return <><PageHeading eyebrow="GROUNDED LEARNING ASSISTANT" title="AI tutor" description="Ask questions about your study material. Answers are drawn from your uploaded passages and show their sources."/>
    <section className="panel tutor-panel"><div className="tutor-intro"><div className="tutor-orb"><BrainCircuit size={26}/></div><h2>What are you learning today?</h2><p>{documents.length === 0 ? 'Upload a PDF first so the tutor has something to work from.' : 'Ask a focused question about your material.'}</p></div>
      <form onSubmit={askTutor} className="tutor-form">
        {documents.length > 0 && <select className="select" value={docId} onChange={e => setDocId(e.target.value)} aria-label="Material to search"><option value="">All my materials</option>{documents.map(d => <option key={d.id} value={d.id}>{d.title}</option>)}</select>}
        <textarea value={question} onChange={e => setQuestion(e.target.value)} placeholder="e.g. What does a HashSet do with duplicate elements?" rows={4}/><button className="primary-button" disabled={busy || !question.trim()}>{busy ? 'Thinking...' : 'Ask LearnPilot'} <Sparkles size={16}/></button></form>
      {answer && <div className="tutor-answer"><strong>LearnPilot response</strong><p className="answer-text">{answer.answer}</p>
        {answer.citations.length > 0 && <div className="citations"><small>SOURCES</small>{answer.citations.map((c, i) => <div className="citation" key={i}><strong>[{i + 1}] {c.documentTitle} · page {c.pageNumber}</strong><span>{c.excerpt}</span></div>)}</div>}
        <small>{answer.grounded ? 'Grounded in your material' : 'Not grounded in your material'} · {answer.mode === 'ai' ? 'AI answer' : answer.mode === 'offline' ? 'Offline mode (quoted passages)' : ''}</small></div>}
    </section>
  </>
}

function StudyPlanPage({ recommendations }: { recommendations: Recommendation[] }) {
  const [completed, setCompleted] = useState<string[]>([])
  return <><PageHeading eyebrow="YOUR NEXT STEPS" title="Study plan" description="A simple starting plan. The production version will schedule tasks using mastery, prerequisites, and revision urgency."/>
    <div className="study-plan">{recommendations.map((r, i) => <div className={`study-task ${completed.includes(r.topic) ? 'task-completed' : ''}`} key={r.topic}><div className="task-check"><CheckCircle2 size={18}/></div><div className="task-content"><span>SESSION {i+1} · {r.priority.toUpperCase()} PRIORITY</span><h3>{r.topic}</h3><p>{r.action}</p><small>{r.reason}</small></div><button className="secondary-button" aria-pressed={completed.includes(r.topic)} onClick={() => setCompleted(old => old.includes(r.topic) ? old.filter(topic => topic !== r.topic) : [...old, r.topic])}>{completed.includes(r.topic) ? 'Completed · undo' : 'Mark complete'}</button></div>)}</div>
  </>
}

function PageHeading({ eyebrow, title, description }: { eyebrow: string; title: string; description: string }) {
  return <div className="page-heading"><div className="eyebrow"><Activity size={14}/>{eyebrow}</div><h1>{title}</h1><p>{description}</p></div>
}

function AuthDialog({ open, close, email, setEmail, password, setPassword, mode, setMode, submit, error, busy }: {
  open: boolean; close: () => void; email: string; setEmail: (v: string) => void; password: string; setPassword: (v: string) => void;
  mode: 'login' | 'register'; setMode: (v: 'login' | 'register') => void; submit: (e: React.FormEvent) => void; error: string; busy: boolean
}) {
  return <div className={`auth-overlay ${open ? 'show' : ''}`} id="auth-dialog" onClick={e => { if (e.target === e.currentTarget) close() }}><form className="auth-dialog" onSubmit={e => { void submit(e); }}><button type="button" className="auth-close icon-button" onClick={close} aria-label="Close"><X size={18}/></button><div className="brand-mark auth-mark"><BrainCircuit size={24}/></div><h2>{mode === 'login' ? 'Welcome back' : 'Create your account'}</h2><p>{mode === 'login' ? 'Sign in to upload material, take quizzes and track progress.' : 'Create a free account. Your materials and progress are private to you.'}</p><label>Email<input type="email" required value={email} onChange={e => setEmail(e.target.value)} placeholder="you@example.com"/></label><label>Password<input type="password" required minLength={8} value={password} onChange={e => setPassword(e.target.value)} placeholder="At least 8 characters"/></label>{error && <div className="form-error">{error}</div>}<button className="primary-button full-width" disabled={busy}>{busy ? 'Please wait...' : mode === 'login' ? 'Sign in' : 'Create account'}</button><button type="button" className="switch-auth" onClick={() => setMode(mode === 'login' ? 'register' : 'login')}>{mode === 'login' ? 'New here? Create an account' : 'Already registered? Sign in'}</button></form></div>
}
