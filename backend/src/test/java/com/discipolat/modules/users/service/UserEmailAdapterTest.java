package com.discipolat.modules.users.service;

import com.discipolat.modules.users.domain.User;
import com.discipolat.modules.users.domain.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Test de l'adaptateur qui rompt le couple {@code audit <-> users} (V0.15).
 *
 * <p>Le contrat testé est celui du port — une résolution <b>en lot</b> et aucune requête quand il
 * n'y a rien à résoudre — parce que c'est ce contrat qui protège un export de 50 000 lignes.
 */
@ExtendWith(MockitoExtension.class)
class UserEmailAdapterTest {

    @Mock private UserRepository userRepository;

    private UserEmailAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new UserEmailAdapter(userRepository);
    }

    @Test
    @DisplayName("résout les emails en UNE requête, quelle que soit la taille du lot")
    void emailsOf_ShouldResolveInOneBatch() {
        UUID pasteur = UUID.randomUUID();
        UUID diacre = UUID.randomUUID();
        when(userRepository.findAllById(Set.of(pasteur, diacre))).thenReturn(List.of(
                User.builder().id(pasteur).email("pasteur@discipolat.com").build(),
                User.builder().id(diacre).email("diacre@discipolat.com").build()));

        Map<UUID, String> emails = adapter.emailsOf(Set.of(pasteur, diacre));

        assertEquals(Map.of(pasteur, "pasteur@discipolat.com", diacre, "diacre@discipolat.com"), emails);
        verify(userRepository).findAllById(Set.of(pasteur, diacre));
    }

    @Test
    @DisplayName("aucune requête quand il n'y a aucun identifiant à résoudre")
    void emailsOf_WithoutId_ShouldNotTouchTheDatabase() {
        assertTrue(adapter.emailsOf(List.of()).isEmpty());
        assertTrue(adapter.emailsOf(null).isEmpty());

        verifyNoInteractions(userRepository);
    }

    @Test
    @DisplayName("un utilisateur sans email fait échouer la résolution — comportement préexistant figé")
    void emailsOf_WithNullEmail_ShouldFailAsBeforeTheInversion() {
        UUID sansEmail = UUID.randomUUID();
        when(userRepository.findAllById(any())).thenReturn(List.of(User.builder().id(sansEmail).build()));

        // Le collecteur refusait déjà les valeurs nulles avant l'inversion de dépendance : ce test
        // fige le fait pour qu'il ne se déplace pas silencieusement dans la PR d'architecture.
        // Le corriger est une PR métier (règle A4), pas une PR de frontière.
        assertThrows(NullPointerException.class, () -> adapter.emailsOf(Set.of(sansEmail)));
    }

    @Test
    @DisplayName("le lot est transmis tel quel : ni tri, ni copie, ni filtrage côté audit")
    void emailsOf_ShouldForwardTheExactCollection() {
        Collection<UUID> ids = List.of(UUID.randomUUID());
        when(userRepository.findAllById(ids)).thenReturn(List.of());

        assertTrue(adapter.emailsOf(ids).isEmpty());
        verify(userRepository).findAllById(ids);
    }
}
