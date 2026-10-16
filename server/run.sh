#!/usr/bin/env bash
# Запуск сервера Qwant (Ktor + sqlite3).
# Использование: ./run.sh   (переменные PORT, QWANT_DB, QWANT_ELECTROLYZERS см. README)
set -euo pipefail
cd "$(dirname "$0")"
exec ../gradlew run "$@"
