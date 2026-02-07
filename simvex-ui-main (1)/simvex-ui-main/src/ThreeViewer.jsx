// ThreeViewer.jsx
import { useEffect, useRef, useState, useMemo, useCallback } from "react";
import * as THREE from "three";
import { OrbitControls } from "three/examples/jsm/controls/OrbitControls";
import { GLTFLoader } from "three/examples/jsm/loaders/GLTFLoader";

/**
 * ThreeViewer - 3D 모델 뷰어 컴포넌트
 *
 * @param {string} modelUrl - GLB 파일 경로
 * @param {Array} parts - 부품 목록
 * @param {string} selectedPartKey - 선택된 부품 키
 * @param {number} assemblyProgress - 조립 진행도 (0~100)
 * @param {Function} onPartClick - 부품 클릭 핸들러
 */
export default function ThreeViewer({
  modelUrl,
  parts = [],
  selectedPartKey,
  assemblyProgress = 100,
  onPartClick,
}) {
  const mountRef = useRef(null);
  const sceneRef = useRef(null);
  const cameraRef = useRef(null);
  const rendererRef = useRef(null);
  const controlsRef = useRef(null);

  //const meshesRef = useRef(new Map()); // meshName -> mesh object
  const originalPositionsRef = useRef(new Map()); // meshName -> original position
  const logicalPartsRef = useRef(new Map());
  const clickableMeshesRef = useRef([]);

  const raycasterRef = useRef(new THREE.Raycaster());
  const mouseRef = useRef(new THREE.Vector2());
  const resizeObserverRef = useRef(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [isModelReady, setIsModelReady] = useState(false);

  const DEFAULT_POS = { x: 3, y: 2, z: 5 };
  const currentModelName = useMemo(() => modelUrl ? modelUrl.split('/').pop().split('.')[0] : "default",[modelUrl]);
  
 // ═══ 2. 저장 로직 (useCallback으로 메모리 효율화) ═══
  const saveSession = useCallback(() => {
    if (!cameraRef.current || !controlsRef.current || !isModelReady){console.error("❌ 카메라나 컨트롤이 없습니다!"); return;}
    console.log("🚀 saveSession 함수 진입 시도!");
    const sessionObj = {
      camera: {
        position: cameraRef.current.position.clone(),
        target: controlsRef.current.target.clone(),
        zoom: cameraRef.current.zoom 
      },
      progress: assemblyProgress,
      lastSeen: new Date().toISOString()
    };

    localStorage.setItem(`viewer_${currentModelName}`, JSON.stringify(sessionObj));
  }, [currentModelName, assemblyProgress, isModelReady]);

  // ═══ 3. 저장 트리거 (Debounce 적용) ═══
  useEffect(() => {
    if (!controlsRef.current || !isModelReady) return;

    let saveTimeout;
    const handleControlChange = () => {
      clearTimeout(saveTimeout);
      saveTimeout = setTimeout(saveSession, 300);
    };

    controlsRef.current.addEventListener("change", handleControlChange);
    return () => {
      controlsRef.current?.removeEventListener("change", handleControlChange);
      clearTimeout(saveTimeout);
    };
  }, [isModelReady, saveSession]);

  // ═══ 4. 시점 복구 로직 (Restore) ═══
  useEffect(() => {
    // 모델이 로드된 직후(isModelReady)에만 실행
    if (!isModelReady || !cameraRef.current || !controlsRef.current) return;

    const rawData = localStorage.getItem(`viewer_${currentModelName}`);
    
    if (rawData) {
      const data = JSON.parse(rawData);
      const { position, target, zoom } = data.camera;

      cameraRef.current.position.set(position.x, position.y, position.z);
      controlsRef.current.target.set(target.x, target.y, target.z);
      cameraRef.current.zoom = zoom || 1;
      
      cameraRef.current.updateProjectionMatrix(); 
      controlsRef.current.update();
      console.log(`[ThreeViewer] ${currentModelName} 세션 복구 완료`);
    } else {
      // 기록 없으면 기본값으로 초기화
      cameraRef.current.position.set(DEFAULT_POS.x, DEFAULT_POS.y, DEFAULT_POS.z);
      controlsRef.current.target.set(0, 0, 0);
      controlsRef.current.update();
    }
  }, [isModelReady, currentModelName]);

  // ═══ 초기 설정 ═══
  useEffect(() => {
    if (!mountRef.current) return;

    // Scene 생성
    const scene = new THREE.Scene();
    scene.background = new THREE.Color(0x0a1520);
    sceneRef.current = scene;

    // Camera 생성
    const camera = new THREE.PerspectiveCamera(50,mountRef.current.clientWidth/mountRef.current.clientHeight,0.1,1000);
    camera.position.set(DEFAULT_POS.x, DEFAULT_POS.y, DEFAULT_POS.z);
    cameraRef.current = camera;

    // Renderer 생성
    const renderer = new THREE.WebGLRenderer({ 
      antialias: true, 
      alpha: true,
      preserveDrawingBuffer: true
    });
    renderer.setSize(mountRef.current.clientWidth, mountRef.current.clientHeight);
    renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2));
    renderer.shadowMap.enabled = true;

    // ✅ 캔버스가 레이아웃에 딱 붙도록
    renderer.domElement.style.display = "block";

    mountRef.current.appendChild(renderer.domElement);
    rendererRef.current = renderer;

    // OrbitControls 생성
    const controls = new OrbitControls(camera, renderer.domElement);
    controls.enableDamping = true;
    controls.dampingFactor = 0.05;
    controls.minDistance = 2;
    controls.maxDistance = 20;
    controlsRef.current = controls;

    // 조명 추가
    const ambientLight = new THREE.AmbientLight(0xffffff, 2);
    scene.add(ambientLight);

    const directionalLight = new THREE.DirectionalLight(0xffffff, 2);
    directionalLight.position.set(8, 8, 5);
    directionalLight.castShadow = true;
    scene.add(directionalLight);

    const fillLight = new THREE.DirectionalLight(0x4a8aff, 2);
    fillLight.position.set(-10, -5, -5);
    scene.add(fillLight);

    // 그리드 헬퍼 (선택사항)
    const gridHelper = new THREE.GridHelper(10, 10, 0x444444, 0x222222);
    scene.add(gridHelper);

    // ✅ mount 크기 기준으로 camera/renderer 리사이즈
    const resizeToMount = () => {
      if (!mountRef.current || !cameraRef.current || !rendererRef.current) return;

      const w = mountRef.current.clientWidth;
      const h = mountRef.current.clientHeight;
      if (!w || !h) return;

      cameraRef.current.aspect = w / h;
      cameraRef.current.updateProjectionMatrix();

      // ✅ 중요: setSize(w,h) 로 "CSS 크기"까지 같이 갱신해야 패널 토글 시 꽉 찬다
      rendererRef.current.setSize(w, h);
      rendererRef.current.setPixelRatio(Math.min(window.devicePixelRatio, 2));
    };

    // 애니메이션 루프
    let animationId;
    const animate = () => {
      animationId = requestAnimationFrame(animate);
      controls.update();
      renderer.render(scene, camera);
    };
    animate();

    // ✅ 1) window resize
    const handleResize = () => resizeToMount();
    window.addEventListener("resize", handleResize);

    // ✅ 2) 레이아웃 변화(패널 토글 등) 감지: ResizeObserver
    resizeObserverRef.current = new ResizeObserver(() => {
      resizeToMount();
    });
    resizeObserverRef.current.observe(mountRef.current);

    // 초기 1회 보정
    resizeToMount();

    // 클린업
    return () => {
      window.removeEventListener("resize", handleResize);

      if (resizeObserverRef.current) {
        resizeObserverRef.current.disconnect();
        resizeObserverRef.current = null;
      }

      cancelAnimationFrame(animationId);
      controls.dispose();
      renderer.dispose();

      if (mountRef.current && renderer.domElement) {
        mountRef.current.removeChild(renderer.domElement);
      }
    };
  }, []);

  useEffect(() => {
  if (!isModelReady || !currentModelName) return;

  const rawData = localStorage.getItem(`viewer_${currentModelName}`);
  
  if (rawData) {
    const data = JSON.parse(rawData);
    const { position, target, zoom } = data.camera;

    // 1. 위치와 타겟 복구
    cameraRef.current.position.set(position.x, position.y, position.z);
    controlsRef.current.target.set(target.x, target.y, target.z);
    
    // 2. 💡 저장된 줌 값 적용 (없으면 기본값 1)
    cameraRef.current.zoom = zoom || 1;
    
    // 3. 변경사항을 반영하기 위해 반드시 호출
    cameraRef.current.updateProjectionMatrix(); 
    controlsRef.current.update();
  }
}, [isModelReady, currentModelName]);


  // ═══ GLB 파일 로드 ═══
  
  useEffect(() => {
    if (!modelUrl || !sceneRef.current) return;
    setIsModelReady(false);
    setLoading(true);
    const loader = new GLTFLoader();

    loader.load(modelUrl, (gltf) => {
      // 1. 기존 모델 제거
      const existingModel = sceneRef.current.getObjectByName("loadedModel");
      if (existingModel) sceneRef.current.remove(existingModel);

      const model = gltf.scene;
      model.name = "loadedModel";

      // 2. 모델 크기 정규화 (유저님 코드)
      const box = new THREE.Box3().setFromObject(model);
      const size = box.getSize(new THREE.Vector3());
      const maxDim = Math.max(size.x, size.y, size.z);
      const scale = 2 / maxDim; 
      model.scale.setScalar(scale);

      // 3. 모델 중심 정렬 (유저님 코드)
      const center = box.getCenter(new THREE.Vector3());
      model.position.sub(center.multiplyScalar(scale));

      // 4. 장면 추가
      sceneRef.current.add(model);
      setIsModelReady(true);

      //5. 카메라/렌더러 리사이즈 보정 (유저님 코드)
      if (mountRef.current && cameraRef.current && rendererRef.current) {
        const w = mountRef.current.clientWidth;
        const h = mountRef.current.clientHeight;
        if (w && h) {
          cameraRef.current.aspect = w / h;
          cameraRef.current.updateProjectionMatrix();
          rendererRef.current.setSize(w, h);
          rendererRef.current.setPixelRatio(Math.min(window.devicePixelRatio, 2));
        }
      }

      setLoading(false);
      console.log(`[ThreeViewer1] Model loaded and normalized: ${modelUrl}`);
    });
  }, [modelUrl]);

  useEffect(() => {
    // 화면에 "loadedModel"이 있고, DB 데이터(parts)도 도착했을 때만 실행
    const model = sceneRef.current?.getObjectByName("loadedModel");
    if (!model || parts.length === 0) return;

    console.log("[ThreeViewer] 2. 데이터 매핑 시작 (parts 연결)");

    // 기존 관리 리스트 초기화
    logicalPartsRef.current.clear();
    originalPositionsRef.current.clear();
    clickableMeshesRef.current = [];

    model.traverse((child) => {
      if (child.isMesh) {
        let current = child;
        let logicalPart = null;
        let partData = null;

        // 부모를 타고 올라가며 DB의 meshName과 일치하는 '진짜 주인' 찾기
        while (current && current !== model) {
          partData = parts.find(p => p.meshName === current.name);
          if (partData) {
            logicalPart = current;
            break;
          }
          current = current.parent;
        }

        if (logicalPart) {
          // 데이터 심어주기 (클릭/애니메이션용)
          child.userData.logicalPart = logicalPart;
          child.userData.partData = partData;
          
          if (!logicalPartsRef.current.has(logicalPart.name)) {
            logicalPartsRef.current.set(logicalPart.name, logicalPart);
            originalPositionsRef.current.set(logicalPart.name, logicalPart.position.clone());
          }
          clickableMeshesRef.current.push(child);
        }
      }
    });
    console.log("[ThreeViewer] 3. 모든 부품 연결 성공!");
  }, [parts, modelUrl,isModelReady]);

  // ═══ 조립/분해 애니메이션 ═══
  // [2] 데이터 매핑 전용 useEffect (JSON 파싱 포함)
