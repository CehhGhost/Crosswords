<template>
   <q-card :class="['q-mx-md q-my-sm' , { 'no-shadow': $q.dark.isActive }]">
      <q-card-section>
        <div class="row items-center justify-between">
          <div><b>ID:</b> {{ doc.id }}</div>
          <div>
            <span><b>Источник:</b> {{ doc.source }}</span>
            <span class="q-ml-sm"><b>Дата:</b> {{ doc.date }}</span>
            <FolderBookmark v-if="is_authed" :documentId="doc.id" />
          </div>
        </div>
        <div class="text-h6 q-mt-sm">{{ doc.title }}</div>
        <div>{{ doc.summary }}</div>
        <div v-if="doc.tags" class="q-mt-sm row wrap">
          <DocumentTags :tags="doc.tags" />
        </div>
        <q-btn
          class="q-mt-sm"
          label="Посмотреть"
          color="primary"
          no-caps
          text-color="secondary"
          @click="viewDocument"
        />
      </q-card-section>
    </q-card>
  </template>
  
  <script>
  import DocumentTags from "../components/DocumentTags.vue";
  import FolderBookmark from "./FolderBookmark.vue";
  export default {
    name: "DocumentCard",
    props: {
      doc: {
        type: Object,
        required: true,
      },
      is_authed: {
        type: Boolean,
        required: true,
      },
    },
    components: {
      DocumentTags,
      FolderBookmark
  },
    methods: {
      viewDocument() {
        this.$router.push(`/documents/${this.doc.id}`);
      },
    },
  };
  </script>
  
  <style scoped>
  </style>
  