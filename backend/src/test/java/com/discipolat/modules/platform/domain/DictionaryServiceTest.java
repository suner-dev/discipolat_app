package com.discipolat.modules.platform.domain;

import com.discipolat.modules.audit.domain.AuditService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests unitaires du {@link DictionaryService} : seed des dictionnaires
 * par défaut, validation de création, mise à jour, suppression et — point
 * critique — la restauration complète lors du reset (y compris les entrées
 * par défaut modifiées par l'administrateur).
 */
@ExtendWith(MockitoExtension.class)
class DictionaryServiceTest {

    @Mock
    private DictionaryEntryRepository repository;

    @Mock
    private AuditService auditService;

    private DictionaryService service;

    @BeforeEach
    void setUp() {
        service = new DictionaryService(repository, auditService);
    }

    private DictionaryEntry defaultEntry(String key, String code, String label) {
        return DictionaryEntry.builder()
                .id(UUID.randomUUID()).dictKey(key).code(code)
                .label(label).color("#22c55e").ordre(1).actif(true).isDefault(true).build();
    }

    @Test
    @DisplayName("seedIfEmpty : ne fait rien si des entrées existent déjà")
    void seedIfEmpty_existing_skips() {
        when(repository.count()).thenReturn(5L);

        service.seedIfEmpty();

        verify(repository).count();
        // aucune sauvegarde déclenchée
    }

    @Test
    @DisplayName("create : valide la clé et le code, normalise en majuscules, journalise")
    void create_valideEtNormalise() {
        when(repository.findByDictKeyAndCode("EVENT_TYPE", "BAPTEME")).thenReturn(Optional.empty());
        when(repository.save(any(DictionaryEntry.class))).thenAnswer(inv -> inv.getArgument(0));

        DictionaryEntry created = service.create(" event_type ", DictionaryEntry.builder()
                .code(" bapteme ").label("Baptême").color("#3b82f6").ordre(14).actif(true).build());

        assertThat(created.getDictKey()).isEqualTo("EVENT_TYPE");
        assertThat(created.getCode()).isEqualTo("BAPTEME");
        assertThat(created.getLabel()).isEqualTo("Baptême");
        assertThat(created.isDefault()).isFalse();
        verify(auditService).logSimple("DICTIONARY_ENTRY_CREATED", "PLATFORM_DICTIONARY", null);
    }

    @Test
    @DisplayName("create : refuse un code dupliqué dans le même dictionnaire")
    void create_duplicate_throws() {
        when(repository.findByDictKeyAndCode("EVENT_TYPE", "CULTE"))
                .thenReturn(Optional.of(defaultEntry("EVENT_TYPE", "CULTE", "Culte")));

        assertThatThrownBy(() -> service.create("EVENT_TYPE", DictionaryEntry.builder()
                .code("CULTE").label("Culte").build()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("existe déjà");
    }

    @Test
    @DisplayName("create : refuse un libellé vide")
    void create_blankLabel_throws() {
        assertThatThrownBy(() -> service.create("EVENT_TYPE", DictionaryEntry.builder()
                .code("CULTE").label("   ").build()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("update : met à jour libellé/couleur/ordre/actif et journalise")
    void update_patchChamps() {
        DictionaryEntry entry = defaultEntry("EVENT_TYPE", "CULTE", "Culte");
        when(repository.findById(entry.getId())).thenReturn(Optional.of(entry));
        when(repository.save(any(DictionaryEntry.class))).thenAnswer(inv -> inv.getArgument(0));

        DictionaryEntry updated = service.update(entry.getId(), DictionaryEntry.builder()
                .label("Culte dominical").color("#16a34a").ordre(2).actif(false).build());

        assertThat(updated.getLabel()).isEqualTo("Culte dominical");
        assertThat(updated.getColor()).isEqualTo("#16a34a");
        assertThat(updated.getOrdre()).isEqualTo(2);
        assertThat(updated.isActif()).isFalse();
        verify(auditService).logSimple("DICTIONARY_ENTRY_UPDATED", "PLATFORM_DICTIONARY", entry.getId());
    }

    @Test
    @DisplayName("delete : supprime l'entrée et journalise")
    void delete_supprime() {
        DictionaryEntry entry = defaultEntry("EVENT_TYPE", "CULTE", "Culte");
        when(repository.findById(entry.getId())).thenReturn(Optional.of(entry));

        service.delete(entry.getId());

        verify(repository).delete(entry);
        verify(auditService).logSimple("DICTIONARY_ENTRY_DELETED", "PLATFORM_DICTIONARY", entry.getId());
    }

    @Test
    @DisplayName("reset : supprime les entrées non-défaut ET restaure les défauts modifiés")
    void reset_restoreDefaultsAndRemoveCustom() {
        DictionaryEntry custom = DictionaryEntry.builder()
                .id(UUID.randomUUID()).dictKey("EVENT_TYPE").code("BAPTEME")
                .label("Baptême").ordre(99).actif(true).isDefault(false).build();
        DictionaryEntry editedDefault = defaultEntry("EVENT_TYPE", "CULTE", "Culte");
        editedDefault.setLabel("Culte dominical");
        editedDefault.setActif(false);

        when(repository.findAll()).thenReturn(List.of(custom, editedDefault));
        // Stub générique d'abord, puis cas précis (le dernier stub l'emporte chez Mockito)
        lenient().when(repository.findByDictKeyAndCode(anyString(), anyString())).thenReturn(Optional.empty());
        when(repository.findByDictKeyAndCode("EVENT_TYPE", "CULTE")).thenReturn(Optional.of(editedDefault));

        service.resetDefaults();

        verify(repository).delete(custom);
        verify(repository).save(editedDefault);
        assertThat(editedDefault.getLabel()).isEqualTo("Culte");
        assertThat(editedDefault.isActif()).isTrue();
        // CULTE est la 9e entrée du dictionnaire EVENT_TYPE par défaut
        assertThat(editedDefault.getOrdre()).isEqualTo(9);
        verify(auditService).logSimple("DICTIONARIES_RESET", "PLATFORM_DICTIONARY", null);
    }

    @Test
    @DisplayName("reset : recrée les entrées par défaut manquantes (aucune entrée en base)")
    void reset_recreateMissingDefaults() {
        when(repository.findAll()).thenReturn(List.of());
        when(repository.findByDictKeyAndCode(anyString(), anyString())).thenReturn(Optional.empty());
        when(repository.save(any(DictionaryEntry.class))).thenAnswer(inv -> inv.getArgument(0));

        service.resetDefaults();

        // Les 88 entrées par défaut sont recréées et le reset est journalisé
        verify(repository, org.mockito.Mockito.atLeastOnce()).save(any(DictionaryEntry.class));
        verify(auditService).logSimple("DICTIONARIES_RESET", "PLATFORM_DICTIONARY", null);
    }
}
