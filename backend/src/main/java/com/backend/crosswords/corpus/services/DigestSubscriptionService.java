package com.backend.crosswords.corpus.services;

import com.backend.crosswords.admin.models.User;
import com.backend.crosswords.admin.services.UserService;
import com.backend.crosswords.corpus.dto.*;
import com.backend.crosswords.corpus.enums.Source;
import com.backend.crosswords.corpus.models.DigestSubscription;
import com.backend.crosswords.corpus.models.DigestTemplate;
import com.backend.crosswords.corpus.models.Tag;
import com.backend.crosswords.corpus.repositories.jpa.DigestSubscriptionRepository;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.*;

@Service
public class DigestSubscriptionService {
    private final UserService userService;
    private final ModelMapper modelMapper;
    private final DigestSubscriptionRepository subscriptionRepository;
    private final TagService tagService;
    private final DigestSubscriptionSettingsService subscriptionSettingsService;
    private final DigestTemplateService templateService;

    public DigestSubscriptionService(UserService userService, ModelMapper modelMapper, DigestSubscriptionRepository subscriptionRepository, TagService tagService, DigestSubscriptionSettingsService subscriptionSettingsService, DigestTemplateService templateService) {
        this.userService = userService;
        this.modelMapper = modelMapper;
        this.subscriptionRepository = subscriptionRepository;
        this.tagService = tagService;
        this.subscriptionSettingsService = subscriptionSettingsService;
        this.templateService = templateService;
    }

    @Transactional
    public void createDigestSubscription(User owner, CreateDigestSubscriptionDTO createDigestSubscriptionDTO) {
        owner = userService.loadUserById(owner.getId());

        var subscription = modelMapper.map(createDigestSubscriptionDTO, DigestSubscription.class);
        subscription.setSendToMail(createDigestSubscriptionDTO.getSubscribeOptions().getSendToMail());
        subscription.setMobileNotifications(createDigestSubscriptionDTO.getSubscribeOptions().getMobileNotifications());
        subscription.setOwner(owner);
        subscription.setCreatedAt(new Timestamp(System.currentTimeMillis()));
        subscription.setIsPublic(createDigestSubscriptionDTO.getIsPublic());

        var template = this.extractTagsAndSourcesAndCreateTemplate(createDigestSubscriptionDTO.getTags(), createDigestSubscriptionDTO.getSources());
        subscription.setTemplate(template);

        subscription = subscriptionRepository.save(subscription);

        subscriptionSettingsService.setSubscribersForSubscription(createDigestSubscriptionDTO.getFollowers(), subscription);
    }

    public DigestSubscription getDigestSubscriptionById(Long id) {
        return subscriptionRepository.findById(id).orElseThrow(() -> new NoSuchElementException("There is no subscriptions with such id!"));
    }

    @Transactional
    public void updateDigestSubscription(User user, Long id, UpdateDigestSubscriptionDTO updateDigestSubscriptionDTO) throws IllegalAccessException {
        var subscription = subscriptionRepository.findById(id).orElseThrow(() -> new NoSuchElementException("There is no subscriptions with such id!"));
        if (!Objects.equals(user.getId(), subscription.getOwner().getId())) {
            throw new IllegalAccessException("You are not an owner of this subscription!");
        }
        subscription.setOwner(userService.getUserByUsername(updateDigestSubscriptionDTO.getOwnersUsername()));
        subscription.setDescription(updateDigestSubscriptionDTO.getDescription());
        subscription.setIsPublic(updateDigestSubscriptionDTO.getPublic());
        subscription.setTitle(updateDigestSubscriptionDTO.getTitle());
        subscription.setSendToMail(updateDigestSubscriptionDTO.getSubscribeOptions().getSendToMail());
        subscription.setMobileNotifications(updateDigestSubscriptionDTO.getSubscribeOptions().getMobileNotifications());

        var template = this.extractTagsAndSourcesAndCreateTemplate(updateDigestSubscriptionDTO.getTags(), updateDigestSubscriptionDTO.getSources());
        subscription.setTemplate(template);

        subscription = subscriptionRepository.save(subscription);

        subscriptionSettingsService.setNewSubscribersForSubscription(updateDigestSubscriptionDTO, subscription);
    }
    @Transactional
    public DigestTemplate extractTagsAndSourcesAndCreateTemplate(List<String> tagsNames, List<String> sourcesNames) {
        if (tagsNames != null && !tagsNames.isEmpty()) {
            tagsNames.replaceAll(String::toLowerCase);
        }
        Set<Tag> tags = tagService.getTagsInNames(tagsNames);
        Set<Source> sources = new HashSet<>();
        for (var source : sourcesNames) {
            sources.add(Source.fromRussianName(source));
        }
        return templateService.createTemplateBySourcesAndTags(sources, tags);
    }

