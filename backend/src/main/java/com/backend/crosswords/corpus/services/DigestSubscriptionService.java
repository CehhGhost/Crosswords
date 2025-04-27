package com.backend.crosswords.corpus.services;

import com.backend.crosswords.admin.models.User;
import com.backend.crosswords.admin.services.UserService;
import com.backend.crosswords.config.Pair;
import com.backend.crosswords.corpus.dto.*;
import com.backend.crosswords.corpus.enums.Source;
import com.backend.crosswords.corpus.models.*;
import com.backend.crosswords.corpus.repositories.elasticsearch.DigestSubscriptionSearchRepository;
import com.backend.crosswords.corpus.repositories.jpa.DigestSubscriptionRepository;
import org.modelmapper.ModelMapper;
import org.opensearch.data.client.orhlc.NativeSearchQueryBuilder;
import org.opensearch.index.query.QueryBuilder;
import org.opensearch.index.query.QueryBuilders;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.*;

@Service
public class DigestSubscriptionService {
    private final UserService userService;
    private final ModelMapper modelMapper;
    private final DigestSubscriptionRepository subscriptionRepository;
    private final DigestSubscriptionSearchRepository subscriptionSearchRepository;
    private final TagService tagService;
    private final DigestSubscriptionSettingsService subscriptionSettingsService;
    private final DigestTemplateService templateService;
    private final ElasticsearchOperations elasticsearchOperations;
    private final DigestRatingService ratingService;

    public DigestSubscriptionService(UserService userService, ModelMapper modelMapper, DigestSubscriptionRepository subscriptionRepository, DigestSubscriptionSearchRepository subscriptionSearchRepository, TagService tagService, DigestSubscriptionSettingsService subscriptionSettingsService, DigestTemplateService templateService, ElasticsearchOperations elasticsearchOperations, DigestRatingService ratingService) {
        this.userService = userService;
        this.modelMapper = modelMapper;
        this.subscriptionRepository = subscriptionRepository;
        this.subscriptionSearchRepository = subscriptionSearchRepository;
        this.tagService = tagService;
        this.subscriptionSettingsService = subscriptionSettingsService;
        this.templateService = templateService;
        this.elasticsearchOperations = elasticsearchOperations;
        this.ratingService = ratingService;
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
        var subscriptionES = new DigestSubscriptionES(subscription.getId(), createDigestSubscriptionDTO.getTitle());
        subscriptionSearchRepository.save(subscriptionES);
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
        subscription.setSendToMail(updateDigestSubscriptionDTO.getSubscribeOptions().getSendToMail());
        subscription.setMobileNotifications(updateDigestSubscriptionDTO.getSubscribeOptions().getMobileNotifications());

        var template = this.extractTagsAndSourcesAndCreateTemplate(updateDigestSubscriptionDTO.getTags(), updateDigestSubscriptionDTO.getSources());
        subscription.setTemplate(template);

        subscription = subscriptionRepository.save(subscription);

        subscriptionSettingsService.setNewSubscribersForSubscription(updateDigestSubscriptionDTO, subscription);

        var subscriptionES = subscriptionSearchRepository.findById(id).orElseThrow(() -> new NoSuchElementException("There is no subscriptions with such id!"));
        subscriptionES.setTitle(updateDigestSubscriptionDTO.getTitle());
        subscriptionSearchRepository.save(subscriptionES);
    }
    @Transactional
    public DigestTemplate extractTagsAndSourcesAndCreateTemplate(List<String> tagsNames, List<String> sourcesNames) {
        Set<Tag> tags = tagService.getTagsInNames(tagsNames);
        Set<Source> sources = new HashSet<>();
        for (var source : sourcesNames) {
            sources.add(Source.fromRussianName(source));
        }
        return templateService.createTemplateBySourcesAndTags(sources, tags);
    }

    public boolean checkDigestSubscriptionDeletion(boolean deleted, DigestSubscription subscription, User user) {
        if (deleted) {
            if (Objects.equals(subscription.getOwner().getId(), user.getId())) {
                subscription.setOwner(null);
                subscriptionRepository.save(subscription);
            }
            if (subscriptionSettingsService.getAllDigestSubscriptionSettingsByDigestSubscription(subscription).isEmpty()) {
                subscriptionRepository.delete(subscription);
                return true;
            }
        }
        return false;
    }

