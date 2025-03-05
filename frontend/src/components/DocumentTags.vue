<template>
    <div v-if="tags && tags.length" class="q-mt-sm row wrap">
      <q-chip
        v-for="(tag, idx) in tags"
        :key="idx"
        class="q-mr-sm q-mb-sm"
        color="primary"
        text-color="secondary"
        :ripple="false"
      >
        {{ tag }}
      </q-chip>
    </div>
  </template>
  
  <script>
  export default {
    name: "DocumentTags",
    props: {
      tags: {
        type: Array,
        required: true,
      },
    },
  };
  </script>
  