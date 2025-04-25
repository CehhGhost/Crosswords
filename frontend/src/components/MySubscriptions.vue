<template>
  <q-card
    :class="[
      'my-subscriptions-container q-pa-md q-mb-md no-shadow',
      $q.dark.isActive ? 'bg-dark' : 'bg-grey-1',
    ]"
  >
    <div
      :class="{ 'text-secondary': !$q.dark.isActive, 'text-white': $q.dark.isActive }"
      class="text-h6 caption q-mb-md"
    >
      Мои подписки
    </div>

    <template v-if="isLoading">
      <div class="q-pa-md">
        <q-spinner size="50px" color="primary" />
      </div>
    </template>

    <template v-else-if="subscriptions.length > 0">
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
            label="Заказать дайджест"
            color="primary"
            no-caps
            text-color="secondary"
            @click="createDigest"
          />
        </div>
      </div>
    </template>

    <template v-else>
      <div
        style="display: flex; flex-direction: column; align-items: center"
        class="flex flex-center flex-col q-pa-md"
      >
        <q-icon name="search" size="100px" color="grey-5" />
        <div class="q-mt-md text-h6 text-center">Похоже, подписок нет. Давайте это исправим!</div>
        <q-btn
          label="Заказать дайджест"
          color="primary"
          no-caps
          text-color="secondary"
          class="q-mt-md"
          @click="createDigest"
        />
      </div>
    </template>
  </q-card>
</template>

<script>
import { backendURL } from 'src/data/lookups'
import SubscriptionCard from './SubscriptionCard.vue'

export default {
  name: 'MySubscriptions',
  components: {
    SubscriptionCard,
  },
  data() {
    return {
      subscriptions: [],
      showAll: false,
      isLoading: true,
    }
  },
  computed: {
    displayedSubscriptions() {
      return this.showAll ? this.subscriptions : this.subscriptions.slice(0, 2)
    },
  },
  methods: {
    toggleShowAll() {
      this.showAll = !this.showAll
    },
    createDigest() {
      this.$router.push('/subscriptions/create')
    },
    async fetchSubscriptions() {
      try {
        const response = await fetch(
          //"https://943bf619978248a7b925ee61ceac158a.api.mockbin.io/",
          backendURL + `subscriptions`,
          {
            method: 'GET',
            headers: {
              'Content-Type': 'application/json',
            },
            credentials: 'include',
          },
        )
        console.log('Response:', response)
        if (!response.ok) {
          throw new Error('Ошибка при получении подписок')
        }
        const data = await response.json()
        this.subscriptions = data.digest_subscriptions || []
      } catch (err) {
        console.error(err)
      } finally {
        this.isLoading = false
      }
    },
  },
  mounted() {
    this.fetchSubscriptions()
  },
}
</script>

<style scoped></style>
