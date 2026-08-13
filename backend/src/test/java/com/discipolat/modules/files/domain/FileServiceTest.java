package com.discipolat.modules.files.domain;

import com.discipolat.common.infrastructure.security.SecurityUtils;
import com.discipolat.modules.souls.domain.WorkspaceScopeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests unitaires du {@link FileService} : périmètre famille de la liste des
 * fichiers. La page Documents (liste sans filtre) doit fonctionner pour tous
 * les rôles pastoraux autorisés par le contrôleur, tout en conservant
 * l'isolation RBAC : un non-super-utilisateur ne voit que les fichiers des
 * familles de son espace métier (aucun 403 à l'ouverture, aucune fuite
 * inter-familles via les filtres de liste).
 */
@ExtendWith(MockitoExtension.class)
class FileServiceTest {

    @Mock
    private FileEntityRepository repository;

    @Mock
    private SecurityUtils securityUtils;

    @Mock
    private WorkspaceScopeService workspaceScopeService;

    private FileService service;

    @BeforeEach
    void setUp() {
        service = new FileService(repository, securityUtils, workspaceScopeService);
    }

    private static final Pageable PAGEABLE = PageRequest.of(0, 20);

    @Test
    @DisplayName("liste globale : le super-utilisateur voit tous les fichiers (familleId null)")
    void findByFamilleId_superuser_seesEverything() {
        when(workspaceScopeService.isSuperUser()).thenReturn(true);

        service.findByFamilleId(null, PAGEABLE);

        verify(repository).findByFamilleIdAndDeletedFalse(isNull(), eq(PAGEABLE));
        verify(repository, never()).findByFamilleIdInAndDeletedFalse(anyCollection(), eq(PAGEABLE));
    }

    @Test
    @DisplayName("liste globale : le non-super-utilisateur est scopé aux familles accessibles (plus de 403)")
    void findByFamilleId_nonSuperuser_scopedToAccessibleFamilies() {
        when(workspaceScopeService.isSuperUser()).thenReturn(false);
        Set<UUID> familyIds = Set.of(UUID.randomUUID(), UUID.randomUUID());
        when(workspaceScopeService.accessibleFamilyIds()).thenReturn(familyIds);
        when(repository.findByFamilleIdInAndDeletedFalse(eq(familyIds), eq(PAGEABLE)))
                .thenReturn(Page.empty(PAGEABLE));

        service.findByFamilleId(null, PAGEABLE);

        verify(repository).findByFamilleIdInAndDeletedFalse(eq(familyIds), eq(PAGEABLE));
        verify(repository, never()).findByFamilleIdAndDeletedFalse(isNull(), eq(PAGEABLE));
    }

    @Test
    @DisplayName("liste globale : aucune famille accessible -> page vide, pas de 403")
    void findByFamilleId_nonSuperuser_noAccessibleFamilies_returnsEmptyPage() {
        when(workspaceScopeService.isSuperUser()).thenReturn(false);
        when(workspaceScopeService.accessibleFamilyIds()).thenReturn(Set.of());

        Page<FileEntity> result = service.findByFamilleId(null, PAGEABLE);

        org.assertj.core.api.Assertions.assertThat(result.getTotalElements()).isZero();
        verify(repository, never()).findByFamilleIdAndDeletedFalse(any(), eq(PAGEABLE));
        verify(repository, never()).findByFamilleIdInAndDeletedFalse(anyCollection(), eq(PAGEABLE));
    }

    @Test
    @DisplayName("famille explicite : accès refusé si elle ne fait pas partie de l'espace métier")
    void findByFamilleId_inaccessibleFamily_throws() {
        UUID familleId = UUID.randomUUID();
        when(workspaceScopeService.isSuperUser()).thenReturn(false);
        when(workspaceScopeService.canAccessFamily(familleId)).thenReturn(false);

        assertThatThrownBy(() -> service.findByFamilleId(familleId, PAGEABLE))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("famille explicite : requête restreinte à cette famille si elle est accessible")
    void findByFamilleId_accessibleFamily_queriesFamilyOnly() {
        UUID familleId = UUID.randomUUID();
        when(workspaceScopeService.isSuperUser()).thenReturn(false);
        when(workspaceScopeService.canAccessFamily(familleId)).thenReturn(true);

        service.findByFamilleId(familleId, PAGEABLE);

        verify(repository).findByFamilleIdInAndDeletedFalse(eq(List.of(familleId)), eq(PAGEABLE));
    }

    @Test
    @DisplayName("filtre catégorie : scopé aux familles accessibles pour un non-super-utilisateur")
    void findByCategorie_nonSuperuser_scoped() {
        when(workspaceScopeService.isSuperUser()).thenReturn(false);
        Set<UUID> familyIds = Set.of(UUID.randomUUID());
        when(workspaceScopeService.accessibleFamilyIds()).thenReturn(familyIds);
        when(repository.findByCategorieAndFamilleIdInAndDeletedFalse(eq("AUTRE"), eq(familyIds), eq(PAGEABLE)))
                .thenReturn(Page.empty(PAGEABLE));

        service.findByCategorie("AUTRE", PAGEABLE);

        verify(repository).findByCategorieAndFamilleIdInAndDeletedFalse(eq("AUTRE"), eq(familyIds), eq(PAGEABLE));
        verify(repository, never()).findByCategorieAndDeletedFalse(eq("AUTRE"), eq(PAGEABLE));
    }

    @Test
    @DisplayName("filtre catégorie : non scopé pour un super-utilisateur")
    void findByCategorie_superuser_unscoped() {
        when(workspaceScopeService.isSuperUser()).thenReturn(true);

        service.findByCategorie("AUTRE", PAGEABLE);

        verify(repository).findByCategorieAndDeletedFalse(eq("AUTRE"), eq(PAGEABLE));
        verify(repository, never()).findByCategorieAndFamilleIdInAndDeletedFalse(anyString(), anyCollection(), eq(PAGEABLE));
    }
}
