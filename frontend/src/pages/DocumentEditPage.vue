<template>
  <div class="page-body q-mt-md">
    <q-page>
      <BackButton :to="'/documents/' + article.id" />
      <div
        class="text-h5 caption"
        :class="{ 'text-secondary': !$q.dark.isActive, 'text-white': $q.dark.isActive }"
      >
        Редактирование статьи
      </div>

      <q-form @submit.prevent="onSaveClick" class="q-gutter-md q-mt-md">
        <q-input
          v-model="article.title"
          label="Название"
          type="textarea"
          autogrow
          outlined
          filled
          :color="$q.dark.isActive ? 'primary' : 'accent'"
        />

        <q-input
          v-model="article.text"
          label="Текст статьи"
          type="textarea"
          outlined
          :color="$q.dark.isActive ? 'primary' : 'accent'"
        />

        <q-input
          v-model="article.summary"
          label="Краткое содержание"
          type="textarea"
          autogrow
          outlined
          :color="$q.dark.isActive ? 'primary' : 'accent'"
        />

        <div class="col-12">
          <div class="row q-col-gutter-md">
            <div class="col-4">
              <q-select
                v-model="article.source"
                :options="sources"
                option-label="label"
                option-value="value"
                label="Источник"
                outlined
                dense
                filled
                :color="$q.dark.isActive ? 'primary' : 'accent'"
              />
            </div>

            <div class="col-4">
              <q-input
                v-model="article.date"
                filled
                dense
                label="Дата (с)"
                mask="##/##/####"
                :color="$q.dark.isActive ? 'primary' : 'accent'"
              >
                <template v-slot:append>
                  <q-icon name="event" class="cursor-pointer">
                    <q-popup-proxy cover transition-show="scale" transition-hide="scale">
                      <q-date
                        v-model="article.date"
                        title="Искать с"
                        subtitle="Выбор даты"
                        color="secondary"
                        mask="DD/MM/YYYY"
                      >
                        <div class="row items-center justify-end">
                          <q-btn v-close-popup label="Закрыть" color="secondary" flat />
                        </div>
                      </q-date>
                    </q-popup-proxy>
                  </q-icon>
                </template>
              </q-input>
            </div>

            <div class="col-4">
              <q-select
                v-model="article.language"
                :options="languages"
                option-label="label"
                option-value="value"
                label="Язык"
                outlined
                dense
                filled
                :color="$q.dark.isActive ? 'primary' : 'accent'"
              />
            </div>
          </div>
        </div>

        <q-select
          v-model="article.tags"
          :options="tagsList"
          multiple
          option-label="label"
          option-value="value"
          label="Теги"
          outlined
          dense
          filled
          :color="$q.dark.isActive ? 'primary' : 'accent'"
        />

        <q-input
          v-model="article.URL"
          label="URL"
          type="textarea"
          autogrow
          outlined
          dense
          filled
          :color="$q.dark.isActive ? 'primary' : 'accent'"
        />

        <q-toggle
          v-model="article.drop_rating_summary"
          label="Сбросить рейтинг краткого содержания"
          color="primary"
        />

        <!-- Сбросить рейтинг классификации -->
        <q-toggle
          v-model="article.drop_rating_classification"
          label="Сбросить рейтинг классификации"
          color="primary"
        />

        <div class="col-12 q-mt-md">
          <div class="row items-start wrap justify-between q-gutter-sm">
            <q-btn
              label="Сохранить"
              color="primary"
              text-color="secondary"
              no-caps
              :loading="isSaving"
              @click="onSaveClick"
            />
            <q-btn label="Отменить" color="secondary" no-caps @click="cancelEdit" />
          </div>
        </div>
      </q-form>
      <ConfirmDialog
        v-model="showConfirmDialog"
        title="Внимание!"
        message="Вы изменили текст статьи. Сохранение этих изменений удалит аннотации на этой статье для всех пользователей корпуса. Уверены, что хотите сохранить изменения?"
        checkboxLabel="Я понимаю, что делаю"
        confirmLabel="Сохранить"
        @confirm="performSave"
        @cancel="onDialogCancel"
      />
    </q-page>
  </div>
</template>

<script>
import { ref, onMounted } from 'vue'
import { useQuasar } from 'quasar'
import { useRouter, useRoute } from 'vue-router'
import {
  availableSources,
  availableTags,
  availableLanguages,
  backendURL,
} from 'src/data/lookups.js'

