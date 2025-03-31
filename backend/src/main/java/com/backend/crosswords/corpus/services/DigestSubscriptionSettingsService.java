package com.backend.crosswords.corpus.services;

import com.backend.crosswords.admin.models.User;
import com.backend.crosswords.admin.services.UserService;
import com.backend.crosswords.corpus.dto.DigestSubscriptionSettingsDTO;
import com.backend.crosswords.corpus.dto.FollowerDTO;
import com.backend.crosswords.corpus.models.DigestSubscription;
import com.backend.crosswords.corpus.models.DigestSubscriptionSettings;
import com.backend.crosswords.corpus.models.DigestSubscriptionSettingsId;
import com.backend.crosswords.corpus.repositories.jpa.DigestSubscriptionSettingsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

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
            userSettingsList.add(this.setParametersForSubscriptionSettingsAndReturn(subscription, user));
        }
        subscriptionSettingsRepository.saveAll(userSettingsList);
    }

    public DigestSubscriptionSettings getSubscriptionSettingsBySubscriptionAndUser(DigestSubscription subscription, User user) {
        return subscriptionSettingsRepository.findById(new DigestSubscriptionSettingsId(subscription.getId(), user.getId())).orElseThrow(() -> new NoSuchElementException("There is no settings between these user and subscription!"));
    }

    @Transactional
    public void setNewSubscribersForSubscription(List<FollowerDTO> followerDTOs, DigestSubscription subscription) {
        var oldFollowers = subscriptionSettingsRepository.findAllByDigestSubscription(subscription);
        oldFollowers.sort(Comparator.comparing(a -> a.getSubscriber().getUsername()));
        List<User> followers = new ArrayList<>();
        for (var followerDTO : followerDTOs) {
            var user = userService.getUserByUsername(followerDTO.getUsername());
            followers.add(user);
        }
        followers.sort(Comparator.comparing(User::getUsername));
        List<User> newFollowers = new ArrayList<>();
        List<DigestSubscriptionSettings> oldSubscriptionSettings = new ArrayList<>();
        List<DigestSubscriptionSettings> userSettingsList = new ArrayList<>();
        for (int i = 0, j = 0; i < oldFollowers.size() && j < followers.size(); ) {
            var oldFollower = oldFollowers.get(i).getSubscriber();
            var follower = followers.get(j);
            var compare = oldFollower.getUsername().compareTo(follower.getUsername());
            if (compare == 0) {
                var oldSettings = subscriptionSettingsRepository.findById(new DigestSubscriptionSettingsId(subscription.getId(), oldFollower.getId())).orElseThrow(
                        () ->  new NoSuchElementException("There is no settings between these user and subscription!"));
                boolean newMobileNotifications = oldFollower.getMobileNotifications() != null ? oldFollower.getMobileNotifications() : subscription.getMobileNotifications();
                boolean newSendToMail = oldFollower.getSendToMail() != null ? oldFollower.getSendToMail() : subscription.getSendToMail();
                if ((newMobileNotifications != oldSettings.getMobileNotifications() || newSendToMail != oldSettings.getSendToMail())) {
                    boolean flag = false; // флаг, показывающий, что хотя бы что-то изменилось в настройках и это нужно сохранить
                    if (!oldSettings.getEdited() || oldSettings.getMobileNotifications() == null) {
                        oldSettings.setMobileNotifications(newMobileNotifications);
                        flag = true;
                    }
                    if (!oldSettings.getEdited() || oldSettings.getSendToMail() == null) {
                        oldSettings.setSendToMail(newSendToMail);
                        flag = true;
                    }
                    if (flag) {
                        userSettingsList.add(oldSettings);
                    }
                }
                ++i;
                ++j;
            } else if (compare > 0) {
                newFollowers.add(follower);
                ++j;
            } else {
                oldSubscriptionSettings.add(subscriptionSettingsRepository
                        .findById(new DigestSubscriptionSettingsId(subscription.getId(), oldFollower.getId())).orElseThrow(
                        () -> new NoSuchElementException("There is no settings between these user and subscription!"))
                );
                ++i;
            }
        }
        subscriptionSettingsRepository.deleteAll(oldSubscriptionSettings);
        for (var user : newFollowers) {
            userSettingsList.add(this.setParametersForSubscriptionSettingsAndReturn(subscription, user));
        }
        subscriptionSettingsRepository.saveAll(userSettingsList);
    }

    private DigestSubscriptionSettings setParametersForSubscriptionSettingsAndReturn(DigestSubscription subscription, User user) {
        var userSettings = new DigestSubscriptionSettings();
        userSettings.setId(new DigestSubscriptionSettingsId(subscription.getId(), user.getId()));
        userSettings.setSubscriber(user);
        userSettings.setDigestSubscription(subscription);
        userSettings.setMobileNotifications(user.getMobileNotifications() != null ? user.getMobileNotifications() : subscription.getMobileNotifications()); // TODO нужно ли как-то выделять настройки подписки owner?
        userSettings.setSendToMail(user.getSendToMail() != null ? user.getSendToMail() : subscription.getSendToMail()); // TODO нужно ли как-то выделять настройки подписки owner?
        return userSettings;
    }

    public void updateDigestSubscriptionSettingsForUser(DigestSubscription subscription, User user, DigestSubscriptionSettingsDTO subscriptionSettingsDTO) {
        var subscriptionSettings = subscriptionSettingsRepository.findById(new DigestSubscriptionSettingsId(subscription.getId(), user.getId())).orElseThrow(
                () -> new NoSuchElementException("There is no settings between these user and subscription!"));
        if (subscriptionSettingsDTO.getSubscribed()) {
            subscriptionSettings.setSendToMail(subscriptionSettings.getSendToMail());
            subscriptionSettings.setMobileNotifications(subscriptionSettings.getMobileNotifications());
            subscriptionSettings.setEdited(true);
            subscriptionSettingsRepository.save(subscriptionSettings);
        } else {
            subscriptionSettingsRepository.delete(subscriptionSettings);
        }
    }

    public List<String> getAllDigestSubscriptionsUsers(DigestSubscription subscription) {
        var settings = subscriptionSettingsRepository.findAllByDigestSubscription(subscription);
        List<String> subscribersUsernames = new ArrayList<>();
        for (var setting : settings) {
            subscribersUsernames.add(setting.getSubscriber().getUsername());
        }
        return subscribersUsernames;
    }
}
