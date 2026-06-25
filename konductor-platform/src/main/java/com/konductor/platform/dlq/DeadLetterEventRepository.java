package com.konductor.platform.dlq;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DeadLetterEventRepository extends JpaRepository<DeadLetterEventEntity, UUID> {
    List<DeadLetterEventEntity> findByStatusOrderByCreatedAtDesc(String status);
}
