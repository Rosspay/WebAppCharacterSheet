package com.example.Back.template.service;

import com.example.Back.entity.User;
import com.example.Back.repository.UserRepository;
import com.example.Back.template.dto.TemplateRequest;
import com.example.Back.template.entity.Template;
import com.example.Back.template.entity.TemplateNode;
import com.example.Back.template.repository.TemplateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
class TemplateServiceTest {

    @Mock private TemplateRepository templateRepository;
    @Mock private UserRepository userRepository;

    private TemplateService templateService;

    @BeforeEach
    void setUp() {
        templateService = new TemplateService(templateRepository, userRepository);
    }

    private User aUser() {
        return User.builder().id(42L).username("alice").build();
    }

    private Template aTemplate(long ownerId, boolean isPublic) {
        return Template.builder()
                .id(7L)
                .ownerId(ownerId)
                .title("DnD 5e")
                .description("desc")
                .isPublic(isPublic)
                .content(List.of())
                .createdAt(LocalDateTime.now().minusDays(1))
                .updatedAt(LocalDateTime.now().minusDays(1))
                .build();
    }

    @Test
    @DisplayName("UT-B-16: create сохраняет шаблон с ownerId текущего пользователя")
    void create_setsOwnerIdAndSaves() {
        when(userRepository.findByUsername("alice")).thenReturn(Mono.just(aUser()));
        when(templateRepository.save(any(Template.class)))
                .thenAnswer(inv -> {
                    Template t = inv.getArgument(0);
                    t.setId(1L);
                    return Mono.just(t);
                });

        var req = new TemplateRequest("DnD 5e", "desc", List.of());

        StepVerifier.create(templateService.create(req, "alice"))
                .assertNext(resp -> {
                    assertThat(resp.ownerId()).isEqualTo(42L);
                    assertThat(resp.title()).isEqualTo("DnD 5e");
                })
                .verifyComplete();

        ArgumentCaptor<Template> captor = ArgumentCaptor.forClass(Template.class);
        verify(templateRepository).save(captor.capture());
        assertThat(captor.getValue().getOwnerId()).isEqualTo(42L);
    }

    @Test
    @DisplayName("UT-B-17: update чужого шаблона → 403 Forbidden")
    void update_foreignTemplate_throwsForbidden() {
        when(userRepository.findByUsername("alice")).thenReturn(Mono.just(aUser()));
        when(templateRepository.findById(7L)).thenReturn(Mono.just(aTemplate(99L, false)));

        var req = new TemplateRequest("new", "d", List.of());

        StepVerifier.create(templateService.update(7L, req, "alice"))
                .expectErrorMatches(t -> t instanceof ResponseStatusException
                        && ((ResponseStatusException) t).getStatusCode() == HttpStatus.FORBIDDEN)
                .verify();
    }

