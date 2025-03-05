<template>
    <q-card :class="['my-subscriptions-container q-pa-md q-mb-md no-shadow' , $q.dark.isActive ? 'bg-dark' : 'bg-grey-1']">
      <div :class="{ 'text-secondary': !$q.dark.isActive, 'text-white': $q.dark.isActive }" class="text-h6  caption q-mb-md">
        Мои подписки
      </div>
  
      <div>
        <subscription-card
          v-for="(digest, index) in displayedSubscriptions"
          :key="index"
          :digest="digest"
        />
      </div>
  
      <div class="row justify-between q-mt-md">
        <div class="col-auto">
          <q-btn
            v-if="subscriptions.length > 2"
            :label="showAll ? 'Скрыть' : 'Показать ещё'"
            color="primary"
            no-caps
            text-color="secondary"
            @click="toggleShowAll"
          />
        </div>
        <div class="col-auto">
          <q-btn
            label="Создать дайджест"
            color="primary"
            no-caps
            text-color="secondary"
            @click="createDigest"
          />
        </div>
      </div>
    </q-card>
  </template>
  
  <script>
  import SubscriptionCard from "./SubscriptonCard.vue"; // Импортируем новый компонент
  
  export default {
    name: "MySubscriptions",
    components: {
      SubscriptionCard,
    },
    data() {
      return {
        subscriptions: [],
        showAll: false,
      };
    },
    computed: {
      displayedSubscriptions() {
        return this.showAll
          ? this.subscriptions
          : this.subscriptions.slice(0, 2);
      },
    },
    methods: {
      toggleShowAll() {
        this.showAll = !this.showAll;
      },
      createDigest() {
        this.$router.push("/subscriptions/create");
      },
      async fetchSubscriptions() {
        try {
          const response = await fetch(
            "https://943bf619978248a7b925ee61ceac158a.api.mockbin.io/",
            {
              method: "GET",
              headers: {
                "Content-Type": "application/json",
              },
            }
          );
          if (!response.ok) {
            throw new Error("Ошибка при получении подписок");
          }
          const data = await response.json();
          this.subscriptions = data.digests || [];
        } catch (err) {
          console.error(err);
        }
      },
    },
    mounted() {
      this.fetchSubscriptions();
    },
  };
  </script>
  
  <style scoped>
  .my-subscriptions-container {
  }
  </style>
  