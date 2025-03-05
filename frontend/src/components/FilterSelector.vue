<template>
  <q-select
    filled
    v-model="selected"
    :label="label"
    :options="filteredOptions"
    multiple
    use-input
    :dense="dense"
    input-debounce="0"
    @filter="filterItems"
    :color="$q.dark.isActive ? 'primary' : 'accent'"
    stack-label
    clearable
    class="full-width"
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
  props: {
    label: {
      type: String,
      required: true,
    },
    options: {
      type: Array,
      required: true,
    },
    value: {
      type: Array,
      default: () => [],
    },
    dense: {
      type: Boolean,
      default: false,
    },
  },
  data() {
    return {
      selected: this.value,
      filteredOptions: [...this.options],
    }
  },
  watch: {
    selected(newVal) {
      this.$emit('update:value', newVal)
    },
  },
  methods: {
    filterItems(val, update) {
      update(() => {
        this.filteredOptions = this.options.filter((option) =>
          option.label.toLowerCase().includes(val.toLowerCase()),
        )
      })
    },
  },
}
</script>
