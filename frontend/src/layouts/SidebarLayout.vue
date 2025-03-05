<template>
    <q-layout view="hhh Lpr fff">
      <Navbar />
  
      <!-- Боковое меню -->
      <div v-if="$q.screen.gt.sm" side="left" bordered>
      <Drawer />
      </div>

    <div v-if="$q.screen.lt.sm" class="q-pa-md">
      <MobileNav />
    </div>
  
      <!-- Контейнер для страниц -->
      <q-page-container>
        <router-view />
      </q-page-container>
  
      <Footer />
    </q-layout>
  </template>
  
  <script setup>
  import { useQuasar } from 'quasar'
  import Footer from 'src/components/FooterComponent.vue'
  import Navbar from '../components/TopNavbar.vue'
  import Drawer from '../components/SettingsDrawer.vue'

  const $q = useQuasar()

  </script>
  