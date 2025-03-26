<template>
  <q-card :class="['q-my-sm', { 'no-shadow': $q.dark.isActive }]">
    <q-card-section>
      <div class="row items-center justify-between">
        <div><b>ID:</b> {{ digest.id }}</div>
        <div>
          <b class="q-mr-sm"
            ><q-icon name="star" color="primary" />
            {{ digest.average_rating != -1 ? digest.average_rating : 'Нет оценок' }}
          </b>
          <b v-if="is_authed">{{ subscribed ? 'Подписан' : 'Не подписан' }}</b>
          <span v-if="subscribed">
            <q-icon v-if="sendToMail && is_authed" name="mail" class="q-ml-xs" />
            <q-icon v-if="mobileNotifications && is_authed" name="phone_iphone" class="q-ml-xs" />
          </span>
          <span class="q-ml-sm"><b>Дата:</b> {{ digest.date }}</span>
        </div>
      </div>

      <div class="row items-center q-mt-sm">
        <div class="text-h4 ellipsis">{{ truncatedTitle }}</div>
      </div>

      <div class="q-mt-sm">{{ truncatedText }}</div>

      <!-- Тэги -->
      <div v-if="digest.tags" class="q-mt-sm row wrap">
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
      required: true,
    },
  },
  data() {
    return {
      dropdown: false,

      // Инициализируем локальное состояние подписки из пропсов
      subscribed: this.digest?.subscribe_options?.subscribed ?? false,
      sendToMail: this.digest?.subscribe_options?.send_to_mail ?? false,
      mobileNotifications: this.digest?.subscribe_options?.mobile_notifications ?? false,
    }
  },
  computed: {
    truncatedTitle() {
      return this.digest.title.length > 22
        ? this.digest.title.substring(0, 22) + '...'
        : this.digest.title
    },
    truncatedText() {
      return this.digest.text.length > 665
        ? this.digest.text.substring(0, 665) + '...'
        : this.digest.text
    },
  },
  methods: {
    viewDigest() {
      this.$router.push(`/digests/${this.digest.id}`)
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
