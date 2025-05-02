<template>
  <q-btn
    flat
    icon="arrow_back"
    label="Назад"
    @click="goBack"
    class="q-mb-md"
  />
</template>

<script setup>
import { useRouter } from 'vue-router'
import { defineProps } from 'vue'

const router = useRouter()

const { to } = defineProps({
  to: {
    type: String,
    required: true
  }
})

function goBack() {
  router.push(to)
}
</script>

<style scoped>
</style>
