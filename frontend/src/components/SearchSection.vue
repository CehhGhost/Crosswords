<template>
  <div class="search-section q-pa-md no-shadow" bg-color="primary" >
    <div class="row items-center">
      <q-select
        v-model="search_mode"
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
        v-model="search_body"
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

    <div v-if="search_mode === 'exact'" class="q-my-sm">
      <q-checkbox v-model="search_in_text" label="Искать в тексте (иначе только по названию)" />
    </div>

    <div v-if="showSources || showTags" class="row q-col-gutter-md">
      <div v-if="showSources" class="col">
        <filter-selector
          v-model="selected_sources"
          :options="availableSources"
          label="Источники"
          dense
          clearable
          :multiple=true
          class="q-my-sm"
          @clear="clearSelection('sources')"
        />
      </div>
      <div v-if="showTags" class="col">
        <filter-selector
          v-model="selected_tags"
          :options="availableTags"
          label="Тэги"
          dense
          :multiple=true
          clearable
          class="q-my-sm"
          @clear="clearSelection('tags')"
        />
      </div>
    </div>

    <div v-if="showDateRange" class="row q-col-gutter-md items-center">
      <q-input
        v-model="date_from"
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
                v-model="date_from"
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
        v-model="date_to"
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
                v-model="date_to"
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
    <div v-if="showFolder" class="row q-col-gutter-md">
      <div class="col">
        <folder-selector v-model="selected_folder" label="Папка" />
      </div>
    </div>

    <div v-if="search_mode === 'semantic' || search_mode === 'exact'" class="q-mt-sm">
      <q-btn label="Сбросить фильтры" color="secondary" no-caps @click="resetFilters" />
    </div>
  </div>
</template>

<script>
import { availableSources, availableTags } from 'src/data/lookups.js'
import FilterSelector from './FilterSelector.vue'
import FolderSelector from './FolderSelector.vue'

export default {
  name: 'SearchSection',
  components: {
    FilterSelector,
    FolderSelector,
  },
  props: {
    is_authed: {
      type: Boolean,
      default: false
    }
  },
  data() {
    return {
      searchModes: [
        { label: 'По смыслу', value: 'semantic' },
        { label: 'По номеру', value: 'id' },
        { label: 'Точный', value: 'exact' },
      ],
      search_mode: 'semantic',
      search_body: '',
      search_in_text: false,
      selected_sources: [],
      selected_tags: [],
      date_from: null,
      date_to: null,
      availableSources,
      availableTags,
      selected_folder: null,
    }
  },
  computed: {
    showSources() {
      return this.search_mode === 'semantic' || this.search_mode === 'exact'
    },
    showTags() {
      return this.search_mode === 'semantic' || this.search_mode === 'exact'
    },
    showDateRange() {
      return this.search_mode === 'semantic' || this.search_mode === 'exact'
    },
    showFolder() {
      return (this.search_mode === 'semantic' || this.search_mode === 'exact') && this.is_authed
    },
  },
  methods: {
    clearSelection(field) {
      if (field === 'sources') {
        this.selected_sources = []
      } else if (field === 'tags') {
        this.selected_tags = []
      }
    },
    resetFilters() {
      this.selected_sources = []
      this.selected_tags = []
      this.date_from = null
      this.date_to = null
      this.search_in_text = false
      this.search_body = ''
      this.selected_folder = null
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
    search_mode: this.search_mode,
    search_body: this.search_body,
  }

  if (this.search_mode === 'semantic' || this.search_mode === 'exact') {
    payload.sources = this.selected_sources.map(source => source.value)
    payload.tags = this.selected_tags.map(tag => tag.value)
    payload.date_from = this.formatToISO(this.date_from)
    payload.date_to = this.formatToISO(this.date_to)
    if (this.selected_folder) {
      payload.folders = this.selected_folder.map(folder => folder.value)
    }
  }

  if (this.search_mode === 'exact') {
    payload.search_in_text = this.search_in_text
  }

  this.$emit('search', payload)
},

  },
}
</script>

<style scoped lang="scss"></style>
