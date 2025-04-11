package com.backend.crosswords.corpus.repositories.jpa;

import com.backend.crosswords.corpus.models.Digest;
import com.backend.crosswords.corpus.models.DigestId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DigestRepository extends JpaRepository<Digest, DigestId> {
}
