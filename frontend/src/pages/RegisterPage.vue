<template>
  <q-page class="flex flex-center q-pa-sm">
    <div class="login-container">
      <!-- Логотип, располагается чуть выше центра -->
      <div class="logo-container">
        <q-img :src="logoSrc" alt="Логотип" class="logo-img" />
      </div>

      <!-- Заголовок -->
      <div class="title text-center q-mt-sm">
        <h4 class="caption">Создайте аккаунт</h4>
      </div>

      <!-- Форма входа -->
      <q-form @submit.prevent="register" class="q-mt-md">
        <!-- Поля для ввода -->
        <q-input
          filled
          v-model="name"
          label="Имя"
          class="q-mb-md"
          :color="$q.dark.isActive ? 'primary' : 'accent'"
        />
        <q-input
          filled
          v-model="surname"
          label="Фамилия"
          class="q-mb-md"
          :color="$q.dark.isActive ? 'primary' : 'accent'"
        />
        <q-input
          filled
          v-model="email"
          label="Email"
          type="email"
          class="q-mb-md"
          :color="$q.dark.isActive ? 'primary' : 'accent'"
        />

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
          <p>Уже есть зарегистрированный аккаунт? <a href="/login">Войти</a></p>
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
export default {
  name: 'RegisterPage',
  data() {
    return {
      email: '',
      password: '',
      name: '',
      surname: '',
    }
  },
  computed: {
    logoSrc() {
      return this.$q.dark.isActive ? darkLogo : lightLogo
    },
  },
  methods: {
    register() {
      const payload = {
        username: this.email,
        password: this.password,
        name: this.name,
        surname: this.surname,
        email: this.email,
      }
      fetch(
        backendURL + 'users/register',
        //'https://60b277858b7d4dbc8347955c8dc89e8e.api.mockbin.io/',
        {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
          },
          body: JSON.stringify(payload),
        },
      )
        .then((response) => {
          if (!response.ok) {
            throw new Error('Ошибка при входе')
          }
          return response.json()
        })
        .then((data) => {
          console.log(data)
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
