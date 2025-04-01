<template>
  <div class="page-body">
    <search-section :is_authed="is_authed" @search="onSearch" />

    <div class="q-mt-md">
      <document-card :is_authed="is_authed" v-for="doc in documents" :key="doc.id" :doc="doc" />
    </div>

    <div class="q-mt-md q-mb-md flex flex-center" v-if="next_page !== -1">
      <q-btn label="Показать еще" color="primary" no-caps text-color="dark" @click="onLoadMore" />
    </div>
  </div>
</template>

<script>
import SearchSection from 'src/components/SearchSection.vue'
import DocumentCard from 'src/components/DocumentCard.vue'
import { backendURL } from 'src/data/lookups'

export default {
  name: 'AllDocuments',
  components: {
    SearchSection,
    DocumentCard,
  },
  data() {
    return {
      // Массив полученных документов
      documents: [],
      // Параметры пагинации, приходящие с сервера и отправляемые ему
      next_page: 0,
      matches_per_page: 20,
      is_authed: false,

      // Текущие фильтры (передаём в запрос)
      // Здесь будем хранить все поля, которые приходят из SearchSection
      currentSearchPayload: {},
    }
  },
  mounted() {
    // При загрузке страницы делаем "пустой" точный поиск
    // Чтобы показать все документы
    const initialPayload = {
      search_mode: 'exact',
      search_body: '',
      sources: [],
      tags: [],
      date_from: null,
      date_to: null,
      search_in_text: false,
    }
    this.onSearch(initialPayload)
  },
  methods: {
    // Обрабатываем событие @search от SearchSection
    onSearch(newPayload) {
      // При любом новом поиске сбрасываем пагинацию в дефолт
      this.next_page = 0
      this.matches_per_page = 20

      // Сохраняем переданные фильтры
      // (newPayload — это searchMode, searchBody, sources, tags, etc.)
      this.currentSearchPayload = { ...newPayload }

      // И сразу загружаем документы
      this.fetchDocuments({ reset: true })
    },

    // Кнопка "Show more"
    onLoadMore() {
      // Если next_page уже -1, то кнопка не должна была показаться,
      // но на всякий случай проверим
      if (this.next_page === -1) return

      // Загружаем следующую страницу
      this.fetchDocuments({ reset: false })
    },

    // Единый метод запроса к API
    // Параметр reset говорит, надо ли "заменять" массив документов
    // или "добавлять" (при пагинации)
    fetchDocuments({ reset }) {
      // Готовим поля, которые всегда нужны на бэкенде
      const payload = {
        ...this.currentSearchPayload, // копируем все фильтры, режимы и т.п.
        next_page: this.next_page,
        matches_per_page: this.matches_per_page,
      }

      fetch(
        backendURL + 'documents/search',
        //'https://60b277858b7d4dbc8347955c8dc89e8e.api.mockbin.io/',
        {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
          },
          credentials: 'include',
          body: JSON.stringify(payload),
        },
      )
        .then((response) => {
          if (!response.ok) {
            if (response.status === 401) {
              this.$router.replace('/login')
            }
            throw new Error('Ошибка при поиске')
          }
          return response.json()
        })
        .then((data) => {
          // Ожидаем формат:
          // {
          //   "documents": [...],
          //   "next_page": число,
          //   "matches_per_page": число
          // }
          const { documents, next_page, is_authed } = data

          if (reset) {
            // При новом поиске полностью заменяем массив
            this.documents = documents
          } else {
            // При "Show more" добавляем новые документы к старым
            this.documents = [...this.documents, ...documents]
          }

          // Обновляем next_page/matches_per_page
          this.next_page = next_page
          this.is_authed = is_authed
          // this.is_authed = false

          // Если next_page === -1, значит больше ничего не грузим
          // (кнопка "Show more" спрячется по v-if="next_page !== -1")
        })
        .catch((error) => {
          console.error('Ошибка поиска:', error)
        })
      console.log(JSON.stringify(payload))
    },
  },
}
</script>

<style scoped lang="scss"></style>
