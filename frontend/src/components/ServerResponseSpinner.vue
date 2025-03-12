<template>
    <div class="q-pa-md q-mt-xl flex flex-center column">
      <div class="text-h4 q-mb-md caption">Ждем ответа от сервера...</div>
      <p>Обычно это занимает всего несколько секунд.</p>
      <q-spinner-dots size="100px" color="primary" />
    </div>
  </template>
  
  <script>
  export default {
    name: 'ServerResponseSpinner'
  }
  </script>
  
  <style scoped>
  /* Дополнительные стили можно добавить здесь */
  </style>
  