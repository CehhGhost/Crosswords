package com.backend.crosswords.corpus.repositories.jpa;

import com.backend.crosswords.corpus.models.DocMeta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Set;

public interface DocMetaRepository extends JpaRepository<DocMeta, Long> {
    @Query("SELECT t FROM DocMeta t LEFT JOIN FETCH t.tags")
    Set<DocMeta> findAllWithTags();
}
