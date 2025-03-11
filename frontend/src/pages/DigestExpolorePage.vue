<template>
  <ServerResponseSpinner v-if="isLoading"/>
  <q-page v-if="!isLoading" class="flex flex-center">
    <div class="full-width q-pa-md page-body">
      <q-carousel
        v-if="featuredDigests.length"
        v-model="activeFeaturedSlide"
        swipeable
        infinite
        :autoplay="autoplay"
        @mouseenter="autoplay = false"
        @mouseleave="autoplay = true"
        transition-prev="slide-right"
        transition-next="slide-left"
        arrows
        control-type="flat"
        animated
        navigation
        control-color="primary"
        style="width: 100%; max-height: 80vh; overflow: hidden"
        class="q-mb-md"
        :class="{ lightBgClass: !$q.dark.isActive }"
      >
        <q-carousel-slide
          v-for="(digest, index) in featuredDigests.slice(0, 3)"
          :key="digest.id"
          :name="String(index)"
          style="cursor: pointer"
          @click="goToDigest(digest.id)"
        >
          <div class="row" style="height: 100%">
            <div
              class="col-12 col-md-6 text-white"
              :style="getLeftStyle(digest)"
              style="display: flex; flex-direction: column; padding: 1rem"
            >
              <div class="row justify-between">
                <div><b>ID:</b> {{ digest.id }}</div>
                <div>
                  <b>{{ digest.date }}</b>
                </div>
              </div>

              <div
                class="row justify-center"
                style="flex: 1; flex-direction: column; align-items: center"
              >
                <div class="text-h5 text-center q-mb-sm">{{ digest.title }}</div>

                <div
                  v-if="digest?.description"
                  class="description text-body3 text-italic text-center text-white"
                >
                  "{{ digest.description }}"
                </div>
              </div>
              <div class="row">
                <div><b>Источники:</b> {{ getLimitedSources(digest.sources) }}</div>
              </div>
            </div>

            <div class="col-12 col-md-6">
              <q-img
                :src="getDigestPic(digest)"
                style="height: 100%; width: 100%; object-fit: cover"
                loading="lazy"
              />
            </div>
          </div>
        </q-carousel-slide>
      </q-carousel>

      <my-subscriptions v-if="is_authed" />
      <locked-content
        description="Войдите в аккаунт, чтобы создавать собственные дайджесты или подписываться на существующие"
        v-else
      />
      <div
        :class="{ 'text-secondary': !$q.dark.isActive, 'text-white': $q.dark.isActive }"
        class="caption text-h4 text-center q-mt-xl q-mb-md"
      >
        Все дайджесты
      </div>

      <div class="row items-center q-mb-sm">
        <q-input
          v-model="search_body"
          filled
          label="Название дайджеста"
          class="col"
          :color="$q.dark.isActive ? 'primary' : 'accent'"
        >
          <template v-slot:append>
            <q-icon name="search" />
          </template>
        </q-input>

        <q-btn
          label="Найти"
          color="primary"
          no-caps
          text-color="dark"
          padding="md"
          unelevated
          :loading="isDigestLoading"
          class="col-auto search-btn"
          @click="fetchDigests"
        />
      </div>
      <!-- даты -->
      <div class="row q-col-gutter-md items-center">
        <!-- дата (с) -->
        <div class="col-6">
          <q-input
            v-model="date_from"
            filled
            dense
            label="Дата (с)"
            mask="##/##/####"
            :color="$q.dark.isActive ? 'primary' : 'accent'"
          >
            <template v-slot:append>
              <q-icon name="event" class="cursor-pointer">
                <q-popup-proxy cover transition-show="scale" transition-hide="scale">
                  <q-date
                    v-model="date_from"
                    title="Искать с"
                    subtitle="Выбор даты"
                    color="secondary"
                    mask="DD/MM/YYYY"
                  >
                    <div class="row items-center justify-end">
                      <q-btn v-close-popup label="Закрыть" color="secondary" flat />
                    </div>
                  </q-date>
                </q-popup-proxy>
              </q-icon>
            </template>
          </q-input>
        </div>

        <!-- дата (по) -->
        <div class="col-6">
          <q-input
            v-model="date_to"
            filled
            dense
            label="Дата (по)"
            mask="##/##/####"
            :color="$q.dark.isActive ? 'primary' : 'accent'"
          >
            <template v-slot:append>
              <q-icon name="event" class="cursor-pointer">
                <q-popup-proxy cover transition-show="scale" transition-hide="scale">
                  <q-date
                    v-model="date_to"
                    title="Искать по"
                    subtitle="Выбор даты"
                    color="secondary"
                    mask="DD/MM/YYYY"
                  >
                    <div class="row items-center justify-end">
                      <q-btn v-close-popup label="Закрыть" color="secondary" flat />
                    </div>
                  </q-date>
                </q-popup-proxy>
              </q-icon>
            </template>
          </q-input>
        </div>
      </div>

      <!-- Теги и Источники -->
      <div class="row q-col-gutter-md items-center">
        <!-- теги -->
        <div class="col-6">
          <filter-selector
            v-model="selected_tags"
            :options="availableTags"
            label="Тэги"
            dense
            clearable
            class="q-my-sm"
            @clear="clearSelection('tags')"
          />
        </div>

        <!-- источники -->
        <div class="col-6">
          <filter-selector
            v-model="selected_sources"
            :options="availableSources"
            label="Источники"
            dense
            clearable
            class="q-my-sm"
            @clear="clearSelection('sources')"
          />
        </div>
      </div>

      <!-- Чекбокс и кнопка -->
      <div class="row q-col-gutter-md items-center q-mb-md justify-between">
        <div class="col-auto">
          <q-checkbox
            v-if="is_authed"
            v-model="subscribeOnly"
            label="Только подписки"
            color="primary"
          />
        </div>
        <div class="col-auto">
          <q-btn
            label="Сбросить фильтры"
            color="secondary"
            no-caps
            text-color="white"
            @click="resetFilters"
            :loading="isDigestLoading"
          />
        </div>
      </div>

      <!-- Список дайджестов -->
      <div>
        <digest-card
          :is_authed="is_authed"
          v-for="digest in digests"
          :key="digest.id"
          :digest="digest"
        />
      </div>
    </div>
  </q-page>
