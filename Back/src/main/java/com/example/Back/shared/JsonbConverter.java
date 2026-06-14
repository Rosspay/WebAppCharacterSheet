package com.example.Back.shared;

import com.example.Back.template.entity.TemplateNode;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.r2dbc.postgresql.codec.Json;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;

import java.util.List;
/**
 * R2DBC converter for the {@code jsonb} ↔ {@link com.example.Back.template.TemplateNode} mapping used by template trees.
 */


public class JsonbConverter {

    @WritingConverter
    @RequiredArgsConstructor
    public static class TemplateContentWriter
            implements Converter<List<TemplateNode>, Json> {

        private final ObjectMapper objectMapper;

        @Override
        @SneakyThrows
        public Json convert(List<TemplateNode> source) {
            return Json.of(objectMapper.writeValueAsString(source));
        }
    }

    @ReadingConverter
    @RequiredArgsConstructor
    public static class TemplateContentReader
            implements Converter<Json, List<TemplateNode>> {

        private final ObjectMapper objectMapper;
        private static final TypeReference<List<TemplateNode>> TYPE_REF =
                new TypeReference<>() {};

        @Override
        @SneakyThrows
        public List<TemplateNode> convert(Json source) {
            return objectMapper.readValue(source.asString(), TYPE_REF);
        }
    }
}
