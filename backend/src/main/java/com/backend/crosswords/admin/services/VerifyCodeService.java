package com.backend.crosswords.admin.services;

import com.backend.crosswords.admin.repositories.VerifyCodeRepository;
import org.springframework.stereotype.Service;

@Service
public class VerifyCodeService {
    private final VerifyCodeRepository verifyCodeRepository;

    public VerifyCodeService(VerifyCodeRepository verifyCodeRepository) {
        this.verifyCodeRepository = verifyCodeRepository;
    }
}
