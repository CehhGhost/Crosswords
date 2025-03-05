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

        <q-btn
          v-if="!subscribed"
          label="Подписаться"
          no-caps
          icon-right="notifications"
          color="primary"
          text-color="secondary"
          class="q-ml-sm q-mt-sm"
          @click="onSubscribeClick"
        />

        <q-btn-dropdown
          v-else
          outline
          @show="onDropdownShow"
          no-caps
          :color="$q.dark.isActive ? 'primary' : 'secondary'"
          class="q-ml-sm q-mt-sm"
          label="Управлять подпиской"
          dropdown-icon="settings"
          
          v-model="dropdown" 
        >
          <q-list>
            <q-item clickable @click="toggleCheckbox('sendToMail')">
  <q-item-section>
    <q-checkbox
      v-model="sendToMail"
      class="custom-checkbox"
      label="На почту"
      @update:model-value="updateSubscription"
      @click.stop
    />
  </q-item-section>
  <q-item-section avatar>
    <q-icon name="email" :color="$q.dark.isActive ? 'primary' : 'secondary'" />
  </q-item-section>
</q-item>

<q-item clickable @click="toggleCheckbox('mobileNotifications')">
  <q-item-section>
    <q-checkbox
      v-model="mobileNotifications"
      keep-color
      label="Мобильные уведомления"
      @update:model-value="updateSubscription"
      @click.stop
    />
  </q-item-section>
  <q-item-section avatar>
    <q-icon name="phone_iphone" :color="$q.dark.isActive ? 'primary' : 'secondary'" />
  </q-item-section>
</q-item>


            <q-separator />

            <q-item clickable @click="onUnsubscribeClick">
              <q-item-section>
                Отписаться
              </q-item-section>
            </q-item>
          </q-list>
        </q-btn-dropdown>
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
      <q-card-section v-if="digest.is_owner" class="row justify-start q-pt-none">
      <q-btn
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
  import DocumentTags from "./DocumentTags.vue"; // Убедитесь, что путь верный
  import { fasCrown } from '@quasar/extras/fontawesome-v6'
  
  export default {
    name: "SubscriptionCard",
    components: {
      DocumentTags,
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
    data() {
    return {
      dropdown: false,
      subscribed: this.digest.subscribe_options.subscribed,
      sendToMail: this.digest.subscribe_options.send_to_mail,
      mobileNotifications: this.digest.subscribe_options.mobile_notifications,
    };
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
    onEditClick() {
    this.$router.push({
      name: 'subscription-edit',
      params: { id: this.digest.id }
    });
  },
    toggleCheckbox(type) {
    this[type] = !this[type]; 
    this.updateSubscription(this[type]); 
  },
  onDropdownShow () {
    this.backup = {
      sendToMail: this.sendToMail,
      mobileNotifications: this.mobileNotifications,
    }
  },

    async onSubscribeClick() {
      this.subscribed = true;
      this.sendToMail = true;  
      this.mobileNotifications = true;
      await this.sendSubscriptionUpdate();
    },

    async onUnsubscribeClick() {
      this.subscribed = false;
      this.sendToMail = false;
      this.mobileNotifications = false;
      this.dropdown = false;
      await this.sendSubscriptionUpdate();
    },

    async updateSubscription() {
      await this.sendSubscriptionUpdate();
    },

    async sendSubscriptionUpdate() {
      const payload = {
        subscribed: this.subscribed,
        send_to_mail: this.sendToMail,
        mobile_notifications: this.mobileNotifications,
      };

      try {
        const response = await fetch(
          "https://4ec3051a148c46c8a8aa6dcfef35cdf8.api.mockbin.io/",
          {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(payload),
          }
        );
        
        console.log(JSON.stringify(payload));

        if (!response.ok) {
          console.error("Ошибка при изменении настроек подписки");
        }
      } catch (error) {
        console.error("Ошибка запроса:", error);
      }
    },
  },
  };
  </script>

  <style scoped>
  .ellipsis {
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }
  </style>
  
  