package com.orchexpay.walletledger.repositories;

import com.orchexpay.walletledger.models.OutboxEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface JpaOutboxRepository extends JpaRepository<OutboxEntity, UUID> {

    List<OutboxEntity> findByPublishedFalseOrderByCreatedAtAsc(Pageable pageable);
}
