<template>
  <q-select
    filled
    v-model="selected"
    :label="label"
    :options="filteredOptions"
    use-input
    input-debounce="0"
    @filter="filterItems"
    @popup-show="fetchFolders"
    multiple
    dense
    :loading="loading"
    :color="$q.dark.isActive ? 'primary' : 'accent'"
    clearable
    class="full-width q-my-sm"
  >
    <template v-slot:no-option>
      <q-item>
        <q-item-section class="text-grey">Нет результатов</q-item-section>
      </q-item>
    </template>
  </q-select>
</template>

<script>

export default {
  name: 'FolderSelector',
  props: {
    label: {
      type: String,
      default: 'Папка',
    },
    value: {
      type: [String, Object, null],
      default: null,
    },
  },
  data() {
    return {
      selected: this.value,
      options: [],
      filteredOptions: [],
      loading: false,
    }
  },
  watch: {
    selected(newVal) {
      this.$emit('input', newVal)
    },
    value(newVal) {
      this.selected = newVal
    },
  },
  methods: {
    fetchFolders() {
      this.loading = true
      // Запрос к бэкенду для получения папок.
      // Замените '/api/folders' на нужный URL, если требуется.
      fetch(
      'https://da60a9bd46b9478585c028e21b6b5e71.api.mockbin.io/', 
      { credentials: 'include' })
        .then((response) => {
          if (response.status === 401) {
            this.$router.replace('/login')
          }
          return response.json()
        })
        .then((data) => {
          // Преобразуем массив строк в формат { label, value }
          this.options = data.folders.map((folder) => ({
            label: folder,
            value: folder,
          }))
          this.filteredOptions = this.options
        })
        .catch((err) => {
          console.error('Ошибка при загрузке папок:', err)
          this.options = []
          this.filteredOptions = []
        })
        .finally(() => {
          this.loading = false
        })
    },
    filterItems(val, update) {
      update(() => {
        if (val === '') {
          this.filteredOptions = this.options
        } else {
          const needle = val.toLowerCase()
          this.filteredOptions = this.options.filter((option) =>
            option.label.toLowerCase().includes(needle),
          )
        }
      })
    },
  },
}
</script>