useEffect(() => {
  const model = sceneRef.current?.getObjectByName("loadedModel");
  if (!model || parts.length === 0 || !isModelReady) return;

  logicalPartsRef.current.clear();
  originalPositionsRef.current.clear();

  model.traverse((child) => {
    if (child.isMesh) {
      let current = child;
      let partData = null;

      while (current && current !== model) {
        partData = parts.find(p => p.meshName === current.name);
        if (partData) break;
        current = current.parent;
      }

      if (partData && !logicalPartsRef.current.has(current.name)) {
        // 💡 JSON 문자열인 content를 객체로 변환
        const meta = typeof partData.content === 'string' 
          ? JSON.parse(partData.content) 
          : partData.content;

        // 1. DB의 position을 '절대적 고향'으로 설정
        const homePos = new THREE.Vector3(
          meta.position.x,
          meta.position.y,
          meta.position.z
        );
        
        // 2. DB의 explodeVector를 '절대적 방향'으로 설정
        const explodeDir = new THREE.Vector3(
          meta.explodeVector.x,
          meta.explodeVector.y,
          meta.explodeVector.z
        );

        // 방향이 0,0,0이면 자동으로 계산 (기존 로직 유지)
        if (explodeDir.length() < 0.001) {
          explodeDir.copy(homePos).normalize();
          if (explodeDir.length() < 0.01) explodeDir.set(0, 1, 0);
        }

        current.position.copy(homePos); // 초기 위치 강제 세팅
        originalPositionsRef.current.set(current.name, homePos.clone());
        current.userData.fixedDir = explodeDir; // 방향 고정
        
        logicalPartsRef.current.set(current.name, current);
      }
    }
  });
}, [parts, isModelReady]);

