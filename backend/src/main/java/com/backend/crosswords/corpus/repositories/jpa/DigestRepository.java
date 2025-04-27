package com.backend.crosswords.corpus.repositories.jpa;

import com.backend.crosswords.corpus.models.Digest;
import com.backend.crosswords.corpus.models.DigestCore;
import com.backend.crosswords.corpus.models.DigestId;
import com.backend.crosswords.corpus.models.DigestSubscription;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Set;

public interface DigestRepository extends JpaRepository<Digest, DigestId> {
    Set<Digest> findAllByCoreIn(List<DigestCore> cores);
    Set<Digest> findAllBySubscriptionIn(List<DigestSubscription> subscriptions);
}