    @Test
    @DisplayName("UT-B-18: update своего шаблона обновляет поля и updatedAt")
    void update_ownTemplate_updatesFields() {
        when(userRepository.findByUsername("alice")).thenReturn(Mono.just(aUser()));
        Template existing = aTemplate(42L, false);
        when(templateRepository.findById(7L)).thenReturn(Mono.just(existing));
        when(templateRepository.save(any(Template.class)))
                .thenAnswer(inv -> Mono.just(inv.getArgument(0, Template.class)));

        var req = new TemplateRequest("new title", "new desc", List.of());
        LocalDateTime before = existing.getUpdatedAt();

        StepVerifier.create(templateService.update(7L, req, "alice"))
                .assertNext(r -> {
                    assertThat(r.title()).isEqualTo("new title");
                    assertThat(r.description()).isEqualTo("new desc");
                    assertThat(existing.getUpdatedAt()).isAfter(before);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("UT-B-19: delete своего шаблона вызывает repository.deleteById")
    void delete_ownTemplate_callsRepository() {
        when(userRepository.findByUsername("alice")).thenReturn(Mono.just(aUser()));
        when(templateRepository.findById(7L)).thenReturn(Mono.just(aTemplate(42L, false)));
        when(templateRepository.deleteById(7L)).thenReturn(Mono.empty());

        StepVerifier.create(templateService.delete(7L, "alice")).verifyComplete();
        verify(templateRepository).deleteById(7L);
    }

    @Test
    @DisplayName("UT-B-20: delete чужого шаблона → 403 Forbidden")
    void delete_foreignTemplate_throwsForbidden() {
        when(userRepository.findByUsername("alice")).thenReturn(Mono.just(aUser()));
        when(templateRepository.findById(7L)).thenReturn(Mono.just(aTemplate(99L, true)));

        StepVerifier.create(templateService.delete(7L, "alice"))
                .expectErrorMatches(t -> t instanceof ResponseStatusException
                        && ((ResponseStatusException) t).getStatusCode() == HttpStatus.FORBIDDEN)
                .verify();
    }

    @Test
    @DisplayName("UT-B-21: togglePublish инвертирует isPublic у своего шаблона")
    void togglePublish_invertsFlag() {
        when(userRepository.findByUsername("alice")).thenReturn(Mono.just(aUser()));
        Template existing = aTemplate(42L, false);
        when(templateRepository.findById(7L)).thenReturn(Mono.just(existing));
        when(templateRepository.save(any(Template.class)))
                .thenAnswer(inv -> Mono.just(inv.getArgument(0, Template.class)));

        StepVerifier.create(templateService.togglePublish(7L, "alice"))
                .assertNext(r -> assertThat(r.isPublic()).isTrue())
                .verifyComplete();
    }

    @Test
    @DisplayName("UT-B-22: getById непубличного чужого шаблона → 403")
    void getById_foreignPrivateTemplate_forbidden() {
        when(userRepository.findByUsername("alice")).thenReturn(Mono.just(aUser()));
        when(templateRepository.findById(7L)).thenReturn(Mono.just(aTemplate(99L, false)));

        StepVerifier.create(templateService.getById(7L, "alice"))
                .expectErrorMatches(t -> t instanceof ResponseStatusException
                        && ((ResponseStatusException) t).getStatusCode() == HttpStatus.FORBIDDEN)
                .verify();
    }

    @Test
    @DisplayName("UT-B-22.1: getById публичного чужого шаблона возвращает шаблон")
    void getById_foreignPublicTemplate_ok() {
        when(userRepository.findByUsername("alice")).thenReturn(Mono.just(aUser()));
        when(templateRepository.findById(7L)).thenReturn(Mono.just(aTemplate(99L, true)));

        StepVerifier.create(templateService.getById(7L, "alice"))
                .assertNext(r -> assertThat(r.id()).isEqualTo(7L))
                .verifyComplete();
    }

    @Test
    @DisplayName("UT-B-23: getById несуществующего шаблона → 404 Not Found")
    void getById_notFound() {
        when(userRepository.findByUsername("alice")).thenReturn(Mono.just(aUser()));
        when(templateRepository.findById(7L)).thenReturn(Mono.empty());

        StepVerifier.create(templateService.getById(7L, "alice"))
                .expectErrorMatches(t -> t instanceof ResponseStatusException
                        && ((ResponseStatusException) t).getStatusCode() == HttpStatus.NOT_FOUND)
                .verify();
    }

    @Test
    @DisplayName("UT-B-24: getPublicTemplates без query вызывает findAllByIsPublicTrue")
    void getPublicTemplates_withoutQuery_callsFindAll() {
        when(templateRepository.findAllByIsPublicTrue(any(Pageable.class)))
                .thenReturn(Flux.just(aTemplate(99L, true)));
        when(templateRepository.countByIsPublicTrue()).thenReturn(Mono.just(1L));

        StepVerifier.create(templateService.getPublicTemplates(null, 0, 20))
                .assertNext(page -> {
                    assertThat(page.items()).hasSize(1);
                    assertThat(page.total()).isEqualTo(1);
                    assertThat(page.totalPages()).isEqualTo(1);
                })
                .verifyComplete();

        verify(templateRepository).findAllByIsPublicTrue(any(Pageable.class));
    }

    @Test
    @DisplayName("UT-B-25: getPublicTemplates с query вызывает searchPublic")
    void getPublicTemplates_withQuery_callsSearch() {
        when(templateRepository.searchPublic(anyString(), any(Pageable.class)))
                .thenReturn(Flux.just(aTemplate(99L, true)));
        when(templateRepository.countByIsPublicTrue()).thenReturn(Mono.just(1L));

        StepVerifier.create(templateService.getPublicTemplates("DnD", 0, 20))
                .expectNextCount(1)
                .verifyComplete();

        verify(templateRepository).searchPublic(anyString(), any(Pageable.class));
    }

    @Test
    @DisplayName("UT-B-23.1: операции с неизвестным пользователем → 404 Not Found")
    void resolveUserId_userNotFound_notFound() {
        when(userRepository.findByUsername("ghost")).thenReturn(Mono.empty());

        StepVerifier.create(templateService.delete(7L, "ghost"))
                .expectErrorMatches(t -> t instanceof ResponseStatusException
                        && ((ResponseStatusException) t).getStatusCode() == HttpStatus.NOT_FOUND)
                .verify();

        verify(templateRepository, org.mockito.Mockito.never()).deleteById(anyLong());
    }
}
