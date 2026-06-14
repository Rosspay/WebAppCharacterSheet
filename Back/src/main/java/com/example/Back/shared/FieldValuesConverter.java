package com.example.Back.shared;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.r2dbc.postgresql.codec.Json;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;

import java.util.Map;
/**
 * R2DBC converter for the {@code jsonb} ↔ {@code Map<String,Object>} mapping used by character field values.
 */

public class FieldValuesConverter {

    private static final TypeReference<Map<String, Object>> TYPE_REF = new TypeReference<>() {};

    @WritingConverter
    @RequiredArgsConstructor
    public static class FieldValuesWriter implements Converter<Map<String, Object>, Json> {
        private final ObjectMapper objectMapper;
        @Override
        @SneakyThrows
        public Json convert(Map<String, Object> source) {
            return Json.of(objectMapper.writeValueAsString(source));
        }
    }

    @ReadingConverter
    @RequiredArgsConstructor
    public static class FieldValuesReader implements Converter<Json, Map<String, Object>> {
        private final ObjectMapper objectMapper;
        @Override
        @SneakyThrows
        public Map<String, Object> convert(Json source) {
            return objectMapper.readValue(source.asString(), TYPE_REF);
        }
    }
}
