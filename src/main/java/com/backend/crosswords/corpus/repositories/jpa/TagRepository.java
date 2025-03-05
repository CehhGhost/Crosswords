package com.backend.crosswords.corpus.repositories.jpa;

import com.backend.crosswords.corpus.models.Tag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TagRepository extends JpaRepository<Tag, Long> {
    Boolean existsTagByName(String name);
    List<Tag> getTagsByNameIsIn(List<String> names);
}
