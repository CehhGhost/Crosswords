package com.backend.crosswords.corpus.repositories.jpa;

import com.backend.crosswords.corpus.models.DigestSubscription;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DigestSubscriptionRepository extends JpaRepository<DigestSubscription, Long> {
}
