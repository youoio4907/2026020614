import { useState, useRef, useCallback, useEffect } from "react";
import "./Shared.css";
import "./Workflowpage.css";

/* ════════════════════════════════════════════ */
/*  WorkflowPage                                */
/* ════════════════════════════════════════════ */
export default function WorkflowPage({ onHome, onStudy, onTest }) {
  const [activeNav, setActiveNav] = useState("Lab");
  const navItems = ["Home", "Study", "CAD", "Lab", "Test"];

  // 노드 데이터 - localStorage에서 복원 또는 기본값
  const [nodes, setNodes] = useState(() => {
    const saved = localStorage.getItem('workflowNodes');
    if (saved) {
      try {
        return JSON.parse(saved);
      } catch (e) {
        console.error('Failed to parse saved nodes:', e);
      }
    }
    return [
      { id: 1, x: 200, y: 200, title: "기획", content: "", color: "#3b82f6" },
      { id: 2, x: 600, y: 50, title: "스케치", content: "", color: "#3b82f6" },
      { id: 3, x: 600, y: 350, title: "모델링", content: "", color: "#3b82f6" },
      { id: 4, x: 1000, y: 200, title: "수치해석", content: "", color: "#3b82f6" },
    ];
  });

  // 연결선 데이터 - localStorage에서 복원
  const [connections, setConnections] = useState(() => {
    const saved = localStorage.getItem('workflowConnections');
    if (saved) {
      try {
        return JSON.parse(saved);
      } catch (e) {
        console.error('Failed to parse saved connections:', e);
      }
    }
    return [];
  });

  // 편집 중인 노드
  const [editingNode, setEditingNode] = useState(null);

  // 드래그 상태
  const [draggingNode, setDraggingNode] = useState(null);
  const [draggingOffset, setDraggingOffset] = useState({ x: 0, y: 0 });

  // 연결선 드래그 상태
  const [connectingFrom, setConnectingFrom] = useState(null); // { nodeId, anchor }
  const [connectingMouse, setConnectingMouse] = useState(null); // { x, y }

  // 줌/팬 상태
  const [zoom, setZoom] = useState(1);
  const [pan, setPan] = useState({ x: 0, y: 0 });
  const [isPanning, setIsPanning] = useState(false);
  const [panStart, setPanStart] = useState({ x: 0, y: 0 });

  const canvasRef = useRef(null);
  // nextId를 localStorage에서 복원
  const getInitialNextId = () => {
    const saved = localStorage.getItem('workflowNextId');
    return saved ? parseInt(saved, 10) : 5;
  };
  const nextIdRef = useRef(getInitialNextId());

  // 노드 데이터가 변경되면 localStorage에 저장
  useEffect(() => {
    localStorage.setItem('workflowNodes', JSON.stringify(nodes));
    // 다음 ID도 저장
    const maxId = nodes.length > 0 ? Math.max(...nodes.map(n => n.id)) : 4;
    const nextId = maxId + 1;
    nextIdRef.current = nextId;
    localStorage.setItem('workflowNextId', nextId.toString());
  }, [nodes]);

  // 연결선 데이터가 변경되면 localStorage에 저장
  useEffect(() => {
    localStorage.setItem('workflowConnections', JSON.stringify(connections));
  }, [connections]);

  const handleNav = (item) => {
      if (item === "CAD") {
    alert("페이지 준비중입니다");
    return;
  }
    setActiveNav(item);
    if (item === "Home") onHome();
    if (item === "Study") onStudy?.();
    if (item === "Test") onTest?.();
  };

  /* ── 노드 드래그 시작 ── */
  const handleNodeMouseDown = (e, node) => {
    if (e.target.classList.contains("wf-anchor")) return; // 앵커는 별도 처리
    e.stopPropagation();
    const rect = canvasRef.current.getBoundingClientRect();
    const mouseX = (e.clientX - rect.left - pan.x) / zoom;
    const mouseY = (e.clientY - rect.top - pan.y) / zoom;
    setDraggingNode(node.id);
    setDraggingOffset({ x: mouseX - node.x, y: mouseY - node.y });
  };

  /* ── 앵커 드래그 시작 (연결선 생성) ── */
  const handleAnchorMouseDown = (e, nodeId, anchor) => {
    e.stopPropagation();
    const rect = canvasRef.current.getBoundingClientRect();
    const mouseX = (e.clientX - rect.left - pan.x) / zoom;
    const mouseY = (e.clientY - rect.top - pan.y) / zoom;
    setConnectingFrom({ nodeId, anchor });
    setConnectingMouse({ x: mouseX, y: mouseY });
  };

  /* ── 앵커 드래그 끝 (연결선 완성) ── */
  const handleAnchorMouseUp = (e, nodeId, anchor) => {
    if (connectingFrom && connectingFrom.nodeId !== nodeId) {
      // 새 연결 추가
      setConnections(prev => [
        ...prev,
        { from: connectingFrom.nodeId, to: nodeId, fromAnchor: connectingFrom.anchor, toAnchor: anchor }
      ]);
    }
    setConnectingFrom(null);
    setConnectingMouse(null);
  };

  /* ── 캔버스 마우스 이동 ── */
  const handleCanvasMouseMove = useCallback((e) => {
    const rect = canvasRef.current.getBoundingClientRect();
    const mouseX = (e.clientX - rect.left - pan.x) / zoom;
    const mouseY = (e.clientY - rect.top - pan.y) / zoom;

    // 노드 드래그 중
    if (draggingNode !== null) {
      setNodes(prev => prev.map(n =>
        n.id === draggingNode
          ? { ...n, x: mouseX - draggingOffset.x, y: mouseY - draggingOffset.y }
          : n
      ));
    }

    // 연결선 드래그 중
    if (connectingFrom) {
      setConnectingMouse({ x: mouseX, y: mouseY });
    }

    // 패닝 중
    if (isPanning) {
      setPan({
        x: pan.x + (e.clientX - panStart.x),
        y: pan.y + (e.clientY - panStart.y)
      });
      setPanStart({ x: e.clientX, y: e.clientY });
    }
  }, [draggingNode, draggingOffset, connectingFrom, isPanning, pan, panStart, zoom]);

  const handleCanvasMouseUp = useCallback(() => {
    setDraggingNode(null);
    if (connectingFrom) {
      // 빈 공간에서 놓으면 취소
      setConnectingFrom(null);
      setConnectingMouse(null);
    }
    setIsPanning(false);
  }, [connectingFrom]);

  /* ── 캔버스 휠 (줌) - 마우스 커서 위치 중심으로 ── */
  const handleCanvasWheel = (e) => {
    // 노드 본문 위에서는 줌 방지 (스크롤 우선)
    if (e.target.classList.contains('wf-node-body') || 
        e.target.classList.contains('wf-node-content') ||
        e.target.classList.contains('wf-node-content-input') ||
        e.target.classList.contains('wf-node-content-text') ||
        e.target.closest('.wf-node-body')) {
      return; // 노드 본문 스크롤만 허용
    }
    
    e.preventDefault();
    const rect = canvasRef.current.getBoundingClientRect();
    const mouseX = e.clientX - rect.left;
    const mouseY = e.clientY - rect.top;
    
    // 줌 전 마우스 위치 (캔버스 좌표계)
    const worldX = (mouseX - pan.x) / zoom;
    const worldY = (mouseY - pan.y) / zoom;
    
    const delta = e.deltaY > 0 ? 0.9 : 1.1;
    const newZoom = Math.max(0.3, Math.min(3, zoom * delta));
    
    // 줌 후 마우스 위치를 유지하도록 pan 조정
    const newPanX = mouseX - worldX * newZoom;
    const newPanY = mouseY - worldY * newZoom;
    
    setZoom(newZoom);
    setPan({ x: newPanX, y: newPanY });
  };

  /* ── 캔버스 빈 곳 클릭 → 패닝 시작 및 편집 모드 해제 ── */
  const handleCanvasMouseDown = (e) => {
    if (e.target === canvasRef.current || e.target.classList.contains("wf-canvas-inner")) {
      setIsPanning(true);
      setPanStart({ x: e.clientX, y: e.clientY });
      setEditingNode(null); // 편집 모드 해제
    }
  };

  /* ── 새 노드 추가 ── */
  const addNode = () => {
    const id = nextIdRef.current++;
    setNodes(prev => [...prev, {
      id,
      x: 300 + Math.random() * 200,
      y: 200 + Math.random() * 200,
      title: "새노드",
      content: "",
      color: "#3b82f6"
    }]);
  };

  /* ── 노드 클릭 → 편집 모드 ── */
  const handleNodeClick = (e, node) => {
    if (e.target.classList.contains("wf-anchor")) return;
    e.stopPropagation();
    setEditingNode(node.id);
  };

  /* ── 노드 본문 클릭 → 편집 모드 (중복 방지) ── */
  const handleNodeBodyClick = (e, node) => {
    e.stopPropagation();
    setEditingNode(node.id);
  };

  /* ── 연결선 삭제 ── */
  const handleConnectionClick = (connIdx) => {
    setConnections(prev => prev.filter((_, i) => i !== connIdx));
  };

  /* ── 노드 삭제 ── */
  const handleDeleteNode = (e, nodeId) => {
    e.stopPropagation();
    setNodes(prev => prev.filter(n => n.id !== nodeId));
    // 해당 노드와 연결된 모든 연결선도 삭제
    setConnections(prev => prev.filter(c => c.from !== nodeId && c.to !== nodeId));
    if (editingNode === nodeId) setEditingNode(null);
  };

  /* ── 제목/내용 수정 ── */
  const handleTitleChange = (id, val) => {
    setNodes(prev => prev.map(n => n.id === id ? { ...n, title: val } : n));
  };
  const handleContentChange = (id, val) => {
    setNodes(prev => prev.map(n => n.id === id ? { ...n, content: val } : n));
  };
  const handleItemChange = (id, idx, val) => {
    setNodes(prev => prev.map(n => {
      if (n.id === id && n.items) {
        const newItems = [...n.items];
        newItems[idx] = val;
        return { ...n, items: newItems };
      }
      return n;
    }));
  };

  /* ── 앵커 위치 계산 ── */
  const getAnchorPos = (node, anchor) => {
    const w = 280, h = node.items ? 140 : 200;
    const cx = node.x + w / 2, cy = node.y + h / 2;
    if (anchor === "left") return { x: node.x, y: cy };
    if (anchor === "right") return { x: node.x + w, y: cy };
    if (anchor === "top") return { x: cx, y: node.y };
    if (anchor === "bottom") return { x: cx, y: node.y + h };
    return { x: cx, y: cy };
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
                  <circle cx="9" cy="9" r="3" /><path d="M9 2v2M9 14v2M2 9h2M14 9h2" />
                </svg>
              </div>
              <span className="nav-logo-text">SIMVEX</span>
            </div>
            <div className="nav-links">
              {navItems.map(item => (
                <button key={item} className={`nav-link${activeNav === item ? " active" : ""}`} onClick={() => handleNav(item)}>{item}</button>
              ))}
            </div>
          </div>
        </nav>

        {/* WORKFLOW BODY */}
        <section className="wf-body">
          <div className="wf-container">

            {/* 캔버스 */}
            <div
              className="wf-canvas"
              ref={canvasRef}
              onMouseMove={handleCanvasMouseMove}
              onMouseUp={handleCanvasMouseUp}
              onMouseDown={handleCanvasMouseDown}
              onWheel={handleCanvasWheel}
            >
              <div
                className="wf-canvas-inner"
                style={{ transform: `translate(${pan.x}px, ${pan.y}px) scale(${zoom})`, transformOrigin: "0 0" }}
              >
                {/* SVG 연결선 */}
                <svg className="wf-connections">
                  {connections.map((conn, i) => {
                    const fromNode = nodes.find(n => n.id === conn.from);
                    const toNode = nodes.find(n => n.id === conn.to);
                    if (!fromNode || !toNode) return null;
                    const p1 = getAnchorPos(fromNode, conn.fromAnchor);
                    const p2 = getAnchorPos(toNode, conn.toAnchor);
                    const dx = p2.x - p1.x, midX = p1.x + dx / 2;
                    return (
                      <g key={i}>
                        {/* 투명한 넓은 클릭 영역 */}
                        <path
                          d={`M${p1.x},${p1.y} C${midX},${p1.y} ${midX},${p2.y} ${p2.x},${p2.y}`}
                          stroke="transparent"
                          strokeWidth="12"
                          fill="none"
                          style={{ cursor: "pointer" }}
                          onClick={() => handleConnectionClick(i)}
                        />
                        {/* 실제 보이는 선 */}
                        <path
                          d={`M${p1.x},${p1.y} C${midX},${p1.y} ${midX},${p2.y} ${p2.x},${p2.y}`}
                          stroke="rgba(255,255,255,0.15)"
                          strokeWidth="2"
                          fill="none"
                          style={{ pointerEvents: "none" }}
                          className="wf-connection-line"
                        />
                      </g>
                    );
                  })}
                  {/* 드래그 중인 임시 연결선 */}
                  {connectingFrom && connectingMouse && (() => {
                    const fromNode = nodes.find(n => n.id === connectingFrom.nodeId);
                    if (!fromNode) return null;
                    const p1 = getAnchorPos(fromNode, connectingFrom.anchor);
                    const p2 = connectingMouse;
                    const dx = p2.x - p1.x, midX = p1.x + dx / 2;
                    return (
                      <path
                        d={`M${p1.x},${p1.y} C${midX},${p1.y} ${midX},${p2.y} ${p2.x},${p2.y}`}
                        stroke="rgba(0,229,255,0.5)"
                        strokeWidth="2"
                        fill="none"
                        strokeDasharray="5,5"
                      />
                    );
                  })()}
                </svg>

                {/* 노드들 */}
                {nodes.map(node => {
                  const isEditing = editingNode === node.id;
                  return (
                    <div
                      key={node.id}
                      className="wf-node"
                      style={{ left: node.x, top: node.y }}
                      onMouseDown={(e) => handleNodeMouseDown(e, node)}
                      onClick={(e) => handleNodeClick(e, node)}
                    >
                      {/* 앵커 (좌우) */}
                      <div
                        className="wf-anchor wf-anchor-left"
                        onMouseDown={(e) => handleAnchorMouseDown(e, node.id, "left")}
                        onMouseUp={(e) => handleAnchorMouseUp(e, node.id, "left")}
                      />
                      <div
                        className="wf-anchor wf-anchor-right"
                        onMouseDown={(e) => handleAnchorMouseDown(e, node.id, "right")}
                        onMouseUp={(e) => handleAnchorMouseUp(e, node.id, "right")}
                      />

                      {/* 노드 헤더 */}
                      <div className="wf-node-header">
                        {isEditing ? (
                          <input
                            className="wf-node-title-input"
                            value={node.title}
                            onChange={(e) => handleTitleChange(node.id, e.target.value)}
                            autoFocus
                            onClick={(e) => e.stopPropagation()}
                          />
                        ) : (
                          <div className="wf-node-title">{node.title}</div>
                        )}
                        <div className="wf-node-actions">
                          <button className="wf-node-attach" onClick={(e) => { e.stopPropagation(); alert('파일 추가 기능 준비 중'); }}>
                            <svg width="12" height="12" viewBox="0 0 12 12" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round">
                              <line x1="6" y1="2" x2="6" y2="10"/>
                              <line x1="2" y1="6" x2="10" y2="6"/>
                            </svg>
                          </button>
                          <button className="wf-node-delete" onClick={(e) => handleDeleteNode(e, node.id)}>
                            <svg width="12" height="12" viewBox="0 0 12 12" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round">
                              <line x1="2" y1="2" x2="10" y2="10"/>
                              <line x1="10" y1="2" x2="2" y2="10"/>
                            </svg>
                          </button>
                        </div>
                      </div>

                      {/* 노드 본문 */}
                      <div className="wf-node-body" onClick={(e) => handleNodeBodyClick(e, node)}>
                        {node.items ? (
                          // 리스트 형태
                          <div className="wf-node-list">
                            {node.items.map((item, idx) => (
                              <div key={idx} className="wf-node-item">
                                <span className="wf-node-bullet">●</span>
                                {isEditing ? (
                                  <input
                                    className="wf-node-item-input"
                                    value={item}
                                    onChange={(e) => handleItemChange(node.id, idx, e.target.value)}
                                    onClick={(e) => e.stopPropagation()}
                                    autoFocus
                                  />
                                ) : (
                                  <span>{item}</span>
                                )}
                              </div>
                            ))}
                          </div>
                        ) : (
                          // 단일 텍스트 형태
                          <div className="wf-node-content">
                            {isEditing ? (
                              <textarea
                                className="wf-node-content-input"
                                value={node.content || ""}
                                onChange={(e) => handleContentChange(node.id, e.target.value)}
                                onClick={(e) => e.stopPropagation()}
                                autoFocus
                              />
                            ) : (
                              <div className="wf-node-content-text">{node.content || "내용 없음"}</div>
                            )}
                          </div>
                        )}
                      </div>
                    </div>
                  );
                })}
              </div>
            </div>

            {/* 좌측 하단 사용법 */}
            <div className="wf-help">
              <div className="wf-help-title">워크플로우 에디터</div>
              <div className="wf-help-item">• 노드를 드래그하여 이동할 수 있습니다</div>
              <div className="wf-help-item">• 주황색 점을 드래그하여 노드를 연결하세요</div>
              <div className="wf-help-item">• 우측 상단 + 버튼으로 새 노드를 추가할 수 있습니다</div>
              <div className="wf-help-item">• 마우스 휠로 줌 인/아웃이 가능합니다</div>
            </div>

            {/* 우측 상단 + 버튼 */}
            <button className="wf-add-btn" onClick={addNode}>
              <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round">
                <line x1="12" y1="5" x2="12" y2="19" />
                <line x1="5" y1="12" x2="19" y2="12" />
              </svg>
            </button>

          </div>
        </section>

      </div>
    </>
  );
}