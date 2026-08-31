#!/bin/sh
# ==========================================================================
#  Jar'ni sirlar fayli bilan ishga tushiradi
# ==========================================================================
#  ⚠️ NEGA BU SKRIPT KERAK
#
#  `deploy/uzcasting.env` ni FAQAT systemd o'qiydi (`EnvironmentFile`).
#  Qo'lda `java -jar backend.jar` deb ishga tushirilsa, u faylni
#  umuman ko'rmaydi: Java faqat MUHIT o'zgaruvchilarini o'qiydi.
#
#  Natijada birinchi uchraydigan xato — `Could not resolve placeholder
#  'app.jwt.secret'`. U to'g'ri xato, lekin sababi «fayl to'ldirilmagan»
#  emas, «fayl o'qilmagan». Bu ikki boshqa narsa va farqi stack
#  trace'dan ko'rinmaydi.
#
#      sh deploy/run.sh
#      sh deploy/run.sh --server.port=9090     # qo'shimcha argumentlar
# ==========================================================================
set -e

DIR=$(cd "$(dirname "$0")" && pwd)
ROOT=$(cd "$DIR/.." && pwd)

ENV_FILE="$DIR/uzcasting.env"
JAR="$ROOT/backend/target/backend-0.0.1-SNAPSHOT.jar"

if [ ! -f "$ENV_FILE" ]; then
    echo "✗ Sirlar fayli yo'q: $ENV_FILE"
    echo
    echo "  cp deploy/uzcasting.env.example deploy/uzcasting.env"
    echo "  chmod 600 deploy/uzcasting.env"
    echo "  nano deploy/uzcasting.env"
    exit 1
fi

if [ ! -f "$JAR" ]; then
    echo "✗ Jar yo'q: $JAR"
    echo
    echo "  ./backend/mvnw -f backend/pom.xml clean package"
    exit 1
fi

# ⚠️ Avval to'liqlik, keyin ishga tushirish.
#
# Aks holda yetishmayotgan qiymat o'nlab qatorli stack trace bilan
# chiqardi va uning ichidan «S3_BUCKET bo'sh» degan xulosani chiqarish
# kerak bo'lardi.
sh "$DIR/check-env.sh" "$ENV_FILE"
echo

# `set -a` — fayldagi har bir qiymat EKSPORT qilinadi.
# Usiz ular faqat shu skriptning o'zgaruvchisi bo'lib qolardi va
# `java` ularni ko'rmasdi.
set -a
# shellcheck disable=SC1090
. "$ENV_FILE"
set +a

echo "→ Ishga tushmoqda: $(basename "$JAR")"
exec java -Xmx4g -jar "$JAR" "$@"