    public void checkDigestSubscriptionDeletion(boolean deleted, DigestSubscription subscription, User user) {
        if (deleted) {
            if (Objects.equals(subscription.getOwner().getId(), user.getId())) {
                subscription.setOwner(null);
                subscriptionRepository.save(subscription);
            }
            if (subscriptionSettingsService.getAllDigestSubscriptionSettingsByDigestSubscription(subscription).isEmpty()) {
                subscriptionRepository.delete(subscription);
            }
        }
    }

    public void updateDigestSubscriptionSettingsForUser(Long id, DigestSubscriptionSettingsDTO subscriptionSettingsDTO, User user) {
        var subscription = subscriptionRepository.findById(id).orElseThrow(() -> new NoSuchElementException("There is no subscriptions with such id!"));
        var deleted = subscriptionSettingsService.updateDigestSubscriptionSettingsForUser(subscription, user, subscriptionSettingsDTO);
        this.checkDigestSubscriptionDeletion(deleted, subscription, user);
    }

    public List<String> getAllDigestSubscriptionsUsers(Long id) {
        var subscription = subscriptionRepository.findById(id).orElseThrow(() -> new NoSuchElementException("There is no subscriptions with such id!"));
        return subscriptionSettingsService.getAllDigestSubscriptionsUsersUsernames(subscription);
    }

    private UsersDigestSubscriptionsDTO transformSubscriptionSettingsIntoUsersDigestSubscriptionsDTO(User user, Collection<DigestSubscription> usersSubscriptions, boolean allAvailable) {
        UsersDigestSubscriptionsDTO usersDigestSubscriptionsDTO = new UsersDigestSubscriptionsDTO();
        for (var usersSubscription : usersSubscriptions) {
            UsersDigestSubscriptionDTO usersDigestSubscriptionDTO = new UsersDigestSubscriptionDTO();
            usersDigestSubscriptionDTO.setId(usersSubscription.getId());
            usersDigestSubscriptionDTO.setCreationDate(usersSubscription.getCreatedAt());
            usersDigestSubscriptionDTO.setDescription(usersSubscription.getDescription());
            usersDigestSubscriptionDTO.setPublic(usersSubscription.getIsPublic());
            usersDigestSubscriptionDTO.setTitle(usersSubscription.getTitle());

            if (allAvailable) {
                var subscriptionSettings = subscriptionSettingsService.getAllDigestSubscriptionSettingsByDigestSubscription(usersSubscription);
                for (var subscriptionSetting : subscriptionSettings) {
                    usersDigestSubscriptionDTO.getFollowers().add(subscriptionSetting.getSubscriber().getUsername());
                }
            }

            try {
                var usersSubscriptionSettings = subscriptionSettingsService.getSubscriptionSettingsBySubscriptionAndUser(usersSubscription, user);
                var sendToMail = usersSubscriptionSettings.getSendToMail() == null ? usersSubscription.getSendToMail() : usersSubscriptionSettings.getSendToMail();
                var mobileNotifications = usersSubscriptionSettings.getMobileNotifications() == null ? usersSubscription.getMobileNotifications() : usersSubscriptionSettings.getMobileNotifications();
                usersDigestSubscriptionDTO.setSubscribeOptions(new GetSubscribeOptionsDTO(sendToMail, mobileNotifications, true));
            } catch (NoSuchElementException e) {
                usersDigestSubscriptionDTO.setSubscribeOptions(new GetSubscribeOptionsDTO(false, false, false));
            }

            var ownersUsername = usersSubscription.getOwner().getUsername();
            usersDigestSubscriptionDTO.setOwnersUsername(ownersUsername);
            usersDigestSubscriptionDTO.setIsOwner(Objects.equals(user.getUsername(), ownersUsername));

            if (usersSubscription.getTemplate() != null) {
                for (var source : usersSubscription.getTemplate().getSources()) {
                    usersDigestSubscriptionDTO.getSources().add(source.getRussianName());
                }
                for (var tag : usersSubscription.getTemplate().getTags()) {
                    usersDigestSubscriptionDTO.getTags().add(tag.getName());
                }
            }
            usersDigestSubscriptionsDTO.getUsersDigestSubscriptions().add(usersDigestSubscriptionDTO);
        }
        return usersDigestSubscriptionsDTO;
    }

