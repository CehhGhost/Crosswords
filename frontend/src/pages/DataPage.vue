<template>
    <q-page class="q-pa-md page-body">
      <q-btn label="к другим настройкам" @click="toggleDrawer" class="q-mb-xl" outline />
      <div class="text-h4 caption q-mb-lg">Управление папками</div>
      <q-card>
        <q-card-section>
          <!-- Если список пуст, выводим сообщение -->
          <div v-if="folders.length === 0" class="text-center q-pa-md">
            У вас нет папок, создайте их на странице с документами
          </div>
          <!-- Если есть папки, выводим их список -->
          <div v-else>
            <div v-for="(folder, index) in folders" :key="folder.name" class="q-mb-md">
              <div class="row items-center">
                <!-- Отображение названия или поля редактирования -->
                <div class="col">
                  <div v-if="!folder.editing">
                    <span>{{ folder.name }}</span>
                    <div v-if="folder.error" class="text-negative text-caption">
                      {{ folder.error }}
                    </div>
                  </div>
                  <div v-else>
                    <q-input v-model="folder.newName" dense autofocus />
                    <div v-if="folder.error" class="text-negative text-caption">
                      {{ folder.error }}
                    </div>
                  </div>
                </div>
                <div class="col-auto">
                  <!-- Кнопка редактирования/сохранения -->
                  <q-btn round dense flat
                         v-if="!folder.editing"
                         icon="edit"
                         :color="$q.dark.isActive ? 'primary' : 'secondary'"
                         @click="enableEditing(index)">
                         <q-tooltip anchor="top middle" self="bottom middle" class="bg-primary text-secondary" transition-show="scale" transition-hide="scale">
                             Изменить название папки </q-tooltip>
                        </q-btn>
                         
                  <template v-else>
                    <q-btn round dense flat v-if="!folder.loading"
                           icon="check"
                           @click="saveFolder(index)" />
                    <q-spinner-dots v-else size="18px" color="primary" />
                  </template>
                  <!-- Скачать -->
                  <q-btn round dense flat icon="download" :color="$q.dark.isActive ? 'primary' : 'secondary'" @click="downloadFolder(folder.name)" > 
                    <q-tooltip anchor="top middle" self="bottom middle" class="bg-primary text-secondary" transition-show="scale" transition-hide="scale">
                        Выгрузить документы из папки в формате JSON </q-tooltip>
                  </q-btn>
                  <!-- Удалить -->
                  <q-btn round dense flat icon="delete" color="negative" @click="deleteFolder(index)" >
                    <q-tooltip anchor="top middle" self="bottom middle" class="bg-primary text-secondary" transition-show="scale" transition-hide="scale">
                        Убрать все документы из папки и удалить ее </q-tooltip>
                     </q-btn>
                </div>
              </div>
              <!-- Разделитель между папками -->
              <q-separator v-if="index < folders.length - 1" />
            </div>
            <!-- Toggle для включения аннотаций -->
            <div class="q-mt-lg">
              <q-toggle v-model="includeAnnotations" label="Включить аннотации в выгрузку" >
                <q-tooltip anchor="bottom middle" self="top middle" class="bg-primary text-secondary" transition-show="scale" transition-hide="scale">
                    Добавляет аннотации в выгрузку папок в формате JSON </q-tooltip>
                 </q-toggle>
            </div>
          </div>
        </q-card-section>
      </q-card>
      <!-- Диалог подтверждения удаления -->
      <ConfirmDialog v-model="deleteDialog"
                     title="Внимание!"
                     message="Вы уверены, что хотите удалить папку?"
                     checkbox-label="Я понимаю, что делаю"
                     confirm-label="ОК"
                     @confirm="confirmDelete" />
    </q-page>
  </template>
  
  <script setup>
  import { ref, onMounted } from 'vue'
  import useDrawer from 'src/composables/useDrawer'
  import ConfirmDialog from 'src/components/ConfirmDialog.vue'
  const { toggleDrawer } = useDrawer()
  
  /* Массив папок. Каждый объект содержит:
     - name: текущее название,
     - newName: значение для редактирования,
     - editing: режим редактирования,
     - error: сообщение об ошибке (если есть),
     - loading: флаг ожидания ответа от сервера при сохранении */
  const folders = ref([])
  
  // Флаг для toggle "Включить аннотации в выгрузку"
  const includeAnnotations = ref(false)
  
  // Загрузка списка папок с бекенда
  const fetchFolders = async () => {
    try {
      const response = await fetch('https://da60a9bd46b9478585c028e21b6b5e71.api.mockbin.io/')
      const data = await response.json()
      folders.value = data.folders.map(name => ({
        name,
        newName: name,
        editing: false,
        error: false,
        loading: false
      }))
    } catch (error) {
      console.error('Ошибка при загрузке папок', error)
    }
  }
  
  // Включить режим редактирования для папки
  const enableEditing = (index) => {
    folders.value[index].editing = true
    folders.value[index].error = false
  }
  
  // Сохранение нового названия папки с учётом возможности изменения только регистра
  const saveFolder = async (index) => {
    const folder = folders.value[index]
    const newName = folder.newName.trim()
  
    // Если имя не изменилось (учитывая регистр), просто выходим из режима редактирования
    if (newName === folder.name) {
      folder.editing = false
      return
    }
  
    // Проверка на дубликат среди остальных папок (без учёта регистра)
    const duplicate = folders.value.find(
      (f, idx) => idx !== index && f.name.toLowerCase() === newName.toLowerCase()
    )
    if (duplicate) {
      folder.error = "Папка с таким названием уже существует"
      folder.newName = folder.name
      return
    }
  
    // Если дошли сюда, значит обновление разрешено (даже если изменение только регистра)
    folder.loading = true
    try {
      const response = await fetch('https://8be73c6cb1434fa6a55467ff489377b5.api.mockbin.io/', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ oldName: folder.name, newName })
      })
      folder.loading = false
      if (!response.ok) {
        folder.error = "Что-то пошло не так при изменении имени папки"
        folder.newName = folder.name
        folder.editing = false
        return
      }
      // Обновляем название папки
      folder.name = newName
      folder.editing = false
      folder.error = false
    } catch (error) {
      console.error('Ошибка при обновлении папки', error)
      folder.loading = false
      folder.error = "Что-то пошло не так при изменении имени папки"
      folder.newName = folder.name
      folder.editing = false
    }
  }
  
  // Скачивание папки с передачей include_annotations
  const downloadFolder = async (folderName) => {
    try {
      const response = await fetch('/api/downloadFolder', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ folderName, include_annotations: includeAnnotations.value })
      })
      if (!response.ok) {
        throw new Error('Ошибка скачивания')
      }
      const blob = await response.blob()
      const url = window.URL.createObjectURL(blob)
      const link = document.createElement('a')
      const disposition = response.headers.get('content-disposition')
      let fileName = folderName + '.zip'
      if (disposition && disposition.indexOf('filename=') !== -1) {
        const filenameRegex = /filename[^;=\n]*=((['"]).*?\2|[^;\n]*)/
        const matches = filenameRegex.exec(disposition)
        if (matches && matches[1]) {
          fileName = matches[1].replace(/['"]/g, '')
        }
      }
      link.href = url
      link.download = fileName
      document.body.appendChild(link)
      link.click()
      document.body.removeChild(link)
      window.URL.revokeObjectURL(url)
    } catch (error) {
      console.error('Ошибка при скачивании папки', error)
    }
  }
  
  // Управление удалением папки с подтверждением через диалог
  const deleteDialog = ref(false)
  const folderToDelete = ref(null)
  
  const deleteFolder = async (index) => {
    folderToDelete.value = { index, name: folders.value[index].name }
    deleteDialog.value = true
  }
  
  const confirmDelete = async () => {
    if (folderToDelete.value !== null) {
      try {
        const folder = folders.value[folderToDelete.value.index]
        const response = await fetch('/api/deleteFolder', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ folderName: folder.name })
        })
        if (!response.ok) {
          throw new Error('Ошибка удаления')
        }
        folders.value.splice(folderToDelete.value.index, 1)
      } catch (error) {
        console.error('Ошибка при удалении папки', error)
      }
      folderToDelete.value = null
    }
  }
  
  onMounted(() => {
    fetchFolders()
  })
  </script>
  
  <style scoped>
  /* Добавьте свои стили при необходимости */
  </style>