package com.simvex.simvex_api.controller;

import com.simvex.simvex_api.domain.*;
import com.simvex.simvex_api.dto.WorkflowDto;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/workflow")
@CrossOrigin(origins = "*")
public class WorkflowController {

    private final WorkflowNodeRepository nodeRepo;
    private final WorkflowConnectionRepository connRepo;
    private final WorkflowAttachmentRepository fileRepo;
    
    // 파일 저장 경로
    private final String UPLOAD_DIR = "uploads/workflow/";

    public WorkflowController(WorkflowNodeRepository nodeRepo, WorkflowConnectionRepository connRepo, WorkflowAttachmentRepository fileRepo) {
        this.nodeRepo = nodeRepo;
        this.connRepo = connRepo;
        this.fileRepo = fileRepo;
    }

    // --- 기본 CRUD ---

    @GetMapping
    public ResponseEntity<?> getAllWorkflow() {
        List<WorkflowNodeEntity> nodes = nodeRepo.findAll();
        List<WorkflowConnectionEntity> conns = connRepo.findAll();

        List<WorkflowDto.NodeResponse> nodeDtos = nodes.stream().map(n -> {
            WorkflowDto.NodeResponse dto = new WorkflowDto.NodeResponse();
            dto.id = n.getId();
            dto.title = n.getTitle();
            dto.content = n.getContent();
            dto.x = n.getX();
            dto.y = n.getY();
            dto.files = n.getAttachments().stream().map(f -> {
                WorkflowDto.FileResponse fd = new WorkflowDto.FileResponse();
                fd.id = f.getId();
                fd.fileName = f.getOriginalFileName();
                fd.url = "/api/workflow/files/download/" + f.getId();
                return fd;
            }).collect(Collectors.toList());
            return dto;
        }).collect(Collectors.toList());

        List<WorkflowDto.ConnectionResponse> connDtos = conns.stream().map(c -> {
            WorkflowDto.ConnectionResponse dto = new WorkflowDto.ConnectionResponse();
            dto.id = c.getId();
            dto.from = c.getFromNodeId();
            dto.to = c.getToNodeId();
            dto.fromAnchor = c.getFromAnchor();
            dto.toAnchor = c.getToAnchor();
            return dto;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(java.util.Map.of("nodes", nodeDtos, "connections", connDtos));
    }

    @PostMapping("/nodes")
    public ResponseEntity<?> createNode(@RequestBody WorkflowDto.NodeRequest req) {
        WorkflowNodeEntity node = new WorkflowNodeEntity(req.title, req.content, req.x, req.y);
        return ResponseEntity.ok(nodeRepo.save(node).getId());
    }

    @PutMapping("/nodes/{id}")
    public ResponseEntity<?> updateNode(@PathVariable Long id, @RequestBody WorkflowDto.NodeRequest req) {
        return nodeRepo.findById(id).map(node -> {
            if(req.title != null) node.setTitle(req.title);
            if(req.content != null) node.setContent(req.content);
            if(req.x != null) node.setX(req.x);
            if(req.y != null) node.setY(req.y);
            nodeRepo.save(node);
            return ResponseEntity.ok().build();
        }).orElse(ResponseEntity.notFound().build());
    }

    // [수정됨] 노드 삭제 시 -> 실제 파일도 삭제
    @DeleteMapping("/nodes/{id}")
    @Transactional
    public ResponseEntity<?> deleteNode(@PathVariable Long id) {
        WorkflowNodeEntity node = nodeRepo.findById(id).orElse(null);
        if (node == null) return ResponseEntity.notFound().build();

        // 1. 노드에 포함된 모든 첨부파일을 실제 디스크에서 삭제
        for (WorkflowAttachmentEntity file : node.getAttachments()) {
            deleteFileFromDisk(file.getFilePath());
        }

        // 2. 연결선 삭제
        connRepo.deleteByFromNodeIdOrToNodeId(id, id);

        // 3. 노드 삭제 (DB의 첨부파일 데이터는 Cascade로 자동 삭제됨)
        nodeRepo.delete(node);
        
        return ResponseEntity.ok().build();
    }

    @PostMapping("/connections")
    public ResponseEntity<?> createConnection(@RequestBody WorkflowDto.ConnectionRequest req) {
        connRepo.save(new WorkflowConnectionEntity(req.from, req.to, req.fromAnchor, req.toAnchor));
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/connections")
    @Transactional
    public ResponseEntity<?> deleteConnection(@RequestParam Long from, @RequestParam Long to) {
        connRepo.deleteByFromNodeIdOrToNodeId(from, to); // 기존 로직 활용 (단순화)
        return ResponseEntity.ok().build();
    }

    // --- 파일 관련 ---

    @PostMapping("/nodes/{nodeId}/files")
    public ResponseEntity<?> uploadFile(@PathVariable Long nodeId, @RequestParam("file") MultipartFile file) throws IOException {
        WorkflowNodeEntity node = nodeRepo.findById(nodeId)
                .orElseThrow(() -> new RuntimeException("Node not found"));

        Path uploadPath = Paths.get(UPLOAD_DIR);
        if (!Files.exists(uploadPath)) Files.createDirectories(uploadPath);

        String originalName = file.getOriginalFilename();
        String storedName = UUID.randomUUID() + "_" + originalName;
        Path filePath = uploadPath.resolve(storedName);

        Files.copy(file.getInputStream(), filePath);

        WorkflowAttachmentEntity attachment = new WorkflowAttachmentEntity(
                originalName, storedName, filePath.toString(),
                file.getSize(), file.getContentType(), node
        );
        fileRepo.save(attachment);

        return ResponseEntity.ok().build();
    }

    @GetMapping("/files/download/{id}")
    public ResponseEntity<Resource> downloadFile(@PathVariable Long id) {
        try {
            WorkflowAttachmentEntity fileEntity = fileRepo.findById(id)
                    .orElseThrow(() -> new RuntimeException("File not found"));

            Path filePath = Paths.get(fileEntity.getFilePath());
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() || resource.isReadable()) {
                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(fileEntity.getContentType()))
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileEntity.getOriginalFileName() + "\"")
                        .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (MalformedURLException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // [추가됨] 개별 파일 삭제 기능
    @DeleteMapping("/files/{id}")
    public ResponseEntity<?> deleteFile(@PathVariable Long id) {
        WorkflowAttachmentEntity fileEntity = fileRepo.findById(id).orElse(null);
        if (fileEntity == null) return ResponseEntity.notFound().build();

        // 1. 실제 디스크에서 파일 삭제
        deleteFileFromDisk(fileEntity.getFilePath());

        // 2. DB에서 삭제
        fileRepo.delete(fileEntity);

        return ResponseEntity.ok().build();
    }

    // 파일 삭제 헬퍼 메소드
    private void deleteFileFromDisk(String pathStr) {
        try {
            Path path = Paths.get(pathStr);
            Files.deleteIfExists(path);
            System.out.println("Deleted file: " + pathStr);
        } catch (IOException e) {
            System.err.println("Failed to delete file: " + pathStr + " (" + e.getMessage() + ")");
        }
    }
}