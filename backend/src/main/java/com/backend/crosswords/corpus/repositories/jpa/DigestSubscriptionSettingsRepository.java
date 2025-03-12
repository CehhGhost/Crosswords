package com.backend.crosswords.corpus.repositories.jpa;

import com.backend.crosswords.corpus.models.DigestSubscriptionSettings;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DigestSubscriptionSettingsRepository extends JpaRepository<DigestSubscriptionSettings, Long> {
}
