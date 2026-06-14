package com.example.Back.character.service;

import com.example.Back.character.dto.CharacterRequest;
import com.example.Back.character.dto.VisibilityRequest;
import com.example.Back.character.entity.Character;
import com.example.Back.character.entity.CharacterAccess;
import com.example.Back.character.repository.CharacterAccessRepository;
import com.example.Back.character.repository.CharacterRepository;
import com.example.Back.entity.User;
import com.example.Back.repository.UserRepository;
import com.example.Back.template.entity.Template;
import com.example.Back.template.repository.TemplateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
class CharacterServiceTest {

    @Mock private CharacterRepository characterRepository;
    @Mock private CharacterAccessRepository characterAccessRepository;
    @Mock private UserRepository userRepository;
    @Mock private TemplateRepository templateRepository;
    @Mock private CharacterPdfService characterPdfService;

    private CharacterService characterService;

    @BeforeEach
    void setUp() {
        characterService = new CharacterService(characterRepository,
                characterAccessRepository, userRepository, templateRepository,
                characterPdfService);
    }

    private User aUser() {
        return User.builder().id(42L).username("alice").build();
    }

    private Template aPublicTemplate(long ownerId) {
        return Template.builder().id(1L).ownerId(ownerId).isPublic(true)
                .title("DnD").content(List.of()).build();
    }

    private Template aPrivateTemplate(long ownerId) {
        return Template.builder().id(1L).ownerId(ownerId).isPublic(false)
                .title("DnD").content(List.of()).build();
    }

    private Character aCharacter(long id, long ownerId, String visibility) {
        return Character.builder()
                .id(id).ownerId(ownerId).templateId(1L)
                .name("Hero").description("d")
                .visibility(visibility)
                .fieldValues(Map.of())
                .build();
    }

    private CharacterRequest aRequest(String visibility, List<String> allowedUsernames) {
        return new CharacterRequest(1L, "Hero", "d", visibility,
                Map.of("hp", 10), allowedUsernames);
    }

