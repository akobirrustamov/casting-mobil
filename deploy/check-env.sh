#!/bin/sh
# ==========================================================================
#  Sozlama to'liqmi — ISHGA TUSHIRISHDAN OLDIN
# ==========================================================================
#  ⚠️ Nega kerak: to'ldirilmagan qiymat ilovani turlicha yiqitadi.
#  `APP_JWT_SECRET` bo'lmasa u darhol, tushunarli xato bilan
#  to'xtaydi. `S3_BUCKET` bo'lmasa esa ilova KO'TARILADI va nosozlik
#  faqat birinchi fayl yuklashda, butunlay boshqa joyda chiqadi.
#
#  Bu skript ikkalasini ham oldindan aytadi.
#
#      sh deploy/check-env.sh deploy/uzcasting.env
# ==========================================================================
set -e

ENV_FILE="${1:-deploy/uzcasting.env}"

if [ ! -f "$ENV_FILE" ]; then
    echo "✗ Fayl topilmadi: $ENV_FILE"
    echo "  Namunadan nusxa oling:"
    echo "    cp deploy/uzcasting.env.example deploy/uzcasting.env"
    exit 1
fi

# shellcheck disable=SC1090
. "$ENV_FILE"

fail=0
warn=0

# ⚠️ Bularsiz ilova UMUMAN ko'tarilmaydi yoki bazaga ulanmaydi.
for key in APP_JWT_SECRET DB_PASSWORD; do
    eval "value=\$$key"
    if [ -z "$value" ]; then
        echo "✗ $key — to'ldirilmagan (ilova ishga tushmaydi)"
        fail=$((fail + 1))
    fi
done

# ⚠️ JWT kaliti qisqa bo'lsa HS256 uni rad etadi va ilova
# ishga tushishda emas, BIRINCHI kirishda yiqiladi.
if [ -n "$APP_JWT_SECRET" ] && [ ${#APP_JWT_SECRET} -lt 32 ]; then
    echo "✗ APP_JWT_SECRET juda qisqa (${#APP_JWT_SECRET} belgi, kamida 32 kerak)"
    echo "  openssl rand -hex 32"
    fail=$((fail + 1))
fi

# S3 yoqilgan bo'lsa — kalitlarsiz fayl yuklash birinchi urinishda
# yiqiladi, lekin ilova ko'tarilaveradi.
if [ "$STORAGE_PROVIDER" = "s3" ]; then
    for key in S3_BUCKET S3_ACCESS_KEY S3_SECRET_KEY; do
        eval "value=\$$key"
        if [ -z "$value" ]; then
            echo "✗ $key — S3 yoqilgan, lekin to'ldirilmagan (fayl yuklanmaydi)"
            fail=$((fail + 1))
        fi
    done
fi

# Bularsiz ilova ishlaydi, lekin bir qism funksiya jim turadi.
if [ -z "$ESKIZ_EMAIL" ] || [ -z "$ESKIZ_PASSWORD" ]; then
    echo "⚠ ESKIZ_* — bo'sh. SMS yuborilmaydi, OTP orqali kirish ishlamaydi."
    warn=$((warn + 1))
fi

if [ -z "$CDN_BASE_URL" ]; then
    echo "⚠ CDN_BASE_URL — bo'sh. Video eski yo'l bilan, server orqali beriladi."
    warn=$((warn + 1))
fi

if [ -n "$APP_GIPERSUPERADMIN_PASSWORD" ]; then
    echo "⚠ APP_GIPERSUPERADMIN_PASSWORD to'ldirilgan."
    echo "  Hisob yaratilgach bu qatorni O'CHIRING va qayta ishga tushiring."
    warn=$((warn + 1))
fi

echo
if [ "$fail" -gt 0 ]; then
    echo "✗ $fail ta majburiy qiymat yetishmayapti — ishga tushirmang."
    exit 1
fi

if [ "$warn" -gt 0 ]; then
    echo "✓ Majburiy qiymatlar joyida ($warn ta ogohlantirish bilan)."
else
    echo "✓ Sozlama to'liq."
fi
