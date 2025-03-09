<template>
  <div class="page-body">
    <search-section :is_authed="is_authed" @search="onSearch" />

    <div class="q-mt-md">
      <document-card
        v-for="doc in documents"
        :key="doc.id"
        :doc="doc"
      />
    </div>

    <div class="q-mt-md q-mb-md flex flex-center" v-if="lastSentencePos !== -1">
  <q-btn
    label="Показать еще"
    color="primary"
    no-caps
    text-color="dark"
    @click="onLoadMore"
  />
</div>

  </div>
</template>

<script>
import SearchSection from "src/components/SearchSection.vue"; // убедитесь, что путь корректен
import DocumentCard from "src/components/DocumentCard.vue";   // убедитесь, что путь корректен

export default {
  name: "AllDocuments",
  components: {
    SearchSection,
    DocumentCard,
  },
  data() {
    return {
      // Массив полученных документов
      documents: [],
      // Параметры пагинации, приходящие с сервера и отправляемые ему
      lastSentencePos: 0,
      matchesPerPage: 20,
      is_authed: false,

      // Текущие фильтры (передаём в запрос)
      // Здесь будем хранить все поля, которые приходят из SearchSection
      currentSearchPayload: {},
    };
  },
  mounted() {
    // При загрузке страницы делаем "пустой" точный поиск
    // Чтобы показать все документы
    const initialPayload = {
      search_mode: "exact",  
      search_body: "",      
      sources: [],
      tags: [],
      date_from: null,
      date_to: null,
      search_in_text: false,
    };
    this.onSearch(initialPayload);
  },
  methods: {
    // Обрабатываем событие @search от SearchSection
    onSearch(newPayload) {
      // При любом новом поиске сбрасываем пагинацию в дефолт
      this.lastSentencePos = 0;
      this.matchesPerPage = 20;

      // Сохраняем переданные фильтры
      // (newPayload — это searchMode, searchBody, sources, tags, etc.)
      this.currentSearchPayload = { ...newPayload };

      // И сразу загружаем документы
      this.fetchDocuments({ reset: true });
    },

    // Кнопка "Show more" 
    onLoadMore() {
      // Если lastSentencePos уже -1, то кнопка не должна была показаться,
      // но на всякий случай проверим
      if (this.lastSentencePos === -1) return;

      // Загружаем следующую страницу
      this.fetchDocuments({ reset: false });
    },

    // Единый метод запроса к API
    // Параметр reset говорит, надо ли "заменять" массив документов
    // или "добавлять" (при пагинации)
    fetchDocuments({ reset }) {
      // Готовим поля, которые всегда нужны на бэкенде
      const payload = {
        ...this.currentSearchPayload,  // копируем все фильтры, режимы и т.п.
        lastSentencePos: this.lastSentencePos,
        matchesPerPage: this.matchesPerPage,
      };

      fetch("https://60b277858b7d4dbc8347955c8dc89e8e.api.mockbin.io/", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify(payload),
      })
        .then((response) => {
          if (!response.ok) {
            throw new Error("Ошибка при поиске");
          }
          return response.json();
        })
        .then((data) => {
          // Ожидаем формат:
          // {
          //   "documents": [...],
          //   "lastSentencePos": число,
          //   "matchesPerPage": число
          // }
          const { documents, lastSentencePos, matchesPerPage, is_authed } = data;

          if (reset) {
            // При новом поиске полностью заменяем массив
            this.documents = documents;
          } else {
            // При "Show more" добавляем новые документы к старым
            this.documents = [...this.documents, ...documents];
          }

          // Обновляем lastSentencePos/matchesPerPage
          this.lastSentencePos = lastSentencePos;
          this.matchesPerPage = matchesPerPage;
          this.is_authed = is_authed;

          // Если lastSentencePos === -1, значит больше ничего не грузим
          // (кнопка "Show more" спрячется по v-if="lastSentencePos !== -1")
        })
        .catch((error) => {
          console.error("Ошибка поиска:", error);
        });
        console.log(JSON.stringify(payload))
    },
  },
};
</script>

<style scoped lang="scss">
</style>