// ═══ 3) 조립/분해 애니메이션 (절대 좌표 기반) ═══
useEffect(() => {
  if (!isModelReady || logicalPartsRef.current.size === 0) return;

  let animationFrameId;
  const lerpFactor = 0.05; // 💡 극강의 묵직함 (0.01~0.02 추천)
  const explosionStrength = 0.1; // 퍼지는 강도

  const animate = () => {
    let isMoving = false;
    const progress = assemblyProgress / 100; // 1이면 조립, 0이면 분해

    logicalPartsRef.current.forEach((part, partName) => {
      const homePos = originalPositionsRef.current.get(partName);
      const explodeDir = part.userData.fixedDir;
      if (!homePos) return;

      // 💡 [논리 구조] 목표 위치 = DB 고향 + (방향 * 분해 거리)
      // progress가 1일 때 moveDistance는 0이 되어 정확히 homePos가 됨
      const moveDistance = progress * explosionStrength;
      const targetPos = homePos.clone().add(explodeDir.clone().multiplyScalar(moveDistance));

      // 현재 위치에서 목표 위치로 서서히 이동
      part.position.lerp(targetPos, lerpFactor);

      // 목표 지점 도달 체크 (부드러운 루프를 위해 계속 실행)
      if (part.position.distanceTo(targetPos) > 0.0001) {
        isMoving = true;
      } else if (progress === 0) {
        part.position.copy(homePos); // 완벽하게 꽂아넣기
      }
    });

    if (isMoving) {
      animationFrameId = requestAnimationFrame(animate);
    }
  };

  animate();
  return () => cancelAnimationFrame(animationFrameId);
}, [assemblyProgress, isModelReady]);

  // ═══ 부품 하이라이트 ═══
  useEffect(() => {
    if (logicalPartsRef.current.size === 0) return;

    clickableMeshesRef.current.forEach((mesh) => {
      if (mesh.material) {
        mesh.material.emissive.set(0x000000);
        mesh.material.emissiveIntensity = 0;
      }
    });
    if (!selectedPartKey) return;


    const selectedPart = parts.find((p) => {
      if (p?.id && selectedPartKey === `id:${p.id}`) return true;
      if (p?.meshName && selectedPartKey === `mesh:${p.meshName}`) return true;
      return false;
    });

     if (!selectedPart) return;

 

    console.log(`[ThreeViewer] Highlighting: ${selectedPart.meshName}`);
    const targetGroup = logicalPartsRef.current.get(selectedPart.meshName);
    if (targetGroup) {
      targetGroup.traverse((child) => {
        if (child.isMesh && child.material) {
          child.material.emissive.set(0x00e5ff);
          child.material.emissiveIntensity = 0.5;
        }
      });
  }

  }, [selectedPartKey, parts]);

  // ═══ 부품 클릭 감지 ═══
  useEffect(() => {
    if (!rendererRef.current || !onPartClick) return;

    const handleClick = (event) => {
      if (!cameraRef.current || !sceneRef.current) return;

      // 마우스 좌표 정규화 (-1 ~ 1)
      const rect = rendererRef.current.domElement.getBoundingClientRect();
      mouseRef.current.x = ((event.clientX - rect.left) / rect.width) * 2 - 1;
      mouseRef.current.y = -((event.clientY - rect.top) / rect.height) * 2 + 1;

      // Raycasting
      raycasterRef.current.setFromCamera(mouseRef.current, cameraRef.current);

      const intersects = raycasterRef.current.intersectObjects(clickableMeshesRef.current, true);

      if (intersects.length > 0) {
        const clickedMesh = intersects[0].object;
        const partData = clickedMesh.userData.partData;

        if (partData) {
          console.log(`[ThreeViewer] Logical Part Clicked: ${partData.meshName}`);
          onPartClick(partData);
        }        
      }else{
          onPartClick(null); console.log("[ThreeViewer] 빈 공간 클릭: 선택 해제");
      }

    };

    rendererRef.current.domElement.addEventListener("click", handleClick);
    return () => rendererRef.current?.domElement.removeEventListener("click", handleClick);
}, [onPartClick]);

  return (
    <div style={{ width: "100%", height: "100%", position: "relative" }}>
      <div ref={mountRef} style={{ width: "100%", height: "100%" }} />

      {loading && (
        <div
          style={{
            position: "absolute",
            top: "50%",
            left: "50%",
            transform: "translate(-50%, -50%)",
            color: "#7dd3e0",
            fontSize: "16px",
            fontWeight: "500",
            textAlign: "center",
          }}
        >
          <div style={{ marginBottom: "10px" }}>3D 모델 로딩 중...</div>
          <div
            style={{
              width: "40px",
              height: "40px",
              border: "3px solid #1a3a4a",
              borderTop: "3px solid #00e5ff",
              borderRadius: "50%",
              animation: "spin 1s linear infinite",
              margin: "0 auto",
            }}
          />
        </div>
      )}

      {error && (
        <div
          style={{
            position: "absolute",
            top: "50%",
            left: "50%",
            transform: "translate(-50%, -50%)",
            color: "#ff6b6b",
            fontSize: "14px",
            textAlign: "center",
            padding: "20px",
            background: "rgba(26, 42, 58, 0.9)",
            borderRadius: "8px",
            border: "1px solid rgba(255, 107, 107, 0.3)",
          }}
        >
          {error}
        </div>
      )}

      <style>{`
        @keyframes spin {
          0% { transform: rotate(0deg); }
          100% { transform: rotate(360deg); }
        }
      `}</style>
    </div>
  );
}