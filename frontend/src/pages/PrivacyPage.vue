<template>
  <q-page class="page-body q-pa-md">
    <q-btn label="к другим настройкам" @click="toggleDrawer" class="q-mb-xl" outline />
    <div class="text-h4 caption">Настройки аккаунта</div>

    <q-card class="q-mb-xl q-mt-md" :class="{ 'no-shadow': $q.dark.isActive }">
      <q-card-section>
        <div class="text-h6 q-mb-md">Параметры аккаунта</div>
        <div><strong>Имя:</strong> {{ first_name }}</div>
        <div><strong>Фамилия:</strong> {{ last_name }}</div>
        <div>
          <strong>username:</strong> {{ username }}
          <q-icon name="info">
            <q-tooltip
              anchor="top middle"
              self="bottom middle"
              class="bg-primary text-secondary"
              transition-show="scale"
              transition-hide="scale"
            >
              Ваш username автоматически создается на основе преддоменной части вашей электронной
              почты. <br />
              Его можно использовать вместо почты для авторизации и добавления пользователей в
              дайджесты.
            </q-tooltip></q-icon
          >
        </div>
        <div><strong>email:</strong> {{ static_email }}</div>
      </q-card-section>
    </q-card>

    <div>
      <h6 class="q-mb-none UIText">Сменить почту</h6>
      <div class="row items-center q-mt-sm">
        <q-input
          filled
          v-model="email"
          label="Email"
          class="col"
          type="email"
          :color="$q.dark.isActive ? 'primary' : 'accent'"
        />
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
        <q-input
          filled
          type="password"
          v-model="oldPassword"
          label="старый пароль"
          :color="$q.dark.isActive ? 'primary' : 'accent'"
        />
        <q-input
          filled
          type="password"
          v-model="newPassword"
          label="новый пароль"
          :color="$q.dark.isActive ? 'primary' : 'accent'"
        />
        <q-btn
          label="Обновить пароль"
          color="primary"
          @click="updatePassword"
          :disable="!oldPassword || !newPassword"
          no-caps
          text-color="secondary"
        />
      </div>
      <div class="q-mt-sm row q-gutter-sm">
        <q-btn label="Выйти из аккаунта" outline @click="logout" no-caps text-color="negative" />
        <q-btn
          label="Выйти из аккаунта на всех устройствах"
          outline
          @click="globalLogout"
          no-caps
          text-color="negative"
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
  </q-page>
</template>

<script>
import { ref, onMounted } from 'vue'
import useDrawer from 'src/composables/useDrawer'
import { useRouter } from 'vue-router'
import { backendURL } from 'src/data/lookups'
import { useQuasar } from 'quasar'
import { emitter } from 'src/boot/emitter'