    public Pair<DigestSubscriptionSettingsDTO, Boolean> updateDigestSubscriptionSettingsForUser(Long id, DigestSubscriptionSettingsDTO subscriptionSettingsDTO, User user) {
        var subscription = subscriptionRepository.findById(id).orElseThrow(() -> new NoSuchElementException("There is no subscriptions with such id!"));
        var resultPair = subscriptionSettingsService.updateDigestSubscriptionSettingsForUser(subscription, user, subscriptionSettingsDTO);
        resultPair.setSecond(this.checkDigestSubscriptionDeletion(resultPair.getSecond(), subscription, user));
        return resultPair;
    }

    public List<String> getAllDigestSubscriptionsUsersExceptOwner(Long id) {
        var subscription = subscriptionRepository.findById(id).orElseThrow(() -> new NoSuchElementException("There is no subscriptions with such id!"));
        return subscriptionSettingsService.getAllDigestSubscriptionsUsersUsernamesExceptOwner(subscription);
    }

    private UsersDigestSubscriptionsDTO transformSubscriptionSettingsIntoUsersDigestSubscriptionsDTO(User user, Collection<DigestSubscription> usersSubscriptions, boolean allAvailable) {
        UsersDigestSubscriptionsDTO usersDigestSubscriptionsDTO = new UsersDigestSubscriptionsDTO();
        for (var usersSubscription : usersSubscriptions) {
            var usersSubscriptionES = subscriptionSearchRepository.findById(usersSubscription.getId()).orElseThrow(() -> new NoSuchElementException("There is no subscriptions with such id!"));
            UsersDigestSubscriptionDTO usersDigestSubscriptionDTO = new UsersDigestSubscriptionDTO();
            usersDigestSubscriptionDTO.setId(usersSubscription.getId());
            usersDigestSubscriptionDTO.setCreationDate(usersSubscription.getCreatedAt());
            usersDigestSubscriptionDTO.setDescription(usersSubscription.getDescription());
            usersDigestSubscriptionDTO.setPublic(usersSubscription.getIsPublic());

            usersDigestSubscriptionDTO.setTitle(usersSubscriptionES.getTitle());

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
        var subscriptionES = subscriptionSearchRepository.findById(subscription.getId()).orElseThrow(() -> new NoSuchElementException("There is no subscriptions with such id!"));
        var subscriptionDTO = modelMapper.map(subscription, DigestSubscriptionDTO.class);
        subscriptionDTO.setTitle(subscriptionES.getTitle());
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
        List<DigestSubscription> digestSubscriptions = new ArrayList<>();
        QueryBuilder queryBuilder = QueryBuilders.matchPhraseQuery("title", searchTerm);
        float minScore = 1F;
        var searchQuery = new NativeSearchQueryBuilder()
                .withQuery(queryBuilder)
                .build();
        var searchHits = elasticsearchOperations.search(searchQuery, DigestSubscriptionES.class, IndexCoordinates.of("digest_subscription"));
        searchHits.forEach(hit ->
        {
            var digestSubscriptionES = hit.getContent();
            var digestSubscription = subscriptionRepository.findById(digestSubscriptionES.getId()).orElseThrow(() -> new NoSuchElementException("There is no subscriptions with such id!"));
            digestSubscriptions.add(digestSubscription);
        });
        return digestSubscriptions;
    }

    public DigestSubscriptionES getDigestSubscriptionESById(Long id) {
        return subscriptionSearchRepository.findById(id).orElseThrow(() -> new NoSuchElementException("There is no subscriptions with such id!"));
    }

    public Boolean checkUsersOwnershipOfDigestSubscriptionById(Long id, User user) {
        var subscription = this.getDigestSubscriptionById(id);
        return subscription.getOwner().getId().equals(user.getId());
    }

    public void changeDigestSubscriptionsOwner(User user, Long id, String newOwnersUsername) throws IllegalAccessException {
        var subscription = this.getDigestSubscriptionById(id);
        if (!subscription.getOwner().getId().equals(user.getId())) {
            throw new IllegalAccessException("You are not an owner of this subscription!");
        }
        var newOwner = userService.getUserByUsername(newOwnersUsername);
        Set<String> followers = new HashSet<>(this.getAllDigestSubscriptionsUsersExceptOwner(id));
        if (!followers.contains(newOwner.getUsername())) {
            throw new IllegalArgumentException("New owner must be a follower of this digest subscription and can't be on old owner of it");
        }
        subscription.setOwner(newOwner);
        subscriptionRepository.save(subscription);
    }

    public String getDigestSubscriptionsTitle(Long subscriptionId) {
        var subscriptionES = subscriptionSearchRepository.findById(subscriptionId).orElseThrow(() -> new NoSuchElementException("There is no subscriptions with such id!"));
        return subscriptionES.getTitle();
    }

    private List<String> extractSubscribersEmailsFromSubscription(DigestSubscription subscription) {
        List<String> subscribersEmails = new ArrayList<>();
        var subscriptionSettings = subscriptionSettingsService.getAllDigestSubscriptionSettingsByDigestSubscription(subscription);
        for (var subscriptionSetting : subscriptionSettings) {
            subscribersEmails.add(subscriptionSetting.getSubscriber().getEmail());
        }
        return subscribersEmails;
    }

    public SubscriptionWithDigestsWrapperDTO getDigestSubscriptionsDigestsByIdAndConvertIntoDTO(Long id, User user) throws IllegalAccessException {
        var subscription = this.getDigestSubscriptionById(id);
        var subscriptionES = this.getDigestSubscriptionESById(id);
        var template = subscription.getTemplate();
        SubscriptionWithDigestsDTO subscriptionWithDigestsDTO = new SubscriptionWithDigestsDTO();

        subscriptionWithDigestsDTO.setId(subscription.getId());
        subscriptionWithDigestsDTO.setDate(subscription.getCreatedAt());
        subscriptionWithDigestsDTO.setTitle(subscriptionES.getTitle());
        subscriptionWithDigestsDTO.setDescription(subscription.getDescription());
        subscriptionWithDigestsDTO.setOwner(subscription.getOwner().getEmail());

        GetSubscribeOptionsDTO subscribeOptionsDTO = new GetSubscribeOptionsDTO();
        subscribeOptionsDTO.setMobileNotifications(subscription.getMobileNotifications());
        subscribeOptionsDTO.setSendToMail(subscription.getSendToMail());
        subscribeOptionsDTO.setSubscribed(false);

        if (user != null) {
            subscriptionWithDigestsDTO.setIsAuthed(true);
            if (subscription.getOwner().getId().equals(user.getId())) {
                subscriptionWithDigestsDTO.setIsOwner(true);
            }
            if (this.extractSubscribersEmailsFromSubscription(subscription).contains(user.getEmail())) {
                subscribeOptionsDTO.setSubscribed(true);
            } else if (!subscription.getIsPublic()) {
                throw new IllegalAccessException("You are not a follower of this private subscription!");
            }
        } else {
            if (!subscription.getIsPublic()) {
                throw new IllegalAccessException("You are not an owner of this private subscription!");
            }
            subscriptionWithDigestsDTO.setIsAuthed(false);
            subscriptionWithDigestsDTO.setIsOwner(false);
        }
        subscriptionWithDigestsDTO.setSubscribeOptions(subscribeOptionsDTO);

        var sourcesAndTagsNames = templateService.getSourcesNamesAndTagsNamesFromTemplate(template);
        subscriptionWithDigestsDTO.setSources(sourcesAndTagsNames.getFirst());
        subscriptionWithDigestsDTO.setTags(sourcesAndTagsNames.getSecond());

        double sumAverage = 0;
        int notNullCounter = 0;
        subscriptionWithDigestsDTO.setDigests(new ArrayList<>());
        for (var digest : subscription.getDigests()) {
            var core = digest.getCore();
            SubscriptionsDigestDTO subscriptionsDigestDTO;
            Double averageDigestCoresRating = ratingService.getCoresAverageRating(core);
            if (averageDigestCoresRating != null) {
                ++notNullCounter;
                sumAverage += averageDigestCoresRating;
            }
            subscriptionsDigestDTO = new SubscriptionsDigestDTO(digest.getId().toString(), averageDigestCoresRating, core.getDate(), core.getText());
            subscriptionWithDigestsDTO.getDigests().add(subscriptionsDigestDTO);
        }
        double subscriptionRating = notNullCounter > 0 ? sumAverage / notNullCounter : -1;
        subscriptionWithDigestsDTO.setAverageRating(subscriptionRating);

        return new SubscriptionWithDigestsWrapperDTO(subscriptionWithDigestsDTO);
    }

    public CheckAccessDTO checkUsersAccessToSubscriptionByIdAndConvertIntoDTO(Long id, User user) {
        var subscription = this.getDigestSubscriptionById(id);
        boolean isAvailable = false;
        if (subscription.getIsPublic()) {
            isAvailable = true;
        } else if (user != null){
            isAvailable = subscription.getOwner().getId().equals(user.getId());
        }
        return new CheckAccessDTO(isAvailable);
    }
}
