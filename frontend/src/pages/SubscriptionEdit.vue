<template>
    <q-page class="page-body q-mt-md">
      <BackButton to="/digests"/>
    <div class="text-h5 caption" :class="{ 'text-secondary': !$q.dark.isActive, 'text-white': $q.dark.isActive }" >
        Редактирование заказа на дайджест
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
          />
          <FilterSelector
            v-model="selectedTags"
            :label="'Теги'"
            :options="availableTags"
          />
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
          <q-chip
            v-for="(chip, index) in addedEmails"
            :key="index"
            :label="chip.email"
            :removable="chip.email !== ownerEmail"
            color="primary"
            text-color="secondary"
            @remove="removeChip(index)"
            class="q-mb-xs"
          >
            <q-icon v-if="chip.has_email" name="mail" color="secondary" class="q-ml-xs">
              <q-tooltip class="bg-primary text-secondary" transition-show="scale" transition-hide="scale">
                Этот пользователь получает уведомления на почту
              </q-tooltip>
            </q-icon>
            <q-icon v-if="chip.has_mobile" name="phone_iphone" color="secondary" class="q-ml-xs">
              <q-tooltip class="bg-primary text-secondary" transition-show="scale" transition-hide="scale">
                Этот пользователь получает мобильные уведомления
              </q-tooltip>
            </q-icon>
            <!-- Стрелочка вверх для изменения владельца -->
            <q-icon
              v-if="chip.email !== ownerEmail"
              name="arrow_upward"
              color="secondary"
              class="q-ml-xs"
              @click="openConfirmationPopup(chip.email)"
            >
              <q-tooltip class="bg-primary text-secondary" transition-show="scale" transition-hide="scale">
                Назначить владельцем
              </q-tooltip>
            </q-icon>
            <!-- Иконка короны для текущего владельца -->
            <q-icon v-if="chip.email === ownerEmail" :name="fasCrown" color="secondary" class="q-ml-xs">
              <q-tooltip class="bg-primary text-secondary" transition-show="scale" transition-hide="scale">
                Это текущий владелец
              </q-tooltip>
            </q-icon>
          </q-chip>
        </div>
  
        <q-btn
          label="Сохранить изменения"
          type="submit"
          icon-right="save"
          color="secondary"
          text-color="white"
          no-caps
          class="q-mt-lg"
        />
      </q-form>
      <ConfirmationPopup
      v-model="isPopupOpen"
      title="Подтверждение передачи прав"
      :message="popupMessage"
      checkboxLabel="Я понимаю, что делаю"
      confirmLabel="Подтвердить"
      @confirm="setAsOwnerConfirmed"
      @cancel="closeConfirmationPopup"
    />
    </q-page>
  </template>
  
  <script>
  import { availableSources, availableTags } from '../data/lookups.js'
  import { fasCrown } from '@quasar/extras/fontawesome-v6'
  import FilterSelector from '../components/FilterSelector.vue'
  import ConfirmationPopup from '../components/ConfirmDialog.vue'
  import BackButton from 'src/components/BackButton.vue'
  
  export default {
  components: {
    FilterSelector,
    ConfirmationPopup,
    BackButton
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
      isPopupOpen: false,
      popupMessage: '', 
    }
  },
  mounted() {
    // const digestId = this.$route.params.id; // Получаем ID из URL
    fetch(
        //`/api/get-digest/${digestId}`
        'https://cd38f834d72d4f5abd4105ca6807a70f.api.mockbin.io/'
    )
      .then(response => response.json())
      .then(data => {
        this.title = data.title;
        this.description = data.description;
        this.selectedSources = data.sources;
        this.selectedTags = data.tags;
        this.notificationEmail = data.subscribe_options.send_to_mail;
        this.notificationMobile = data.subscribe_options.mobile_notifications;
        this.isPublic = data.public;
        this.ownerEmail = data.owner;
        // Заполняем подписчиков с учетом настроек уведомлений
        this.addedEmails = data.followers.map(follower => ({
          email: follower.email,
          has_email: follower.send_to_mail,
          has_mobile: follower.mobile_notifications,
        }));
      })
      .catch(error => console.error('Ошибка при получении данных дайджеста:', error));
  },
  methods: {
    addEmail() {
      if (
        this.addedEmails.some((chip) => chip.email === this.email) ||
        this.email === this.ownerEmail
      ) {
        this.emailExists = true;
        return;
      } else {
        this.emailExists = false;
      }

      if (this.email) {
        fetch('https://a37743da82a54b24895ba26ea5cbc277.api.mockbin.io/', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
          },
          body: JSON.stringify({ credentials: this.email }),
        })
          .then((response) => response.json())
          .then((data) => {
            const { has_email, has_mobile } = data;
            if (has_email !== undefined || has_mobile !== undefined) {
              this.emailError = false;
              this.addedEmails.push({
                email: this.email,
                has_email,
                has_mobile,
              });
              this.email = '';
            }
          })
          .catch((error) => {
            console.error('Ошибка:', error);
            this.emailError = true;
          });
      }
    },
    removeChip(index) {
      this.addedEmails.splice(index, 1);
    },
    openConfirmationPopup(email) {
      this.emailToBeOwner = email;
      this.popupMessage = `Вы уверены, что хотите передать права владельца пользователю с email: ${email}?`;
      this.isPopupOpen = true;
    },
    closeConfirmationPopup() {
      this.isPopupOpen = false;
    },
    setAsOwnerConfirmed() {
      this.ownerEmail = this.emailToBeOwner;
      this.isPopupOpen = false;
    },
    setAsOwner(newOwnerEmail) {
      // Изменяем владельца
      this.ownerEmail = newOwnerEmail;
    },
    submitForm() {
      const requestData = {
        title: this.title,
        description: this.description,
        sources: this.selectedSources,
        tags: this.selectedTags,
        subscribe_options: {
          send_to_mail: this.notificationEmail,
          mobile_notifications: this.notificationMobile,
        },
        public: this.isPublic,
        followers: this.addedEmails.map((email) => ({
          email: email.email,
          send_to_mail: email.has_email,
          mobile_notifications: email.has_mobile,
        })),
        owner: this.ownerEmail,  // Передаем новый email владельца
      };
      console.log(JSON.stringify(requestData));

      // Отправка данных на сервер
      const digestId = this.$route.params.id;
      fetch(`/api/update-digest/${digestId}`, {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(requestData),
      })
        .then((response) => response.json())
        .then((data) => {
          console.log('Изменения успешно сохранены:', data);
        })
        .catch((error) => {
          console.error('Ошибка при сохранении изменений:', error);
        });
    },
  },
}
  </script>
  
  <style scoped>
  .q-mb-xs {
    margin-bottom: 5px;
  }
  </style>
  