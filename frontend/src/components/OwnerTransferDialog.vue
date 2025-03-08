<template>
  <q-dialog v-model="show">
    <q-card>
      <q-card-section>
        <div class="text-h6 caption">Выберите нового владельца подписки</div>
        <div>
          Похоже, вы пытаетесь отписаться от дайджеста, который принадлежит вам.
          Выберите, кому передать права редактирования:
          <q-input
          v-model="filter"
          placeholder="Фильтр по почте"
          clearable
        />
        </div>
      </q-card-section>
      <!-- Список с фиксированной высотой для скроллинга -->
      <q-list
        bordered
        separator
        style="max-height: 300px; overflow-y: auto;"
      >
        <q-item
          v-for="(email, index) in filteredEmails"
          :key="index"
          clickable
          :active-class="$q.dark.isActive ? 'text-primary' : 'text-accent'"
          :active="selectedEmail === email"
          @click="selectEmail(email)"
        >
          <q-item-section>{{ email }}</q-item-section>
          <q-item-section side>
            <q-icon name="check" v-if="selectedEmail === email" />
          </q-item-section>
        </q-item>
      </q-list>

      <!-- Если при передаче прав произошла ошибка, показываем сообщение -->
      <div v-if="transferError" class="q-mt-sm text-negative text-caption">
        {{ transferError }}
      </div>

      <q-card-actions class="justify-between">
        <q-btn
          label="Отмена"
          color="secondary"
          no-caps
          @click="onCancel"
          :disable="transferLoading"
        />
        <q-btn
          label="Удтвердить владельца и отписаться"
          no-caps
          color="primary"
          text-color="secondary"
          :disable="!selectedEmail || transferLoading"
          @click="onConfirm"
        >
          <!-- Спиннер внутри кнопки, если идёт загрузка -->
          <q-spinner-dots
            v-if="transferLoading"
            size="18px"
            class="q-ml-sm"
          />
        </q-btn>
      </q-card-actions>
    </q-card>
  </q-dialog>
</template>

<script>
export default {
  name: "OwnerTransferDialog",
  props: {
    subscriberEmails: {
      type: Array,
      required: true,
    },
    modelValue: {
      type: Boolean,
      default: false,
    },
    transferLoading: {
      type: Boolean,
      default: false,
    },
    transferError: {
      type: String,
      default: "",
    },
  },
  data() {
    return {
      show: this.modelValue,
      selectedEmail: null,
      filter: '',
    };
  },
  computed: {
    filteredEmails() {
      if (!this.filter) {
        return this.subscriberEmails;
      }
      return this.subscriberEmails.filter(email =>
        email.toLowerCase().startsWith(this.filter.toLowerCase())
      );
    },
  },
  watch: {
    modelValue(val) {
      this.show = val;
    },
    show(val) {
      this.$emit("update:modelValue", val);
    },
  },
  methods: {
    selectEmail(email) {
      this.selectedEmail = email;
    },
    onCancel() {
      this.selectedEmail = null;
      this.$emit("cancel");
      this.show = false;
    },
    onConfirm() {
      if (this.selectedEmail) {
        this.$emit("confirm", this.selectedEmail);
      }
    },
  },
};
</script>
