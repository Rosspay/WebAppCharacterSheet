package com.example.Back.shared;

import com.example.Back.template.entity.TemplateNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.r2dbc.postgresql.codec.Json;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;


class JsonbConverterTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("UT-B-29: roundtrip списка из всех 5 типов узлов")
    void roundtrip_allNodeTypes() {
        var content = List.<TemplateNode>of(
                new TemplateNode.ContainerNode("c1", 0, "Атрибуты", "CONTAINER",
                        List.of(
                                new TemplateNode.BlockNode("b1", 0, "Сила",
                                        "BLOCK", 10),
                                new TemplateNode.CounterNode("cnt1", 1,
                                        "ОЗ", "COUNTER", 5, 10))),
                new TemplateNode.TableNode("t1", 1, "Инвентарь", "TABLE", 3, 3),
                new TemplateNode.TextFieldNode("tf1", 2, "TEXT_FIELD",
                        "Описание...")
        );

        var writer = new JsonbConverter.TemplateContentWriter(objectMapper);
        var reader = new JsonbConverter.TemplateContentReader(objectMapper);

        Json serialized = writer.convert(content);
        assertThat(serialized).isNotNull();
        assertThat(serialized.asString()).contains("CONTAINER")
                .contains("BLOCK").contains("COUNTER")
                .contains("TABLE").contains("TEXT_FIELD");

        List<TemplateNode> restored = reader.convert(serialized);

        assertThat(restored).hasSize(3);
        assertThat(restored.get(0)).isInstanceOf(TemplateNode.ContainerNode.class);
        TemplateNode.ContainerNode container = (TemplateNode.ContainerNode) restored.get(0);
        assertThat(container.children()).hasSize(2);
        assertThat(container.children().get(0)).isInstanceOf(TemplateNode.BlockNode.class);
        assertThat(container.children().get(1)).isInstanceOf(TemplateNode.CounterNode.class);
        assertThat(restored.get(1)).isInstanceOf(TemplateNode.TableNode.class);
        assertThat(restored.get(2)).isInstanceOf(TemplateNode.TextFieldNode.class);
    }
}
