package com.example.Back.template.dto;

import com.example.Back.template.entity.TemplateNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record TemplateRequest(
        @NotBlank @Size(max = 150)
        String title,

        @Size(max = 1000)
        String description,

        @NotNull
        List<TemplateNode> content
) {}
