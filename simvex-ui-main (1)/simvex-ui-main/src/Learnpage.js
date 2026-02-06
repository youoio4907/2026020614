import { useState, useRef, useCallback, useEffect } from "react";
import "./Shared.css";
import "./Learnpage.css";
import ThreeViewer from "./ThreeViewer";

/* ════════════════════════════════════════════ */
/*  부품 SVG 아이콘들                             */
/* ════════════════════════════════════════════ */

const PartFan = () => (
  <svg viewBox="0 0 40 40" fill="none">
    <circle cx="20" cy="20" r="18" fill="#1a2a3a" stroke="#3a6a8a" strokeWidth="1" />
    {Array.from({ length: 10 }).map((_, i) => {
      const a = (i / 10) * 360,
        r = (a * Math.PI) / 180;
      return (
        <line
          key={i}
          x1={20 + 3 * Math.cos(r)}
          y1={20 + 3 * Math.sin(r)}
          x2={20 + 14 * Math.cos(r)}
          y2={20 + 14 * Math.sin(r)}
          stroke="#7dd3e0"
          strokeWidth="2.2"
          strokeLinecap="round"
        />
      );
    })}
    <circle cx="20" cy="20" r="3.5" fill="#00e5ff" />
  </svg>
);
const PartCompressor = () => (
  <svg viewBox="0 0 40 40" fill="none">
    <rect x="4" y="10" width="10" height="20" rx="2" fill="#4a8aaa" opacity="0.7" />
    <rect x="15" y="8" width="10" height="24" rx="2" fill="#5aaac4" opacity="0.8" />
    <rect x="26" y="6" width="10" height="28" rx="2" fill="#7dd3e0" />
    <line x1="4" y1="20" x2="36" y2="20" stroke="rgba(255,255,255,0.15)" strokeWidth="0.8" />
  </svg>
);
const PartCombustor = () => (
  <svg viewBox="0 0 40 40" fill="none">
    <ellipse cx="20" cy="22" rx="12" ry="14" fill="#1a2a3a" stroke="#6a4a2a" strokeWidth="1" />
    <ellipse cx="20" cy="24" rx="7" ry="8" fill="#ff8844" opacity="0.7" />
    <ellipse cx="20" cy="25" rx="4" ry="5" fill="#ffcc44" opacity="0.85" />
    <ellipse cx="20" cy="26" rx="2" ry="3" fill="#fff3c0" />
  </svg>
);
const PartTurbine = () => (
  <svg viewBox="0 0 40 40" fill="none">
    <circle cx="20" cy="20" r="16" fill="#1a2a3a" stroke="#8a6aaa" strokeWidth="1" />
    {Array.from({ length: 8 }).map((_, i) => {
      const a = (i / 8) * 360 + 10,
        r = (a * Math.PI) / 180;
      return (
        <path
          key={i}
          d={`M${20 + 3 * Math.cos(r)},${20 + 3 * Math.sin(r)} Q${20 + 9 * Math.cos(r + 0.3)},${
            20 + 9 * Math.sin(r + 0.3)
          } ${20 + 13 * Math.cos(r)},${20 + 13 * Math.sin(r)}`}
          stroke="#a88bd4"
          strokeWidth="2.2"
          fill="none"
          strokeLinecap="round"
        />
      );
    })}
    <circle cx="20" cy="20" r="3" fill="#c4a8f0" />
  </svg>
);
const PartNozzle = () => (
  <svg viewBox="0 0 40 40" fill="none">
    <path d="M8,12 L28,8 L32,20 L28,32 L8,28 Z" fill="#2a4060" stroke="#5a8aaa" strokeWidth="1" />
    <path d="M28,12 L36,14 L36,26 L28,28" fill="#1a3050" stroke="#4a7a8a" strokeWidth="0.8" />
    <ellipse cx="37" cy="20" rx="4" ry="3" fill="#ffaa44" opacity="0.6" />
    <ellipse cx="39" cy="20" rx="2" ry="1.5" fill="#fff3c0" opacity="0.7" />
  </svg>
);
const PartCase = () => (
  <svg viewBox="0 0 40 40" fill="none">
    <ellipse cx="20" cy="20" rx="14" ry="17" fill="#1e3550" stroke="#4a7a9a" strokeWidth="1.2" />
    <ellipse cx="20" cy="20" rx="5" ry="6" fill="#0a1520" stroke="#2a5570" strokeWidth="0.8" />
    <path d="M6,20 L2,20" stroke="#5abcd0" strokeWidth="1.5" strokeLinecap="round" />
    <circle cx="2" cy="20" r="2" fill="#2a4a6a" stroke="#5abcd0" strokeWidth="0.8" />
  </svg>
);
const PART_ICONS = [PartFan, PartCompressor, PartCombustor, PartTurbine, PartNozzle, PartCase];

