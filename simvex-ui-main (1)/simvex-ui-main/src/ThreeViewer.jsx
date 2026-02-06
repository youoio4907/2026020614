// ThreeViewer.jsx
import { useEffect, useRef, useState } from "react";
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

  const meshesRef = useRef(new Map()); // meshName -> mesh object
  const originalPositionsRef = useRef(new Map()); // meshName -> original position

  const raycasterRef = useRef(new THREE.Raycaster());
  const mouseRef = useRef(new THREE.Vector2());

  const resizeObserverRef = useRef(null);

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // ═══ 초기 설정 ═══
  useEffect(() => {
    if (!mountRef.current) return;

    // Scene 생성
    const scene = new THREE.Scene();
    scene.background = new THREE.Color(0x0a1520);
    sceneRef.current = scene;

    // Camera 생성
    const camera = new THREE.PerspectiveCamera(
      50,
      mountRef.current.clientWidth / mountRef.current.clientHeight,
      0.1,
      1000
    );
    camera.position.set(3, 2, 5);
    cameraRef.current = camera;

    // Renderer 생성
    const renderer = new THREE.WebGLRenderer({ antialias: true, alpha: true });
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
    const ambientLight = new THREE.AmbientLight(0xffffff, 0.6);
    scene.add(ambientLight);

    const directionalLight = new THREE.DirectionalLight(0xffffff, 0.8);
    directionalLight.position.set(10, 10, 5);
    directionalLight.castShadow = true;
    scene.add(directionalLight);

    const fillLight = new THREE.DirectionalLight(0x4a8aff, 0.3);
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

  // ═══ GLB 파일 로드 ═══
  useEffect(() => {
    if (!modelUrl || !sceneRef.current) return;

    setLoading(true);
    setError(null);

    const loader = new GLTFLoader();

    loader.load(
      modelUrl,
      (gltf) => {
        const model = gltf.scene;

        // 기존 모델 제거
        const existingModel = sceneRef.current.getObjectByName("loadedModel");
        if (existingModel) {
          sceneRef.current.remove(existingModel);
        }

        model.name = "loadedModel";

        // 모든 mesh 수집 및 원본 위치 저장
        meshesRef.current.clear();
        originalPositionsRef.current.clear();

        model.traverse((child) => {
          if (child.isMesh) {
            child.castShadow = true;
            child.receiveShadow = true;

            // mesh 이름으로 매핑 (GLB의 노드 이름 사용)
            const meshName = child.name || child.parent?.name;
            if (meshName) {
              meshesRef.current.set(meshName, child);

              // 원본 위치 저장 (부모 기준 상대 위치)
              originalPositionsRef.current.set(meshName, child.position.clone());

              console.log(`[ThreeViewer] Mesh found: ${meshName}`);
            }
          }
        });

        // 모델 크기 정규화
        const box = new THREE.Box3().setFromObject(model);
        const size = box.getSize(new THREE.Vector3());
        const maxDim = Math.max(size.x, size.y, size.z);
        const scale = 2 / maxDim; // 2 units 크기로 정규화
        model.scale.setScalar(scale);

        // 모델 중심 정렬
        const center = box.getCenter(new THREE.Vector3());
        model.position.sub(center.multiplyScalar(scale));

        sceneRef.current.add(model);
        setLoading(false);

        console.log(`[ThreeViewer] Model loaded: ${modelUrl}`);
        console.log(`[ThreeViewer] Found ${meshesRef.current.size} meshes`);

        // ✅ 모델 로드 직후에도 한번 리사이즈(토글 직후 로드되는 케이스 보정)
        if (mountRef.current && cameraRef.current && rendererRef.current) {
          const w = mountRef.current.clientWidth;
          const h = mountRef.current.clientHeight;
          if (w && h) {
            cameraRef.current.aspect = w / h;
            cameraRef.current.updateProjectionMatrix();
            rendererRef.current.setSize(w, h); // ✅ 여기 also false 제거
            rendererRef.current.setPixelRatio(Math.min(window.devicePixelRatio, 2));
          }
        }
      },
      (xhr) => {
        const pct = xhr.total ? (xhr.loaded / xhr.total) * 100 : 0;
        console.log(`[ThreeViewer] Loading: ${pct.toFixed(0)}%`);
      },
      (err) => {
        console.error("[ThreeViewer] Load error:", err);
        setError(`모델 로드 실패: ${err?.message || "unknown error"}`);
        setLoading(false);
      }
    );
  }, [modelUrl]);

  // ═══ 조립/분해 애니메이션 ═══
  useEffect(() => {
    if (!sceneRef.current || meshesRef.current.size === 0) return;

    const progress = assemblyProgress / 100; // 0~1
    const explosionFactor = 1.5; // 분해 시 이동 거리 배율

    meshesRef.current.forEach((mesh, meshName) => {
      const originalPos = originalPositionsRef.current.get(meshName);
      if (!originalPos) return;

      // 분해 방향: 원점에서 바깥쪽으로
      const explosionDir = new THREE.Vector3();

      // mesh의 월드 위치 기준으로 폭발 방향 계산
      const worldPos = new THREE.Vector3();
      mesh.getWorldPosition(worldPos);

      // 중심에서 바깥쪽으로 방향 벡터
      explosionDir.copy(worldPos).normalize();

      // 방향이 0인 경우 (정확히 중심) 임의 방향 설정
      if (explosionDir.length() < 0.01) {
        explosionDir
          .set(Math.random() - 0.5, Math.random() - 0.5, Math.random() - 0.5)
          .normalize();
      }

      // 분해된 위치 = 원본 위치 + (폭발 방향 * 거리)
      const distance = originalPos.length() * explosionFactor;
      const explodedPos = originalPos
        .clone()
        .add(explosionDir.multiplyScalar(distance * (1 - progress)));

      // 부드러운 보간 (ease-out)
      mesh.position.lerp(explodedPos, 0.1);
    });
  }, [assemblyProgress]);

  // ═══ 부품 하이라이트 ═══
  useEffect(() => {
    if (meshesRef.current.size === 0) return;

    if (!selectedPartKey) {
      // 모든 하이라이트 제거
      meshesRef.current.forEach((mesh) => {
        if (mesh.material) {
          mesh.material.emissive = new THREE.Color(0x000000);
          mesh.material.emissiveIntensity = 0;
        }
      });
      return;
    }

    // 선택된 부품 찾기
    const selectedPart = parts.find((p) => {
      if (p?.id && selectedPartKey === `id:${p.id}`) return true;
      if (p?.meshName && selectedPartKey === `mesh:${p.meshName}`) return true;
      return false;
    });

    if (!selectedPart?.meshName) return;

    console.log(`[ThreeViewer] Highlighting: ${selectedPart.meshName}`);

    // 모든 mesh 초기화 + 선택 mesh만 발광
    meshesRef.current.forEach((mesh, meshName) => {
      if (!mesh.material) return;

      if (meshName === selectedPart.meshName) {
        mesh.material.emissive = new THREE.Color(0x00e5ff);
        mesh.material.emissiveIntensity = 0.5;
      } else {
        mesh.material.emissive = new THREE.Color(0x000000);
        mesh.material.emissiveIntensity = 0;
      }
    });
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

      const intersects = raycasterRef.current.intersectObjects(
        sceneRef.current.children,
        true
      );

      if (intersects.length > 0) {
        const clickedMesh = intersects[0].object;
        const meshName = clickedMesh.name || clickedMesh.parent?.name;

        console.log(`[ThreeViewer] Clicked mesh: ${meshName}`);

        // parts에서 해당 mesh 찾기
        const part = parts.find((p) => p.meshName === meshName);
        if (part) {
          onPartClick(part);
        }
      }
    };

    rendererRef.current.domElement.addEventListener("click", handleClick);

    return () => {
      if (rendererRef.current?.domElement) {
        rendererRef.current.domElement.removeEventListener("click", handleClick);
      }
    };
  }, [parts, onPartClick]);

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
