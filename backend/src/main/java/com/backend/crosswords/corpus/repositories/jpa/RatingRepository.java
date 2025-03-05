package com.backend.crosswords.corpus.repositories.jpa;

import com.backend.crosswords.corpus.models.Rating;
import com.backend.crosswords.corpus.models.RatingId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RatingRepository extends JpaRepository<Rating, RatingId> {
}
