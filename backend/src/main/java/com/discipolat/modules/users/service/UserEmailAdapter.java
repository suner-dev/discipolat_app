package com.discipolat.modules.users.service;

import com.discipolat.modules.audit.domain.UserEmailPort;
import com.discipolat.modules.users.domain.User;
import com.discipolat.modules.users.domain.UserRepository;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Adaptateur du port de lecture des emails (V0.15) : {@code audit} demande, {@code users} répond.
 *
 * <p>Le contexte {@code users} reste libre d'appeler {@code AuditService} pour journaliser ses
 * opérations — c'est le seul sens qui subsiste entre les deux contexts, et il est licite
 * (règle R6 : ce qui est interdit, c'est la réciprocité, pas l'orientation).
 *
 * <p><b>Pourquoi cette classe est dans {@code service} et non dans {@code domain}</b> :
 * {@code @Component} est une décision de câblage, pas du métier. La règle R4 gèle d'ailleurs le
 * nombre d'annotations de conteneur dans les domaines à 2 948 — une de plus ferait rougir la CI,
 * et ce serait la bonne réaction : le domaine de {@code users} n'a pas à savoir que Spring existe.
 *
 * <p>La lecture se fait dans le transactionnel ouvert par l'appelant ({@code AuditService},
 * {@code @Transactional(readOnly = true)}) : pas de nouvelle transaction, pas d'ouverture
 * paresseuse surprise, même plan d'exécution qu'avant l'inversion.
 */
@Component
public class UserEmailAdapter implements UserEmailPort {

    private final UserRepository userRepository;

    public UserEmailAdapter(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public Map<UUID, String> emailsOf(Collection<UUID> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }
        return userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, User::getEmail));
    }
}
