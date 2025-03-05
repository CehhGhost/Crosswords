import axios from 'axios'


const api = axios.create({
  baseURL: 'https://example.com/api', // Поменять на backend URL
  withCredentials: true // чтобы куки (refresh-токен) передавались автоматически
})



api.interceptors.request.use(config => {
    const accessToken = localStorage.getItem('accessToken')
    if (accessToken) {
      config.headers.Authorization = `Bearer ${accessToken}`
    }
    return config
  }, error => {
    return Promise.reject(error)
  })
  
  let isRefreshing = false
  let refreshSubscribers = []
  
  function onRrefreshed(token) {
    refreshSubscribers.forEach(cb => cb(token))
    refreshSubscribers = []
  }
  
  function addRefreshSubscriber(cb) {
    refreshSubscribers.push(cb)
  }
  
  // Response interceptor
  api.interceptors.response.use(
    response => {
      // Если всё ок, просто возвращаем результат
      return response
    },
    async error => {
      const { config, response } = error
      const originalRequest = config
  
      // Проверяем, что у нас 401 от сервера
      if (response && response.status === 401) {
        // Проверяем, не пробовали ли мы уже рефрешить
        if (!isRefreshing) {
          isRefreshing = true
          try {
            // Вызываем запрос на обновление токена
            const refreshResponse = await axios.post(
              'https://example.com/api/refresh', 
              {}, 
              { withCredentials: true }
            )
            const newAccessToken = refreshResponse.data.accessToken
            if (newAccessToken) {
              // Сохраняем новый токен
              localStorage.setItem('accessToken', newAccessToken)
              // Оповещаем всех, кто ждет
              isRefreshing = false
              onRrefreshed(newAccessToken)
            }
            // Повторяем оригинальный запрос
            originalRequest.headers.Authorization = `Bearer ${newAccessToken}`
            return api(originalRequest)
          } catch (err) {
            isRefreshing = false
            // Если рефреш не удался, редиректим на логин или выбрасываем ошибку
            // Вы можете сделать logout() и router.push('/login') при необходимости
            return Promise.reject(err)
          }
        } else {
          // Если мы уже в процессе refresh
          return new Promise((resolve) => {
            addRefreshSubscriber(token => {
              // Обновляем заголовок и повторяем запрос
              originalRequest.headers.Authorization = 'Bearer ' + token
              resolve(api(originalRequest))
            })
          })
        }
      }
  
      // Если это другая ошибка, просто пробрасываем её дальше
      return Promise.reject(error)
    }
  )
// Экспортируем этот экземпляр, чтобы использовать по всему приложению
export default api
