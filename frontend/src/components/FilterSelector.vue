<template>
  <q-select
    filled
    v-model="innerValue"
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
    :required="required && (innerValue?.length || 0) === 0"
  >
    <template v-slot:no-option>
      <q-item><q-item-section class="text-grey">Нет результатов</q-item-section></q-item>
    </template>
  </q-select>
</template>

<script>
export default {
  name: 'FilterSelector',
  props: {
    label:      { type: String,  required: true },
    options:    { type: Array,   required: true },
    modelValue: { type: Array,   default: () => [] },
    dense:      { type: Boolean, default: false },
    required:   { type: Boolean, default: false },
  },
  emits: ['update:modelValue'],
  data() {
    return {
      filteredOptions: [...this.options],
    }
  },
  computed: {
    innerValue: {
      get() {
        return this.modelValue
      },
      set(val) {
        this.$emit('update:modelValue', val)
      }
    }
  },
  watch: {
    options(newOpts) {
      this.filteredOptions = [...newOpts]
    }
  },
  methods: {
    filterItems(val, update) {
      update(() => {
        this.filteredOptions = this.options.filter(option =>
          option.label.toLowerCase().includes(val.toLowerCase())
        )
      })
    },
  },
}
</script>
