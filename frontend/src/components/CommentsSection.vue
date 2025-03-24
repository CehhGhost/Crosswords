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
export default {
  name: 'CommentsSection',
  props: {
    articleId: {
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
      // GET-запрос для получения комментариев по articleId.
      fetch(
        //`https://example.com/api/comments?articleId=${this.articleId}`
        'https://2d54fe2c64a94fe6ace1277926ab4b47.api.mockbin.io/',
      )
        .then((response) => {
          if (!response.ok) {
            throw new Error(`Ошибка загрузки комментариев: ${response.status}`)
          }
          return response.json()
        })
        .then((data) => {
          // Ожидается объект вида: { "comments": [ { id, text, createdAt, updatedAt }, ... ] }
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
      if (!this.newCommentText.trim()) return
      this.isPosting = true
      const now = new Date().toISOString()
      const newComment = {
        text: this.newCommentText,
        articleId: String(this.articleId),
        createdAt: now,
        updatedAt: now,
      }
      // POST-запрос для создания нового комментария. В ответе должен прийти новый объект комментария.
      fetch(`https://d9d09fc87da74a3088ee44ce681593c4.api.mockbin.io/`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(newComment),
      })
        .then((response) => {
          if (!response.ok) {
            if (response.status === 401) {
              this.$router.replace('/login')
            } else {
              throw new Error(`Ошибка отправки комментария: ${response.status}`)
            }
          }
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
      const now = new Date().toISOString()
      const updatedComment = {
        id: comment.id,
        text: this.editCommentText,
        articleId: comment.articleId,
        createdAt: comment.createdAt,
        updatedAt: now,
      }
      // PUT запрос для обновления комментария.
      fetch(
        //
        // `https://example.com/api/comments/${comment.id}`
        'https://9dfb689c9e154d47828042573d7dbd22.api.mockbin.io/',
        {
          method: 'PUT',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify(updatedComment),
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
          // Если бекенд не возвращает тело, продолжаем
          return response.text()
        })
        .then(() => {
          // Обновляем локально комментарий, используя данные, которые отправили
          const index = this.comments.findIndex((c) => c.id === comment.id)
          if (index !== -1) {
            // Обновляем комментарий в локальном массиве
            this.comments.splice(index, 1, { ...comment, ...updatedComment })
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
        'https://f776e92186144f8e8cc6cfc807c771e8.api.mockbin.io/',
        {
          method: 'DELETE',
          headers: { 'Content-Type': 'application/json' },
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
      // Если updatedAt отличается от createdAt, показываем обе даты
      const created = this.formatDate(comment.createdAt)
      if (comment.updatedAt && comment.updatedAt !== comment.createdAt) {
        const updated = this.formatDate(comment.updatedAt)
        return `${created} (изм. ${updated})`
      }
      return created
    },
  },
}
</script>

<style scoped></style>
