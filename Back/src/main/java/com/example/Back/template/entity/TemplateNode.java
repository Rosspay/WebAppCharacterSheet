package com.example.Back.template.entity;


import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.util.List;


@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXISTING_PROPERTY,
        property = "type",
        visible = true
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = TemplateNode.ContainerNode.class,  name = "CONTAINER"),
        @JsonSubTypes.Type(value = TemplateNode.BlockNode.class,      name = "BLOCK"),
        @JsonSubTypes.Type(value = TemplateNode.CounterNode.class,    name = "COUNTER"),
        @JsonSubTypes.Type(value = TemplateNode.TableNode.class,      name = "TABLE"),
        @JsonSubTypes.Type(value = TemplateNode.TextFieldNode.class,  name = "TEXT_FIELD"),
})
/**
 * Template tree node: contains its type, key, metadata and children.
 */
public sealed interface TemplateNode
        permits TemplateNode.ContainerNode,
        TemplateNode.BlockNode,
        TemplateNode.CounterNode,
        TemplateNode.TableNode,
        TemplateNode.TextFieldNode {

    String id();
    int    order();
    String type();

    record ContainerNode(
            @JsonProperty("id")       String id,
            @JsonProperty("order")    int order,
            @JsonProperty("title")    String title,
            @JsonProperty("type")     String type,
            @JsonProperty("children") List<TemplateNode> children
    ) implements TemplateNode {
        @JsonCreator
        public ContainerNode {}
    }

    record BlockNode(
            @JsonProperty("id")           String id,
            @JsonProperty("order")        int order,
            @JsonProperty("label")        String label,
            @JsonProperty("type")         String type,
            @JsonProperty("defaultValue") Integer defaultValue
    ) implements TemplateNode {
        @JsonCreator
        public BlockNode {}
    }

    record CounterNode(
            @JsonProperty("id")           String id,
            @JsonProperty("order")        int order,
            @JsonProperty("label")        String label,
            @JsonProperty("type")         String type,
            @JsonProperty("currentValue") int currentValue,
            @JsonProperty("maxValue")     Integer maxValue
    ) implements TemplateNode {
        @JsonCreator
        public CounterNode {}
    }

    record TableNode(
            @JsonProperty("id")      String id,
            @JsonProperty("order")   int order,
            @JsonProperty("label")   String label,
            @JsonProperty("type")    String type,
            @JsonProperty("rows")    int rows,
            @JsonProperty("columns") int columns
    ) implements TemplateNode {
        @JsonCreator
        public TableNode {}
    }

    record TextFieldNode(
            @JsonProperty("id")          String id,
            @JsonProperty("order")       int order,
            @JsonProperty("type")        String type,
            @JsonProperty("placeholder") String placeholder
    ) implements TemplateNode {
        @JsonCreator
        public TextFieldNode {}
    }
}