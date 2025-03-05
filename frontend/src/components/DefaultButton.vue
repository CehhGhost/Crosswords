<template>
  <q-btn
    :label="label"
    :to="to"
    color="primary"
    no-caps
    text-color="dark"
    unelevated
    :ripple="{ center: true }"
    class="default-button"
  />
</template>

<script>
export default {
  name: 'DefaultButton',
  props: {
    label: {
      type: String,
      required: true
    },
    to: {
      type: [String, Object],
      required: true
    }
  }
};
</script>

<style scoped>
.default-button {
  font-weight: 400; /* Толще текст */
  padding: 12px 24px;
}
</style>
