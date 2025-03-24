package com.backend.crosswords.corpus.repositories.jpa;

import com.backend.crosswords.corpus.models.Comment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentRepository extends JpaRepository<Comment, Long> {
}
