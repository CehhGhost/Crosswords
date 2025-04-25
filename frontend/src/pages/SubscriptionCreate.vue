<template>
  <q-page class="page-body q-mt-md">
    <BackButton to="/digests" />
    <div
      class="text-h5 caption"
      :class="{ 'text-secondary': !$q.dark.isActive, 'text-white': $q.dark.isActive }"
    >
      Заказ дайджеста
    </div>
    <q-form @submit="submitForm" class="q-gutter-md">
      <q-input
        v-model="title"
        label="Название"
        filled
        required
        class="q-mt-lg"
        :color="$q.dark.isActive ? 'primary' : 'accent'"
      />

      <div class="q-mt-sm row items-center q-gutter-sm">
        <FilterSelector
          v-model="selectedSources"
          :label="'Источники'"
          :options="availableSources"
          :required=true
        />
        <FilterSelector v-model="selectedTags" :label="'Теги'" :options="availableTags" />
      </div>

      <q-input
        v-model="description"
        label="Описание"
        type="textarea"
        outlined
        :color="$q.dark.isActive ? 'primary' : 'accent'"
      />

      <div class="q-mt-md">
        <span>Настройки уведомлений:</span>
        <q-checkbox v-model="notificationEmail" label="На почту" />
        <q-checkbox v-model="notificationMobile" label="В мобильное приложение" />
      </div>
      <div class="q-mt-md">
        <span>Доступ:</span>
        <q-checkbox v-model="isPublic" label="Сделать публичным" />
      </div>

      <div class="row items-center q-mt-md">
        <q-input
          v-model="email"
          class="col"
          :color="$q.dark.isActive ? 'primary' : 'accent'"
          label="Добавить получателя"
          filled
          @keyup.enter="addEmail"
        />
        <q-btn
          label="Добавить"
          @click="addEmail"
          color="primary"
          no-caps
          text-color="dark"
          padding="md"
          unelevated
          class="col-auto search-btn"
        />
      </div>
      <div v-if="emailError" class="text-negative">Данный пользователь не найден в системе</div>
      <div v-if="emailExists" class="text-negative">Этот человек уже был добавлен в рассылку</div>

      <div class="q-mt-sm">
        <!-- тут потом заменить на email текущего пользователя-->
        <q-chip :label="ownerEmail" color="primary" text-color="secondary" class="q-mb-xs">
          <q-icon :name="fasCrown" color="secondary" class="q-ml-xs">
            <q-tooltip
              anchor="top middle"
              self="bottom middle"
              class="bg-primary text-secondary"
              transition-show="scale"
              transition-hide="scale"
            >
              Вы владелец этого дайджеста
            </q-tooltip>
          </q-icon>
        </q-chip>
        <q-chip
          v-for="(chip, index) in addedEmails"
          :key="index"
          :label="chip.email"
          removable
          color="primary"
          text-color="secondary"
          @remove="removeChip(index)"
          class="q-mb-xs"
        >
          <q-icon v-if="chip.send_to_mail" name="mail" color="secondary" class="q-ml-xs">
            <q-tooltip
              class="bg-primary text-secondary"
              transition-show="scale"
              transition-hide="scale"
            >
              Этот пользователь получает уведомления на почту
            </q-tooltip>
          </q-icon>
          <q-icon v-if="chip.mobile_notifications" name="phone_iphone" color="secondary" class="q-ml-xs">
            <q-tooltip
              class="bg-primary text-secondary"
              transition-show="scale"
              transition-hide="scale"
            >
              Этот пользователь получает мобильные уведомления
            </q-tooltip>
          </q-icon>
        </q-chip>
      </div>

      <q-btn
        label="Создать дайджест"
        type="submit"
        icon-right="add"
        color="secondary"
        text-color="white"
        no-caps
        class="q-mt-lg"
      />
    </q-form>
  </q-page>
</template>

<script>
import { availableSources, availableTags, backendURL } from '../data/lookups.js'
import { fasCrown } from '@quasar/extras/fontawesome-v6'
import FilterSelector from '../components/FilterSelector.vue'
import BackButton from 'src/components/BackButton.vue'
import { useRouter } from 'vue-router'

const router = useRouter()

export default {
  components: {
    FilterSelector,
    BackButton,
  },
  setup() {
    return {
      fasCrown,
    }
  },
  data() {
    return {
      title: '',
      description: '',
      notificationEmail: false,
      notificationMobile: false,
      isPublic: false,
      email: '',
      addedEmails: [],
      emailError: false,
      emailExists: false,
      selectedSources: [],
      selectedTags: [],
      availableSources,
      availableTags,
      ownerEmail: '',
      router,
    }
  },
  mounted() {
    this.fetchOwnerEmail()
  },
  methods: {
    async fetchOwnerEmail() {
      try {
        const response = await fetch(
          // '/api/get-owner-email'
          // 'https://f52a38db04bb4e4ab4c5b6b0bd1f9285.api.mockbin.io/'
          backendURL + `users/get_email`, {
            method: 'GET',
            headers: {
              'Content-Type': 'application/json',
            },
            credentials: 'include',
          }
        )
        if (!response.ok) {
          throw new Error('Не удалось получить email владельца')
        }
        const data = await response.json()
        this.ownerEmail = data.email // Записываем email в переменную
      } catch (error) {
        console.error('Ошибка при получении email владельца:', error)
      }
    },
    addEmail() {
      // Проверка на наличие email в списке добавленных
      if (
        this.addedEmails.some((chip) => chip.email === this.email) ||
        this.email === this.ownerEmail
      ) {
        this.emailExists = true
        return
      } else {
        this.emailExists = false
      }

      if (this.email) {
        fetch(
          //`https://a37743da82a54b24895ba26ea5cbc277.api.mockbin.io/`
          backendURL + `users/subscription_settings/check`, {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
          },
          credentials: 'include',
          body: JSON.stringify({ username: this.email }),
        })
          .then((response) => {
            if (response.status === 401) {
              this.$router.replace('/login')
            }
            return response.json()
          })
          .then((data) => {
            console.log(data)
            const { send_to_mail, mobile_notifications } = data
            if (send_to_mail !== undefined || mobile_notifications !== undefined) {
              this.emailError = false
              this.addedEmails.push({
                email: this.email,
                send_to_mail,
                mobile_notifications,
              })
              this.email = ''
            }
          })
          .catch((error) => {
            console.error('Ошибка:', error)
            this.emailError = true
          })
      }
    },
    removeChip(index) {
      this.addedEmails.splice(index, 1)
    },
    submitForm() {
      const requestData = {
        title: this.title,
        description: this.description,
        sources: this.selectedSources.map((source) => source.value),
        tags: this.selectedTags.map((tag) => tag.value),
        subscribe_options: {
          send_to_mail: this.notificationEmail,
          mobile_notifications: this.notificationMobile,
        },
        public: this.isPublic,
        followers: this.addedEmails.map((email) => email.email),
      }
      console.log(JSON.stringify(requestData))

      // Отправляем данные на сервер
      fetch(backendURL + 'subscriptions/create', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        credentials: 'include',
        body: JSON.stringify(requestData),
      })
        .then((response) => response.json())
        .then((data) => {
          console.log(data)
          this.$router.replace('/digests')
        })
        .catch((error) => {
          console.error('Ошибка при создании дайджеста:', error)
        })
    },
  },
}
</script>

<style scoped>
.q-mb-xs {
  margin-bottom: 5px;
}
</style>