/* 3D 뷰어 SVG */
const ViewerEngineSVG = () => (
  <svg viewBox="0 0 380 300" fill="none">
    <circle cx="190" cy="150" r="50" fill="#4a7a9a" />
    <text x="190" y="160" textAnchor="middle" fill="#fff" fontSize="16">
      3D 뷰어
    </text>
  </svg>
);

const PRODUCT_INFO = {
  title: "제트 엔진 — 완제품 개요",
  desc: "제트 엔진은 가스 터빈 원리를 이용한 항공기 추진 장치로, 공기를 흡입→압축→연소→배기하는 순환 구조입니다.",
  sections: [
    { title: "작동 원리", desc: "브레이톤 순환(Brayton Cycle)을 기반으로 작동합니다." },
    { title: "주요 적용 분야", desc: "상업 항공기, 군용 전투기 등에 사용됩니다." },
  ],
};

const difficultyPercent = 72; // 0~100


const QUIZ_DATA = [
  { 
    question: "제트 엔진의 주요 구성 요소가 아닌 것은?", 
    options: ["압축기", "연소실", "터빈", "프로펠러"], 
    answer: 3 
  },
  { 
    question: "압축기의 주요 역할은 무엇인가?", 
    options: ["공기를 냉각", "공기를 고압으로 압축", "연료를 분사", "배기가스 배출"], 
    answer: 1 
  },
  { 
    question: "제트 엔진의 작동 원리는?", 
    options: ["카르노 사이클", "브레이톤 사이클", "랭킨 사이클", "오토 사이클"], 
    answer: 1 
  },
  { 
    question: "터빈의 주요 기능은?", 
    options: ["연료 연소", "공기 흡입", "압축기 구동", "추력 발생"], 
    answer: 2 
  },
  { 
    question: "노즐에서 발생하는 것은?", 
    options: ["압축 공기", "고온 가스", "추진력", "냉각수"], 
    answer: 2 
  }
];

const INIT_MEMOS = [];

