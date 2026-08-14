package com.discipolat.modules.platform.infrastructure;

import com.discipolat.modules.platform.domain.PlatformModule;
import com.discipolat.modules.platform.domain.PlatformModuleRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ModuleGateFilterTest {

    @Mock
    private PlatformModuleRepository moduleRepository;
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private FilterChain chain;
    @Mock
    private java.io.PrintWriter writer;

    private ModuleGateFilter filter;

    @BeforeEach
    void setUp() throws Exception {
        filter = new ModuleGateFilter(moduleRepository);
        lenient().when(response.getWriter()).thenReturn(writer);
    }

    private PlatformModule module(String key, boolean enabled) {
        return PlatformModule.builder().key(key).enabled(enabled).build();
    }

    @Test
    void inventaireDesactive_bloqueSesAPIsMaisPasLeRestDuDepartement() throws Exception {
        when(moduleRepository.findByEnabledFalse()).thenReturn(List.of(module("DEPT_INVENTORY", false)));
        // Le cache 30 s : premier appel charge la liste des modules désactivés
        when(request.getRequestURI()).thenReturn("/api/v1/departments/abc/equipment");
        filter.doFilterInternal(request, response, chain);
        verify(chain, never()).doFilter(any(), any());
        verify(response).setStatus(403);

        // Les autres sous-outils restent accessibles
        when(request.getRequestURI()).thenReturn("/api/v1/departments/abc/checklists");
        filter.doFilterInternal(request, response, chain);
        verify(chain, times(1)).doFilter(any(), any());

        // La gestion du département (membres, équipes…) reste accessible
        when(request.getRequestURI()).thenReturn("/api/v1/departments/abc/members");
        filter.doFilterInternal(request, response, chain);
        verify(chain, times(2)).doFilter(any(), any());
    }

    @Test
    void sousModuleRapports_bloqueGenerateEtListe() throws Exception {
        when(moduleRepository.findByEnabledFalse()).thenReturn(List.of(module("DEPT_REPORTS", false)));
        when(request.getRequestURI()).thenReturn("/api/v1/departments/abc/reports/generate");
        filter.doFilterInternal(request, response, chain);
        verify(chain, never()).doFilter(any(), any());

        when(request.getRequestURI()).thenReturn("/api/v1/departments/abc/reports/list");
        filter.doFilterInternal(request, response, chain);
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    void documentationActive_passe() throws Exception {
        // Aucun module désactivé
        when(moduleRepository.findByEnabledFalse()).thenReturn(List.of());
        when(request.getRequestURI()).thenReturn("/api/v1/departments/abc/documents");
        filter.doFilterInternal(request, response, chain);
        verify(chain, times(1)).doFilter(any(), any());
    }
}
