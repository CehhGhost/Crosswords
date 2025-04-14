package com.backend.crosswords.corpus.services;

import com.backend.crosswords.admin.models.User;
import com.backend.crosswords.admin.services.UserService;
import com.backend.crosswords.corpus.models.DigestCore;
import com.backend.crosswords.corpus.repositories.jpa.DigestRatingRepository;
import org.springframework.stereotype.Service;

@Service
public class DigestRatingService {
    private final DigestRatingRepository ratingRepository;
    private final UserService userService;

    public DigestRatingService(DigestRatingRepository ratingRepository, UserService userService) {
        this.ratingRepository = ratingRepository;
        this.userService = userService;
    }

    public Double getCoresAverageRating(DigestCore core) {
        Double averageRating = 0.0;
        var ratings = ratingRepository.findAllByCore(core);
        if (ratings.size() == 0) {
            return null;
        }
        for (var rating : ratings) {
            averageRating += rating.getDigestRating();
        }
        return averageRating / ratings.size();
    }

    public Integer getCoresUsersRating(DigestCore core, User user) {
        var rating = ratingRepository.findByCoreAndUser(core, user);
        return rating.isEmpty() ? null : rating.get().getDigestRating();
    }
}
