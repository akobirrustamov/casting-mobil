#!/bin/sh
# ==========================================================================
#  Sozlama to'ldirilganmi — ISHGA TUSHIRISHDAN OLDIN
# ==========================================================================
#  ⚠️ Nega kerak: to'ldirilmagan qiymat ilovani TURLICHA yiqitadi va
#  ikkinchisi yomonroq.
#
#    app.jwt.secret yo'q     → darhol, tushunarli xato bilan to'xtaydi
#    s3.bucket to'ldirilmagan→ ilova KO'TARILADI va nosozlik faqat
#                              birinchi fayl yuklashda, butunlay
#                              boshqa joyda chiqadi
#
#      sh deploy/check-config.sh deploy/application.properties
# ==========================================================================
set -e

FILE="${1:-deploy/application.properties}"

if [ ! -f "$FILE" ]; then
    echo "✗ Fayl topilmadi: $FILE"
    exit 1
fi

fail=0
warn=0

# ⚠️ To'ldirilmagan joylar shu belgi bilan qoldirilgan.
#
# Izohga olingan qatorlar HISOBGA OLINMAYDI: 3-qismdagi admin paroli
# ataylab izohda turadi va uni «to'ldirilmagan» deb ko'rsatish yolg'on
# ogohlantirish bo'lardi. Yolg'on ogohlantirish esa tekshiruvni
# befoyda qiladi — odam uni o'qimay o'tib ketishni o'rganadi.
missing=$(grep -n "BU_YERGA" "$FILE" | grep -v ":[[:space:]]*#" || true)
if [ -n "$missing" ]; then
    echo "✗ To'ldirilmagan joylar:"
    echo "$missing" | sed 's/^/    /'
    fail=$((fail + 1))
fi

# Kalitni faylning o'zidan o'qiymiz.
secret=$(grep '^app.jwt.secret=' "$FILE" | cut -d= -f2- | tr -d '\r\n' || true)
if [ -z "$secret" ]; then
    echo "✗ app.jwt.secret bo'sh — ilova ishga tushmaydi"
    fail=$((fail + 1))
elif [ ${#secret} -lt 32 ]; then
    # ⚠️ Qisqa kalit ishga tushirishda emas, birinchi kirishda
    # yiqilardi. Endi ilova uni o'zi ham ushlaydi, lekin bu yerda
    # aytilgani tezroq.
    echo "✗ app.jwt.secret juda qisqa (${#secret} belgi, kamida 32)"
    fail=$((fail + 1))
fi

# Bularsiz ilova ishlaydi, lekin bir qism funksiya jim turadi.
if ! grep -q '^eskiz.email=.\+' "$FILE" 2>/dev/null; then
    echo "⚠ eskiz.email bo'sh — SMS yuborilmaydi, OTP orqali kirish ishlamaydi"
    warn=$((warn + 1))
fi

if grep -q '^app.gipersuperadmin.password=' "$FILE" 2>/dev/null; then
    echo "⚠ Admin paroli faylda ochiq turibdi."
    echo "  Hisob yaratilgach o'sha ikki qatorni QAYTA IZOHGA OLING."
    warn=$((warn + 1))
fi

if grep -q '^app.bootstrap.allow-weak-password=true' "$FILE" 2>/dev/null; then
    echo "✗ app.bootstrap.allow-weak-password=true — bu faqat lokal stend uchun"
    fail=$((fail + 1))
fi

echo
if [ "$fail" -gt 0 ]; then
    echo "✗ Sozlama tayyor emas — ishga tushirmang."
    exit 1
fi

if [ "$warn" -gt 0 ]; then
    echo "✓ Majburiy qiymatlar joyida ($warn ta ogohlantirish bilan)."
else
    echo "✓ Sozlama to'liq."
fi
