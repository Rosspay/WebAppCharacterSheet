package com.example.Back.character.service;

import com.example.Back.character.entity.Character;
import com.example.Back.template.entity.Template;
import com.example.Back.template.entity.TemplateNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


class CharacterPdfServiceTest {

    private static final byte[] PDF_MAGIC = {'%', 'P', 'D', 'F'};

    private final CharacterPdfService service = new CharacterPdfService();

    private Character character(Map<String, Object> values) {
        return Character.builder()
                .id(1L).ownerId(2L).templateId(3L)
                .name("Гэндальф")
                .description("Серый волшебник")
                .visibility("PRIVATE")
                .fieldValues(values)
                .build();
    }

    @Test
    @DisplayName("UT-B-70: generate возвращает корректный PDF-байтстрим (магия %PDF)")
    void generate_returnsPdfMagic() {
        Template tmpl = Template.builder()
                .id(3L).ownerId(2L).isPublic(false).title("DnD 5e")
                .content(List.of()).build();

        byte[] bytes = service.generate(character(Map.of()), tmpl);

        assertThat(bytes.length).isGreaterThan(100);
        assertThat(bytes[0]).isEqualTo(PDF_MAGIC[0]);
        assertThat(bytes[1]).isEqualTo(PDF_MAGIC[1]);
        assertThat(bytes[2]).isEqualTo(PDF_MAGIC[2]);
        assertThat(bytes[3]).isEqualTo(PDF_MAGIC[3]);
    }

    @Test
    @DisplayName("UT-B-71: generate работает на null-шаблоне (мягкое поведение)")
    void generate_withNullTemplate() {
        byte[] bytes = service.generate(character(Map.of()), null);
        assertThat(bytes.length).isGreaterThan(100);
    }

    @Test
    @DisplayName("UT-B-72: generate обходит все типы TemplateNode без ошибок")
    void generate_rendersAllNodeTypes() {
        TemplateNode.BlockNode block = new TemplateNode.BlockNode(
                "lvl", 0, "Уровень", "BLOCK", 1);
        TemplateNode.CounterNode counter = new TemplateNode.CounterNode(
                "hp", 1, "Очки здоровья", "COUNTER", 10, 30);
        TemplateNode.TextFieldNode text = new TemplateNode.TextFieldNode(
                "bio", 2, "TEXT_FIELD", "Биография");
        TemplateNode.TableNode table = new TemplateNode.TableNode(
                "items", 3, "Инвентарь", "TABLE", 2, 2);
        TemplateNode.ContainerNode container = new TemplateNode.ContainerNode(
                "main", 4, "Основное", "CONTAINER",
                List.of(block, counter, text, table));

        Template tmpl = Template.builder()
                .id(3L).ownerId(2L).isPublic(true)
                .title("Полный шаблон")
                .content(List.of(container))
                .build();

        Map<String, Object> values = Map.of(
                "lvl", 5,
                "hp", 25,
                "bio", "Маг, странник, друг хоббитов",
                "items_0_0", "Посох",
                "items_0_1", "Меч",
                "items_1_0", "Книга заклинаний",
                "items_1_1", "Кошель"
        );

        byte[] bytes = service.generate(character(values), tmpl);
        assertThat(bytes.length).isGreaterThan(500);
    }

    @Test
    @DisplayName("UT-B-73: generate переживает отсутствующие значения для полей")
    void generate_withMissingFieldValues() {
        TemplateNode.BlockNode block = new TemplateNode.BlockNode(
                "lvl", 0, "Уровень", "BLOCK", 1);
        Template tmpl = Template.builder()
                .id(3L).ownerId(2L).isPublic(true).title("t")
                .content(List.of(block)).build();

        byte[] bytes = service.generate(character(Map.of()), tmpl);
        assertThat(bytes.length).isGreaterThan(100);
    }

    @Test
    @DisplayName("UT-B-74: generate корректно обрабатывает вложенные ContainerNode")
    void generate_withNestedContainers() {
        TemplateNode.BlockNode inner = new TemplateNode.BlockNode(
                "f", 0, "Сила", "BLOCK", 0);
        TemplateNode.ContainerNode child = new TemplateNode.ContainerNode(
                "inner", 0, "Атрибуты", "CONTAINER", List.of(inner));
        TemplateNode.ContainerNode outer = new TemplateNode.ContainerNode(
                "outer", 0, "Персонаж", "CONTAINER", List.of(child));

        Template tmpl = Template.builder()
                .id(3L).ownerId(2L).isPublic(false).title("Nested")
                .content(List.of(outer)).build();

        byte[] bytes = service.generate(character(Map.of("f", 15)), tmpl);
        assertThat(bytes.length).isGreaterThan(100);
    }

    @Test
    @DisplayName("UT-B-75: generate бросает IllegalStateException при null-character")
    void generate_throwsOnNullCharacter() {
        assertThatThrownBy(() -> service.generate(null, null))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("UT-B-76: generate с пустым TableNode (0 строк/столбцов) — не падает")
    void generate_withZeroSizedTable() {
        TemplateNode.TableNode table = new TemplateNode.TableNode(
                "t", 0, "Пусто", "TABLE", 0, 0);
        Template tmpl = Template.builder()
                .id(3L).ownerId(2L).isPublic(true).title("t")
                .content(List.of(table)).build();

        byte[] bytes = service.generate(character(Map.of()), tmpl);
        assertThat(bytes.length).isGreaterThan(100);
    }
}
