package com.example.Back.character.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("character_access")
public class CharacterAccess {
    @Id
    private Long id;
    private Long characterId;
    private Long userId;
}
