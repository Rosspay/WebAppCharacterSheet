package com.example.Back.shared.config;

import com.example.Back.shared.FieldValuesConverter;
import com.example.Back.shared.JsonbConverter;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.r2dbc.convert.R2dbcCustomConversions;
import org.springframework.data.r2dbc.dialect.PostgresDialect;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class R2dbcConfig {

    private final ObjectMapper objectMapper;

    @Bean
    public R2dbcCustomConversions r2dbcCustomConversions() {

        var templateWriter = new JsonbConverter.TemplateContentWriter(objectMapper);
        var templateReader = new JsonbConverter.TemplateContentReader(objectMapper);

        var fieldWriter = new FieldValuesConverter.FieldValuesWriter(objectMapper);
        var fieldReader = new FieldValuesConverter.FieldValuesReader(objectMapper);

        return R2dbcCustomConversions.of(PostgresDialect.INSTANCE, List.of(templateWriter, templateReader, fieldWriter, fieldReader));
    }
}
