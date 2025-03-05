<template>
  <q-layout view="hhh lpr fff">
    <!-- Navbar -->
    <Navbar />
    

    <!-- Контейнер для страниц -->
    <q-page-container>
      <!-- Отступы для содержимого -->
      <q-page >
        <!-- Здесь будет меняться контент страниц -->
        <router-view />
      </q-page>
    </q-page-container>
    <Footer />
  </q-layout>
</template>

<script setup>
  import Footer from 'src/components/FooterComponent.vue';
  import Navbar from '../components/TopNavbar.vue'
</script>
