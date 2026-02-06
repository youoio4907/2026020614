// src/main/java/com/simvex/simvex_api/controller/ModelController.java
package com.simvex.simvex_api.controller;

import com.simvex.simvex_api.dto.ModelDto;
import com.simvex.simvex_api.dto.PartDto;
import com.simvex.simvex_api.model.ModelEntity;
import com.simvex.simvex_api.model.ModelRepository;
import com.simvex.simvex_api.part.PartRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/models")
@CrossOrigin(origins = "*")
public class ModelController {

    private final ModelRepository modelRepository;
    private final PartRepository partRepository;

    public ModelController(ModelRepository modelRepository, PartRepository partRepository) {
        this.modelRepository = modelRepository;
        this.partRepository = partRepository;
    }

    @GetMapping
    public List<ModelDto> listModels() {
        return modelRepository.findAll().stream()
                .map(ModelDto::from)
                .toList();
    }

    @GetMapping("/{id}/parts")
    public ResponseEntity<List<PartDto>> listParts(@PathVariable Long id) {
        ModelEntity model = modelRepository.findById(id).orElse(null);
        if (model == null) return ResponseEntity.notFound().build();

        List<PartDto> parts = partRepository.findByModelIdOrderByIdAsc(id).stream()
                .map(PartDto::from)
                .toList();

        return ResponseEntity.ok(parts);
    }
}
