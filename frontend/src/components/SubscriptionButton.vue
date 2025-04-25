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
        <q-item clickable @click="toggleCheckbox('send_to_mail')">
          <q-item-section>
            <q-checkbox
              v-model="send_to_mail"
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

        <q-item clickable @click="toggleCheckbox('mobile_notifications')">
          <q-item-section>
            <q-checkbox
              v-model="mobile_notifications"
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
import { backendURL } from 'src/data/lookups'
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
    triggeredFrom: {
      type: String,
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
      send_to_mail: this.digest.subscribe_options.send_to_mail,
      mobile_notifications: this.digest.subscribe_options.mobile_notifications,
      // данные для диалога передачи прав
      subscriberEmails: [],
      showOwnerTransferDialog: false,
      transferLoading: false,
      transferError: '',
      backendURL
    }
  },
  methods: {
    async onSubscribeClick() {
      if (this.loadingSubscription) return
      this.loadingSubscription = true

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
        send_to_mail: this.send_to_mail,
        mobile_notifications: this.mobile_notifications,
      }
    },
    async onUnsubscribeClick() {
      if (this.loadingDropdown) return
      this.loadingDropdown = true
      // Если пользователь является владельцем, проверяем список подписчиков
      if (this.digest.is_owner) {
        try {
          const response = await fetch(
            // "https://3faa0d5a1c344675acdf7cae9c36a1f8.api.mockbin.io/"
            this.backendURL + this.triggeredFrom + `/${this.digest.id}/followers`, {credentials: 'include',})
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
      const originalsend_to_mail = this.send_to_mail
      const originalmobile_notifications = this.mobile_notifications
      // Обновляем локальное состояние для формирования payload
      this.send_to_mail = null
      this.mobile_notifications = null
      const success = await this.sendSubscriptionUpdate()
      if (success) {
        this.subscribed = false
      } else {
        // Если запрос не удался – откатываем состояние
        this.send_to_mail = originalsend_to_mail
        this.mobile_notifications = originalmobile_notifications
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
        subscribed: this.subscribed,
        send_to_mail: this.send_to_mail,
        mobile_notifications: this.mobile_notifications,
      }

      try {
        const response = await fetch(
          //'https://d41ad2c28576472aacba47ad07e4bf5b.api.mockbin.io/'
          this.backendURL + this.triggeredFrom + `/${this.digest.id}/settings/update`, {
          method: 'PUT',
          headers: { 'Content-Type': 'application/json' },
          credentials: 'include',
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
        this.send_to_mail = data.send_to_mail
        this.mobile_notifications = data.mobile_notifications
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
