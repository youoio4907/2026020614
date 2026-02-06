// src/main/java/com/simvex/simvex_api/ai/PromptTemplateService.java
package com.simvex.simvex_api.ai;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@Service
public class PromptTemplateService {

    public String render(
            String mode,
            String context,
            String question,
            String notes
    ) {
        String templateName = chooseTemplate(mode, notes);
        String template = loadTemplate(templateName);

        return template
                .replace("{{context}}", safe(context))
                .replace("{{question}}", safe(question))
                .replace("{{notes}}", safe(notes));
    }

    private String chooseTemplate(String mode, String notes) {
        if ("PART".equals(mode)) {
            if (notes != null && !notes.isBlank()) {
                return "prompts/part_with_notes.txt";
            }
            return "prompts/part.txt";
        }
        return "prompts/global.txt";
    }

    private String loadTemplate(String path) {
        try {
            ClassPathResource res = new ClassPathResource(path);
            return new String(res.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Prompt template load failed: " + path, e);
        }
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }
}
