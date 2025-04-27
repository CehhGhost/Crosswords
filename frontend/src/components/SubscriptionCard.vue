<template>
    <q-card
      flat
      bordered
      :class="['q-my-sm no-shadow' , $q.dark.isActive ? 'bg-dark' : 'white']"
    >
      <q-card-section>
        <div class="row items-center wrap q-mb-sm">
          <div class="row items-center q-mr-sm">
            <q-icon :name="fasCrown" class="q-mr-xs" />
            <span class="text-caption q-mr-md">
              {{ digest.owner }}
            </span>
          </div>
  
          <div class="row items-center q-mr-sm ">
            <q-icon name="event" size="xs" class="q-mr-xs" />
            <span class="text-caption q-mr-md">
              {{ digest.creation_date }}
            </span>
          </div>
  
          <div class="row items-center q-mr-sm">
            <q-icon
              :name="digest.public ? 'public' : 'lock'"
              size="xs"
              class="q-mr-xs"
            />
            <span class="text-caption">
              {{ digest.public ? 'Публичный' : 'Приватный' }}
            </span>
          </div>
        </div>
  
        <div class="row items-center q-mt-sm">
        <div class="text-h4 ellipsis">{{ truncatedTitle }}</div>
        <subscription-button
          :digest="digest"
          triggeredFrom="subscriptions"
          @remove-subscription="handleRemove"
        />
      </div>
  
        <div class="text-body2 q-my-xs">
          {{ digest.description }}
        </div>
        <document-tags :tags="digest.tags" />
        <div class="text-caption q-mt-sm">
          <b>Источники: </b>
          <span v-if="digest.sources && digest.sources.length">
            {{ digest.sources.join(', ') }}
          </span>
        </div>
      </q-card-section>
      <q-card-section class="row justify-start q-pt-none">
        <q-btn
        label="Посмотреть"
        no-caps
        text-color="secondary"
        color="primary"
        @click="onViewClick"
        class="q-mr-sm"
      />
      <q-btn v-if="digest.is_owner"
        icon-right="edit"
        label="Редактировать"
        no-caps
        color="secondary"
        @click="onEditClick"
      />
    </q-card-section>
    </q-card>
  </template>
  
  <script>
import DocumentTags from "./DocumentTags.vue";
import { fasCrown } from '@quasar/extras/fontawesome-v6'
import SubscriptionButton from "./SubscriptionButton.vue"

export default {
  name: "SubscriptionCard",
  components: {
    DocumentTags,
    SubscriptionButton,
  },
  setup() {
    return {
      fasCrown,
    }
  },
  props: {
    digest: {
      type: Object,
      required: true,
    },
  },

  computed: {
    truncatedTitle() {
      return this.digest.title.length > 22
        ? this.digest.title.substring(0, 22) + "..."
        : this.digest.title;
    },
    truncatedText() {
      return this.digest.text.length > 665
        ? this.digest.text.substring(0, 665) + "..."
        : this.digest.text;
    },
  },
  methods: {
    handleRemove(id) {
      this.$emit('subscription-removed', id);
    },
    onEditClick() {
      this.$router.push({
        name: 'subscription-edit',
        params: { id: this.digest.id }
      });
    },
    onViewClick() {
      this.$router.push({
        name: 'subscription-view',
        params: { id: this.digest.id }
      });
    },
  },
};
</script>