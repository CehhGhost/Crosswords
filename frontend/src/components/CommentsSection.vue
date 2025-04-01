<template>
  <div>
    <!-- Поле для ввода нового комментария -->
    <div class="q-py-md">
      <q-input
        v-model="newCommentText"
        placeholder="Оставьте персональную заметку"
        outlined
        type="textarea"
        class="q-mb-md"
        style="border-radius: 8px"
      />
      <div class="row justify-end">
        <q-btn
          label="Отправить"
          no-caps
          text-color="secondary"
          color="primary"
          @click="submitComment"
          :disable="isPosting || !newCommentText.trim()"
        />
      </div>
    </div>

    <!-- Лоадер, если комментарии загружаются -->
    <div v-if="isLoadingComments" class="row justify-center q-pa-md">
      <q-spinner-dots size="30px" color="primary" />
    </div>

    <div v-else>
      <q-card
        v-for="comment in comments"
        :key="comment.id"
        class="q-pa-sm q-mb-md"
        :class="['q-px-md q-py-sm', { 'no-shadow': $q.dark.isActive }]"
        style="position: relative; border-radius: 8px"
      >
        <div class="row items-center justify-between">
          <div class="row items-center text-caption">
            <q-icon name="event" class="q-mr-xs" />
            {{ formatCommentDate(comment) }}
          </div>
          <div>
            <q-btn flat round dense size="sm" icon="edit" @click="enableEdit(comment)" />
            <q-btn flat round dense size="sm" icon="delete" @click="deleteComment(comment)" />
          </div>
        </div>

        <div class="q-mt-xs">
          <!-- Если комментарий в режиме редактирования -->
          <div v-if="editingCommentId === comment.id">
            <q-input v-model="editCommentText" outlined dense autogrow style="border-radius: 8px" />
            <div class="row q-mt-sm">
              <q-btn
                label="Сохранить"
                no-caps
                text-color="secondary"
                color="primary"
                @click="updateComment(comment)"
                class="q-mr-sm"
              />
              <q-btn label="Отмена" no-caps color="secondary" flat @click="cancelEdit" />
            </div>
          </div>
          <!-- Если не редактируется, просто отображаем текст -->
          <div v-else>
            {{ comment.text }}
          </div>
        </div>
      </q-card>
    </div>
  </div>
</template>

<script>
import { backendURL } from 'src/data/lookups'

export default {
  name: 'CommentsSection',
  props: {
    article_id: {
      type: [String, Number],
      required: true,
    },
  },
  data() {
    return {
      comments: [],
      newCommentText: '',
      isLoadingComments: false,
      isPosting: false,
      editingCommentId: null, // id редактируемого комментария
      editCommentText: '', // текст редактируемого комментария
    }
  },
  created() {
    this.fetchComments()
  },
  methods: {
    fetchComments() {
      this.isLoadingComments = true
      // GET-запрос для получения комментариев по article_id.
      fetch(
        // 'https://2d54fe2c64a94fe6ace1277926ab4b47.api.mockbin.io/',
        backendURL + `documents/${this.article_id}/comment`,
        {
          credentials: 'include',
        },
      )
        .then((response) => {
          if (!response.ok) {
            throw new Error(`Ошибка загрузки комментариев: ${response.status}`)
          }
          return response.json()
        })
        .then((data) => {
          // Ожидается объект вида: { "comments": [ { id, text, created_at, updated_at }, ... ] }
          this.comments = data.comments
        })
        .catch((error) => {
          console.error(error)
          this.$q.notify({
            message: error.message,
            color: 'negative',
            position: 'top',
          })
        })
        .finally(() => {
          this.isLoadingComments = false
        })
    },
    submitComment() {
      console.log('Start')
      if (!this.newCommentText.trim()) return
      console.log('start2')
      this.isPosting = true
      const newComment = {
        text: this.newCommentText,
      }
      // POST-запрос для создания нового комментария. В ответе должен прийти новый объект комментария.
      fetch(
        //`https://d9d09fc87da74a3088ee44ce681593c4.api.mockbin.io/`
        backendURL + `documents/${this.article_id}/comment`,
        {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify(newComment),
          credentials: 'include',
        },
      )
        .then((response) => {
          if (!response.ok) {
            if (response.status === 401) {
              this.$router.replace('/login')
            } else {
              throw new Error(`Ошибка отправки комментария: ${response.status}`)
            }
          }
          console.log(JSON.stringify(newComment))
          console.log('AAAAAA')
          console.log(response)
          return response.json()
        })
        .then((postedComment) => {
          this.comments.push(postedComment)
          this.newCommentText = ''
        })
        .catch((error) => {
          console.error(error)
          this.$q.notify({
            message: error.message,
            color: 'negative',
            position: 'top',
          })
        })
        .finally(() => {
          this.isPosting = false
        })
    },
    enableEdit(comment) {
      this.editingCommentId = comment.id
      this.editCommentText = comment.text
    },
    cancelEdit() {
      // Выход из режима редактирования
      this.editingCommentId = null
      this.editCommentText = ''
    },
    updateComment(comment) {
  if (!this.editCommentText.trim()) return
  const payload = {
    text: this.editCommentText,
  }
  // PUT запрос для обновления комментария.
  fetch(
    backendURL + `documents/${this.article_id}/comment/${comment.id}`,
    {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload),
      credentials: 'include',
    },
  )
    .then((response) => {
      if (!response.ok) {
        if (response.status === 401) {
          this.$router.replace('/login')
        } else {
          throw new Error(`Ошибка обновления комментария: ${response.status}`)
        }
      }
      return response.json()
    })
    .then((updatedComment) => {
      // Обновляем комментарий в локальном массиве, перезаписывая старый объект новым
      const index = this.comments.findIndex((c) => c.id === comment.id)
      if (index !== -1) {
        this.comments.splice(index, 1, updatedComment)
      }
      this.cancelEdit()
    })
    .catch((error) => {
      console.error(error)
      this.$q.notify.create({
        message: error.message,
        color: 'negative',
        position: 'top',
      })
    })
},
    deleteComment(comment) {
      // Отправляем delete запрос для удаления комментария
      fetch(
        //`https://example.com/api/comments/${comment.id}`
        //'https://f776e92186144f8e8cc6cfc807c771e8.api.mockbin.io/',
        backendURL + `documents/${this.article_id}/comment/${comment.id}`,
        {
          method: 'DELETE',
          headers: { 'Content-Type': 'application/json' },
          credentials: 'include',
        },
      )
        .then((response) => {
          if (!response.ok) {
            if (response.status === 401) {
              this.$router.replace('/login')
            } else {
              throw new Error(`Ошибка удаления комментария: ${response.status}`)
            }
          }
          // Если запрос успешный, удаляем комментарий из массива
          const index = this.comments.findIndex((c) => c.id === comment.id)
          if (index !== -1) {
            this.comments.splice(index, 1)
          }
        })
        .catch((error) => {
          console.error(error)
          this.$q.notify.create({
            message: error.message,
            color: 'negative',
            position: 'top',
          })
        })
    },
    formatDate(dateStr) {
      const date = new Date(dateStr)
      if (isNaN(date.getTime())) {
        return ''
      }
      return date.toLocaleDateString('ru-RU')
    },

    formatCommentDate(comment) {
      // Если updated_at отличается от created_at, показываем обе даты
      const created = this.formatDate(comment.created_at)
      if (comment.updated_at && comment.updated_at !== comment.created_at) {
        const updated = this.formatDate(comment.updated_at)
        return `${created} (изм. ${updated})`
      }
      return created
    },
  },
}
</script>

<style scoped></style>
