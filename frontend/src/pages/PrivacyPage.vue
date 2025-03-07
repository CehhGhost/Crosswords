<template>
  <q-page class="page-body q-pa-xs">
    <div class="q-gutter-lg">
      <q-btn label="к другим настройкам" @click="toggleDrawer" class="q-mb-md" outline />
      <h4 class="caption">Настройки аккаунта</h4>

      <div>
        <h6 class="q-mb-none UIText">Сменить почту</h6>
        <div class="row items-center q-mt-sm">
          <q-input filled v-model="email" label="Email" class="col" type="email" :color="$q.dark.isActive ? 'primary' : 'accent'"/>
          <q-btn
            label="Обновить"
            color="primary"
            no-caps
            text-color="secondary"
            @click="updateEmail"
            padding="md"
            unelevated
          />
        </div>
      </div>

      <div>
        <h6 class="q-mb-lg UIText">Изменить пароль</h6> 
        <div class="q-gutter-md">
          <q-input filled type="password" v-model="oldPassword" label="старый пароль" :color="$q.dark.isActive ? 'primary' : 'accent'"/>
          <q-input filled type="password" v-model="newPassword" label="новый пароль" :color="$q.dark.isActive ? 'primary' : 'accent'"/>
          <q-btn
            label="Обновить пароль"
            color="primary"
            @click="updatePassword"
            :disable="!oldPassword || !newPassword"
            no-caps
            text-color="secondary"
          />
        </div>
      </div>

      <div>
        <h4 class="q-mb-none caption">Настройки уведомлений</h4>
        <div class="q-gutter-md q-mt-sm">
          <q-toggle v-model="sendToMail" label="Разрешить уведомления на почту" />
          <q-toggle v-model="mobileNotifications" label="Разрешить мобильные уведомления" />
          <q-checkbox
            v-model="allowForeignSubs"
            label="Разрешить другим пользователями добавлять вас в рассылку дайджестов"
          />

          <div v-if="allowForeignSubs" class="q-ml-lg q-gutter-md">
            <q-toggle
              v-model="foreignSendToMail"
              label="Разрешить посторонние уведомления на почту"
            />
            <q-toggle
              v-model="foreignMobileNotifications"
              label="Разрешить посторонние мобильные уведомления"
            />
          </div>
        </div>
      </div>
    </div>
  </q-page>
</template>

<script>
import { ref, onMounted } from 'vue'
import axios from 'axios'
import useDrawer from 'src/composables/useDrawer'
export default {
  name: 'AccountSettingsPage',
  setup() {
    const { toggleDrawer } = useDrawer()
    const email = ref('')
    const oldPassword = ref('')
    const newPassword = ref('')
    const mobileNotifications = ref(false)
    const sendToMail = ref(false)
    const allowForeignSubs = ref(false)
    const foreignMobileNotifications = ref(false)
    const foreignSendToMail = ref(false)
    onMounted(async () => {
      try {
        const response = await axios.get('/api/user-details')
        const data = response.data
        email.value = data.email
        mobileNotifications.value = data.mobile_notifications
        sendToMail.value = data.send_to_mail
        allowForeignSubs.value = data.allow_foreign_subs
        foreignMobileNotifications.value = data.foreign_mobile_notifications
        foreignSendToMail.value = data.foreign_send_to_mail
      } catch (error) {
        console.error('Error fetching user details:', error)
      }
    })

    const updateEmail = async () => {
      try {
        await axios.post('/api/update-email', { email: email.value })
      } catch (error) {
        console.error('Error updating email:', error)
      }
    }

    const updatePassword = async () => {
      try {
        await axios.post('/api/update-password', {
          oldPassword: oldPassword.value,
          newPassword: newPassword.value,
        })
        oldPassword.value = ''
        newPassword.value = ''
      } catch (error) {
        console.error('Error updating password:', error)
      }
    }

    return {
      email,
      oldPassword,
      newPassword,
      mobileNotifications,
      sendToMail,
      allowForeignSubs,
      foreignMobileNotifications,
      foreignSendToMail,
      updateEmail,
      updatePassword,
      toggleDrawer
    }
  },
}
</script>

<style scoped>
.row {
  display: flex;
  align-items: center;
}
</style>
