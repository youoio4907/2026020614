// ExamFieldSelectPage.js
import { useState } from "react";
import "./Shared.css";
import "./Studypage.css"; // Study 페이지와 같은 스타일 사용

/* 분야별 SVG 아이콘 (Studypage와 동일) */
const GearIcon = () => (
  <svg viewBox="0 0 58 58" fill="none">
    <defs>
      <linearGradient id="gearG1" x1="0%" y1="0%" x2="100%" y2="100%">
        <stop offset="0%" stopColor="#7ecae6" />
        <stop offset="100%" stopColor="#3a9bbf" />
      </linearGradient>
    </defs>
    <circle cx="22" cy="28" r="11" fill="url(#gearG1)" />
    <circle cx="22" cy="28" r="5" fill="#1a2040" />
  </svg>
);

const ChipIcon = () => (
  <svg viewBox="0 0 58 58" fill="none">
    <defs>
      <linearGradient id="chipBody" x1="0%" y1="0%" x2="100%" y2="100%">
        <stop offset="0%" stopColor="#60a5fa" />
        <stop offset="100%" stopColor="#3b82f6" />
      </linearGradient>
    </defs>
    <rect x="14" y="16" width="30" height="26" rx="4" fill="url(#chipBody)" />
  </svg>
);

const RocketIcon = () => (
  <svg viewBox="0 0 58 58" fill="none">
    <ellipse cx="29" cy="50" rx="4" ry="7" fill="#ff8844" opacity="0.7" />
    <path d="M29,6 Q22,20 20,34 L38,34 Q36,20 29,6 Z" fill="#fcd34d" />
  </svg>
);

const AtomIcon = () => (
  <svg viewBox="0 0 58 58" fill="none">
    <ellipse cx="29" cy="29" rx="22" ry="10" fill="none" stroke="#a78bfa" strokeWidth="1.5" />
    <circle cx="29" cy="29" r="6" fill="#a78bfa" opacity="0.9" />
  </svg>
);

const BeakerIcon = () => (
  <svg viewBox="0 0 58 58" fill="none">
    <path d="M18,16 L18,44 Q18,48 22,48 L36,48 Q40,48 40,44 L40,16" fill="#34d399" opacity="0.5" />
  </svg>
);

const BridgeIcon = () => (
  <svg viewBox="0 0 58 58" fill="none">
    <rect x="6" y="28" width="5" height="22" rx="2" fill="#60a5fa" />
    <rect x="47" y="28" width="5" height="22" rx="2" fill="#60a5fa" />
    <rect x="4" y="26" width="50" height="4" rx="2" fill="#60a5fa" />
  </svg>
);

const FIELDS = [
  { icon: <GearIcon />, glowColor: "#7ecae6", title: "기계 공학" },
  { icon: <ChipIcon />, glowColor: "#60a5fa", title: "전기 전자 공학" },
  { icon: <RocketIcon />, glowColor: "#fcd34d", title: "항공 우주 공학" },
  { icon: <AtomIcon />, glowColor: "#a78bfa", title: "재료 과학" },
  { icon: <BeakerIcon />, glowColor: "#34d399", title: "화학 공학" },
  { icon: <BridgeIcon />, glowColor: "#60a5fa", title: "토목 공학" },
];

export default function ExamFieldSelectPage({ onHome, onStudy, onLab, onTest, onFieldSelect }) {
  const [activeNav, setActiveNav] = useState("Test");
  const navItems = ["Home", "Study", "CAD", "Lab", "Test"];

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

        {/* BODY */}
        <section className="study-body">
          <div className="inner">
            <h2 className="study-title">모의고사 분야 선택</h2>

            <div className="study-grid">
              {FIELDS.map((field, i) => (
                <div
                  key={i}
                  className="study-card"
                  onClick={() => onFieldSelect?.(field.title)}
                  style={{ cursor: "pointer" }}
                >
                  <div className="study-icon-wrap">
                    {field.icon}
                    <div className="icon-glow" style={{ background: field.glowColor }} />
                  </div>
                  <h3 className="study-card-title">{field.title}</h3>
                  <p className="study-card-desc">이 분야의 모의고사를 선택하세요.</p>
                </div>
              ))}
            </div>
          </div>
        </section>

      </div>
    </>
  );
}