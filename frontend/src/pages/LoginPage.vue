<template>
    <q-page class="row justify-center items-center">
      <q-card class="col-12 col-sm-6 col-md-4">
        <q-card-section>
          <div class="text-h6">Вход</div>
        </q-card-section>
        
        <q-card-section>
          <q-input filled v-model="form.email" label="Email" />
          <q-input filled v-model="form.password" type="password" label="Пароль" />
        </q-card-section>
  
        <q-card-actions align="right">
          <q-btn label="Войти" color="primary" @click="login" />
        </q-card-actions>
      </q-card>
    </q-page>
  </template>
  
  <script setup>
  import { ref } from 'vue'
  import { useRouter } from 'vue-router'
  import api from 'src/boot/axios' 
  
  const router = useRouter()
  
  const form = ref({
    email: '',
    password: ''
  })
  
  async function login() {
    try {
      const response = await api.post('/login', form.value)
      if (response.status === 200) {
        const { accessToken } = response.data
        localStorage.setItem('accessToken', accessToken)
        router.push('/')
      }
    } catch (error) {
      console.error('Ошибка при логине', error)
    }
  }
  </script>
  