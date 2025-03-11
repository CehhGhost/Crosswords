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
          <p >
            Нет аккаунта? <a href="/register">Зарегистрируйтесь - это быстро!</a>
          </p>
        </div>
        
        <!-- Кнопка входа -->
        <q-btn type="submit" label="Войти" no-caps text-color="secondary" color="primary" class="full-width" />
      </q-form>
    </div>
  </q-page>
</template>

<script>
import lightLogo from '../assets/crosswords_mono.png';
import darkLogo from '../assets/crosswords_mono_white.png';
export default {
  name: 'LoginPage',
  data() {
    return {
      email: '',
      password: ''
    }
  },
  computed: {
    logoSrc() {
      return this.$q.dark.isActive ? darkLogo : lightLogo;
    }
  },
  methods: {
    login() {
      // Здесь можно добавить логику аутентификации
      console.log('Email:', this.email);
      console.log('Password:', this.password);
    }
  }
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