/* ════════════════════════════════════════════ */
/*  LearnPage                                   */
/* ════════════════════════════════════════════ */
export default function LearnPage({ onHome, onStudy, selectedModel, onLab, onTest, onBack }) {
  const [activeNav, setActiveNav] = useState("Study");
  const [activeTab, setActiveTab] = useState("조립품");

  /* ✅ 부품 관련 상태 */
  const [parts, setParts] = useState([]);
  const [partsLoading, setPartsLoading] = useState(false);
  const [partsErr, setPartsErr] = useState("");
  const [selectedPartKey, setSelectedPartKey] = useState(null);

  /* ✅ 조립/분해 슬라이더 */
  const [assemblyProgress, setAssemblyProgress] = useState(100); // 100 = 완전 조립, 0 = 완전 분해

  const [showInfoPanel, setShowInfoPanel] = useState(true);
  const [showProductPanel, setShowProductPanel] = useState(true);
  const [memos, setMemos] = useState(INIT_MEMOS);
  const [expandedMemo, setExpandedMemo] = useState(null);
  const [quizIdx, setQuizIdx] = useState(0);
  const [quizSelected, setQuizSelected] = useState(null);
  const [quizResults, setQuizResults] = useState([]);
  const [quizSubmitted, setQuizSubmitted] = useState(false);
  const [quizFinished, setQuizFinished] = useState(false);

  /* ── AI 채팅 ── */
  const [chatMsgs, setChatMsgs] = useState([{ role: "ai", text: "안녕하세요! 궁금한 점이 있으신가요?" }]);
  const [chatInput, setChatInput] = useState("");
  const [isAiLoading, setIsAiLoading] = useState(false);
  const chatBottomRef = useRef(null);

  const navItems = ["Home", "Study", "CAD", "Lab", "Test"];
  const tabs = ["조립품", "퀴즈"];

  /* 드래그 스크롤 */
  const scrollRef = useRef(null);
  const isDragging = useRef(false);
  const startX = useRef(0);
  const scrollLeft = useRef(0);

  const onMouseDown = useCallback((e) => {
    if (e.target.tagName === "TEXTAREA") return;
    isDragging.current = true;
    startX.current = e.pageX - scrollRef.current.offsetLeft;
    scrollLeft.current = scrollRef.current.scrollLeft;
  }, []);
  const onMouseLeave = useCallback(() => {
    isDragging.current = false;
  }, []);
  const onMouseUp = useCallback(() => {
    isDragging.current = false;
  }, []);
  const onMouseMove = useCallback((e) => {
    if (!isDragging.current) return;
    e.preventDefault();
    scrollRef.current.scrollLeft = scrollLeft.current - (e.pageX - scrollRef.current.offsetLeft - startX.current);
  }, []);

  const resetQuiz = () => {
    setQuizIdx(0);
    setQuizSelected(null);
    setQuizResults([]);
    setQuizSubmitted(false);
    setQuizFinished(false);
  };
  const handleTabClick = (t) => {
    setActiveTab(t);
    if (t === "퀴즈") resetQuiz();
  };

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

  /* ✅ 부품 키 생성 (안정적 선택) */
  const getPartKey = (part) => {
    if (part?.id) return `id:${part.id}`;
    if (part?.meshName) return `mesh:${part.meshName}`;
    return null;
  };

  /* ✅ 선택된 부품 계산 */
  const selectedPart = parts.find((p) => getPartKey(p) === selectedPartKey) || parts[0] || null;

  /* ✅ modelUrl 정규화 함수 */
  const normalizeModelUrl = (selectedModel) => {
    if (!selectedModel?.modelUrl) return null;
    let modelUrl = selectedModel.modelUrl;

    console.log("[Learnpage] normalizeModelUrl called:");
    console.log("  - selectedModel.title:", selectedModel.title);
    console.log("  - selectedModel.modelUrl:", modelUrl);

    // 공백을 언더스코어로 변환 (파일 시스템 호환)
    const safeTitle = selectedModel.title.replace(/ /g, "_");
    console.log("  - safeTitle (공백→_):", safeTitle);

    let result;

    // URL 디코딩 (Robot%20Arm → Robot Arm → Robot_Arm)
    if (modelUrl.includes("%20")) {
      modelUrl = decodeURIComponent(modelUrl);
      modelUrl = modelUrl.replace(/ /g, "_");
      console.log("  - URL decoded & space fixed:", modelUrl);
    }

    // 절대 경로인 경우
    if (modelUrl.startsWith("/") || modelUrl.startsWith("http")) {
      result = modelUrl;

      // 경로가 폴더로 끝나면 (/) 파일명 추가
      if (result.endsWith("/")) {
        result = `${result}${safeTitle}.glb`;
        console.log("  - Added filename:", result);
      }

      console.log("  → Final absolute path:", result);
    } else {
      // 상대 경로인 경우
      result = `/assets/3d/${safeTitle}/${modelUrl}`;
      console.log("  → Combined path:", result);
    }

    return result;
  };

  /* ✅ 부품 로드 (모델 변경 시) */
  useEffect(() => {
    let ignore = false;

    (async () => {
      if (!selectedModel?.id) {
        setParts([]);
        setPartsErr("");
        return;
      }

      setPartsLoading(true);
      setPartsErr("");
      try {
        const res = await fetch(`/api/models/${selectedModel.id}/parts`);
        if (!res.ok) throw new Error(`HTTP ${res.status}`);
        const data = await res.json();
        if (!ignore) {
          const loadedParts = Array.isArray(data) ? data : [];
          setParts(loadedParts);

          // 첫 부품 자동 선택
          if (loadedParts.length > 0) {
            setSelectedPartKey(getPartKey(loadedParts[0]));
          } else {
            setSelectedPartKey(null);
          }
        }
      } catch (e) {
        if (!ignore) {
          setPartsErr(e.message || "부품 목록 로드 실패");
          setParts([]);
        }
      } finally {
        if (!ignore) setPartsLoading(false);
      }
    })();

    return () => {
      ignore = true;
    };
  }, [selectedModel?.id]);

  /* ✅ 채팅 스크롤 하단 고정 */
  useEffect(() => {
    chatBottomRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [chatMsgs, isAiLoading]);

  /* ✅ AI 요청 (슬림 notes) */
  const sendChat = async () => {
    const question = chatInput.trim();
    if (!question || isAiLoading) return;

    setChatInput("");
    const userMsg = { role: "user", text: question };
    setChatMsgs((prev) => [...prev, userMsg]);
    setIsAiLoading(true);

    try {
      if (!selectedModel?.id) {
        throw new Error("선택된 모델이 없습니다.");
      }

      const meshName = selectedPart?.meshName || selectedModel?.title || selectedModel?.modelUrl || null;

      const notesObj = {
        model: {
          id: selectedModel.id,
          title: selectedModel.title,
          modelUrl: selectedModel.modelUrl,
        },
        part: selectedPart
          ? {
              id: selectedPart.id,
              meshName: selectedPart.meshName,
              name: selectedPart.name || selectedPart.title,
              type: selectedPart.type,
              description: selectedPart.description || selectedPart.desc,
              function: selectedPart.function,
              material: selectedPart.material,
              structure: selectedPart.structure,
              fileUrl: selectedPart.fileUrl || selectedPart.content?.fileUrl,
            }
          : null,
        ui: {
          activeTab,
          assemblyProgress,
        },
      };

      const payload = {
        modelId: selectedModel.id,
        meshName,
        question,
        notes: JSON.stringify(notesObj),
      };

      const res = await fetch("/api/ai/ask", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(payload),
      });

      if (!res.ok) {
        const text = await res.text().catch(() => "");
        throw new Error(`HTTP ${res.status} ${text}`.trim());
      }

      const data = await res.json().catch(() => null);
      const answer = (data && (data.answer || data.message || data.content)) || (typeof data === "string" ? data : null) || "응답 파싱 실패";

      setChatMsgs((prev) => [...prev, { role: "ai", text: answer }]);
    } catch (err) {
      setChatMsgs((prev) => [...prev, { role: "ai", text: `오류: ${err.message || "요청 실패"}` }]);
    } finally {
      setIsAiLoading(false);
    }
  };

  /* 메모장 */
  const addMemo = () => setMemos((prev) => [...prev, { title: "", content: "" }]);
  const deleteMemo = (idx) => {
    setMemos((prev) => prev.filter((_, i) => i !== idx));
    if (expandedMemo === idx) setExpandedMemo(null);
  };
  const updateMemo = (idx, field, val) => {
    setMemos((prev) => prev.map((m, i) => (i === idx ? { ...m, [field]: val } : m)));
  };

  /* 퀴즈 */
  const submitQuiz = () => {
    setQuizSubmitted(true);
    const correct = QUIZ_DATA[quizIdx].answer === quizSelected;
    setQuizResults((prev) => [...prev, { question: QUIZ_DATA[quizIdx].question, correct }]);
  };
  const nextQuestion = () => {
    if (quizIdx < QUIZ_DATA.length - 1) {
      setQuizIdx(quizIdx + 1);
      setQuizSelected(null);
      setQuizSubmitted(false);
    } else {
      setQuizFinished(true);
    }
  };

  /* ✅ 부품 선택 핸들러 */
  const handlePartSelect = (part) => {
    setSelectedPartKey(getPartKey(part));
  };

  /* ✅ PDF 리포트 생성 */
  const generatePdfReport = () => {
    console.log("PDF 리포트 생성 요청");
    alert("PDF 리포트 생성 기능은 추후 구현 예정입니다.");
    // TODO: 실제 PDF 생성 API 호출
  };

  return (
    <>
      <div className="noise-overlay" />
      <div className="ambient-glow glow-1" />
      <div className="ambient-glow glow-2" />

      <div className="page-wrapper">
        {/* NAV */}
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
                <button key={item} className={`nav-link${activeNav === item ? " active" : ""}`} onClick={() => handleNav(item)}>
                  {item}
                </button>
              ))}
            </div>
          </div>
        </nav>

        {/* LEARN BODY */}
        <section className="learn-body">
          <div className="inner">
            {/* 상단: 탭 + PDF 버튼 */}
            <div className="learn-header-row">

              <div className="learn-tabs-wrap">
                {/* ⬅ 뒤로가기 버튼 */}
                <button className="learn-back-btn" onClick={onBack} title="뒤로가기">
                  ‹
                </button>

                <div className="learn-tabs">
                {tabs.map((t) => (
                  <button key={t} className={`learn-tab${activeTab === t ? " active" : ""}`} onClick={() => handleTabClick(t)}>
                    {t}
                  </button>
                ))}
                </div>

              </div>

              <button className="pdf-report-btn" onClick={generatePdfReport}>
                <svg width="14" height="14" viewBox="0 0 16 16" fill="none" stroke="currentColor" strokeWidth="1.5">
                  <path d="M4 2v12M12 2v12M2 8h12" strokeLinecap="round" />
                  <rect x="3" y="1" width="10" height="14" rx="1" fill="none" />
                </svg>
                PDF 리포트 생성
              </button>
            </div>

            <div className="learn-content-row">
              <div className="viewer-panel">
                {activeTab === "퀴즈" ? (
                  <div className="quiz-container">
                    {!quizFinished ? (
                      <>
                        {/* 퀴즈 헤더 */}
                        <div className="quiz-header">
                          <h2 className="quiz-main-title">제트 엔진 퀴즈</h2>
                          <div className="quiz-progress">
                            <div className="quiz-progress-bar">
                              <div 
                                className="quiz-progress-fill" 
                                style={{ width: `${((quizIdx + 1) / QUIZ_DATA.length) * 100}%` }}
                              />
                            </div>
                            <div className="quiz-progress-text">{quizIdx + 1}/{QUIZ_DATA.length}</div>
                          </div>
                        </div>

                        {/* 퀴즈 질문 */}
                        <div className="quiz-question-section">
                          <div className="quiz-question">{QUIZ_DATA[quizIdx].question}</div>
                        </div>

                        {/* 퀴즈 옵션 */}
                        <div className="quiz-options-grid">
                          {QUIZ_DATA[quizIdx].options.map((opt, i) => {
                            let optionClass = "quiz-option-new";
                            
                            if (quizSubmitted) {
                              if (i === QUIZ_DATA[quizIdx].answer) {
                                optionClass += " correct";
                              } else if (i === quizSelected) {
                                optionClass += " wrong";
                              }
                            } else if (quizSelected === i) {
                              optionClass += " selected";
                            }

                            return (
                              <button
                                key={i}
                                className={optionClass}
                                onClick={() => !quizSubmitted && setQuizSelected(i)}
                                disabled={quizSubmitted}
                              >
                                <span className="quiz-option-number">{i + 1}.</span>
                                <span className="quiz-option-text">{opt}</span>
                              </button>
                            );
                          })}
                        </div>

                        

                        {/* 피드백 및 버튼 */}
                        <div className="quiz-feedback">
                          {quizSubmitted && (
                            <div className={`quiz-feedback-box ${quizResults[quizResults.length - 1].correct ? "correct" : "wrong"}`}>
                              {quizResults[quizResults.length - 1].correct 
                                ? `✓ 정답 해설 – 정답은 ${QUIZ_DATA[quizIdx].options[QUIZ_DATA[quizIdx].answer]}입니다.`
                                : `✗ 정답 해설 – 정답은 ${QUIZ_DATA[quizIdx].options[QUIZ_DATA[quizIdx].answer]}입니다.`
                              }
                            </div>
                          )}
                        </div>

                        {/* 하단 버튼 */}
                        <div className="quiz-actions">
                          <button className="quiz-btn-secondary" onClick={resetQuiz}>
                            이전 문제
                          </button>
                          {!quizSubmitted ? (
                            <button 
                              className="quiz-btn-primary" 
                              onClick={submitQuiz} 
                              disabled={quizSelected === null}
                            >
                              다음 문제
                            </button>
                          ) : (
                            <button className="quiz-btn-primary" onClick={nextQuestion}>
                              {quizIdx < QUIZ_DATA.length - 1 ? "다음 문제" : "결과 보기"}
                            </button>
                          )}
                        </div>
                      </>
                    ) : (
                      <div className="quiz-final-result">
                        <div className="quiz-final-title">퀴즈 완료!</div>
                        <div className="quiz-final-score">
                          <span className="quiz-score-big">{quizResults.filter((r) => r.correct).length}</span>
                          <span className="quiz-score-divider">/</span>
                          <span className="quiz-score-total">{quizResults.length}</span>
                        </div>
                        <div className="quiz-final-message">
                          {quizResults.filter((r) => r.correct).length === quizResults.length 
                            ? "완벽합니다! 🎉" 
                            : quizResults.filter((r) => r.correct).length >= quizResults.length * 0.6
                            ? "잘하셨습니다! 👏"
                            : "다시 도전해보세요! 💪"
                          }
                        </div>
                        <button className="quiz-btn-restart" onClick={resetQuiz}>
                          다시 풀기
                        </button>
                      </div>
                    )}
                  </div>
                ) : (

                  <div className="viewer-body-row">
                    {/* 왼쪽 토글 버튼 (완제품 개요 복원) */}
                    {!showProductPanel && (
                      <button className="viewer-toggle-btn-side left" onClick={() => setShowProductPanel(true)} title="완제품 개요 보기">
                        <svg width="16" height="16" viewBox="0 0 16 16" fill="none" stroke="currentColor" strokeWidth="2">
                          <path d="M6 2l6 6-6 6" />
                        </svg>
                      </button>
                    )}

                    {/* 좌측: 완제품 개요 */}
                    {showProductPanel && (
                      <div className="viewer-product">
                        <div className="viewer-product-header">
                          <div className="viewer-product-title">완제품 개요</div>
                          <button className="viewer-info-close" onClick={() => setShowProductPanel(false)}>
                            ✕
                          </button>
                        </div>
                        <div className="viewer-product-body">
                          <div className="viewer-product-model-title">{selectedModel?.title || "모델 선택 필요"}</div>
                          <div className="viewer-info-product-desc">
                            {selectedModel?.modelUrl ? `파일: ${selectedModel.modelUrl}` : PRODUCT_INFO.desc}
                          </div>

                          {PRODUCT_INFO.sections.map((section, idx) => (
                            <div key={idx} className="viewer-product-section">
                              <div className="viewer-product-section-title">{section.title}</div>
                              <div className="viewer-product-section-desc">{section.desc}</div>
                            </div>
                          ))}
                        </div>
                      </div>
                    )}

                    {/* 중앙: 3D 뷰어 */}
                    <div className={`viewer-3d${(!showInfoPanel || !showProductPanel) ? " expanded" : ""}`}>
                      <div className="viewer-3d-inner">
                        {/* ✅ ? 도움말 버튼 (hover 시 사용법 표시) */}
                        <div className="viewer-help">
                          <button className="viewer-help-btn" type="button" aria-label="3D 뷰어 사용법">
                            ?
                          </button>

                          <div className="viewer-help-tooltip">
                            <div className="viewer-help-title">3D 뷰어 사용법</div>
                            <div className="viewer-help-line">좌클릭 : 화면 회전</div>
                            <div className="viewer-help-line">우클릭 : 화면 이동</div>
                            <div className="viewer-help-line">휠 : 줌 인/아웃</div>
                          </div>
                        </div>

                        {selectedModel?.modelUrl ? (
                          <ThreeViewer
                            modelUrl={normalizeModelUrl(selectedModel)}
                            parts={parts}
                            selectedPartKey={selectedPartKey}
                            assemblyProgress={assemblyProgress}
                            onPartClick={handlePartSelect}
                          />
                        ) : (
                          <ViewerEngineSVG />
                        )}
                      </div>
                    </div>

                    {/* 조립/분해 슬라이더 */}
                    <div className="assembly-slider-container">
                      <div className="assembly-slider-label">조립 및 분해</div>
                      <div className="assembly-slider-track">
                        <input
                          type="range"
                          min="0"
                          max="100"
                          value={assemblyProgress}
                          onChange={(e) => setAssemblyProgress(Number(e.target.value))}
                          className="assembly-slider"
                        />
                        <div className="assembly-slider-markers">
                          <span className="assembly-slider-marker-label">분해</span>
                          <span className="assembly-slider-marker-label">조립</span>
                        </div>
                      </div>
                    </div>

                    {/* 우측: 부품 설명 */}
                    {showInfoPanel && (
                      <div className="viewer-info">
                        <div className="viewer-info-header">
                          <div className="viewer-info-title">부품 설명</div>
                          <button className="viewer-info-close" onClick={() => setShowInfoPanel(false)}>
                            ✕
                          </button>
                        </div>

                        {partsLoading && <div className="viewer-parts-status">불러오는 중…</div>}
                        {partsErr && <div className="viewer-parts-status error">오류: {partsErr}</div>}

                        {!partsLoading && !partsErr && (
                          /* ✅ 헤더 아래 전체를 스크롤 컨테이너로 감싼다 */
                          <div className="viewer-info-scroll">
                            {/* 부품 썸네일 그리드 */}
                            {parts.length > 0 && (
                              <div className="viewer-parts-grid">
                                {parts.map((part, idx) => {
                                  const partKey = getPartKey(part);
                                  const isActive = partKey === selectedPartKey;
                                  const IconComponent = PART_ICONS[idx % PART_ICONS.length];

                                  return (
                                    <div
                                      key={partKey}
                                      className={`viewer-part-thumb${isActive ? " active" : ""}`}
                                      onClick={() => handlePartSelect(part)}
                                      title={part.meshName}
                                    >
                                      <IconComponent />
                                    </div>
                                  );
                                })}
                              </div>
                            )}

                            {/* 선택된 부품 상세 정보 (새 디자인) */}
                            <div className="viewer-part-detail-new">
                              <div className="part-section">
                                <div className="part-section-title">압축기</div>
                                <div className="part-section-content">
                                  공기를 흡입하여 고압으로 압축하기 위한 장치로 높은 압력을 만들어 연소실로 보냅니다.
                                </div>
                              </div>

                              <div className="part-section">
                                <div className="part-section-title">연소실</div>
                                <div className="part-section-content">
                                  압축된 공기와 연료를 혼합하여 폭발시키는 부분으로 높은 온도와 압력을 만들어 내는 곳입니다.
                                </div>
                              </div>

                              <div className="part-section">
                                <div className="part-section-title">터빈</div>
                                <div className="part-section-content">
                                  연소실에서 나온 고온 고압의 가스를 이용하여 회전하며 압축기를 구동시킵니다.
                                </div>
                              </div>

                              <div className="part-section">
                                <div className="part-section-title">노즐</div>
                                <div className="part-section-content">
                                  터빈을 지나 배출되는 가스를 이용하여 추진력을 발생시키며 비행기를 앞으로 밀어줍니다.
                                </div>
                              </div>

                              
                            </div>
                          </div>
                        )}
                      </div>
                    )}



                    {/* 오른쪽 토글 버튼 (부품 설명 복원) */}
                    {!showInfoPanel && (
                      <button className="viewer-toggle-btn-side right" onClick={() => setShowInfoPanel(true)} title="부품 설명 보기">
                        <svg width="16" height="16" viewBox="0 0 16 16" fill="none" stroke="currentColor" strokeWidth="2">
                          {/*<path d="M6 2l6 6-6 6" />*/}
                          <path d="M10 2L4 8l6 6" />
                        </svg>
                      </button>
                    )}
                  </div>
                )}
              </div>

              {/* ═══ 오른쪽: AI + 메모장 ═══ */}
              <div className="right-panel">
                <div className="ai-card">
                  <div className="ai-card-header">
                    <div className="ai-card-title">
                      <span className="ai-status-dot" />
                      <svg width="16" height="16" viewBox="0 0 16 16" fill="none" className="ai-sparkle-icon">
                        <path d="M8 1l1.8 4.2L14 7l-4.2 1.8L8 13l-1.8-4.2L2 7l4.2-1.8z" fill="#00e5ff" opacity="0.85" />
                      </svg>
                      AI 어시스턴트
                    </div>
                    <span className="ai-active-badge">{isAiLoading ? "답변 생성 중..." : "Active"}</span>
                  </div>
                  <div className="ai-chat-body">
                    {chatMsgs.map((msg, i) => (
                      <div key={i} className={`ai-chat-msg ${msg.role}`}>
                        <div className="ai-chat-bubble">{msg.text}</div>
                      </div>
                    ))}
                    {isAiLoading && (
                      <div className="ai-chat-msg ai">
                        <div className="ai-chat-bubble">...</div>
                      </div>
                    )}
                    <div ref={chatBottomRef} />
                  </div>
                </div>

                <div className="memo-card">
                  <div className="memo-header">
                    <button className="memo-header-note-tab">Note</button>
                    <button className="memo-header-add" onClick={addMemo}>
                      +
                    </button>
                  </div>
                  <div
                    className="memo-notes-scroll"
                    ref={scrollRef}
                    onMouseDown={onMouseDown}
                    onMouseLeave={onMouseLeave}
                    onMouseUp={onMouseUp}
                    onMouseMove={onMouseMove}
                  >
                    {memos.map((memo, idx) => (
                      <div key={idx} className="memo-note">
                        <div className="memo-note-top">
                          <span className="memo-note-label">{memo.label}</span>
                          <div className="memo-note-actions">
                            <button className="memo-note-expand" onClick={() => setExpandedMemo(idx)}>
                              ↗
                            </button>
                            <button className="memo-note-delete" onClick={() => deleteMemo(idx)}>
                              ×
                            </button>
                          </div>
                        </div>
                        <div className="memo-note-body">
                          <textarea
                            className="memo-note-title-input"
                            placeholder="제목..."
                            value={memo.title}
                            onChange={(e) => updateMemo(idx, "title", e.target.value)}
                            rows={1}
                          />
                          <div className="memo-note-divider" />
                          <textarea
                            className="memo-note-content-input"
                            placeholder="내용..."
                            value={memo.content}
                            onChange={(e) => updateMemo(idx, "content", e.target.value)}
                          />
                        </div>
                      </div>
                    ))}
                  </div>
                </div>
              </div>
            </div>

            {/* 입력 바 */}
            <div className="learn-input-bar">
              <div className="learn-input-label">무엇이 궁금하신가요?</div>
              <div className="learn-input-wrap">
                <div className="learn-input-plus">+</div>
                <input
                  className="learn-input-field"
                  type="text"
                  placeholder="질문을 입력하세요..."
                  value={chatInput}
                  onChange={(e) => setChatInput(e.target.value)}
                  onKeyDown={(e) => e.key === "Enter" && sendChat()}
                  disabled={isAiLoading}
                />
              </div>
              <button className="learn-send-btn" onClick={sendChat} disabled={isAiLoading}>
                <svg width="15" height="15" viewBox="0 0 16 16" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                  <path d="M14 2L7 9M14 2l-4 12-3-5-5-3 12-4z" />
                </svg>
              </button>
            </div>
          </div>
        </section>


      </div>

      {expandedMemo !== null && memos[expandedMemo] && (
        <div className="memo-modal-overlay" onClick={() => setExpandedMemo(null)}>
          <div className="memo-modal" onClick={(e) => e.stopPropagation()}>
            <div className="memo-modal-header">
              <span className="memo-modal-label">{memos[expandedMemo].label}</span>
              <div className="memo-modal-header-actions">
                <button className="memo-modal-delete" onClick={() => deleteMemo(expandedMemo)}>
                  삭제
                </button>
                <button className="memo-modal-close" onClick={() => setExpandedMemo(null)}>
                  ✕
                </button>
              </div>
            </div>
            <textarea
              className="memo-modal-title"
              placeholder="제목..."
              value={memos[expandedMemo].title}
              onChange={(e) => updateMemo(expandedMemo, "title", e.target.value)}
            />
            <div className="memo-modal-divider" />
            <textarea
              className="memo-modal-content"
              placeholder="내용..."
              value={memos[expandedMemo].content}
              onChange={(e) => updateMemo(expandedMemo, "content", e.target.value)}
            />
          </div>
        </div>
      )}
    </>
  );
}