package com.konductor.projector.repository;

import com.konductor.projector.entity.EventStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EventStatusRepository extends JpaRepository<EventStatus, Short> {
    List<EventStatus> findByActiveTrueOrderByIdAsc();

    Optional<EventStatus> findByCodeAndActiveTrue(String code);
}
