// Exampage.js
import { useEffect, useMemo, useState } from "react";
import "./Shared.css";
import "./Exampage.css";

/**
 * 남은 시간을 분:초 형식으로 변환
 */
function formatTime(seconds) {
  const mins = Math.floor(seconds / 60);
  const secs = seconds % 60;
  return `${mins}:${secs < 10 ? "0" : ""}${secs}`;
}

export default function ExamPage({
  field,
  selectedModels = [],
  onHome,
  onStudy,
  onLab,
  onTest,
  onBack,
}) {
  const [activeNav, setActiveNav] = useState("Test");
  const navItems = ["Home", "Study", "CAD", "Lab", "Test"];

  // 상태 관리: start | loading | inProgress | result
  const [examState, setExamState] = useState("start");
  const [questions, setQuestions] = useState([]); // DB에서 가져온 문제들
  const [currentQ, setCurrentQ] = useState(0);
  const [userAnswers, setUserAnswers] = useState({});
  const [timeLeft, setTimeLeft] = useState(30 * 60); // 30분

  // ✅ DB에서 문제 가져오기
  const fetchQuestions = async () => {
    if (selectedModels.length === 0) return;
    setExamState("loading");

    try {
      const ids = selectedModels.map((m) => m.id).join(",");
      const res = await fetch(`/api/models/exam?modelIds=${ids}`);
      
      if (!res.ok) throw new Error("문제를 불러오는데 실패했습니다.");
      
      const data = await res.json();
      setQuestions(data); // DB 데이터: { question, options, answer, modelTitle ... }

      // 시험 시작 설정
      setExamState("inProgress");
      setCurrentQ(0);
      setUserAnswers({});
      setTimeLeft(30 * 60);
    } catch (err) {
      alert("오류: " + err.message);
      setExamState("start");
    }
  };

  // ✅ 타이머 작동
  useEffect(() => {
    if (examState !== "inProgress") return;

    const timer = setInterval(() => {
      setTimeLeft((prev) => {
        if (prev <= 1) {
          clearInterval(timer);
          handleSubmit(); // 시간 종료 시 자동 제출
          return 0;
        }
        return prev - 1;
      });
    }, 1000);

    return () => clearInterval(timer);
  }, [examState]);

  const handleNav = (item) => {
    if (item === "CAD") {
      alert("페이지 준비중입니다");
      return;
    }
    setActiveNav(item);
    if (item === "Home") onHome();
    if (item === "Study") onStudy();
    if (item === "Lab") onLab?.();
    if (item === "Test") onTest?.();
  };

  const startExam = () => {
    fetchQuestions(); // DB 호출
  };

  const selectOption = (qIdx, optIdx) => {
    setUserAnswers((prev) => ({ ...prev, [qIdx]: optIdx }));
  };

  const goToQuestion = (idx) => {
    setCurrentQ(idx);
  };

  const handleSubmit = () => {
    setExamState("result");
  };

  const retryExam = () => {
    fetchQuestions(); // 다시 풀기 시 새로운 문제 로드
  };

  // 결과 계산 (DB 필드명: answer)
  const correctCount = useMemo(() => {
    let cnt = 0;
    questions.forEach((q, i) => {
      if (userAnswers[i] === q.answer) cnt++;
    });
    return cnt;
  }, [questions, userAnswers]);

  const score = useMemo(() => {
    if (questions.length === 0) return 0;
    return Math.round((correctCount / questions.length) * 100);
  }, [correctCount, questions.length]);

  // ✅ [추가 기능] 가장 많이 틀린 모델(취약점) 분석
  const weakModel = useMemo(() => {
    if (!questions || questions.length === 0) return null;

    const wrongCounts = {};
    let totalWrong = 0;

    questions.forEach((q, i) => {
      if (userAnswers[i] !== q.answer) {
        // DTO의 modelTitle 필드 사용 (없으면 '기타' 처리)
        const title = q.modelTitle || "기타";
        wrongCounts[title] = (wrongCounts[title] || 0) + 1;
        totalWrong++;
      }
    });

    if (totalWrong === 0) return null; // 다 맞았으면 취약점 없음

    // 가장 오답이 많은 모델 찾기
    let maxWrong = -1;
    let worstModelName = null;
    
    Object.entries(wrongCounts).forEach(([title, count]) => {
      if (count > maxWrong) {
        maxWrong = count;
        worstModelName = title;
      }
    });
    
    return worstModelName;
  }, [questions, userAnswers]);


  // ─── 시작 화면 (디자인 원본 유지) ───
  if (examState === "start" || examState === "loading") {
    return (
      <>
        <div className="noise-overlay" />
        <div className="ambient-glow glow-1" />
        <div className="ambient-glow glow-2" />

        <div className="page-wrapper">
          <nav className="nav">
            <div className="inner">
              <div className="nav-logo" onClick={onHome}>
                <span className="nav-logo-text">SIMVEX</span>
              </div>
              <div className="nav-links">
                {navItems.map((item) => (
                  <button
                    key={item}
                    className={`nav-link${activeNav === item ? " active" : ""}`}
                    onClick={() => handleNav(item)}
                  >
                    {item}
                  </button>
                ))}
              </div>
            </div>
          </nav>

          <section className="exam-body">
            <div className="inner">
              <div className="exam-start">
                <div className="exam-start-icon">
                  <svg width="64" height="64" viewBox="0 0 64 64" fill="none">
                    <rect x="12" y="8" width="40" height="48" rx="4" fill="rgba(37,99,235,0.2)" stroke="#2563eb" strokeWidth="2" />
                    <line x1="20" y1="20" x2="44" y2="20" stroke="#2563eb" strokeWidth="2" strokeLinecap="round" />
                    <line x1="20" y1="28" x2="44" y2="28" stroke="#2563eb" strokeWidth="2" strokeLinecap="round" />
                    <line x1="20" y1="36" x2="36" y2="36" stroke="#2563eb" strokeWidth="2" strokeLinecap="round" />
                    <circle cx="22" cy="44" r="2" fill="#2563eb" />
                    <circle cx="30" cy="44" r="2" fill="#2563eb" />
                    <circle cx="38" cy="44" r="2" fill="#2563eb" />
                  </svg>
                </div>

                <h2 className="exam-start-title">{field} 모의고사</h2>

                <div className="exam-selected-products">
                  <div className="exam-selected-label">선택된 모델</div>
                  <div className="exam-selected-list">
                    {selectedModels.map((m, i) => (
                      <span key={i}>
                        {m.title}
                        {i < selectedModels.length - 1 ? ", " : ""}
                      </span>
                    ))}
                  </div>
                </div>

                <div className="exam-start-info">
                  <div className="exam-info-item">
                    <div className="exam-info-label">문제 수</div>
                    <div className="exam-info-value">20문제</div>
                  </div>
                  <div className="exam-info-item">
                    <div className="exam-info-label">시험 시간</div>
                    <div className="exam-info-value">30분</div>
                  </div>
                </div>

                <p className="exam-start-desc">
                  선택한 모델에 대한 종합 문제가 출제됩니다.
                  <br />
                  제한 시간 내에 최선을 다해 풀어보세요!
                </p>

                <button className="exam-start-btn" onClick={startExam} disabled={examState === "loading"}>
                  {examState === "loading" ? "문제 생성 중..." : "시험 시작"}
                </button>

                <button className="exam-back-btn" onClick={onBack} disabled={examState === "loading"}>
                  모델 다시 선택
                </button>
              </div>
            </div>
          </section>
        </div>
      </>
    );
  }

  // ─── 시험 진행 화면 (DB 필드명 적용: q.question, q.options) ───
  if (examState === "inProgress") {
    const q = questions[currentQ];
    if (!q) return null;

    const progress = ((currentQ + 1) / questions.length) * 100;
    const answeredCount = Object.keys(userAnswers).length;

    return (
      <>
        <div className="noise-overlay" />
        <div className="ambient-glow glow-1" />
        <div className="ambient-glow glow-2" />

        <div className="page-wrapper">
          <nav className="nav">
            <div className="inner">
              <div className="nav-logo" onClick={onHome}>
                <div className="nav-logo-icon">
                  <svg viewBox="0 0 18 18" fill="none" stroke="#fff" strokeWidth="2" strokeLinecap="round">
                    <circle cx="9" cy="9" r="3" />
                    <path d="M9 2v2M9 14v2M2 9h2M14 9h2" />
                  </svg>
                </div>
                <span className="nav-logo-text">SIMVEX</span>
              </div>
            </div>
          </nav>

          <section className="exam-body">
            <div className="inner">
              <div className="exam-progress">
                <div className="exam-header">
                  <div className="exam-progress-info">
                    문제 {currentQ + 1} / {questions.length} (답변: {answeredCount})
                  </div>
                  <div className="exam-timer">
                    ⏱️ {formatTime(timeLeft)}
                  </div>
                </div>

                <div className="exam-progress-bar-bg">
                  <div className="exam-progress-bar-fill" style={{ width: `${progress}%` }} />
                </div>

                <div className="exam-question-card">
                  <div className="exam-question-num">
                    문제 {currentQ + 1}
                    {/* 모델 이름이 있으면 작게 표시 */}
                    {q.modelTitle && (
                      <span style={{ fontSize: "0.8em", color: "#64748b", marginLeft: "10px", fontWeight: "normal" }}>
                         | {q.modelTitle}
                      </span>
                    )}
                  </div>
                  
                  {/* DB 데이터: question */}
                  <div className="exam-question-text">{q.question}</div>

                  <div className="exam-options">
                    {/* DB 데이터: options */}
                    {q.options && q.options.map((opt, i) => (
                      <button
                        key={i}
                        className={`exam-option${userAnswers[currentQ] === i ? " selected" : ""}`}
                        onClick={() => selectOption(currentQ, i)}
                      >
                        <div className="exam-option-num">{i + 1}</div>
                        <div className="exam-option-text">{opt}</div>
                        {userAnswers[currentQ] === i && <div className="exam-option-check">✓</div>}
                      </button>
                    ))}
                  </div>
                </div>

                <div className="exam-nav-btns">
                  <button
                    className="exam-prev-btn"
                    onClick={() => setCurrentQ((p) => Math.max(0, p - 1))}
                    disabled={currentQ === 0}
                  >
                    ◀ 이전
                  </button>

                  {currentQ < questions.length - 1 ? (
                    <button className="exam-next-btn" onClick={() => setCurrentQ((p) => p + 1)}>
                      다음 ▶
                    </button>
                  ) : (
                    <button className="exam-submit-btn" onClick={handleSubmit}>
                      제출하기
                    </button>
                  )}
                </div>

                <div className="exam-question-nav">
                  {questions.map((_, i) => (
                    <button
                      key={i}
                      className={`exam-q-num${currentQ === i ? " active" : ""}${
                        userAnswers[i] !== undefined ? " answered" : ""
                      }`}
                      onClick={() => goToQuestion(i)}
                    >
                      {i + 1}
                    </button>
                  ))}
                </div>
              </div>
            </div>
          </section>
        </div>
      </>
    );
  }

  // ─── 결과 화면 (디자인 원본 유지 + 취약점 분석 추가) ───
  if (examState === "result") {
    let resultMsg = "";
    if (score >= 90) resultMsg = "🎉 훌륭합니다! 완벽에 가까운 점수입니다!";
    else if (score >= 70) resultMsg = "👍 잘 하셨습니다! 좋은 성적입니다.";
    else if (score >= 50) resultMsg = "💪 조금만 더 공부하면 더 좋은 결과가 있을 거예요.";
    else resultMsg = "📚 다시 한번 복습하고 도전해보세요!";

    return (
      <>
        <div className="noise-overlay" />
        <div className="ambient-glow glow-1" />
        <div className="ambient-glow glow-2" />

        <div className="page-wrapper">
          <nav className="nav">
            <div className="inner">
              <div className="nav-logo" onClick={onHome}>
                <div className="nav-logo-icon">
                  <svg viewBox="0 0 18 18" fill="none" stroke="#fff" strokeWidth="2" strokeLinecap="round">
                    <circle cx="9" cy="9" r="3" />
                    <path d="M9 2v2M9 14v2M2 9h2M14 9h2" />
                  </svg>
                </div>
                <span className="nav-logo-text">SIMVEX</span>
              </div>
              <div className="nav-links">
                {navItems.map((item) => (
                  <button
                    key={item}
                    className={`nav-link${activeNav === item ? " active" : ""}`}
                    onClick={() => handleNav(item)}
                  >
                    {item}
                  </button>
                ))}
              </div>
            </div>
          </nav>

          <section className="exam-body">
            <div className="inner">
              <div className="exam-result">
                <div className="exam-result-icon">
                  <svg width="80" height="80" viewBox="0 0 80 80" fill="none">
                    <circle cx="40" cy="40" r="36" fill="rgba(34,197,94,0.15)" stroke="#22c55e" strokeWidth="3" />
                    <path
                      d="M25 40 L35 50 L55 30"
                      stroke="#22c55e"
                      strokeWidth="4"
                      strokeLinecap="round"
                      strokeLinejoin="round"
                    />
                  </svg>
                </div>

                <h2 className="exam-result-title">시험 완료!</h2>

                <div className="exam-result-score">
                  <span className="exam-score-num">{correctCount}</span>
                  <span className="exam-score-den">/ {questions.length}</span>
                </div>

                <div className="exam-result-percent">{score}점</div>

                {/* ✅ [추가] 취약점 분석 UI */}
                {weakModel && (
                  <div style={{
                    marginTop: "24px",
                    marginBottom: "10px",
                    padding: "16px",
                    background: "rgba(239, 68, 68, 0.1)", 
                    border: "1px solid rgba(239, 68, 68, 0.3)",
                    borderRadius: "8px",
                    maxWidth: "400px",
                    marginLeft: "auto",
                    marginRight: "auto",
                    textAlign: "center"
                  }}>
                    <div style={{ fontSize: "14px", color: "#fca5a5", marginBottom: "4px" }}>
                      집중 학습 필요
                    </div>
                    <div style={{ fontSize: "18px", fontWeight: "bold", color: "#fff" }}>
                      {weakModel}
                    </div>
                    <div style={{ fontSize: "13px", color: "#d1d5db", marginTop: "4px" }}>
                      이 모델에서 오답이 가장 많이 발생했습니다.
                    </div>
                  </div>
                )}

                <p className="exam-result-msg">{resultMsg}</p>

                <div className="exam-result-btns">
                  <button className="exam-retry-btn" onClick={retryExam}>
                    다시 풀기
                  </button>
                  <button className="exam-home-btn" onClick={onHome}>
                    홈으로
                  </button>
                </div>
              </div>
            </div>
          </section>
        </div>
      </>
    );
  }

  return null;
}