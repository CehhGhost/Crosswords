<template>
  <div class="page-body">
    <q-page v-if="!isLoading" class="q-pa-md">
      <BackButton to="/digests"/>
      <q-card :class="['q-px-md q-py-sm', { 'no-shadow': $q.dark.isActive }]">
        <div class="row items-center justify-between">
          <div class="text-caption row items-center text-left">
            <q-icon name="calendar_today" class="q-mr-xs" />
            {{ DigestData?.date }}
            <q-icon :name="fasCrown" class="q-ml-sm q-mr-xs" />
            {{ DigestData?.owner }}
            <q-icon name="star" color="primary" class="q-ml-sm q-mr-xs" />
            {{ DigestData.average_rating != -1 ? DigestData.average_rating : "Нет оценок" }}
          </div>
        </div>
        <div class="text-h4 q-my-sm">
          {{ DigestData?.title }}
          <q-btn
            v-if="!subscribed"
            label="Подписаться"
            no-caps
            icon-right="notifications"
            color="primary"
            text-color="secondary"
            class="q-ml-sm q-mt-sm"
            @click="onSubscribeClick"
          />

          <q-btn-dropdown
            v-else
            outline
            @show="onDropdownShow"
            no-caps
            :color="$q.dark.isActive ? 'primary' : 'secondary'"
            class="q-ml-sm q-mt-sm"
            label="Управлять подпиской"
            dropdown-icon="settings"
            v-model="dropdown"
          >
            <q-list>
              <q-item clickable @click="toggleCheckbox('sendToMail')">
                <q-item-section>
                  <q-checkbox
                    v-model="sendToMail"
                    class="custom-checkbox"
                    label="На почту"
                    @update:model-value="updateSubscription"
                    @click.stop
                  />
                </q-item-section>
                <q-item-section avatar>
                  <q-icon name="email" :color="$q.dark.isActive ? 'primary' : 'secondary'" />
                </q-item-section>
              </q-item>

              <q-item clickable @click="toggleCheckbox('mobileNotifications')">
                <q-item-section>
                  <q-checkbox
                    v-model="mobileNotifications"
                    keep-color
                    label="Мобильные уведомления"
                    @update:model-value="updateSubscription"
                    @click.stop
                  />
                </q-item-section>
                <q-item-section avatar>
                  <q-icon name="phone_iphone" :color="$q.dark.isActive ? 'primary' : 'secondary'" />
                </q-item-section>
              </q-item>
              <q-separator />
              <q-item clickable @click="onUnsubscribeClick">
                <q-item-section> Отписаться </q-item-section>
              </q-item>
            </q-list>
          </q-btn-dropdown>
        </div>

        <div>
          <text-body1 :class="['text-italic',
              { 'text-yellow': $q.dark.isActive, 'text-accent': !$q.dark.isActive },
            ]"
            >"{{ DigestData?.description }}"</text-body1
          >
        </div>

        <div class="q-my-sm">
          <div class="q-mb-xs">Оцените качество этого дайджетса: 
            <q-rating
            v-model="userRating"
            max="5"
            color="primary"
            icon="star"
            size="sm"
            no-reset
            @update:model-value="onRatingChange"
          /></div>
        </div>

        <div class="q-my-md">
          {{ DigestData?.text }}
        </div>
        <q-separator />
        <div class="q-mt-sm">
          <DocumentTags :tags="DigestData?.tags" />
        </div>
        <q-expansion-item
          v-if="DigestData?.urls && DigestData.urls.length"
          :label="`Показать источники (${DigestData.urls.length})`"
          class="q-mt-xs"
        >
          <div v-for="(url, index) in DigestData.urls" :key="index">
            <a :href="url" target="_blank" :class="['q-mb-xs', { 'text-primary': $q.dark.isActive, 'text-accent': !$q.dark.isActive }]">{{ url }} </a>
          </div>
        </q-expansion-item>
      </q-card>

      <div class="row items-start wrap justify-between q-gutter-sm q-mt-md">
        <q-btn
          label="PDF версия"
          color="secondary"
          size="md"
          icon-right="download"
          no-caps
          @click="downloadPdf"
        />

        <div class="row items-start wrap col-auto">
          <q-btn
            v-if="showAdmin"
            label="Редактировать"
            color="primary"
            size="md"
            class="q-mr-md"
            no-caps
            icon-right="edit"
            text-color="secondary"
            @click="showEditDialog = true"
          />
          <q-btn
            v-if="showAdmin"
            label="Удалить"
            color="secondary"
            size="md"
            class="q-mr-md"
            no-caps
            icon-right="delete"
            @click="showDeleteDialog = true"
          />
        </div>
      </div>

      <ConfirmDialog
        v-model="showEditDialog"
        title="Внимание!"
        message="Редактирование документа уберет аннотации всех пользователей! Редактирование текста документа крайне нежелательно и должно применяться только в экстренных случаях. Документы, подвергшиеся редактированию, будут помечены соответствующим образом."
        confirmLabel="Редактировать"
        checkboxLabel="Я понимаю, что я делаю"
        @confirm="goToEditPage"
        @cancel="onEditCancel"
      />

      <ConfirmDialog
        v-model="showDeleteDialog"
        title="Внимание!"
        message="Удаление документа необратимо."
        confirmLabel="Удалить"
        checkboxLabel="Я понимаю, что я делаю"
        @confirm="onDeleteConfirm"
        @cancel="onDeleteCancel"
      />
    </q-page>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'

