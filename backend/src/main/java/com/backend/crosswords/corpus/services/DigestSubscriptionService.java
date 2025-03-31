package com.backend.crosswords.corpus.services;

import com.backend.crosswords.admin.models.User;
import com.backend.crosswords.admin.services.UserService;
import com.backend.crosswords.corpus.dto.CreateDigestSubscriptionDTO;
import com.backend.crosswords.corpus.dto.DigestSubscriptionSettingsDTO;
import com.backend.crosswords.corpus.dto.UpdateDigestSubscriptionDTO;
import com.backend.crosswords.corpus.enums.Source;
import com.backend.crosswords.corpus.models.DigestSubscription;
import com.backend.crosswords.corpus.models.Tag;
import com.backend.crosswords.corpus.repositories.jpa.DigestSubscriptionRepository;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        subscription = subscriptionRepository.save(subscription);

        subscriptionSettingsService.setSubscribersForSubscription(createDigestSubscriptionDTO.getFollowers(), subscription);

        this.extractTagsAndSourcesAndCreateTemplate(createDigestSubscriptionDTO.getTags(), createDigestSubscriptionDTO.getSources());
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
        subscription.setPublic(updateDigestSubscriptionDTO.getPublic());
        subscription.setTitle(updateDigestSubscriptionDTO.getTitle());
        subscription.setSendToMail(updateDigestSubscriptionDTO.getSubscribeOptions().getSendToMail());
        subscription.setMobileNotifications(updateDigestSubscriptionDTO.getSubscribeOptions().getMobileNotifications());
        subscription = subscriptionRepository.save(subscription);

        subscriptionSettingsService.setNewSubscribersForSubscription(updateDigestSubscriptionDTO.getFollowers(), subscription);

        this.extractTagsAndSourcesAndCreateTemplate(updateDigestSubscriptionDTO.getTags(), updateDigestSubscriptionDTO.getSources());
    }
    @Transactional
    public void extractTagsAndSourcesAndCreateTemplate(List<String> tagsNames, List<String> sourcesNames) {
        Set<Tag> tags = tagService.getTagsInNames(tagsNames);
        Set<Source> sources = new HashSet<>();
        for (var source : sourcesNames) {
            sources.add(Source.fromRussianName(source));
        }
        templateService.createTemplateBySourcesAndTags(sources, tags);
    }

    public void updateDigestSubscriptionSettingsForUser(Long id, DigestSubscriptionSettingsDTO subscriptionSettingsDTO, User user) {
        var subscription = subscriptionRepository.findById(id).orElseThrow(() -> new NoSuchElementException("There is no subscriptions with such id!"));
        subscriptionSettingsService.updateDigestSubscriptionSettingsForUser(subscription, user, subscriptionSettingsDTO);
    }

    public List<String> getAllDigestSubscriptionsUsers(Long id) {
        var subscription = subscriptionRepository.findById(id).orElseThrow(() -> new NoSuchElementException("There is no subscriptions with such id!"));
        return subscriptionSettingsService.getAllDigestSubscriptionsUsers(subscription);
    }
}