export default {
  name: 'AccountSettingsPage',
  setup() {
    const $q = useQuasar()
    const { toggleDrawer } = useDrawer()
    const first_name = ref('')
    const last_name = ref('')
    const username = ref('')
    const email = ref('')
    const static_email = ref('')
    const oldPassword = ref('')
    const newPassword = ref('')
    const mobileNotifications = ref(false)
    const sendToMail = ref(false)
    const allowForeignSubs = ref(false)
    const foreignMobileNotifications = ref(false)
    const foreignSendToMail = ref(false)
    const router = useRouter()

    onMounted(async () => {
      try {
        const response = await fetch(backendURL + 'users/personal_info', {
          credentials: 'include',
        })
        const data = await response.json()
        console.log('User data:', data)
        // Заполняем все поля из ответа API
        first_name.value = data.first_name
        last_name.value = data.second_name
        username.value = data.username
        email.value = data.email
        static_email.value = data.email
        allowForeignSubs.value = data.subscribable
        mobileNotifications.value = data.personal_mobile_notifications
        sendToMail.value = data.personal_send_to_mail
        foreignMobileNotifications.value = data.mobile_notifications
        foreignSendToMail.value = data.send_to_mail
      } catch (error) {
        console.error('Error fetching user details:', error)
        $q.notify({
          type: 'negative',
          message: 'Не удалось загрузить данные пользователя',
          position: 'top',
        })
      }
    })

    const updateEmail = async () => {
      try {
        const response = await fetch(backendURL + `users/change/email`, {
          method: 'PATCH',
          headers: {
            'Content-Type': 'application/json',
          },
          credentials: 'include',
          body: JSON.stringify({
            new_email: email.value,
          }),
        })
        if (response.ok) {
          $q.notify({
            type: 'positive',
            message: 'email успешно обновлён',
            position: 'top',
          })
          const data = await response.json()
          email.value = data.email
          static_email.value = data.email
          username.value = data.username
        } else if (response.status === 401) {
          router.replace('/login')
        } else if (response.status === 400) {
          console.error('Error updating email')
          $q.notify({
            type: 'negative',
            message: 'Новый email не может быть таким же, как старый, а также не может быть пустым',
            position: 'top',
          })
        } else {
          console.error('Error updating email')
          $q.notify({
            type: 'negative',
            message: 'Ошибка при обновлении email',
            position: 'top',
          })
        }
      } catch (error) {
        console.error('Error updating email:', error)
        $q.notify({
          type: 'negative',
          message: 'Ошибка при обновлении email' + error,
          position: 'top',
        })
      }
    }

    const updatePassword = async () => {
      try {
        const response = await fetch(backendURL + `users/change/password`, {
          method: 'PATCH',
          headers: {
            'Content-Type': 'application/json',
          },
          credentials: 'include',
          body: JSON.stringify({
            old_password: oldPassword.value,
            new_password: newPassword.value,
          }),
        })
        oldPassword.value = ''
        newPassword.value = ''
        if (response.ok) {
          $q.notify({
            type: 'positive',
            message: 'Пароль успешно обновлён',
            position: 'top',
          })
        } else if (response.status === 401) {
          router.replace('/login')
        } else if (response.status === 403) {
          console.error('Error updating password')
          $q.notify({
            type: 'negative',
            message: 'Неверный старый пароль',
            position: 'top',
          })
        } else if (response.status === 400) {
          console.error('Error updating password')
          $q.notify({
            type: 'negative',
            message:
              'Новый пароль не может быть таким же, как старый, а также не может быть пустым',
            position: 'top',
          })
        } else {
          const errorData = await response.json()
          console.error('Error updating password:', errorData)
          $q.notify({
            type: 'negative',
            message: 'Ошибка при обновлении пароля',
            position: 'top',
          })
        }
      } catch (error) {
        console.error('Error updating password:', error)
        $q.notify({
          type: 'negative',
          message: 'Ошибка при обновлении пароля',
          position: 'top',
        })
      }
    }

    const logout = async () => {
      const response = await fetch(backendURL + `users/logout`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        credentials: 'include',
      })
      if (!response.ok) {
        if (response.status === 401) {
          router.replace('/login')
        } else {
          console.error('Ошибка при выходе из аккаунта')
          $q.notify({
            type: 'negative',
            message: 'Ошибка при выходе из аккаунта',
            position: 'top',
          })
        }
        return
      }
      emitter.emit('auth-changed')
      router.replace('/login')
      $q.notify({
        type: 'positive',
        message: 'Вы вышли из аккаунта',
        position: 'top',
      })
    }

    const globalLogout = async () => {
      const response = await fetch(backendURL + `users/logout/full`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        credentials: 'include',
      })
      if (!response.ok) {
        if (response.status === 401) {
          router.replace('/login')
        } else {
          console.error('Ошибка при выходе из аккаунта')
          $q.notify({
            type: 'negative',
            message: 'Ошибка при выходе из аккаунта',
            position: 'top',
          })
        }
        return
      }
      emitter.emit('auth-changed')
      router.replace('/login')
      $q.notify({
        type: 'positive',
        message: 'Вы вышли из аккаунта',
        position: 'top',
      })
    }

    return {
      first_name,
      last_name,
      username,
      email,
      static_email,
      oldPassword,
      newPassword,
      mobileNotifications,
      sendToMail,
      allowForeignSubs,
      foreignMobileNotifications,
      foreignSendToMail,
      updateEmail,
      updatePassword,
      toggleDrawer,
      logout,
      globalLogout,
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
