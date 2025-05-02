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
        <q-toggle
          v-model="personal_send_to_mail"
          @update:model-value="updateSubscriptions"
          label="Разрешить уведомления на почту"
        />
        <q-toggle
          v-model="personal_mobile_notifications"
          @update:model-value="updateSubscriptions"
          label="Разрешить мобильные уведомления"
        />
        <q-checkbox
          v-model="allowForeignSubs"
          label="Разрешить другим пользователями добавлять вас в рассылку дайджестов"
          @update:model-value="updateSubscriptions"
        />

        <div v-if="allowForeignSubs" class="q-ml-lg q-gutter-md">
          <q-toggle
            v-model="send_to_mail"
            label="Разрешить посторонние уведомления на почту"
            @update:model-value="updateSubscriptions"
          />
          <q-toggle
            v-model="mobile_notifications"
            label="Разрешить посторонние мобильные уведомления"
            @update:model-value="updateSubscriptions"
          />
        </div>
      </div>
    </div>
  </q-page>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import useDrawer from 'src/composables/useDrawer'
import { useRouter } from 'vue-router'
import { backendURL } from 'src/data/lookups'
import { useQuasar } from 'quasar'
import { emitter } from 'src/boot/emitter'

const $q = useQuasar()
const router = useRouter()
const { toggleDrawer } = useDrawer()

const first_name = ref('')
const last_name = ref('')
const username = ref('')
const email = ref('')
const static_email = ref('')
const oldPassword = ref('')
const newPassword = ref('')
const personal_mobile_notifications = ref(false)
const personal_send_to_mail = ref(false)
const allowForeignSubs = ref(false)
const send_to_mail = ref(false)
const mobile_notifications = ref(false)

onMounted(async () => {
  try {
    const resp = await fetch(`${backendURL}users/personal_info`, { credentials: 'include' })
    const data = await resp.json()
    first_name.value = data.first_name
    last_name.value = data.second_name
    username.value = data.username
    email.value = data.email
    static_email.value = data.email
    allowForeignSubs.value = data.subscribable
    personal_mobile_notifications.value = data.personal_mobile_notifications
    personal_send_to_mail.value = data.personal_send_to_mail
    mobile_notifications.value = data.mobile_notifications
    send_to_mail.value = data.send_to_mail
  } catch (err) {
    console.error('Error fetching user details:', err)
    $q.notify({ type: 'negative', message: 'Не удалось загрузить данные пользователя', position: 'top' })
  }
})

async function updateSubscriptions() {
  try {
    const payload = {
      subscribable: allowForeignSubs.value,
      send_to_mail: send_to_mail.value,
      mobile_notifications: mobile_notifications.value,
      personal_send_to_mail: personal_send_to_mail.value,
      personal_mobile_notifications: personal_mobile_notifications.value
    }
    const res = await fetch(`${backendURL}users/subscription_settings/set`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      credentials: 'include',
      body: JSON.stringify(payload)
    })
    if (!res.ok) throw new Error(`Status ${res.status}`)
    $q.notify({
      type: 'positive',
      message: 'Настройки уведомлений сохранены',
      position: 'top',
      badgeColor: 'yellow',
      badgeTextColor: 'dark'
    })
  } catch (err) {
    console.error('Ошибка при сохранении уведомлений', err)
    $q.notify({ type: 'negative', message: 'Не удалось сохранить настройки уведомлений', position: 'top' })
  }
}

async function updateEmail() {
  try {
    const res = await fetch(`${backendURL}users/change/email`, {
      method: 'PATCH',
      headers: { 'Content-Type': 'application/json' },
      credentials: 'include',
      body: JSON.stringify({ new_email: email.value })
    })
    if (res.ok) {
      $q.notify({ type: 'positive', message: 'Email успешно обновлён', position: 'top' })
      const data = await res.json()
      email.value = data.email
      static_email.value = data.email
      username.value = data.username
    } else if (res.status === 401) {
      router.replace('/login')
    } else if (res.status === 400) {
      $q.notify({ type: 'negative', message: 'Новый email не может быть таким же, как старый или пустым', position: 'top' })
    } else {
      $q.notify({ type: 'negative', message: 'Ошибка при обновлении email', position: 'top' })
    }
  } catch (err) {
    console.error('Error updating email:', err)
    $q.notify({ type: 'negative', message: 'Ошибка при обновлении email', position: 'top' })
  }
}

async function updatePassword() {
  try {
    const res = await fetch(`${backendURL}users/change/password`, {
      method: 'PATCH',
      headers: { 'Content-Type': 'application/json' },
      credentials: 'include',
      body: JSON.stringify({ old_password: oldPassword.value, new_password: newPassword.value })
    })
    oldPassword.value = ''
    newPassword.value = ''
    if (res.ok) {
      $q.notify({ type: 'positive', message: 'Пароль успешно обновлён', position: 'top' })
    } else if (res.status === 401) {
      router.replace('/login')
    } else if (res.status === 403) {
      $q.notify({ type: 'negative', message: 'Неверный старый пароль', position: 'top' })
    } else if (res.status === 400) {
      $q.notify({ type: 'negative', message: 'Новый пароль не может быть таким же, как старый или пустым', position: 'top' })
    } else {
      $q.notify({ type: 'negative', message: 'Ошибка при обновлении пароля', position: 'top' })
    }
  } catch (err) {
    console.error('Error updating password:', err)
    $q.notify({ type: 'negative', message: 'Ошибка при обновлении пароля', position: 'top' })
  }
}

async function logout() {
  const res = await fetch(`${backendURL}users/logout`, { method: 'POST', headers: { 'Content-Type': 'application/json' }, credentials: 'include' })
  if (!res.ok) {
    if (res.status === 401) router.replace('/login')
    else $q.notify({ type: 'negative', message: 'Ошибка при выходе из аккаунта', position: 'top' })
    return
  }
  emitter.emit('auth-changed')
  router.replace('/login')
  $q.notify({ type: 'positive', message: 'Вы вышли из аккаунта', position: 'top' })
}

async function globalLogout() {
  const res = await fetch(`${backendURL}users/logout/full`, { method: 'POST', headers: { 'Content-Type': 'application/json' }, credentials: 'include' })
  if (!res.ok) {
    if (res.status === 401) router.replace('/login')
    else $q.notify({ type: 'negative', message: 'Ошибка при выходе из аккаунта', position: 'top' })
    return
  }
  emitter.emit('auth-changed')
  router.replace('/login')
  $q.notify({ type: 'positive', message: 'Вы вышли из аккаунта', position: 'top' })
}
</script>

<style scoped>
.row {
  display: flex;
  align-items: center;
}
</style>