    @Test
    @DisplayName("UT-B-30: create с публичным шаблоном — успех, по умолчанию PRIVATE")
    void create_withPublicTemplate_defaultsToPrivate() {
        when(userRepository.findByUsername("alice")).thenReturn(Mono.just(aUser()));
        when(templateRepository.findById(1L)).thenReturn(Mono.just(aPublicTemplate(99L)));
        when(characterRepository.save(any(Character.class)))
                .thenAnswer(inv -> {
                    Character c = inv.getArgument(0);
                    c.setId(10L);
                    return Mono.just(c);
                });
        when(characterAccessRepository.deleteAllByCharacterId(10L)).thenReturn(Mono.empty());
        when(characterAccessRepository.findAllByCharacterId(10L)).thenReturn(Flux.empty());

        StepVerifier.create(characterService.create(aRequest(null, null), "alice"))
                .assertNext(c -> {
                    assertThat(c.visibility()).isEqualTo("PRIVATE");
                    assertThat(c.ownerId()).isEqualTo(42L);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("UT-B-31: create с приватным чужим шаблоном → 403")
    void create_withForeignPrivateTemplate_forbidden() {
        when(userRepository.findByUsername("alice")).thenReturn(Mono.just(aUser()));
        when(templateRepository.findById(1L)).thenReturn(Mono.just(aPrivateTemplate(99L)));

        StepVerifier.create(characterService.create(aRequest("PRIVATE", null), "alice"))
                .expectErrorMatches(t -> t instanceof ResponseStatusException
                        && ((ResponseStatusException) t).getStatusCode() == HttpStatus.FORBIDDEN)
                .verify();
    }

    @Test
    @DisplayName("UT-B-32: create RESTRICTED+allowedUsernames создаёт записи доступа")
    void create_restricted_persistsAccessList() {
        when(userRepository.findByUsername("alice")).thenReturn(Mono.just(aUser()));
        when(userRepository.findByUsername("bob"))
                .thenReturn(Mono.just(User.builder().id(3L).username("bob").build()));
        when(userRepository.findByUsername("carol"))
                .thenReturn(Mono.just(User.builder().id(7L).username("carol").build()));
        when(userRepository.findById(3L))
                .thenReturn(Mono.just(User.builder().id(3L).username("bob").build()));
        when(userRepository.findById(7L))
                .thenReturn(Mono.just(User.builder().id(7L).username("carol").build()));
        when(templateRepository.findById(1L)).thenReturn(Mono.just(aPublicTemplate(42L)));
        when(characterRepository.save(any(Character.class)))
                .thenAnswer(inv -> {
                    Character c = inv.getArgument(0);
                    c.setId(10L);
                    return Mono.just(c);
                });
        when(characterAccessRepository.deleteAllByCharacterId(10L)).thenReturn(Mono.empty());
        when(characterAccessRepository.save(any(CharacterAccess.class)))
                .thenAnswer(inv -> Mono.just(inv.getArgument(0, CharacterAccess.class)));
        when(characterAccessRepository.findAllByCharacterId(10L))
                .thenReturn(Flux.just(
                        CharacterAccess.builder().userId(3L).build(),
                        CharacterAccess.builder().userId(7L).build()));

        var req = aRequest("RESTRICTED", List.of("bob", "carol"));

        StepVerifier.create(characterService.create(req, "alice"))
                .assertNext(c -> assertThat(c.allowedUsernames())
                        .containsExactlyInAnyOrder("bob", "carol"))
                .verifyComplete();

        ArgumentCaptor<CharacterAccess> captor =
                ArgumentCaptor.forClass(CharacterAccess.class);
        verify(characterAccessRepository, atLeast(2)).save(captor.capture());
        assertThat(captor.getAllValues())
                .extracting(CharacterAccess::getUserId)
                .containsExactlyInAnyOrder(3L, 7L);
    }

    @Test
    @DisplayName("UT-B-33: setVisibility RESTRICTED→PUBLIC очищает access-list")
    void setVisibility_publicClearsAccessList() {
        when(userRepository.findByUsername("alice")).thenReturn(Mono.just(aUser()));
        when(characterRepository.findById(10L))
                .thenReturn(Mono.just(aCharacter(10L, 42L, "RESTRICTED")));
        when(characterRepository.save(any(Character.class)))
                .thenAnswer(inv -> Mono.just(inv.getArgument(0, Character.class)));
        when(characterAccessRepository.deleteAllByCharacterId(10L)).thenReturn(Mono.empty());
        when(characterAccessRepository.findAllByCharacterId(10L)).thenReturn(Flux.empty());

        var req = new VisibilityRequest("PUBLIC", null);

        StepVerifier.create(characterService.setVisibility(10L, req, "alice"))
                .assertNext(c -> {
                    assertThat(c.visibility()).isEqualTo("PUBLIC");
                    assertThat(c.allowedUsernames()).isEmpty();
                })
                .verifyComplete();

        verify(characterAccessRepository).deleteAllByCharacterId(10L);
        verify(characterAccessRepository, never()).save(any());
    }

    @Test
    @DisplayName("UT-B-34: getById чужого PRIVATE персонажа → 403")
    void getById_foreignPrivate_forbidden() {
        when(userRepository.findByUsername("alice")).thenReturn(Mono.just(aUser()));
        when(characterRepository.findById(10L))
                .thenReturn(Mono.just(aCharacter(10L, 99L, "PRIVATE")));

        lenient().when(characterAccessRepository.existsByCharacterIdAndUserId(anyLong(), anyLong()))
                .thenReturn(Mono.just(false));

        StepVerifier.create(characterService.getById(10L, "alice"))
                .expectErrorMatches(t -> t instanceof ResponseStatusException
                        && ((ResponseStatusException) t).getStatusCode() == HttpStatus.FORBIDDEN)
                .verify();
    }

    @Test
    @DisplayName("UT-B-35: getById чужого PUBLIC персонажа — доступ открыт")
    void getById_foreignPublic_ok() {
        when(userRepository.findByUsername("alice")).thenReturn(Mono.just(aUser()));
        when(characterRepository.findById(10L))
                .thenReturn(Mono.just(aCharacter(10L, 99L, "PUBLIC")));
        when(characterAccessRepository.findAllByCharacterId(10L)).thenReturn(Flux.empty());

        StepVerifier.create(characterService.getById(10L, "alice"))
                .assertNext(c -> assertThat(c.id()).isEqualTo(10L))
                .verifyComplete();
    }

    @Test
    @DisplayName("UT-B-36: getById RESTRICTED + пользователь в access — доступ открыт")
    void getById_restricted_withAccess_ok() {
        when(userRepository.findByUsername("alice")).thenReturn(Mono.just(aUser()));
        when(characterRepository.findById(10L))
                .thenReturn(Mono.just(aCharacter(10L, 99L, "RESTRICTED")));
        when(characterAccessRepository.existsByCharacterIdAndUserId(10L, 42L))
                .thenReturn(Mono.just(true));
        when(characterAccessRepository.findAllByCharacterId(10L))
                .thenReturn(Flux.just(CharacterAccess.builder().userId(42L).build()));
        when(userRepository.findById(42L))
                .thenReturn(Mono.just(User.builder().id(42L).username("alice").build()));

        StepVerifier.create(characterService.getById(10L, "alice"))
                .assertNext(c -> assertThat(c.id()).isEqualTo(10L))
                .verifyComplete();
    }

    @Test
    @DisplayName("UT-B-37: getById RESTRICTED + пользователь НЕ в access → 403")
    void getById_restricted_withoutAccess_forbidden() {
        when(userRepository.findByUsername("alice")).thenReturn(Mono.just(aUser()));
        when(characterRepository.findById(10L))
                .thenReturn(Mono.just(aCharacter(10L, 99L, "RESTRICTED")));
        when(characterAccessRepository.existsByCharacterIdAndUserId(10L, 42L))
                .thenReturn(Mono.just(false));

        StepVerifier.create(characterService.getById(10L, "alice"))
                .expectErrorMatches(t -> t instanceof ResponseStatusException
                        && ((ResponseStatusException) t).getStatusCode() == HttpStatus.FORBIDDEN)
                .verify();
    }

    @Test
    @DisplayName("UT-B-38: update чужого персонажа → 403")
    void update_foreign_forbidden() {
        when(userRepository.findByUsername("alice")).thenReturn(Mono.just(aUser()));
        when(characterRepository.findById(10L))
                .thenReturn(Mono.just(aCharacter(10L, 99L, "PRIVATE")));

        StepVerifier.create(characterService.update(10L,
                        aRequest("PRIVATE", null), "alice"))
                .expectErrorMatches(t -> t instanceof ResponseStatusException
                        && ((ResponseStatusException) t).getStatusCode() == HttpStatus.FORBIDDEN)
                .verify();
    }

    @Test
    @DisplayName("UT-B-39: delete своего персонажа: сначала access, потом character")
    void delete_own_callsAccessDeleteFirst() {
        when(userRepository.findByUsername("alice")).thenReturn(Mono.just(aUser()));
        when(characterRepository.findById(10L))
                .thenReturn(Mono.just(aCharacter(10L, 42L, "PRIVATE")));
        when(characterAccessRepository.deleteAllByCharacterId(10L)).thenReturn(Mono.empty());
        when(characterRepository.deleteById(10L)).thenReturn(Mono.empty());

        StepVerifier.create(characterService.delete(10L, "alice")).verifyComplete();

        var ordered = org.mockito.Mockito.inOrder(characterAccessRepository,
                characterRepository);
        ordered.verify(characterAccessRepository).deleteAllByCharacterId(10L);
        ordered.verify(characterRepository).deleteById(10L);
    }
}
