# Вход через Google и облачное сохранение (разовая настройка)

1. **Firebase**: console.firebase.google.com → создай проект.
   - Build → Authentication → Sign-in method → включи **Google**.
   - Build → Firestore Database → создай базу, вкладка Rules:
     ```
     rules_version = '2';
     service cloud.firestore { match /databases/{db}/documents {
       match /users/{uid} { allow read, write: if request.auth != null && request.auth.uid == uid; }
     }}
     ```
   - Project settings → скопируй **Web API Key** и **Project ID**.
2. **Web client ID**: Authentication → Google → Web SDK configuration → скопируй *Web client ID*.
3. **Android client** (console.cloud.google.com → APIs & Services → Credentials → Create OAuth client ID → Android):
   - Package name: `com.facefit.app`
   - SHA-1: `0E:1A:F9:DB:2A:06:DB:EF:7F:D1:1C:55:29:75:30:41:0A:CF:18:0E` (ключ лежит в `ci/debug.keystore`, сборка всегда им подписывается)
   - Если экран согласия в режиме Testing — добавь свой Gmail в Test users.
4. В `www/index.html` заполни `const CFG={apiKey:"…",projectId:"…",webClientId:"…"}`.
5. Запушь — GitHub Actions соберёт APK.
