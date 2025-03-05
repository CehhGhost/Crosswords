<template>
  <div>
    <q-btn label="Открыть меню" @click="rightDrawerOpen = true" />

    <q-drawer v-model="rightDrawerOpen" side="left" overlay bordered>
      <q-list>
        <q-item clickable v-close-popup @click="onPrivacy">
          <q-item-section>
            <div class="row items-center justify-center">
              <q-icon name="lock" class="q-mr-sm" />
              <span>Приватность</span>
            </div>
          </q-item-section>
        </q-item>
        <q-item clickable v-close-popup @click="onExport" v-ripple>
          <q-item-section>
          <div class="row items-center justify-center">
              <q-icon name="download" class="q-mr-sm" />
              <span>Экспорт данных</span>
            </div>
          </q-item-section>
        </q-item>
      </q-list>
    </q-drawer>
  </div>
</template>

<script>
export default {
  data() {
    return {
      rightDrawerOpen: true,
    }
  },
  methods: {
    onPrivacy() {
      console.log('Кнопка "Приватность" нажата')
    },
    onExport() {
      console.log('Кнопка "Экспорт данных" нажата')
    },
  },
}
</script>

<style scoped>
</style>
