package com.backend.crosswords.corpus.services;

import com.backend.crosswords.corpus.dto.CreateTagDTO;
import com.backend.crosswords.corpus.dto.TagDTO;
import com.backend.crosswords.corpus.models.DocMeta;
import com.backend.crosswords.corpus.models.Tag;
import com.backend.crosswords.corpus.repositories.jpa.TagRepository;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class TagService {
    private final TagRepository tagRepository;
    private final ModelMapper modelMapper;

    public TagService(TagRepository tagRepository, ModelMapper modelMapper) {
        this.tagRepository = tagRepository;
        this.modelMapper = modelMapper;
    }

    public void createTag(CreateTagDTO createTagDTO) {
        if (tagRepository.existsTagByName(createTagDTO.getName())) {
            throw new IllegalArgumentException("The tag with such name is already existing!");
        }
        var tag = modelMapper.map(createTagDTO, Tag.class);
        tagRepository.save(tag);
    }

    // TODO переделать с четом новых ссылок на теги (например шаблонов дайджестов)
    public void deleteTagById(Long id) {
        var tag = tagRepository.findById(id);
        if (tag.isEmpty()) {
            throw new NoSuchElementException("There is no tags with such id!");
        }
        tagRepository.delete(tag.get());
    }

    public Set<Tag> getTagsInNamesAndSaveForDoc(List<TagDTO> tagDTOs, DocMeta docMeta) {
        List<String> names = new ArrayList<>();
        for (var tagDTO : tagDTOs) {
            names.add(tagDTO.getName());
        }
        var tags= tagRepository.getTagsByNameIsIn(names);
        for (var tag : tags) {
            tag.getDocs().add(docMeta);
            tagRepository.save(tag);
        }
        return tags;
    }

    public void removeTagsFromDoc(DocMeta docMeta) {
        Iterator<Tag> iterator = docMeta.getTags().iterator();
        while (iterator.hasNext()) {
            Tag tag = iterator.next();
            tag.getDocs().remove(docMeta);
            iterator.remove();
            tagRepository.save(tag);
        }
    }

    public Set<String> getSetOfTagsNames(Collection<Tag> tags) {
        Set<String> tagsNames = new HashSet<>();
        for (var tag : tags) {
            tagsNames.add(tag.getName());
        }
        return tagsNames;
    }

    public Set<Tag> getTagsInNames(List<String> tagNames) {
        return tagRepository.getTagsByNameIsIn(tagNames);
    }

    public void createDefaultTags() {
        var defaultTags = List.of("политика", "внутренняя политика", "внешняя политика", "дипломатия", "законы", "выборы", "политические партии", "геополитика", "экономика", "макроэкономика", "финансы", "банки", "трансграничные переводы", "кредитование", "гранты", "инвестиции", "энергетика", "инфляция", "фондовый рынок", "бизнес", "стартапы", "технологии", "IT", "цифровизация", "система быстрых платежей", "искусственный интеллект", "кибербезопасность", "наука", "космос", "медицина", "здравоохранение", "эпидемии", "психология", "образование", "школа", "университеты", "соцподдержка", "труд", "занятость", "миграция", "права человека", "экология", "климат", "загрязнение", "ресурсы", "транспорт", "инфраструктура", "происшествия", "криминал", "ДТП", "катастрофы", "расследования", "терроризм", "скандалы", "коррупция", "протесты", "армия", "вооруженный конфликт", "мир", "международные отношения", "США", "Европа", "Азия", "Россия", "СНГ", "Ближний Восток", "Африка", "Латинская Америка", "спорт", "футбол", "хоккей", "баскетбол", "олимпийские игры", "спортсмены", "соревнования", "трансферы", "культура", "кино", "музыка", "театр", "искусство", "мода", "литература", "лайфстайл", "еда", "рецепты", "путешествия", "туризм", "визы", "авиация", "отношения", "семья", "саморазвитие", "ЗОЖ", "фитнес", "ментальное здоровье", "цифровые технологии", "соцсети", "цензура", "СМИ", "журналистика", "аналитика", "интервью", "расследование", "прогноз", "рейтинг", "обзор", "утечка", "санкции", "блокчейн", "криптовалюта", "NFT", "IPO", "слияния и поглощения", "старение", "удаленка", "рынок труда", "цены", "зарплаты", "социальные выплаты", "тарифы", "налоги", "реформа", "инновации", "тренд", "инцидент");
        List<Tag> newTags = new ArrayList<>();
        for (var defaultTag : defaultTags) {
            if (!tagRepository.existsTagByName(defaultTag)) {
                newTags.add(new Tag(defaultTag));
            }
        }
        tagRepository.saveAll(newTags);
    }
}
