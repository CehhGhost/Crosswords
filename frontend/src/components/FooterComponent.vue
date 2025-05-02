<template>
  <q-footer class="bg-secondary text-white">
    <q-toolbar>
      <q-toolbar-title>
        <span>© 2025 Умный корпус текстов СМИ</span>
      </q-toolbar-title>
      <div>
        <q-btn
          flat
          dense
          round
          icon="mdi-facebook"
          class="q-mx-sm"
          @click="openLink('https://vk.ru')"
        />
        <q-btn
          flat
          dense
          round
          icon="mdi-instagram"
          class="q-mx-sm"
          @click="openLink('https://vk.ru')"
        />
        <q-btn
          flat
          dense
          round
          icon="mdi-twitter"
          class="q-mx-sm"
          @click="openLink('https://vk.ru')"
        />
      </div>
    </q-toolbar>
  </q-footer>
</template>

<script setup>
function openLink(url) {
  window.open(url, '_blank')
}
</script>

<style scoped>
</style>
