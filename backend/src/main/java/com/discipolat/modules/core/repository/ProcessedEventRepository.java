package com.discipolat.modules.core.repository;

import com.discipolat.modules.core.domain.ProcessedEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, Long> {

    boolean existsByConsumerAndEventId(String consumer, Long eventId);
}