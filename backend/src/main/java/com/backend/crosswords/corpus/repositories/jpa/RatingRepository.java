package com.backend.crosswords.corpus.repositories.jpa;

import com.backend.crosswords.admin.models.User;
import com.backend.crosswords.corpus.models.DocMeta;
import com.backend.crosswords.corpus.models.Rating;
import com.backend.crosswords.corpus.models.RatingId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RatingRepository extends JpaRepository<Rating, RatingId> {
    Optional<Rating> findByUserAndDoc(User user, DocMeta docMeta);
}