</template>

<script>
import DigestCard from '../components/DigestCard.vue'
import { availableTags, availableSources } from '../data/lookups.js'
import MySubscriptions from '../components/MySubscriptions.vue'
import FilterSelector from 'src/components/FilterSelector.vue'
import LockedContent from 'src/components/LockedContent.vue'
import ServerResponseSpinner from 'src/components/ServerResponseSpinner.vue'

export default {
  name: 'NewsPage',
  components: {
    DigestCard,
    MySubscriptions,
    FilterSelector,
    LockedContent,
    ServerResponseSpinner,
  },
  data() {
    return {
      search_body: '',
      date_from: '',
      date_to: '',
      selected_tags: [],
      selected_sources: [],
      subscribeOnly: false,
      digests: [],
      featuredDigests: [],
      isDigestLoading: false,
      isLoading: true,
      activeFeaturedSlide: '0',
      is_authed: false,
      autoplay: true,

      // это из lookups.js
      availableTags,
      availableSources,
      colorPalette: [
        '#072449',
        '#092449',
        '#121858',
        '#151240',
        '#161E4C',
        '#1E151C',
        '#121C18',
        '#1C1E0D',
        '#201717',
        '#351510',
      ],

      // Чтобы при повторном рендере не генерировать другой цвет,
      // можно хранить цвета для уже встреченных дайджестов в объекте (кэш).
      digestColors: {},
    }
  },
  methods: {
    getLimitedSources(sources) {
      if (sources.length > 3) {
        return sources.slice(0, 3).join(', ') + ' ...'
      }
      return sources.join(', ')
    },
    goToDigest(id) {
      this.$router.push(`/digests/${id}`)
    },
    formatToISO(str) {
      if (!str) return null
      const [day, month, year] = str.split('/')
      if (!day || !month || !year) return null
      const utcDateMs = Date.UTC(+year, +month - 1, +day)
      const isoDateTime = new Date(utcDateMs).toISOString()
      return isoDateTime.split('T')[0]
    },
    // Берём цепочку тегов, ищем первый digest_pic
    getDigestPic(digest) {
      if (!digest.tags || digest.tags.length === 0) {
        return 'images/default.jpg'
      }
      // идём по тегам по порядку
      for (const tagValue of digest.tags) {
        // Находим соответствие в availableTags
        const foundTag = this.availableTags.find((t) => t.value === tagValue)
        // если нашли и у тега есть digest_pic -> используем его
        if (foundTag && foundTag.digest_pic) {
          return foundTag.digest_pic
        }
      }
      // если ни один тег не дал фото -> берём "default"
      return 'images/default.jpg'
    },

    // логика определения цвета (рандом из colorPalette)
    // При желании, можно брать "случайный цвет" прямо тут, но тогда
    // при каждом обновлении Vue будет менять цвет. Поэтому обычно кэшируем в digestColors.
    getLeftStyle(digest) {
      if (!this.digestColors[digest.id]) {
        const randomIndex = Math.floor(Math.random() * this.colorPalette.length)
        this.digestColors[digest.id] = this.colorPalette[randomIndex]
      }
      return {
        backgroundColor: this.digestColors[digest.id],
      }
    },
    async fetchDigests() {
      this.isDigestLoading = true

      try {
        const params = new URLSearchParams()

        if (this.search_body) {
          params.append('search_body', this.search_body)
        }

        // Даты
        if (this.date_from) {
          params.append('date_from', this.formatToISO(this.date_from))
        }
        if (this.date_to) {
          params.append('date_to', this.formatToISO(this.date_to))
        }

        // Теги
        if (this.selected_tags && this.selected_tags.length) {
          this.selected_tags.forEach((tag) => {
            params.append('tags', tag)
          })
        }

        if (this.selected_sources && this.selected_sources.length) {
          this.selected_sources.forEach((source) => {
            params.append('sources', source)
          })
        }
        params.append('subscribe_only', this.subscribeOnly ? 'true' : 'false')
        const response = await fetch(
          //`https://example.com/api/digests?${params.toString()}`,
          `https://38eb0762b63f400b81812fc5431695d1.api.mockbin.io/`,
          {
            method: 'GET',
            headers: {
              'Content-Type': 'application/json',
            },
          },
        )
        console.log(params.toString())
        if (!response.ok) {
          throw new Error('Ошибка при получении дайджестов')
        }

        const data = await response.json()
        // Предполагается, что формат ответа:
        // {
        //   "digests": [ ... ]
        // }
        this.digests = data.digests || []
        this.is_authed = data.is_authed
      } catch (error) {
        console.error(error)
      } finally {
        this.isDigestLoading = false
      }
    },
    async fetchFeaturedDigests() {
      try {
        const response = await fetch(`https://d0ef77d78d0747daa591ac2497df51ed.api.mockbin.io/`, {
          method: 'GET',
          headers: { 'Content-Type': 'application/json' },
        })

        if (!response.ok) {
          throw new Error('Ошибка при получении featured дайджестов')
        }
        const data = await response.json()
        console.log(data)
        // Ожидаем тот же формат { digests: [...] }
        this.featuredDigests = data.digests || []
      } catch (error) {
        console.error(error)
      }
    },
    resetFilters() {
      this.selected_sources = []
      this.selected_tags = []
      this.date_from = null
      this.date_to = null
      this.search_in_text = false
      this.search_body = ''
    },
  },
  async mounted() {
    try {
      await Promise.all([
        this.fetchFeaturedDigests(),
        this.fetchDigests()
      ]);
    } catch (error) {
      console.error("Ошибка при загрузке дайджестов:", error);
    } finally {
      this.isLoading = false;
    }
  },
}
</script>

<style scoped lang="scss">
.description {
  display: -webkit-box;
  -webkit-line-clamp: 10;
  line-clamp: 10;
  -webkit-box-orient: vertical;
  overflow: hidden;
  text-overflow: ellipsis;
  font-size: 0.875rem;
}

/* Для мобильных устройств */
@media (max-width: 600px) {
  .description {
    -webkit-line-clamp: 3;
    line-clamp: 3;
  }
}
.lightBgClass {
  background-color: $lightaccent;
}
</style>
