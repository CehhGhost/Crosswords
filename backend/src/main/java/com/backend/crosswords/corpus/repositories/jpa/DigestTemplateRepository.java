package com.backend.crosswords.corpus.repositories.jpa;

import com.backend.crosswords.corpus.models.DigestTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DigestTemplateRepository extends JpaRepository<DigestTemplate, String> {
}
