<template>
  <q-card :class="['q-my-sm', { 'no-shadow': $q.dark.isActive }]">
    <q-card-section>
      <div class="row items-center justify-between">
        <div><b>ID:</b> {{ digest.id }}</div>
        <div class="row items-center">
          <!-- Рейтинг -->
          <template v-if="digest.average_rating != null">
            <b class="q-mr-sm">
              <q-icon name="star" color="primary" />
              {{ displayRating }}
            </b>
          </template>
          <!-- Нет оценок или рейтинг не передан -->
          <template v-else>
            <b class="q-mr-sm">Нет оценок</b>
          </template>

          <!-- Статус подписки -->
          <b v-if="is_authed && hasSubscribeOptions">
            {{ subscribed ? 'Подписан' : 'Не подписан' }}
          </b>
          <span v-if="hasSubscribeOptions && subscribed">
            <q-icon v-if="sendToMail && is_authed" name="mail" class="q-ml-xs" />
            <q-icon v-if="mobileNotifications && is_authed" name="phone_iphone" class="q-ml-xs" />
          </span>

          <!-- Дата -->
          <span class="q-ml-sm"><b>Дата:</b> {{ digest.date }}</span>
        </div>
      </div>

      <div v-if="digest.tags" class="row items-center q-mt-sm">
        <div class="text-h4 ellipsis">{{ truncatedTitle }}</div>
      </div>

      <div class="q-mt-sm">{{ truncatedText }}</div>

      <!-- Тэги -->
      <div v-if="digest.tags && digest.tags.length" class="q-mt-sm row wrap">
        <DocumentTags :tags="digest.tags" />
      </div>

      <div class="row items-center justify-between q-mt-sm">
        <q-btn
          label="Посмотреть"
          color="primary"
          no-caps
          text-color="secondary"
          @click="viewDigest"
        />
        <q-btn
          label="PDF версия"
          color="secondary"
          icon-right="download"
          no-caps
          @click="downloadPdf"
        />
      </div>
    </q-card-section>
  </q-card>
</template>

<script>
import DocumentTags from '../components/DocumentTags.vue'

export default {
  name: 'DocumentCard',
  components: {
    DocumentTags,
  },
  props: {
    digest: {
      type: Object,
      required: true,
    },
    is_authed: {
      type: Boolean,
      default: false,
    },
  },
  data() {
    // Подписки и опции уведомлений
    return {
      subscribed: this.digest.subscribe_options?.subscribed ?? false,
      sendToMail: this.digest.subscribe_options?.send_to_mail ?? false,
      mobileNotifications: this.digest.subscribe_options?.mobile_notifications ?? false,
    }
  },
  computed: {
    // Есть ли вообще опции подписки
    hasSubscribeOptions() {
      return !!this.digest.subscribe_options;
    },
    // Форматированное отображение рейтинга
    displayRating() {
      const r = this.digest.average_rating;
      return (r != null && r !== -1) ? r : 'Нет оценок';
    },
    // Сокращённый заголовок
    truncatedTitle() {
      const t = this.digest.title || '';
      return t.length > 22 ? t.slice(0, 22) + '...' : t;
    },
    // Сокращённый текст
    truncatedText() {
      const txt = this.digest.text || '';
      return txt.length > 665 ? txt.slice(0, 665) + '...' : txt;
    },
  },
  methods: {
    viewDigest() {
      this.$router.push(`/digests/${this.digest.id}`)
      console.log(`/digests/${this.digest.id}`)
    },

    downloadPdf() {
      console.log('Скачать PDF для документа', this.digest.id)
      // window.open(pdfUrl, "_blank");
    },
  },
}
</script>

<style scoped>
.ellipsis {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
</style>