    public UsersDigestSubscriptionsDTO getAllUsersDigestSubscriptionsAndTransformIntoUsersDigestSubscriptionsDTO(User user) {
        var usersSubscriptions = subscriptionSettingsService.getAllUsersDigestSubscriptions(user);
        return this.transformSubscriptionSettingsIntoUsersDigestSubscriptionsDTO(user, usersSubscriptions, false);
    }

    public DigestSubscriptionDTO getDigestSubscriptionByIdAndTransformIntoDTO(Long id) {
        var subscription = this.getDigestSubscriptionById(id);
        var subscriptionDTO = modelMapper.map(subscription, DigestSubscriptionDTO.class);
        subscriptionDTO.setOwnersUsername(subscription.getOwner().getUsername());

        subscriptionDTO.setSources(new ArrayList<>());
        subscriptionDTO.setTags(new ArrayList<>());
        if (subscription.getTemplate() != null) {
            for (var source : subscription.getTemplate().getSources()) {
                subscriptionDTO.getSources().add(source.getRussianName());
            }
            for (var tag : subscription.getTemplate().getTags()) {
                subscriptionDTO.getTags().add(tag.getName());
            }
        }

        subscriptionDTO.setFollowers(new ArrayList<>());
        var subscriptionSettings = subscriptionSettingsService.getAllDigestSubscriptionSettingsByDigestSubscription(subscription);
        for (var subscriptionSetting : subscriptionSettings) {
            DigestSubscriptionFollowerDTO digestSubscriptionFollowerDTO = new DigestSubscriptionFollowerDTO();
            digestSubscriptionFollowerDTO.setEmail(subscriptionSetting.getSubscriber().getEmail());
            digestSubscriptionFollowerDTO.setMobileNotifications(subscriptionSetting.getMobileNotifications());
            digestSubscriptionFollowerDTO.setSendToMail(subscriptionSetting.getSendToMail());
            subscriptionDTO.getFollowers().add(digestSubscriptionFollowerDTO);
        }

        subscriptionDTO.setSubscribeOptions(new SetSubscribeOptionsDTO(subscription.getSendToMail(), subscription.getMobileNotifications()));
        return subscriptionDTO;
    }

    public UsersDigestSubscriptionsDTO getAllUsersAvailableDigestSubscriptionsAndTransformIntoUsersDigestSubscriptionsDTO(User user) {
        var usersSubscriptions = subscriptionSettingsService.getAllUsersDigestSubscriptions(user);
        var publicSubscriptions = subscriptionRepository.findAllByIsPublic(true);
        usersSubscriptions.addAll(publicSubscriptions);

        return this.transformSubscriptionSettingsIntoUsersDigestSubscriptionsDTO(user, usersSubscriptions, true);
    }

    public List<DigestSubscription> getAllDigestSubscriptionsByTemplate(DigestTemplate template) {
        return subscriptionRepository.findAllByTemplate(template);
    }

    public List<DigestSubscription> getAllDigestSubscriptionsBySearchTerm(String searchTerm) {
        return subscriptionRepository.findAllByTitleContains(searchTerm);
    }
}
