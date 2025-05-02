<template>
  <q-select
    filled
    v-model="innerValue"
    :label="label"
    :options="filteredOptions"
    :multiple="multiple"
    use-input
    :dense="dense"
    input-debounce="0"
    @filter="filterItems"
    :color="$q.dark.isActive ? 'primary' : 'accent'"
    stack-label
    clearable
    class="full-width"
    :required="required && (multiple ? (innerValue?.length || 0) === 0 : !innerValue)"
  >
    <template v-slot:no-option>
      <q-item>
        <q-item-section class="text-grey">Нет результатов</q-item-section>
      </q-item>
    </template>
  </q-select>
</template>

<script setup>
import { ref, watch, computed } from 'vue'

// Пропы компонента
const props = defineProps({
  label: { type: String, required: true },
  options: { type: Array, required: true },
  modelValue: {
    type: [Array, String, Number, Object],
    default: () => []
  },
  multiple: { type: Boolean, default: false },
  dense: { type: Boolean, default: false },
  required: { type: Boolean, default: false }
})

// Эмитим событие обновления v-model
const emit = defineEmits(['update:modelValue'])

// Внутреннее состояние фильтрованных опций
const filteredOptions = ref([...props.options])

// Слежка за изменениями исходных опций
watch(
  () => props.options,
  (newOpts) => {
    filteredOptions.value = [...newOpts]
  }
)

// Доступ к v-model через computed
const innerValue = computed({
  get() {
    return props.modelValue
  },
  set(val) {
    emit('update:modelValue', val)
  }
})

// Функция фильтрации опций
function filterItems(val, update) {
  update(() => {
    filteredOptions.value = props.options.filter(option =>
      option.label.toLowerCase().includes(val.toLowerCase())
    )
  })
}
</script>

<style scoped>
.full-width {
  width: 100%;
}
</style>
