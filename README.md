# 🔔 Twitch Stream Alarm — Android App (Kotlin)

Приложение-будильник: сигналит когда начинается стрим на Twitch.
Проверяет статус через **неофициальный GraphQL API Twitch** без OAuth.

---

## 📁 Структура проекта

```
TwitchStreamAlarm/
├── build.gradle                          ← project-level gradle
├── settings.gradle
└── app/
    ├── build.gradle                      ← зависимости
    └── src/main/
        ├── AndroidManifest.xml
        ├── java/com/twitchalarm/
        │   ├── App.kt                    ← Application class, notification channels
        │   ├── api/
        │   │   └── TwitchApi.kt          ← GQL клиент (batch-запросы)
        │   ├── data/
        │   │   ├── Streamer.kt           ← Room Entity
        │   │   ├── StreamerDao.kt        ← Room DAO
        │   │   └── AppDatabase.kt        ← Room Database (singleton)
        │   ├── work/
        │   │   ├── StreamCheckWorker.kt  ← WorkManager worker (каждые 5 минут)
        │   │   └── BootReceiver.kt       ← перезапуск после reboot
        │   └── ui/
        │       ├── MainActivity.kt       ← главный экран
        │       ├── StreamerAdapter.kt    ← RecyclerView adapter
        │       └── AlarmActivity.kt      ← полноэкранный будильник
        └── res/
            ├── layout/
            │   ├── activity_main.xml
            │   ├── activity_alarm.xml
            │   └── item_streamer.xml
            ├── values/
            │   ├── colors.xml
            │   ├── strings.xml
            │   └── themes.xml
            └── drawable/
                ├── ic_twitch.xml
                ├── bg_live.xml
                └── bg_offline.xml
```

---

## 🚀 Быстрый старт в Android Studio

1. **Открой** Android Studio → File → New → Import Project
2. Выбери папку `TwitchStreamAlarm`
3. Дождись Gradle sync
4. Запусти на устройстве (API 26+, т.е. Android 8.0+)

> ⚠️ Эмулятор подойдёт для тестирования UI, но будильник лучше проверять на реальном устройстве.

---

## ⚙️ Как работает

### 1. Twitch GraphQL API
- **Endpoint:** `https://gql.twitch.tv/gql`
- **Client-ID:** `kimne78kx3ncx6brgo4mv6wki5h1ko` (публичный ID веб-клиента Twitch)
- **Авторизация:** не нужна — работает без токена
- **Запрос:** GraphQL с aliases для batch-проверки всех стримеров за 1 HTTP-запрос

```graphql
{
  u0: user(login: "xqc") {
    displayName
    stream { title viewersCount game { name } }
  }
  u1: user(login: "pokimane") {
    displayName
    stream { title viewersCount game { name } }
  }
}
```

### 2. Фоновая проверка
- **WorkManager** с `PeriodicWorkRequest` раз в **5 минут**
- Ограничение: Android не гарантирует точный интервал (может быть 5–15 минут)
- Запускается при старте приложения и восстанавливается после reboot через `BootReceiver`

### 3. Алгоритм определения «начала стрима»
```
prev.isLive == false  AND  current.isLive == true  →  СРАБОТАЛ БУДИЛЬНИК
```
Состояние сохраняется в Room БД после каждой проверки.

### 4. Будильник
При обнаружении нового стрима:
- Показывается **полноэкранное уведомление** (fullScreenIntent)
- Открывается `AlarmActivity` поверх lock screen
- Играет **системный звук будильника** (RingtoneManager.TYPE_ALARM)
- Включается **вибрация** с паттерном
- Кнопка **«Смотреть»** открывает приложение Twitch или сайт в браузере

---

## 📱 Функции UI

| Действие | Как |
|----------|-----|
| Добавить стримера | Ввести ник → нажать «+» |
| Проверить существование | Автоматически при добавлении через API |
| Включить/выключить будильник для стримера | Switch на карточке |
| Удалить стримера | Кнопка ✕ или свайп влево |
| Тест будильника | Долгое нажатие на карточку |
| Статус «в эфире» | Красная точка + кол-во зрителей + игра |

---

## 🛠 Технологии

| Компонент | Библиотека |
|-----------|-----------|
| База данных | Room 2.6 |
| Фоновые задачи | WorkManager 2.9 |
| HTTP / GraphQL | OkHttp 4.12 |
| JSON парсинг | org.json (встроенный) |
| UI | Material Components 1.11 |
| Асинхронность | Kotlin Coroutines |
| View Binding | включён в `build.gradle` |

---

## ⚡ Частые вопросы

**Q: Почему 5 минут, а не мгновенно?**
Android ограничивает фоновые процессы. WorkManager — самый надёжный способ. Для мгновенных уведомлений нужен собственный сервер с webhooks.

**Q: Что если Twitch изменит API?**
Неофициальный GQL API использует тот же Client-ID что и браузер. Если изменится — поменяй только `CLIENT_ID` и `GQL_URL` в `TwitchApi.kt`.

**Q: Батарейка не садится?**
WorkManager использует Doze Mode: если телефон не используется, Android может задержать проверку. Это нормальное поведение.

**Q: Приложение не видит стрим?**
Убедись, что у приложения есть разрешение на уведомления (Android 13+) и что переключатель на карточке включён.

---

## 🔧 Возможные улучшения

- [ ] Пуш-уведомления через Firebase (мгновенное срабатывание)
- [ ] Виджет на рабочий стол
- [ ] Настройка интервала проверки
- [ ] Выбор рингтона будильника
- [ ] Темная тема UI
- [ ] История стримов
- [ ] Несколько аккаунтов/профилей
