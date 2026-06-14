package com.example.Back.character.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;
/**
 * Character access rule entity persisted in the {@code character_access} table.
 */

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
