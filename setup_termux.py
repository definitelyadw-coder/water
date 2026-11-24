#!/usr/bin/env python3
"""FaceFit: автонастройка Firebase + входа через Google из Termux.
Запуск из папки проекта:  python setup_termux.py
Делает: ставит firebase-tools, создаёт проект и Android-приложение, добавляет SHA-1,
создаёт Firestore + правила, вписывает ключи в www/index.html, (опц.) пушит в git.
Вручную остаётся один клик: включить провайдер Google в консоли (API для этого нет)."""
import json, os, re, secrets, shutil, subprocess, sys, time

PKG = "com.facefit.app"
SHA1 = "0E:1A:F9:DB:2A:06:DB:EF:7F:D1:1C:55:29:75:30:41:0A:CF:18:0E"
ROOT = os.path.dirname(os.path.abspath(__file__))
os.chdir(ROOT)
INDEX = "www/index.html"

def sh(cmd, check=True, capture=True):
    r = subprocess.run(cmd, shell=True, text=True, capture_output=capture)
    if check and r.returncode:
        print(f"\n[ошибка] {cmd}\n{(r.stdout or '')}{(r.stderr or '')}")
        sys.exit(1)
    return (r.stdout or "") + (r.stderr or "")

def step(t): print(f"\n=== {t}")

step("1/7 Установка инструментов")
if shutil.which("pkg"):
    sh("pkg install -y nodejs git", check=False, capture=False)
if not shutil.which("firebase"):
    sh("npm install -g firebase-tools", capture=False)

step("2/7 Вход в Google (Firebase CLI)")
print("Сейчас откроется ссылка: открой её в браузере, войди и вставь код сюда.")
if "No authorized accounts" in sh("firebase login:list", check=False):
    subprocess.run("firebase login --no-localhost", shell=True)

step("3/7 Проект Firebase")
state = ".fb_setup.json"
cfg = json.load(open(state)) if os.path.exists(state) else {}
if "pid" not in cfg:
    pid = "facefit-" + secrets.token_hex(3)
    sh(f'firebase projects:create {pid} --display-name FaceFit')
    cfg["pid"] = pid
    json.dump(cfg, open(state, "w"))
pid = cfg["pid"]
print("Проект:", pid)

step("4/7 Android-приложение и SHA-1")
def apps():
    out = sh(f"firebase apps:list ANDROID --project {pid} --json", check=False)
    try:
        r = json.loads(out[out.index("{"):out.rindex("}") + 1])["result"]
        return r if isinstance(r, list) else r.get("apps", [])
    except Exception:
        return []
def find_app():
    for _ in range(6):
        l = apps()
        for a in l:
            if PKG in (a.get("packageName"), a.get("namespace")):
                return a
        if l:
            return l[0]
        time.sleep(5)
app = find_app()
if not app:
    out = sh(f'firebase apps:create ANDROID FaceFit --package-name {PKG} --project {pid}')
    m = re.search(r"1:\d+:android:[0-9a-f]+", out)
    app = {"appId": m.group(0)} if m else find_app()
if not app:
    sys.exit("Не нашёл созданное приложение: проверь `firebase apps:list ANDROID --project " + pid + "`")
appid = app["appId"]
sh(f"firebase apps:android:sha:create {appid} {SHA1} --project {pid}", check=False)

def sdkcfg():
    out = sh(f"firebase apps:sdkconfig ANDROID {appid} --project {pid}", check=False)
    try:
        return json.loads(out[out.index("{"):out.rindex("}") + 1])
    except Exception:
        return {}
def web_client(j):
    for c in j.get("client", []):
        for o in c.get("oauth_client", []):
            if o.get("client_type") == 3:
                return o["client_id"]
    return ""

step("5/7 Включи вход через Google (один клик)")
print(f"Открой: https://console.firebase.google.com/project/{pid}/authentication/providers")
print("Нажми «Get started» (если есть) → Google → Enable → выбери почту поддержки → Save.")
input("Когда сделаешь — нажми Enter... ")

j = sdkcfg()
api_key = ""
try: api_key = j["client"][0]["api_key"][0]["current_key"]
except Exception: pass
web = web_client(j)
if not web:
    print("Web client ID не нашёлся автоматически.")
    print(f"Открой тот же экран → Google → Web SDK configuration и скопируй Web client ID.")
    web = input("Вставь Web client ID: ").strip()
if not api_key:
    api_key = input("Вставь Web API Key (Project settings → General): ").strip()

step("6/7 Firestore и правила")
open("firestore.rules", "w").write(
"""rules_version = '2';
service cloud.firestore { match /databases/{db}/documents {
  match /users/{uid} { allow read, write: if request.auth != null && request.auth.uid == uid; }
}}
""")
json.dump({"firestore": {"rules": "firestore.rules"}}, open("firebase.json", "w"))
out = sh(f'firebase firestore:databases:create "(default)" --location eur3 --project {pid}', check=False)
if "rror" in out and "already exists" not in out:
    print("Не удалось создать базу автоматически. Создай вручную (Test/Production не важно, правила зальём):")
    print(f"https://console.firebase.google.com/project/{pid}/firestore")
    input("Создай базу и нажми Enter... ")
sh(f"firebase deploy --only firestore:rules --project {pid}", capture=False)

step("7/7 Вписываю ключи в приложение")
h = open(INDEX, encoding="utf-8").read()
new = f'const CFG={{apiKey:"{api_key}",projectId:"{pid}",webClientId:"{web}"}}'
h2, n = re.subn(r'const CFG=\{[^}]*\}', new, h, count=1)
if not n: sys.exit("В index.html не найдена строка CFG")
open(INDEX, "w", encoding="utf-8").write(h2)
print("Готово:", new)

if os.path.isdir(".git") and input("Закоммитить и запушить (запустит сборку APK)? [y/N] ").lower() == "y":
    sh("git add -A && git commit -m 'Enable Google sync' && git push", capture=False)
print("\nОсталось: если OAuth в режиме Testing — добавь свою почту в Test users:")
print(f"https://console.cloud.google.com/auth/audience?project={pid}")
print("Потом скачай APK из GitHub Actions, поставь и нажми «Войти».")
