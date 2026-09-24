package com.discipolat.modules.core.service;

import com.discipolat.modules.core.repository.OutboxEventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxDispatcherTest {

    @Mock private OutboxPublisher outboxPublisher;
    @Mock private OutboxEventRepository outboxEventRepository;
    @InjectMocks private OutboxDispatcher dispatcher;

    @Test
    void cleanupDeletesOnlyPublishedEventsOlderThanThirtyDays() {
        when(outboxEventRepository.deletePublishedBefore(org.mockito.ArgumentMatchers.any())).thenReturn(2);

        dispatcher.cleanupPublishedEvents();

        verify(outboxEventRepository).deletePublishedBefore(argThat(cutoff ->
                cutoff.isBefore(OffsetDateTime.now().minusDays(29))
                        && cutoff.isAfter(OffsetDateTime.now().minusDays(31))));
    }
}
