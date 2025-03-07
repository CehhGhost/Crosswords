<template>
    <div class="subscription-btn-wrapper">
      <!-- Если пользователь не подписан, показываем кнопку "Подписаться" -->
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
  
      <!-- Если пользователь подписан, показываем dropdown для управления подпиской -->
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
  
      <!-- Диалог для передачи прав владельца (popup) -->
      <owner-transfer-dialog
        v-model="showOwnerTransferDialog"
        :subscriberEmails="subscriberEmails"
        :transferLoading="transferLoading"
        :transferError="transferError"
        @cancel="handleOwnerTransferCancel"
        @confirm="handleOwnerTransferConfirm"
      />
    </div>
  </template>
  
  <script>
  import OwnerTransferDialog from "./OwnerTransferDialog.vue";
  
  export default {
    name: "SubscriptionButton",
    components: {
      OwnerTransferDialog,
    },
    props: {
      digest: {
        type: Object,
        required: true,
      },
      ownerChangeBackendUrl: {
        type: String,
        required: true,
      },
      subscriptionUpdateBackendUrl: {
        type: String,
        required: true,
      },
    },
    data() {
      return {
        dropdown: false,
        // начальное состояние берём из digest.subscribe_options
        subscribed: this.digest.subscribe_options.subscribed,
        sendToMail: this.digest.subscribe_options.send_to_mail,
        mobileNotifications: this.digest.subscribe_options.mobile_notifications,
        // данные для диалога передачи прав
        subscriberEmails: [],
        showOwnerTransferDialog: false,
        transferLoading: false,
        transferError: "",
      };
    },
    methods: {
      async onSubscribeClick() {
        this.subscribed = true;
        this.sendToMail = true;
        this.mobileNotifications = true;
        await this.sendSubscriptionUpdate();
      },
      toggleCheckbox(type) {
        this[type] = !this[type];
        this.updateSubscription();
      },
      onDropdownShow() {
        this.backup = {
          sendToMail: this.sendToMail,
          mobileNotifications: this.mobileNotifications,
        };
      },
      async onUnsubscribeClick() {
        // Если пользователь является владельцем, проверяем список подписчиков
        if (this.digest.is_owner) {
          try {
            // GET запрос для получения списка подписчиков с передачей id в URL
            const response = await fetch(
              //`https://your-backend.example.com/api/get-subscribers?digestId=${this.digest.id}`
              "https://3faa0d5a1c344675acdf7cae9c36a1f8.api.mockbin.io/"
            );
            const result = await response.json();
            if (result && result.length) {
              // Если список не пуст – открываем диалог для выбора нового владельца
              this.subscriberEmails = result;
              this.showOwnerTransferDialog = true;
              return; // отписка произойдёт после подтверждения в диалоге
            }
          } catch (error) {
            console.error("Ошибка получения списка подписчиков:", error);
          }
        }
        // Если не владелец или список пуст, выполняем отписку как обычно
        this.executeUnsubscribe();
      },
      async executeUnsubscribe() {
        this.subscribed = false;
        this.sendToMail = false;
        this.mobileNotifications = false;
        this.dropdown = false;
        await this.sendSubscriptionUpdate();
      },
      async handleOwnerTransferConfirm(newOwnerEmail) {
        // При подтверждении, не закрываем диалог до получения ответа от backend
        this.transferLoading = true;
        this.transferError = "";
        try {
          // Отправляем запрос на передачу прав новому владельцу
          const response = await fetch(
            //`${this.ownerChangeBackendUrl}/transfer-ownership`
            "https://6c42bdb2e6e94c5094cbc38dbd534d3f.api.mockbin.io/", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({
                id: this.digest.id,
                owner: newOwnerEmail,
            }),
          });
          if (!response.ok) {
            this.transferError = "Что-то пошло не так при передаче прав";
            return;
          }
          // После успешной передачи прав выполняем отписку
          await this.executeUnsubscribe();
          this.showOwnerTransferDialog = false;
        } catch (error) {
          console.error("Ошибка запроса:", error);
          this.transferError = "Что-то пошло не так при передаче прав";
        } finally {
          this.transferLoading = false;
        }
      },
      handleOwnerTransferCancel() {
        console.log("Выбор нового владельца отменён");
      },
      async updateSubscription() {
        await this.sendSubscriptionUpdate();
      },
      async sendSubscriptionUpdate() {
        const payload = {
          id: this.digest.id,
          subscribed: this.subscribed,
          send_to_mail: this.sendToMail,
          mobile_notifications: this.mobileNotifications,
        };
  
        try {
          const response = await fetch(
            //this.subscriptionUpdateBackendUrl
            "https://4ec3051a148c46c8a8aa6dcfef35cdf8.api.mockbin.io/"
            , {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(payload),
          });
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
  .subscription-btn-wrapper {
    display: flex;
    align-items: center;
  }
  </style>
  