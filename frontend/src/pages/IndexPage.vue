<template>
  <q-page class="q-pa-md flex flex-center">
    <div class="text-center q-mx-auto page-container">
      <h1 class="text-h3 text-md-h4 text-weight-bold q-mb-lg">Все важные СМИ - в одном месте</h1>
      <p class="text-body1">
        Умный корпус текстов СМИ автоматически собирает и обрабатывает новости из крупных
        издательств и собирает их в одном месте.
      </p>
      <ul class="q-mb-lg">
        <li class="text-body1 q-mb-sm">Ежедневно собираем новости с различных источников</li>
        <li class="text-body1 q-mb-sm">Анализируем и классифицируем их, присваиваем теги</li>
        <li class="text-body1 q-mb-sm">
          Отправляем результат в корпус, в мобильное приложение или на почту
        </li>
      </ul>

      <DefaultButton label="Посмотреть новости" :to="{ name: 'documents' }" class="q-mb-xl" />

      <h1 class="text-h5 text-md-h4 text-weight-bold q-mb-md">Наши источники</h1>
      <div class="horizontal-scroll q-mt-md row items-start q-gutter-sm">
        <SourceCard
          v-for="source in sources"
          :key="source.name"
          :name="source.name"
          :background="source.background"
          class="col-auto"
        />
      </div>
      <p class="q-mt-md">и многие другие...</p>
    </div>
  </q-page>
</template>

<script setup>
import { ref } from 'vue'
import DefaultButton from '../components/DefaultButton.vue'
import SourceCard from '../components/SourceCard.vue'

const sources = ref([
  { name: 'Издательство 1', background: '/src/assets/kommersant.jpg' },
  { name: 'Издательство 2', background: '/src/assets/CBRU.jpg' },
  { name: 'Издательство 3', background: '/src/assets/interfax.jpg' },
  { name: 'Издательство 5', background: '/src/assets/CBTJ.jpg' },
  { name: 'Издательство 6', background: '/src/assets/ria.jpg' },
  { name: 'Издательство 4', background: '/src/assets/CBUZ.jpg' }
])
</script>

<style scoped lang="scss">
.horizontal-scroll {
  display: flex;
  flex-wrap: nowrap;
  overflow-x: auto;
  gap: 10px;
  padding: 10px 0;
}

.horizontal-scroll::-webkit-scrollbar {
  height: 6px;
}

.horizontal-scroll::-webkit-scrollbar-thumb {
  background-color: var(--q-primary);
  border-radius: 4px;
}

.horizontal-scroll::-webkit-scrollbar-track {
  background-color: $scrollbar-light;
}

body.body--dark .horizontal-scroll::-webkit-scrollbar-track {
  background-color: $scrollbar-dark;
}

.source-card {
  flex: 0 0 auto;
  width: 150px;
  height: 90px;
  margin: 5px;
}

@media (max-width: 768px) {
  .source-card {
    width: 120px;
    height: 70px;
  }
}

.page-container {
  max-width: 650px;
  padding: 0 16px;
  width: 100%;
  box-sizing: border-box;
}
</style>
