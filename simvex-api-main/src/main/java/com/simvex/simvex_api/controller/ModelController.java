// src/main/java/com/simvex/simvex_api/controller/ModelController.java
package com.simvex.simvex_api.controller;

import com.simvex.simvex_api.domain.*;
import com.simvex.simvex_api.dto.*;
import com.simvex.simvex_api.model.ModelEntity;
import com.simvex.simvex_api.model.ModelRepository;
import com.simvex.simvex_api.part.PartRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/models")
@CrossOrigin(origins = "*")
public class ModelController {

    private final ModelRepository modelRepository;
    private final PartRepository partRepository;
    private final QuizRepository quizRepository;
    private final MemoRepository memoRepository;

    public ModelController(ModelRepository modelRepository, PartRepository partRepository, 
                           QuizRepository quizRepository, MemoRepository memoRepository) {
        this.modelRepository = modelRepository;
        this.partRepository = partRepository;
        this.quizRepository = quizRepository;
        this.memoRepository = memoRepository;
    }

    // 모델 목록
    @GetMapping
    public List<ModelDto> listModels() {
        return modelRepository.findAll().stream()
                .map(ModelDto::from)
                .toList();
    }

    // 모델 상세
    @GetMapping("/{id}")
    public ResponseEntity<ModelDto> getModel(@PathVariable Long id) {
        return modelRepository.findById(id)
                .map(ModelDto::from)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // 부품 목록
    @GetMapping("/{id}/parts")
    public ResponseEntity<List<PartDto>> listParts(@PathVariable Long id) {
        if (!modelRepository.existsById(id)) return ResponseEntity.notFound().build();
        List<PartDto> parts = partRepository.findByModelIdOrderByIdAsc(id).stream()
                .map(PartDto::from)
                .toList();
        return ResponseEntity.ok(parts);
    }

    // 퀴즈 목록
    @GetMapping("/{id}/quizzes")
    public ResponseEntity<List<QuizDto>> listQuizzes(@PathVariable Long id) {
        List<QuizDto> quizzes = quizRepository.findByModelIdOrderByIdAsc(id).stream()
                .map(QuizDto::from)
                .toList();
        return ResponseEntity.ok(quizzes);
    }

    // 모의고사 (랜덤 20문제)
    @GetMapping("/exam")
    public ResponseEntity<List<QuizDto>> generateExam(@RequestParam List<Long> modelIds) {
        List<QuizEntity> allQuizzes = quizRepository.findByModelIds(modelIds);
        Collections.shuffle(allQuizzes);
        List<QuizDto> selected = allQuizzes.stream()
                .limit(20)
                .map(QuizDto::from)
                .toList();
        return ResponseEntity.ok(selected);
    }

    // 메모 목록
    @GetMapping("/{id}/memos")
    public ResponseEntity<List<MemoDto>> listMemos(@PathVariable Long id) {
        List<MemoDto> memos = memoRepository.findByModelIdOrderByIdAsc(id).stream()
                .map(MemoDto::from)
                .toList();
        return ResponseEntity.ok(memos);
    }

    // 메모 생성 (dto.getTitle() 사용)
    @PostMapping("/{id}/memos")
    public ResponseEntity<MemoDto> createMemo(@PathVariable Long id, @RequestBody MemoDto dto) {
        return modelRepository.findById(id).map(model -> {
            // ✅ dto.getTitle(), dto.getContent() 사용
            MemoEntity entity = new MemoEntity(dto.getTitle(), dto.getContent());
            entity.setModel(model);
            MemoEntity saved = memoRepository.save(entity);
            return ResponseEntity.ok(MemoDto.from(saved));
        }).orElse(ResponseEntity.notFound().build());
    }
}