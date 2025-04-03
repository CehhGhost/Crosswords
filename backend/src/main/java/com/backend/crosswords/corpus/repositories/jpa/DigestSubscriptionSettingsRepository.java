package com.backend.crosswords.corpus.repositories.jpa;

import com.backend.crosswords.admin.models.User;
import com.backend.crosswords.corpus.models.DigestSubscription;
import com.backend.crosswords.corpus.models.DigestSubscriptionSettings;
import com.backend.crosswords.corpus.models.DigestSubscriptionSettingsId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Set;

public interface DigestSubscriptionSettingsRepository extends JpaRepository<DigestSubscriptionSettings, DigestSubscriptionSettingsId> {
    List<DigestSubscriptionSettings> findAllByDigestSubscription(DigestSubscription subscription);
    List<DigestSubscriptionSettings> findAllBySubscriber(User user);
    List<DigestSubscriptionSettings> findAllBySubscriberOrDigestSubscription_IsPublic(User user, Boolean isPublic);
}
