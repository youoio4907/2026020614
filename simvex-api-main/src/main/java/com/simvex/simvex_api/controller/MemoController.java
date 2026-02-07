package com.simvex.simvex_api.controller;

import com.simvex.simvex_api.domain.MemoEntity;
import com.simvex.simvex_api.domain.MemoRepository;
import com.simvex.simvex_api.dto.MemoDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/memos")
@CrossOrigin(origins = "*")
public class MemoController {

    private final MemoRepository memoRepository;

    public MemoController(MemoRepository memoRepository) {
        this.memoRepository = memoRepository;
    }

    // 메모 수정 (저장)
    @PutMapping("/{id}")
    public ResponseEntity<MemoDto> updateMemo(@PathVariable Long id, @RequestBody MemoDto dto) {
        return memoRepository.findById(id).map(memo -> {
            memo.setTitle(dto.getTitle());
            memo.setContent(dto.getContent());
            MemoEntity updated = memoRepository.save(memo);
            return ResponseEntity.ok(MemoDto.from(updated));
        }).orElse(ResponseEntity.notFound().build());
    }

    // 메모 삭제
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMemo(@PathVariable Long id) {
        if (memoRepository.existsById(id)) {
            memoRepository.deleteById(id);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }
}