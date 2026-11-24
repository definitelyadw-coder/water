#!/usr/bin/env bash
# FaceFit: заливка проекта на GitHub + сборка APK + скачивание APK (Termux)
# Запуск из папки проекта:   bash push_build.sh [имя_репозитория]
set -e
cd "$(dirname "$0")"

step() { echo; echo "=== $1"; }

# ---------- 1. Инструменты ----------
step "1/7 Установка git и GitHub CLI"
if command -v pkg >/dev/null 2>&1; then
  pkg install -y git gh >/dev/null 2>&1 || pkg install -y git gh
fi
command -v git >/dev/null || { echo "git не установлен"; exit 1; }
command -v gh  >/dev/null || { echo "gh не установлен"; exit 1; }

# ---------- 2. Вход в GitHub ----------
step "2/7 Вход в GitHub"
if ! gh auth status >/dev/null 2>&1; then
  echo "Сейчас появится одноразовый код и ссылка github.com/login/device."
  echo "Открой ссылку в браузере, введи код и подтверди."
  gh auth login -h github.com -p https -w
fi
gh auth setup-git
LOGIN=$(gh api user -q .login)
echo "Аккаунт: $LOGIN"

# ---------- 3. Имя репозитория ----------
step "3/7 Репозиторий"
REPO="${1:-}"
if [ -z "$REPO" ]; then
  read -r -p "Имя репозитория [FaceFit]: " REPO
  REPO="${REPO:-FaceFit}"
fi
FULL="$LOGIN/$REPO"

# ---------- 4. Git-репозиторий локально ----------
step "4/7 Подготовка файлов"
# папка на общей памяти (/storage/emulated/0/...) принадлежит другому пользователю — разрешаем git её
git config --global --add safe.directory "$PWD"
git config --global --add safe.directory '*'
git config --global user.name  >/dev/null 2>&1 || git config --global user.name  "$LOGIN"
git config --global user.email >/dev/null 2>&1 || git config --global user.email "$LOGIN@users.noreply.github.com"

[ -d .git ] || git init -q
git config core.fileMode false   # на общей памяти нет прав доступа к файлам
git checkout -q -B main

if [ ! -f .gitignore ]; then
  printf 'node_modules/\nandroid/\n*.apk\n' > .gitignore
fi

git add -A
git diff --cached --quiet || git commit -q -m "FaceFit: update"

# ---------- 5. Создание/подключение репозитория и push ----------
step "5/7 Загрузка на GitHub"
if gh repo view "$FULL" >/dev/null 2>&1; then
  echo "Репозиторий $FULL уже есть — подключаю."
  git remote remove origin 2>/dev/null || true
  git remote add origin "https://github.com/$FULL.git"
  git fetch -q origin || true
  if git rev-parse -q --verify origin/main >/dev/null; then
    # сохраняем файлы, которые уже есть на GitHub (например capacitor.config),
    # а при конфликте берём локальные версии
    git merge origin/main --allow-unrelated-histories -X ours -q -m "Merge remote" || true
  fi
else
  # приватный, т.к. внутри лежит ci/debug.keystore
  gh repo create "$FULL" --private --source=. --remote=origin >/dev/null
  echo "Создан приватный репозиторий $FULL"
fi

# Capacitor нужен конфиг, иначе "npx cap add android" упадёт
if ! ls capacitor.config.* >/dev/null 2>&1; then
  echo '{"appId":"com.facefit.app","appName":"FaceFit","webDir":"www"}' > capacitor.config.json
  git add capacitor.config.json
  git commit -q -m "Add capacitor config"
  echo "Добавлен capacitor.config.json"
fi

git push -u origin main

# ---------- 6. Ожидание сборки ----------
step "6/7 Сборка APK на GitHub Actions"
SHA=$(git rev-parse HEAD)
RUN=""
for _ in $(seq 1 20); do
  RUN=$(gh run list -R "$FULL" --commit "$SHA" --limit 1 --json databaseId -q '.[0].databaseId' 2>/dev/null || true)
  [ -n "$RUN" ] && break
  sleep 3
done
if [ -z "$RUN" ]; then
  gh workflow run build.yml -R "$FULL"
  sleep 6
  RUN=$(gh run list -R "$FULL" --workflow build.yml --limit 1 --json databaseId -q '.[0].databaseId')
fi
echo "Запуск #$RUN (обычно 4–8 минут)…"

if ! gh run watch "$RUN" -R "$FULL" --interval 10 --exit-status >/dev/null; then
  echo
  echo "[ошибка] Сборка упала. Последние строки лога:"
  gh run view "$RUN" -R "$FULL" --log-failed | tail -40
  echo "Полный лог: https://github.com/$FULL/actions/runs/$RUN"
  exit 1
fi
echo "Сборка готова."

# ---------- 7. Скачивание APK ----------
step "7/7 Скачивание APK"
DEST="$HOME/storage/downloads"
if [ ! -d "$DEST" ]; then
  command -v termux-setup-storage >/dev/null && termux-setup-storage && sleep 5
fi
[ -d "$DEST" ] || DEST="$PWD"

TMP="$DEST/.facefit_dl"
rm -rf "$TMP"
gh run download "$RUN" -R "$FULL" -n FaceFit-apk -D "$TMP"
mv -f "$TMP/app-debug.apk" "$DEST/FaceFit.apk"
rm -rf "$TMP"

echo
echo "Готово! APK: $DEST/FaceFit.apk"
echo "Открой его из «Загрузок» и установи."
