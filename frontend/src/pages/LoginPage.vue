<template>
  <q-page class="flex flex-center q-pa-sm">
    <div class="login-container">
      <!-- Логотип, располагается чуть выше центра -->
      <div class="logo-container">
        <q-img :src="logoSrc" alt="Логотип" class="logo-img" />
      </div>

      <!-- Заголовок -->
      <div class="title text-center q-mt-sm">
        <h4 class="caption">Добро пожаловать!</h4>
      </div>

      <!-- Форма входа -->
      <q-form @submit.prevent="login" class="q-mt-md">
        <!-- Поля для ввода -->
        <q-input
          filled
          v-model="email"
          label="Email"
          
          class="q-mb-md"
          :color="$q.dark.isActive ? 'primary' : 'accent'"
        />
        <!-- type="email" добавить выше-->
        <q-input
          filled
          v-model="password"
          label="Пароль"
          type="password"
          class="q-mb-md"
          :color="$q.dark.isActive ? 'primary' : 'accent'"
        />

        <!-- Текст с предложением регистрации -->
        <div class="text-center q-mb-md">
          <p>Нет аккаунта? <a href="/register">Зарегистрируйтесь - это быстро!</a></p>
        </div>

        <!-- Кнопка входа -->
        <q-btn
          type="submit"
          label="Войти"
          no-caps
          text-color="secondary"
          color="primary"
          class="full-width"
        />
      </q-form>
    </div>
  </q-page>
</template>

<script>
import lightLogo from '../assets/crosswords_mono.png'
import darkLogo from '../assets/crosswords_mono_white.png'
import { backendURL } from 'src/data/lookups'
import { emitter } from 'src/boot/emitter'
export default {
  name: 'LoginPage',
  data() {
    return {
      email: '',
      password: '',
    }
  },
  computed: {
    logoSrc() {
      return this.$q.dark.isActive ? darkLogo : lightLogo
    },
  },
  methods: {
    login() {
      const payload = {
        username: this.email,
        password: this.password,
      }
      fetch(
        backendURL + 'users/login',
        //'https://60b277858b7d4dbc8347955c8dc89e8e.api.mockbin.io/',
        {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
          },
          credentials: 'include',
          body: JSON.stringify(payload),
        },
      )
      .then((response) => {
          if (!response.ok) {
            console.log(response)
            this.$q.notify({
              type: 'negative',
              message: 'Ошибка входа. Проверьте свои данные.',
              position: 'top',
            })
            throw new Error('Ошибка при входе')
          }
          return response.json()
        })
        .then((data) => {
          console.log(data)
          emitter.emit('auth-changed')
          this.$router.replace('/')
        })
        .catch((error) => {
          console.error('Ошибка входа:', error)
        })
    },
  },
}
</script>

<style scoped>
.login-container {
  /* Поднимаем контейнер чуть выше центра */
  margin-top: -50px;
}

.logo-container {
  display: flex;
  justify-content: center;
  margin-bottom: 16px;
}
</style>
