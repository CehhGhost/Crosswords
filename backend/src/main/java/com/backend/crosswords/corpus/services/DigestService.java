package com.backend.crosswords.corpus.services;

import com.backend.crosswords.admin.models.User;
import com.backend.crosswords.corpus.repositories.jpa.DigestCoreRepository;
import org.springframework.stereotype.Service;

import java.util.NoSuchElementException;

@Service
public class DigestService {
    private final DigestCoreRepository digestCoreRepository;
    private final DigestSubscriptionService subscriptionService;
    private final DigestSubscriptionSettingsService subscriptionSettingsService;

    public DigestService(DigestCoreRepository digestCoreRepository, DigestSubscriptionService subscriptionService, DigestSubscriptionSettingsService subscriptionSettingsService) {
        this.digestCoreRepository = digestCoreRepository;
        this.subscriptionService = subscriptionService;
        this.subscriptionSettingsService = subscriptionSettingsService;
    }

    // TODO доделать
    public Object getDigestById(String digestId, User user) {
        var ids = digestId.split("#");
        var core = digestCoreRepository.findById(Long.valueOf(ids[0])).orElseThrow(() -> new NoSuchElementException("There is no digests with such id!"));
        var template = core.getTemplate();
        var subscription = subscriptionService.getDigestSubscriptionById(Long.valueOf(ids[1]));
        var rating = core.getRatings();
        var subscriptionSettings = subscriptionSettingsService.getSubscriptionSettingsBySubscriptionAndUser(subscription, user);
        return null;
    }
}
