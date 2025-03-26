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
      :loading="loadingSubscription"
      :disable="loadingSubscription"
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
      :loading="loadingDropdown"
      :disable="loadingDropdown"
    >
      <q-list>
        <q-item clickable @click="toggleCheckbox('sendToMail')">
          <q-item-section>
            <q-checkbox
              v-model="sendToMail"
              class="custom-checkbox"
              label="На почту"
              @update:model-value="sendSubscriptionUpdate"
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
              @update:model-value="sendSubscriptionUpdate"
              @click.stop
            />
          </q-item-section>
          <q-item-section avatar>
            <q-icon name="phone_iphone" :color="$q.dark.isActive ? 'primary' : 'secondary'" />
          </q-item-section>
        </q-item>

        <q-separator />

        <q-item clickable @click="onUnsubscribeClick">
          <q-item-section> Отписаться </q-item-section>
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
import OwnerTransferDialog from './OwnerTransferDialog.vue'

export default {
  name: 'SubscriptionButton',
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
    is_digest: {
      type: Boolean,
      required: true,
    },
  },
  data() {
    return {
      dropdown: false,
      loadingSubscription: false,
      loadingDropdown: false,
      // начальное состояние берём из digest.subscribe_options
      subscribed: this.digest.subscribe_options.subscribed,
      sendToMail: this.digest.subscribe_options.send_to_mail,
      mobileNotifications: this.digest.subscribe_options.mobile_notifications,
      // данные для диалога передачи прав
      subscriberEmails: [],
      showOwnerTransferDialog: false,
      transferLoading: false,
      transferError: '',
    }
  },
  methods: {
    async onSubscribeClick() {
      if (this.loadingSubscription) return
      this.loadingSubscription = true
      // Сохраняем исходное состояние

      // Обновляем локальное состояние для формирования payload

      const success = await this.sendSubscriptionUpdate()
      if (!success) {
        this.subscribed = false
      } else {
        this.subscribed = true
      }
      this.loadingSubscription = false
    },
    async toggleCheckbox(type) {
      // Сохраняем предыдущее значение чекбокса
      const originalValue = this[type]
      this[type] = !this[type]
      const success = await this.sendSubscriptionUpdate()
      if (!success) {
        // Если обновление не удалось – возвращаем прежнее значение
        this[type] = originalValue
      }
    },

    onDropdownShow() {
      this.backup = {
        sendToMail: this.sendToMail,
        mobileNotifications: this.mobileNotifications,
      }
    },
    async onUnsubscribeClick() {
      if (this.loadingDropdown) return
      this.loadingDropdown = true
      // Если пользователь является владельцем, проверяем список подписчиков
      if (this.digest.is_owner) {
        try {
          const response = await fetch('https://3faa0d5a1c344675acdf7cae9c36a1f8.api.mockbin.io/')
          const result = await response.json()
          if (result && result.length) {
            this.subscriberEmails = result
            this.showOwnerTransferDialog = true
            this.loadingDropdown = false
            return
          }
          if (response.status === 401) {
              this.$router.replace('/login')
            }
        } catch (error) {
          console.error('Ошибка получения списка подписчиков:', error)
        }
      }
      await this.executeUnsubscribe()
    },
    async executeUnsubscribe() {
      // Сохраняем исходное состояние
      const originalSubscribed = this.subscribed
      const originalSendToMail = this.sendToMail
      const originalMobileNotifications = this.mobileNotifications
      // Обновляем локальное состояние для формирования payload
      this.sendToMail = null
      this.mobileNotifications = null
      const success = await this.sendSubscriptionUpdate()
      if (success) {
        this.subscribed = false
      } else {
        // Если запрос не удался – откатываем состояние
        this.sendToMail = originalSendToMail
        this.mobileNotifications = originalMobileNotifications
        this.subscribed = originalSubscribed
      }
      this.loadingDropdown = false
    },
    async handleOwnerTransferConfirm(newOwnerEmail) {
      this.transferLoading = true
      this.transferError = ''
      try {
        const response = await fetch(
          'https://6c42bdb2e6e94c5094cbc38dbd534d3f.api.mockbin.io/', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            id: this.digest.id,
            owner: newOwnerEmail,
          }),
        })
        if (!response.ok) {
          if (response.status === 401) {
            this.$router.replace('/login')
          } else {
            this.transferError = 'Что-то пошло не так при передаче прав'
          }
          return
        }
        // После успешной передачи прав выполняем отписку
        await this.executeUnsubscribe()
        this.showOwnerTransferDialog = false
      } catch (error) {
        console.error('Ошибка запроса:', error)
        this.transferError = 'Что-то пошло не так при передаче прав'
      } finally {
        this.transferLoading = false
      }
    },
    handleOwnerTransferCancel() {
      console.log('Выбор нового владельца отменён')
    },

    async sendSubscriptionUpdate() {
      const payload = {
        id: this.digest.id,
        subscribed: this.subscribed,
        send_to_mail: this.sendToMail,
        mobile_notifications: this.mobileNotifications,
        is_digest: this.is_digest,
      }

      try {
        const response = await fetch('https://d41ad2c28576472aacba47ad07e4bf5b.api.mockbin.io/', {
          method: 'PUT',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify(payload),
        })
        if (!response.ok) {
          if (response.status === 401) {
            this.$router.replace('/login')
          } else {
            console.error('Ошибка при изменении настроек подписки')
          }
          return false
        }
        // Если запрос успешен – обновляем значения из ответа
        const data = await response.json()
        console.log('Payload:', JSON.stringify(payload))
        this.sendToMail = data.send_to_mail
        this.mobileNotifications = data.mobile_notifications
        return true
      } catch (error) {
        console.error('Ошибка запроса:', error)
        return false
      }
    },
  },
}
</script>

<style scoped>
.subscription-btn-wrapper {
  display: flex;
  align-items: center;
}
</style>
