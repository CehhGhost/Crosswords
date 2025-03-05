<template>
  <div class="search-section q-pa-md no-shadow" bg-color="primary" >
    <div class="row items-center">
      <q-select
        v-model="searchMode"
        :options="searchModes"
        option-value="value"
        option-label="label"
        emit-value
        map-options
        filled
        label="Режим"
        label-color="secondary"
        class="col-auto"
        bg-color="primary"
        :color="$q.dark.isActive ? 'primary' : 'accent'"
      />
      <q-input
        v-model="searchBody"
        filled
        label="Строка поиска"
        class="col"
        :color="$q.dark.isActive ? 'primary' : 'accent'"
      >
        <template v-slot:append>
          <q-icon name="search" />
        </template>
      </q-input>

      <q-btn
        label="Найти"
        color="primary"
        no-caps
        text-color="dark"
        padding="md"
        unelevated
        class="col-auto search-btn"
        @click="emitSearch"
      />
    </div>

    <div v-if="searchMode === 'exact'" class="q-my-sm">
      <q-checkbox v-model="searchInText" label="Искать в тексте (иначе только по названию)" />
    </div>

    <div v-if="showSources || showTags" class="row q-col-gutter-md">
      <div v-if="showSources" class="col">
        <filter-selector
          v-model="selectedSources"
          :options="availableSources"
          label="Источники"
          dense
          clearable
          class="q-my-sm"
          @clear="clearSelection('sources')"
        />
      </div>
      <div v-if="showTags" class="col">
        <filter-selector
          v-model="selectedTags"
          :options="availableTags"
          label="Тэги"
          dense
          clearable
          class="q-my-sm"
          @clear="clearSelection('tags')"
        />
      </div>
    </div>

    <div v-if="showDateRange" class="row q-col-gutter-md items-center">
      <q-input
        v-model="dateFrom"
        filled
        dense
        label="Дата (с)"
        mask="##/##/####"
        class="col"
        :color="$q.dark.isActive ? 'primary' : 'accent'"
      >
        <template v-slot:append>
          <q-icon name="event" class="cursor-pointer">
            <q-popup-proxy cover transition-show="scale" transition-hide="scale">
              <q-date
                v-model="dateFrom"
                title="Искать с"
                subtitle="Выбор даты"
                color="secondary"
                mask="DD/MM/YYYY"
              >
                <div class="row items-center justify-end">
                  <q-btn v-close-popup label="Закрыть" color="secondary" flat />
                </div>
              </q-date>
            </q-popup-proxy>
          </q-icon>
        </template>
      </q-input>

      <q-input
        v-model="dateTo"
        filled
        dense
        label="Дата (по)"
        mask="##/##/####"
        class="col"
        :color="$q.dark.isActive ? 'primary' : 'accent'"
      >
        <template v-slot:append>
          <q-icon name="event" class="cursor-pointer">
            <q-popup-proxy cover transition-show="scale" transition-hide="scale">
              <q-date
                v-model="dateTo"
                title="Искать по"
                subtitle="Выбор даты"
                color="secondary"
                mask="DD/MM/YYYY"
              >
                <div class="row items-center justify-end">
                  <q-btn v-close-popup label="Закрыть" color="secondary" flat />
                </div>
              </q-date>
            </q-popup-proxy>
          </q-icon>
        </template>
      </q-input>
    </div>

    <div v-if="searchMode === 'semantic' || searchMode === 'exact'" class="q-mt-sm">
      <q-btn label="Сбросить фильтры" color="secondary" no-caps @click="resetFilters" />
    </div>
  </div>
</template>

<script>
import { availableSources, availableTags } from 'src/data/lookups.js'
import FilterSelector from './FilterSelector.vue'
export default {
  name: 'SearchSection',
  components: {
    FilterSelector,
  },
  data() {
    return {
      searchModes: [
        { label: 'По смыслу', value: 'semantic' },
        { label: 'По номеру', value: 'id' },
        { label: 'Точный', value: 'exact' },
      ],
      searchMode: 'semantic',
      searchBody: '',
      searchInText: false,
      selectedSources: [],
      selectedTags: [],
      dateFrom: null,
      dateTo: null,
      availableSources,
      availableTags,
    }
  },
  computed: {
    showSources() {
      return this.searchMode === 'semantic' || this.searchMode === 'exact'
    },
    showTags() {
      return this.searchMode === 'semantic' || this.searchMode === 'exact'
    },
    showDateRange() {
      return this.searchMode === 'semantic' || this.searchMode === 'exact'
    },
  },
  methods: {
    clearSelection(field) {
      if (field === 'sources') {
        this.selectedSources = []
      } else if (field === 'tags') {
        this.selectedTags = []
      }
    },
    resetFilters() {
      this.selectedSources = []
      this.selectedTags = []
      this.dateFrom = null
      this.dateTo = null
      this.searchInText = false
      this.searchBody = ''
    },
    formatToISO(str) {
      if (!str) return null
      const [day, month, year] = str.split('/')
      if (!day || !month || !year) return null
      const utcDateMs = Date.UTC(+year, +month - 1, +day)
      const isoDateTime = new Date(utcDateMs).toISOString()
      const isoDateOnly = isoDateTime.split('T')[0]

      return isoDateOnly
    },
    emitSearch() {
      const payload = {
        searchMode: this.searchMode,
        searchBody: this.searchBody,
      }

      if (this.searchMode === 'semantic' || this.searchMode === 'exact') {
        payload.sources = this.selectedSources
        payload.tags = this.selectedTags
        payload.dateFrom = this.formatToISO(this.dateFrom)
        payload.dateTo = this.formatToISO(this.dateTo)
      }

      if (this.searchMode === 'exact') {
        payload.searchInText = this.searchInText
      }

      this.$emit('search', payload)
    },
  },
}
</script>

<style scoped lang="scss"></style>
