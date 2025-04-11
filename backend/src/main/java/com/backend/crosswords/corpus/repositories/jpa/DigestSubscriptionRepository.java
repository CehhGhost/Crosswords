package com.backend.crosswords.corpus.repositories.jpa;

import com.backend.crosswords.corpus.models.DigestSubscription;
import com.backend.crosswords.corpus.models.DigestTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DigestSubscriptionRepository extends JpaRepository<DigestSubscription, Long> {
    List<DigestSubscription> findAllByTemplate(DigestTemplate template);
}