import ConfirmDialog from 'src/components/ConfirmDialog.vue'
import BackButton from 'src/components/BackButton.vue'

export default {
  name: 'EditArticle',
  components: {
    ConfirmDialog,
    BackButton,
  },
  setup() {
    const $q = useQuasar()
    const router = useRouter()

    const route = useRoute()
    const sources = availableSources
    const tagsList = availableTags
    const languages = availableLanguages

    const article = ref({
      id: '',
      title: '',
      text: '',
      summary: '',
      source: '',
      date: '',
      language: '',
      tags: [],
      URL: '',
      drop_rating_summary: false,
      drop_rating_classification: false,
    })
    const originalText = ref('')
    const isSaving = ref(false)
    const showConfirmDialog = ref(false)
    const formatToISO = (str) => {
      if (!str) return null
      const [day, month, year] = str.split('/')
      if (!day || !month || !year) return null
      const utcDateMs = Date.UTC(+year, +month - 1, +day)
      const isoDateTime = new Date(utcDateMs).toISOString()
      console.log(isoDateTime.split('T')[0])
      return isoDateTime.split('T')[0]
    }
    const fetchArticle = async () => {
      try {
        const id = route.params.id
        const response = await fetch(
          //'https://api.example.com/get-article?id=1'
          //'https://478dbea22d894b9a8f6623f77a2ed9ee.api.mockbin.io/'
          backendURL + `documents/${id}`,
          {
            method: 'GET',
            credentials: 'include',
          },
        )
        if (!response.ok) {
          throw new Error('Ошибка при получении данных статьи')
        }
        const data = await response.json()

        article.value = {
          ...article.value,
          id: data.id,
          title: data.title,
          text: data.text,
          summary: data.summary,
          source: data.source,
          date: data.date,
          language: data.language,
          tags: data.tags,
          URL: data.URL,
          drop_rating_summary: false,
          drop_rating_classification: false,
        }
        originalText.value = data.text
      } catch (err) {
        $q.notify({
          message: err.message,
          color: 'negative',
          position: 'top',
        })
      }
    }

    onMounted(() => {
      fetchArticle()
    })

    /**
     * Логика при нажатии на "Сохранить".
     * Если текст не меняли — сохраняем сразу.
     * Если текст поменяли — показываем диалог подтверждения.
     */
    const onSaveClick = () => {
      if (article.value.text !== originalText.value) {
        showConfirmDialog.value = true
      } else {
        performSave()
      }
    }

    const performSave = async () => {
      isSaving.value = true
      try {
        const now = new Date().toISOString()
        const body = {
          id: article.value.id,
          title: article.value.title,
          text: article.value.text,
          summary: article.value.summary,
          source: article.value.source,
          date: formatToISO(article.value.date),
          language: article.value.language,
          tags: article.value.tags.map(tag => tag.value),
          URL: article.value.URL,
          drop_rating_summary: article.value.drop_rating_summary,
          drop_rating_classification: article.value.drop_rating_classification,
          last_edit: now,
        }
        console.log(body)

        const response = await fetch(
          //'https://api.example.com/update-article'
          // 'https://4ec3051a148c46c8a8aa6dcfef35cdf8.api.mockbin.io/',
          backendURL + `documents/${article.value.id}/edit`,
          {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            credentials: 'include',
            body: JSON.stringify(body),
          },
        )

        if (!response.ok) {
          throw new Error('Ошибка при сохранении статьи')
        }

        // Если успех - переходим на страницу просмотра документа
        router.push(`/documents/${article.value.id}`)

        $q.notify({
          message: 'Изменения успешно сохранены!',
          color: 'positive',
          position: 'top',
        })
      } catch (err) {
        $q.notify({
          message: err.message,
          color: 'negative',
          position: 'top',
        })
      } finally {
        isSaving.value = false
      }
    }

    // Нажали "Отменить" (просто возвращаемся на просмотр)
    const cancelEdit = () => {
      router.push(`/documents/${article.value.id}`)
    }

    // При отмене диалога подтверждения, ничего не делаем
    const onDialogCancel = () => {
      // Можно потом сделать что-то при отмене, если нужно
    }

    return {
      article,
      sources,
      tagsList,
      languages,
      isSaving,
      showConfirmDialog,

      onSaveClick,
      performSave,
      cancelEdit,
      onDialogCancel,
      formatToISO,
    }
  },
}
</script>

<style scoped lang="scss"></style>
