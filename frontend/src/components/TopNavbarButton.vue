<template>
    <q-btn
      flat
      no-caps
      :label="label"
      :to="to"
      :ripple="ripple"
      class="q-mr-sm flat-button-text"
    />
  </template>
  
  <script>
  export default {
    name: 'TopNavbarButton',
    props: {
      label: {
        type: String,
        required: true,
      },
      to: {
        type: [String, Object],
        required: true,
      },
      ripple: {
        type: [Boolean, Object],
        default: () => ({ center: true }),
      },
    },
  };
  </script>
  
  <style scoped>

  </style>
  