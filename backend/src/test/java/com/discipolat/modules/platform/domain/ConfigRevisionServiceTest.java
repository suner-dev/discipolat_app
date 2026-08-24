package com.discipolat.modules.platform.domain;

import com.discipolat.common.infrastructure.security.SecurityUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;
import com.discipolat.common.infrastructure.security.SecurityTestHelper;

@ExtendWith(MockitoExtension.class)
class ConfigRevisionServiceTest {

    @Mock private ConfigRevisionRepository repository;
    @Mock private SecurityUtils securityUtils;

    private ConfigRevisionService service;

    @BeforeEach
    void setUp() {
        SecurityTestHelper.loginAs(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        service = new ConfigRevisionService(repository, securityUtils);
    }

    @Test
    void record_persistsRevisionWithAuthor() {
        UUID userId = UUID.randomUUID();
        when(securityUtils.getCurrentUserId()).thenReturn(userId);

        service.record("PLATFORM_MODULE", "SOULS", "MODULE_ENABLED", Map.of("enabled", true));

        verify(repository).save(argThat(r ->
                "PLATFORM_MODULE".equals(r.getEntityType())
                        && "SOULS".equals(r.getEntityKey())
                        && "MODULE_ENABLED".equals(r.getAction())
                        && userId.equals(r.getUserId())
                        && Boolean.TRUE.equals(r.getPayload().get("enabled"))));
    }

    @Test
    void record_withoutAuthenticatedUser_keepsNullAuthor() {
        when(securityUtils.getCurrentUserId()).thenThrow(new RuntimeException("no user"));

        service.record("PLATFORM_MENU", "souls", "MENU_DELETED", Map.of());

        verify(repository).save(argThat(r -> "MENU_DELETED".equals(r.getAction()) && r.getUserId() == null));
    }

    @Test
    void list_filtersByEntityType() {
        Pageable pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));
        when(repository.findFiltered("PLATFORM_MODULE", pageable)).thenReturn(new PageImpl<>(List.of()));

        service.list("PLATFORM_MODULE", pageable);

        verify(repository).findFiltered(eq("PLATFORM_MODULE"), any());
    }

    @Test
    void list_withNullEntityType_returnsAll() {
        Pageable pageable = PageRequest.of(0, 20);
        when(repository.findFiltered(isNull(), eq(pageable))).thenReturn(new PageImpl<>(List.of()));

        service.list(null, pageable);

        verify(repository).findFiltered(isNull(), eq(pageable));
    }
}