import { useRoute, useRouter } from 'vue-router'
import DocumentTags from '../components/DocumentTags.vue'
import ConfirmDialog from '../components/ConfirmDialog.vue'
import { fasCrown } from '@quasar/extras/fontawesome-v6'
import BackButton from 'src/components/BackButton.vue'

const route = useRoute()
const router = useRouter()

const DigestData = ref(null)
const userRating = ref(0)
const subscribed = ref(false)
const sendToMail = ref(false)
const mobileNotifications = ref(false)
const dropdown = ref(false)
const showEditDialog = ref(false)
const showDeleteDialog = ref(false)
const showAdmin = ref(false)
const backup = ref({})
const isLoading = ref(true)

onMounted(async () => {
  // const id = route.params.id
  try {
    const response = await fetch(`https://bcad5d0cfef44db6bb7d2d37aa447e3f.api.mockbin.io/`)
    if (!response.ok) {
      if (response.status === 404) {
        router.replace('/404')
      } else {
        console.error('Ошибка HTTP:', response.status)
      }
      return
    }

    DigestData.value = await response.json()
    if (DigestData.value && DigestData.value.subscribe_options) {
      subscribed.value = DigestData.value.subscribe_options.subscribed
      sendToMail.value = DigestData.value.subscribe_options.send_to_mail
      mobileNotifications.value = DigestData.value.subscribe_options.mobile_notifications
    }
  } catch (error) {
    console.error('Ошибка при загрузке документа:', error)
  } finally {
    isLoading.value = false 
  }
})

async function onRatingChange(newRating) {
  try {
    const id = route.params.id
    const postResponse = await fetch(`http://localhost:3000/digests/${id}/rating`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({ rating: newRating }),
    })
    if (!postResponse.ok) {
      console.error('Ошибка при отправке рейтинга:', postResponse.status)
    } else {
      DigestData.value.rating_classification = newRating
    }
  } catch (err) {
    console.error('Ошибка при отправке рейтинга:', err)
  }
}

function goToEditPage() {
  const currentId = route.params.id
  router.push(`/documents/${currentId}/edit`)
}
function onEditCancel() {
  console.log('Редактирование отменено')
}

function onDropdownShow() {
  backup.value = {
    sendToMail: sendToMail.value,
    mobileNotifications: mobileNotifications.value,
  }
}

function toggleCheckbox(type) {
  if (type === 'sendToMail') {
    sendToMail.value = !sendToMail.value
  } else if (type === 'mobileNotifications') {
    mobileNotifications.value = !mobileNotifications.value
  }
  updateSubscription()
}

async function onSubscribeClick() {
  subscribed.value = true
  sendToMail.value = true
  mobileNotifications.value = true
  await sendSubscriptionUpdate()
}

async function onUnsubscribeClick() {
  subscribed.value = false
  sendToMail.value = false
  mobileNotifications.value = false
  dropdown.value = false
  await sendSubscriptionUpdate()
}

async function updateSubscription() {
  await sendSubscriptionUpdate()
}

async function sendSubscriptionUpdate() {
  const payload = {
    subscribed: subscribed.value,
    send_to_mail: sendToMail.value,
    mobile_notifications: mobileNotifications.value,
  }

  try {
    const response = await fetch(
      'https://4ec3051a148c46c8a8aa6dcfef35cdf8.api.mockbin.io/',
      {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload),
      }
    )
    console.log(JSON.stringify(payload))

    if (!response.ok) {
      console.error('Ошибка при изменении настроек подписки')
    }
  } catch (error) {
    console.error('Ошибка запроса:', error)
  }
}

function downloadPdf() {
  console.log('Скачать PDF для документа', DigestData.value?.id)
}

async function onDeleteConfirm() {
  const currentId = route.params.id
  try {
    const response = await fetch(`http://localhost:3000/documents/${currentId}`, {
      method: 'DELETE',
    })
    if (!response.ok) {
      console.error('Ошибка при удалении документа:', response.status)
    } else {
      console.log('Документ удалён')
      router.push('/documents')
    }
  } catch (error) {
    console.error('Ошибка при удалении:', error)
  }
}

function onDeleteCancel() {
  console.log('Удаление отменено')
}
</script>

<style scoped lang="scss">
.q-expansion-item {
  background-color: #f7f7f7 !important;

  .q-dark & {
    background-color: #2b2b2b !important;
  }
}
</style>
