<template>
    <q-btn
      flat
      icon="arrow_back"
      label="Назад"
      @click="goBack"
      class="q-mb-md"
    />
  </template>
  
  <script>
  export default {
    name: 'BackButton',
    props: {
      to: {
        type: String,
        required: true
      }
    },
    methods: {
      goBack() {
        this.$router.push(this.to)
      }
    }
  }
  </script>