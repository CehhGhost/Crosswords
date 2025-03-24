package com.backend.crosswords.corpus.services;

import com.backend.crosswords.admin.models.User;
import com.backend.crosswords.admin.services.UserService;
import com.backend.crosswords.corpus.models.DigestSubscription;
import com.backend.crosswords.corpus.models.DigestSubscriptionSettings;
import com.backend.crosswords.corpus.models.DigestSubscriptionSettingsId;
import com.backend.crosswords.corpus.repositories.jpa.DigestSubscriptionSettingsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

@Service
public class DigestSubscriptionSettingsService {
    private final DigestSubscriptionSettingsRepository subscriptionSettingsRepository;
    private final UserService userService;

    public DigestSubscriptionSettingsService(DigestSubscriptionSettingsRepository subscriptionSettingsRepository, UserService userService) {
        this.subscriptionSettingsRepository = subscriptionSettingsRepository;
        this.userService = userService;
    }

    @Transactional
    public void setSubscribersForSubscription(List<String> subscribers, DigestSubscription subscription) {
        User user = subscription.getOwner();
        List<DigestSubscriptionSettings> userSettingsList = new ArrayList<>();
        for (int i = -1; i < subscribers.size(); ++i) {
            if (i != -1) {
                user = userService.getUserByUsername(subscribers.get(i));
            }
            var userSettings = new DigestSubscriptionSettings();
            userSettings.setId(new DigestSubscriptionSettingsId(subscription.getId(), user.getId()));
            userSettings.setSubscriber(user);
            userSettings.setDigestSubscription(subscription);
            userSettings.setMobileNotifications(user.getMobileNotifications() != null ? user.getMobileNotifications() : subscription.getMobileNotifications());
            userSettings.setSendToMail(user.getSendToMail() != null ? user.getSendToMail() : subscription.getSendToMail());
            userSettingsList.add(userSettings);
        }
        subscriptionSettingsRepository.saveAll(userSettingsList);
    }

    public DigestSubscriptionSettings getSubscriptionSettingsBySubscriptionAndUser(DigestSubscription subscription, User user) {
        return subscriptionSettingsRepository.findById(new DigestSubscriptionSettingsId(subscription.getId(), user.getId())).orElseThrow(() -> new NoSuchElementException("There is no settings between these user and subscription!"));
    }
}
