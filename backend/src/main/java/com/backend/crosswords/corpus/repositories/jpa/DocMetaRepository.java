package com.backend.crosswords.corpus.repositories.jpa;

import com.backend.crosswords.corpus.models.DocMeta;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocMetaRepository extends JpaRepository<DocMeta, Long> {
}
