#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
UzCasting — Miro doskasini to'liq yig'ish.

11 ta frame: dizayn tizimi, mahsulot/monetizatsiya, 22 ta mobil ekran
wireframe'i 5 ta ketma-ketlikka bo'lingan holda, ekranlar inventari,
roadmap, backend/API va risklar.

Ishlatish:
    export MIRO_TOKEN='eyJ...'
    python3 tools/miro_build.py probe    # doskada nima borligini ko'rish
    python3 tools/miro_build.py build    # yig'ish

Token olish: https://miro.com/app/settings/user-profile/apps
  → Create new app → boards:read + boards:write → Install app and get OAuth token

MUHIM: barcha koordinatalar CHAP-YUQORI burchak bo'yicha beriladi,
helper'lar Miro'ning markaz-koordinatasiga o'zi o'giradi.
"""
import json
import os
import sys
import time
import urllib.error
import urllib.parse
import urllib.request

BOARD = os.environ.get("MIRO_BOARD", "uXjVHyaTTxc=")
TOKEN = os.environ.get("MIRO_TOKEN")
HINT = (
    "\nHaqiqiy Miro tokeni kerak.\n"
    "  1. https://miro.com/app/settings/user-profile/apps\n"
    "  2. Create new app -> boards:read + boards:write\n"
    "  3. Install app and get OAuth token -> nusxalang\n"
    "  4. MIRO_TOKEN='<tokeningiz>' python3 tools/miro_build.py build\n"
)
if not TOKEN or TOKEN.startswith("eyJ...") or TOKEN in ("", "..."):
    sys.exit("MIRO_TOKEN o'rnatilmagan yoki o'rnini bosuvchi matn qoldirilgan." + HINT)

API = "https://api.miro.com/v2/boards/" + urllib.parse.quote(BOARD, safe="")

# ---- ТЗ palitrasi (V2, 18-bet) ----
INK = "#07070D"
SURFACE = "#11111F"
SURFACE2 = "#1A1A2E"
BORDER = "#252540"
PURPLE = "#7C3AED"
MAGENTA = "#EC4899"
CYAN = "#22D3EE"
GOLD = "#F5C542"
WHITE = "#FFFFFF"
MUTED = "#9A9AB8"
DISABLED = "#5A5A75"
SUCCESS = "#34D399"
DANGER = "#F87171"
PANEL = "#0D0D16"
DARKRED = "#2A0D12"
DARKGOLD = "#2A1A0D"

created = []


def call(method, path, body=None):
    url = API + path
    data = json.dumps(body).encode("utf-8") if body is not None else None
    req = urllib.request.Request(url, data=data, method=method)
    req.add_header("Authorization", "Bearer " + TOKEN)
    req.add_header("Content-Type", "application/json")
    req.add_header("Accept", "application/json")
    for attempt in range(6):
        try:
            with urllib.request.urlopen(req, timeout=60) as r:
                return json.loads(r.read() or b"{}")
        except urllib.error.HTTPError as e:
            msg = e.read().decode("utf-8", "replace")[:500]
            if e.code in (429, 500, 502, 503):
                time.sleep(2 + attempt * 2)
                continue
            if e.code in (401, 403):
                sys.exit("Miro tokeni qabul qilinmadi (%s)." % e.code + HINT)
            if e.code == 404:
                sys.exit("Doska topilmadi: %s\nToken shu doska joylashgan team'ga o'rnatilganini "
                         "tekshiring." % BOARD)
            raise RuntimeError("%s %s -> %s %s" % (method, path, e.code, msg))
        except Exception:
            if attempt == 5:
                raise
            time.sleep(1 + attempt)
    raise RuntimeError("retries exhausted: " + path)


# --------------------------------------------------------------------
# Helper'lar — hammasi CHAP/YUQORI koordinata qabul qiladi
# --------------------------------------------------------------------
def frame(left, top, w, h, title):
    body = {
        "data": {"title": title, "format": "custom", "type": "freeform"},
        "position": {"x": left + w / 2.0, "y": top + h / 2.0},
        "geometry": {"width": w, "height": h},
        "style": {"fillColor": PANEL},
    }
    try:
        r = call("POST", "/frames", body)
    except RuntimeError:
        body.pop("style", None)
        r = call("POST", "/frames", body)
    created.append(r["id"])
    return r["id"]


def shape(left, top, w, h, content="", fill=SURFACE, border=BORDER, color=WHITE,
          size=13, align="left", valign="top", stype="round_rectangle", bw="2"):
    body = {
        "data": {"shape": stype, "content": content},
        "position": {"x": left + w / 2.0, "y": top + h / 2.0},
        "geometry": {"width": w, "height": h},
        "style": {
            "fillColor": fill, "borderColor": border, "borderWidth": str(bw),
            "color": color, "fontSize": str(size), "textAlign": align,
            "textAlignVertical": valign, "fillOpacity": "1", "borderOpacity": "1",
        },
    }
    r = call("POST", "/shapes", body)
    created.append(r["id"])
    return r["id"]


def text(left, top, w, content, size=18, color=WHITE, align="left"):
    body = {
        "data": {"content": content},
        "position": {"x": left + w / 2.0, "y": top + size * 0.9},
        "geometry": {"width": w},
        "style": {"color": color, "fontSize": str(size), "textAlign": align},
    }
    r = call("POST", "/texts", body)
    created.append(r["id"])
    return r["id"]


def link(a, b, color=PURPLE, shape_="elbowed"):
    body = {
        "startItem": {"id": a}, "endItem": {"id": b},
        "shape": shape_,
        "style": {"strokeColor": color, "strokeWidth": "3", "endStrokeCap": "arrow"},
    }
    r = call("POST", "/connectors", body)
    created.append(r["id"])
    return r["id"]


def P(*lines):
    return "".join("<p>%s</p>" % l if l else "<p>&nbsp;</p>" for l in lines)


def card(left, top, w, h, title, lines, accent=PURPLE, fill=SURFACE, size=14):
    return shape(left, top, w, h,
                 "<p><strong>%s</strong></p><p>&nbsp;</p>%s" % (title, P(*lines)),
                 fill=fill, border=accent, color=WHITE, size=size)


# --------------------------------------------------------------------
# Telefon wireframe
# --------------------------------------------------------------------
PW, PH = 420, 900          # telefon o'lchami
PPAD = 14                  # ichki padding


def blk(t, h, f=SURFACE, b=BORDER, c=WHITE, s=11, a="center"):
    return {"t": t, "h": h, "f": f, "b": b, "c": c, "s": s, "a": a}


def phone(left, top, num, title, blocks, accent=PURPLE, note=""):
    """Telefon ekrani wireframe'i. Qaytaradi: tashqi shape id (connector uchun)."""
    text(left, top - 62, PW, "<p><strong>%s · %s</strong></p>" % (num, title), 19, accent)
    outer = shape(left, top, PW, PH, "", fill=INK, border=accent, bw="3")
    y = top + PPAD
    iw = PW - PPAD * 2
    for b in blocks:
        shape(left + PPAD, y, iw, b["h"], "<p>%s</p>" % b["t"],
              fill=b["f"], border=b["b"], color=b["c"], size=b["s"],
              align=b["a"], valign="middle", bw="1")
        y += b["h"] + 6
    if note:
        text(left, top + PH + 12, PW, "<p>%s</p>" % note, 12, MUTED)
    return outer


def tabbar(active=0):
    names = ["Bosh", "Casting", "Premyera", "Xabar", "Profil"]
    names[active] = "<strong>%s</strong>" % names[active]
    return blk(" · ".join(names), 46, SURFACE2, PURPLE, WHITE, 11)


def statusbar():
    return blk("9:41&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; ▮▮▮ ⚡",
               24, INK, INK, MUTED, 10)


# ====================================================================
# EKRANLAR
# ====================================================================
def screens_auth(fx, fy):
    """03 · Kirish ketma-ketligi."""
    ids = []
    gap = 120
    x = fx

    ids.append(phone(x, fy, "S01", "Splash / Launch", [
        statusbar(),
        blk("", 240, INK, INK),
        blk("◆<br><strong>UZCASTING</strong>", 170, INK, PURPLE, WHITE, 22),
        blk("Ijodkorlar uchun platforma", 40, INK, INK, MUTED, 11),
        blk("", 200, INK, INK),
        blk("● ● ●&nbsp;&nbsp; loading", 40, INK, INK, PURPLE, 11),
        blk("", 106, INK, INK),
    ], PURPLE, "Logo + neon glow, 1–2 sek → onboarding yoki Home"))
    x += PW + gap

    ids.append(phone(x, fy, "S02", "Onboarding", [
        statusbar(),
        blk("Skip", 34, INK, INK, MUTED, 11, "right"),
        blk("[ cinematic illustration ]", 330, SURFACE2, BORDER, MUTED, 12),
        blk("<strong>Castingni toping</strong>", 44, INK, INK, WHITE, 17),
        blk("Rollar, portfolio va professional<br>imkoniyatlar bir joyda", 66, INK, INK, MUTED, 12),
        blk("● ○ ○ ○", 36, INK, INK, PURPLE, 13),
        blk("", 152, INK, INK),
        blk("<strong>BOSHLASH</strong>", 52, PURPLE, PURPLE, WHITE, 13),
    ], PURPLE, "3–4 slayd: casting · premyera · creator daromadi"))
    x += PW + gap

    ids.append(phone(x, fy, "S03", "Kirish — telefon", [
        statusbar(),
        blk("◆ UZCASTING", 54, INK, INK, WHITE, 15),
        blk("", 44, INK, INK),
        blk("<strong>Iltimos, telefon raqamingizni<br>kiriting</strong>", 66, INK, INK, WHITE, 15),
        blk("", 20, INK, INK),
        blk("📞 &nbsp;+998 &nbsp;|&nbsp; 91 123 45 67", 58, SURFACE, PURPLE, WHITE, 13),
        blk("", 24, INK, INK),
        blk("— yoki —", 32, INK, INK, MUTED, 11),
        blk("G &nbsp; Google orqali kirish", 52, SURFACE, BORDER, WHITE, 12),
        blk("", 190, INK, INK),
        blk("<u>Foydalanuvchi kelishuvi</u> va<br><u>Maxfiylik siyosatiga</u> roziman", 60, INK, INK, CYAN, 11),
        blk("<strong>DAVOM ETISH</strong>", 52, PURPLE, PURPLE, WHITE, 13),
    ], PURPLE, "Bir ekran = bir amal. Telefon ikkala tarmoqda ham tasdiqlanadi"))
    x += PW + gap

    ids.append(phone(x, fy, "S04", "OTP tasdiqlash", [
        statusbar(),
        blk("←", 40, INK, INK, WHITE, 15, "left"),
        blk("", 40, INK, INK),
        blk("<strong>SMS kodni kiriting</strong>", 46, INK, INK, WHITE, 16),
        blk("+998 91 123 45 67 raqamiga<br>yuborildi", 56, INK, INK, MUTED, 12),
        blk("", 24, INK, INK),
        blk("[ 5 ] &nbsp; [ 8 ] &nbsp; [ 2 ] &nbsp; [ _ ]", 74, SURFACE, PURPLE, WHITE, 18),
        blk("", 20, INK, INK),
        blk("Qayta yuborish · 00:47", 40, INK, INK, MUTED, 11),
        blk("⚠ Kod noto'g'ri", 36, INK, INK, DANGER, 11),
        blk("", 220, INK, INK),
        blk("<strong>TASDIQLASH</strong>", 52, PURPLE, PURPLE, WHITE, 13),
    ], PURPLE, "error state ko'rsatilgan · yangi user bo'lsa → ism + til"))
    x += PW + gap

    ids.append(phone(x, fy, "S05", "Home (kirish nuqtasi)", home_blocks(), MAGENTA,
                     "Barcha keyingi oqimlar shu ekrandan boshlanadi"))
    for a, b in zip(ids, ids[1:]):
        link(a, b, PURPLE)
    return ids


def home_blocks():
    return [
        statusbar(),
        blk("◆ UZCASTING &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; 🔍", 46, INK, INK, WHITE, 13),
        blk("( ) ( ) ( ) ( ) ( ) &nbsp; stories → mashhur ijodkorlar", 62, SURFACE, BORDER, MUTED, 10),
        blk("<strong>PREMYERA</strong><br>Hero karusel — bugungi premyera", 150, SURFACE2, MAGENTA, WHITE, 13),
        blk("Aktyor | Model | Bloger | Influencer →", 52, SURFACE, CYAN, WHITE, 10),
        blk("<strong>Bugungi premyeralar</strong>&nbsp;&nbsp;&nbsp; Barchasi ›", 30, INK, INK, WHITE, 11, "left"),
        blk("[▯ 🔒] [▯ 🔒] [▯ ✓] &nbsp;→", 108, SURFACE, BORDER, MUTED, 10),
        blk("<strong>Mashhur ijodkorlar</strong>&nbsp;&nbsp;&nbsp; Barchasi ›", 30, INK, INK, WHITE, 11, "left"),
        blk("[◯] [◯] [◯] [◯] &nbsp;→", 92, SURFACE, BORDER, MUTED, 10),
        blk("<strong>Casting e'lonlari</strong>&nbsp;&nbsp;&nbsp; Barchasi ›", 30, INK, INK, WHITE, 11, "left"),
        blk("[ card ] [ card ] &nbsp;→", 78, SURFACE, BORDER, MUTED, 10),
        blk("<strong>PREMIUM — reklamasiz tomosha</strong>", 50, GOLD, GOLD, INK, 12),
        tabbar(0),
    ]


def screens_catalog(fx, fy):
    """04 · Home → katalog → ijodkor profili."""
    ids = []
    gap = 120
    x = fx

    ids.append(phone(x, fy, "S05", "Home", home_blocks(), MAGENTA))
    x += PW + gap

    ids.append(phone(x, fy, "S06", "Kategoriyalar (10)", [
        statusbar(),
        blk("← &nbsp; <strong>Yo'nalishlar</strong>", 46, INK, INK, WHITE, 14, "left"),
        blk("01 Aktyorlar", 66, SURFACE, PURPLE, WHITE, 12),
        blk("02 Modellar", 66, SURFACE, MAGENTA, WHITE, 12),
        blk("03 Blogerlar", 66, SURFACE, CYAN, WHITE, 12),
        blk("04 Influencerlar", 66, SURFACE, GOLD, WHITE, 12),
        blk("05 Musiqachilar &nbsp; ⚠ API'da yo'q", 66, SURFACE, DISABLED, MUTED, 11),
        blk("06 Raqqoslar &nbsp; ⚠ API'da yo'q", 66, SURFACE, DISABLED, MUTED, 11),
        blk("07 Foto/Video &nbsp; ⚠ API'da yo'q", 66, SURFACE, DISABLED, MUTED, 11),
        blk("08 Styling &nbsp; ⚠ TZ'da ekran yo'q", 66, SURFACE, DISABLED, MUTED, 11),
        blk("09 Kurslar &nbsp; ⚠ TZ'da ekran yo'q", 66, SURFACE, DISABLED, MUTED, 11),
        blk("10 Casting e'lonlari", 66, SURFACE, MAGENTA, WHITE, 12),
        tabbar(0),
    ], CYAN, "Yashil = boevoy API'da bor (4/10). Kulrang = baza'da tur yo'q"))
    x += PW + gap

    ids.append(phone(x, fy, "S07", "Katalog (aktyorlar)", [
        statusbar(),
        blk("← &nbsp; <strong>Aktyorlar</strong> &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; 🔍", 46, INK, INK, WHITE, 14, "left"),
        blk("Barchasi | Mashhur | Yangi | Tavsiya →", 42, SURFACE, BORDER, MUTED, 10),
        blk("⚙ Shahar ▾ &nbsp; Yosh ▾ &nbsp; Tajriba ▾", 42, SURFACE, CYAN, CYAN, 10),
        blk("[◯ foto]&nbsp;✓<br>Ism · Toshkent · 24", 118, SURFACE, BORDER, WHITE, 10),
        blk("[◯ foto]&nbsp;✓<br>Ism · Samarqand · 19", 118, SURFACE, BORDER, WHITE, 10),
        blk("[◯ foto]<br>Ism · Andijon · 27", 118, SURFACE, BORDER, WHITE, 10),
        blk("[◯ foto]&nbsp;✓<br>Ism · Buxoro · 22", 118, SURFACE, BORDER, WHITE, 10),
        blk("··· skeleton loading ···", 60, SURFACE2, BORDER, MUTED, 10),
        tabbar(0),
    ], CYAN, "Faqat isWebShow=true fotolar. age bo'lmasa birthday'dan hisoblanadi"))
    x += PW + gap

    ids.append(phone(x, fy, "S08", "Ijodkor profili", [
        statusbar(),
        blk("← &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; ♡ &nbsp; ⋯", 40, INK, INK, WHITE, 13, "left"),
        blk("( ◯ katta avatar )&nbsp; ✓ verified", 130, SURFACE2, GOLD, WHITE, 12),
        blk("<strong>Ism Familiya</strong><br>Aktyor · Toshkent", 56, INK, INK, WHITE, 14),
        blk("Bio: professional tajriba, tillar, ...", 44, INK, INK, MUTED, 11),
        blk("128 loyiha &nbsp;|&nbsp; 4.2K follower &nbsp;|&nbsp; 89K ko'rish", 48, SURFACE, BORDER, WHITE, 10),
        blk("<strong>KUZATISH</strong> &nbsp;&nbsp; | &nbsp;&nbsp; Bog'lanish", 50, PURPLE, PURPLE, WHITE, 12),
        blk("<strong>Portfolio</strong>", 26, INK, INK, WHITE, 11, "left"),
        blk("[▯][▯][▯]<br>[▯][▯][▯]", 132, SURFACE, BORDER, MUTED, 10),
        blk("<strong>Videolar / Reels</strong>", 26, INK, INK, WHITE, 11, "left"),
        blk("[▶][▶][▶] →", 74, SURFACE, BORDER, MUTED, 10),
        blk("⭐ <strong>STARS YUBORISH</strong> &nbsp; 1 240 ⭐", 50, GOLD, GOLD, INK, 12),
        blk("", 22, INK, INK),
    ], GOLD, "Casting uchun tayyor CV — oddiy social profil emas"))

    for a, b in zip(ids, ids[1:]):
        link(a, b, CYAN)
    return ids


def screens_casting(fx, fy):
    """05 · Casting oqimi."""
    ids = []
    gap = 120
    x = fx

    ids.append(phone(x, fy, "S09", "Casting lentasi", [
        statusbar(),
        blk("<strong>Casting e'lonlari</strong> &nbsp;&nbsp;&nbsp;&nbsp;&nbsp; 🔍", 46, INK, INK, WHITE, 14, "left"),
        blk("🔍 Qidiruv...", 44, SURFACE, BORDER, MUTED, 11),
        blk("Shahar ▾ | Kategoriya ▾ | Yosh ▾ | To'lovli ▾", 42, SURFACE, CYAN, CYAN, 10),
        blk("<strong>Serial uchun aktyor</strong> &nbsp;&nbsp; 💰 To'lovli<br>Toshkent · 12 kun qoldi &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; ♡", 106, SURFACE, MAGENTA, WHITE, 11),
        blk("<strong>Reklama roligi — model</strong> &nbsp;&nbsp; 💰 To'lovli<br>Samarqand · 3 kun qoldi &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; ♡", 106, SURFACE, MAGENTA, WHITE, 11),
        blk("<strong>Klip uchun raqqos</strong> &nbsp;&nbsp;&nbsp;&nbsp;&nbsp; Bepul<br>Toshkent · 21 kun qoldi &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; ♡", 106, SURFACE, BORDER, WHITE, 11),
        blk("<strong>Massovka</strong> &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; Bepul<br>Buxoro · muddati tugagan", 106, SURFACE, DISABLED, MUTED, 11),
        blk("", 44, INK, INK),
        tabbar(1),
    ], CYAN))
    x += PW + gap

    ids.append(phone(x, fy, "S10", "Casting detali", [
        statusbar(),
        blk("← &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; ♡ &nbsp; ⤴", 40, INK, INK, WHITE, 13, "left"),
        blk("[ referens rasm / video ]", 150, SURFACE2, BORDER, MUTED, 11),
        blk("<strong>Serial uchun bosh rol — aktyor</strong>", 44, INK, INK, WHITE, 14),
        blk("💰 To'lovli &nbsp;|&nbsp; ⏳ 12 kun qoldi", 38, INK, INK, MAGENTA, 11),
        blk("<strong>Talablar</strong><br>Yosh 22–30 · bo'y 175+ · o'zbek tili", 74, SURFACE, BORDER, WHITE, 11),
        blk("<strong>Sana va joy</strong><br>18.09.2026 · Toshkent, Film studiya", 74, SURFACE, BORDER, WHITE, 11),
        blk("<strong>To'lov</strong> — 2 000 000 so'm / kun", 46, SURFACE, GOLD, GOLD, 11),
        blk("📎 Foto / video yuklash (majburiy)", 56, SURFACE, CYAN, CYAN, 11),
        blk("<strong>ARIZA BERISH</strong>", 54, PURPLE, PURPLE, WHITE, 13),
        blk("", 46, INK, INK),
    ], MAGENTA))
    x += PW + gap

    ids.append(phone(x, fy, "S11", "Ariza statusi", [
        statusbar(),
        blk("← &nbsp; <strong>Mening arizalarim</strong>", 46, INK, INK, WHITE, 14, "left"),
        blk("✓ &nbsp;<strong>Ariza yuborildi</strong><br>Serial uchun bosh rol · 14.08.2026", 92, SURFACE, SUCCESS, WHITE, 11),
        blk("⏳ &nbsp;<strong>Ko'rib chiqilmoqda</strong><br>Reklama roligi — model", 92, SURFACE, GOLD, WHITE, 11),
        blk("🎉 &nbsp;<strong>Qabul qilindi</strong><br>Klip uchun raqqos · suhbat 20.08", 92, SURFACE, SUCCESS, WHITE, 11),
        blk("✕ &nbsp;<strong>Rad etildi</strong><br>Massovka · 02.08.2026", 92, SURFACE, DANGER, MUTED, 11),
        blk("", 60, INK, INK),
        blk("Status o'zgarsa — push + Xabarlar'ga<br>deep-link keladi", 66, SURFACE2, CYAN, MUTED, 11),
        blk("", 174, INK, INK),
        tabbar(1),
    ], SUCCESS, "success · empty · error holatlari shu shablonda"))

    for a, b in zip(ids, ids[1:]):
        link(a, b, MAGENTA)
    return ids


def screens_money(fx, fy):
    """06 · Monetizatsiya yadrosi."""
    ids = []
    gap = 110
    x = fx

    ids.append(phone(x, fy, "S12", "Premyera katalogi", [
        statusbar(),
        blk("<strong>Premyera</strong> &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; 🔍", 46, INK, INK, WHITE, 14, "left"),
        blk("<strong>Barchasi</strong> | Seriallar | Ko'rsatuvlar | Filmlar", 42, SURFACE, MAGENTA, WHITE, 10),
        blk("Janr ▾ &nbsp;|&nbsp; Til ▾", 38, SURFACE, BORDER, MUTED, 10),
        blk("[▯ poster]&nbsp;🔒<br>3 000", 128, SURFACE, MAGENTA, WHITE, 10),
        blk("[▯ poster]&nbsp;✓ olingan<br>Ko'rish", 128, SURFACE, SUCCESS, WHITE, 10),
        blk("[▯ poster]&nbsp;🔒<br>3 000", 128, SURFACE, MAGENTA, WHITE, 10),
        blk("[▯ poster]&nbsp;PREMIUM", 128, SURFACE, GOLD, WHITE, 10),
        blk("", 30, INK, INK),
        tabbar(2),
    ], MAGENTA, "locked / purchased birinchi qarashda ko'rinadi"))
    x += PW + gap

    ids.append(phone(x, fy, "S13", "Serial detali", [
        statusbar(),
        blk("← &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; ♡", 38, INK, INK, WHITE, 13, "left"),
        blk("[ blur kadr ]&nbsp;&nbsp; ▶ &nbsp;&nbsp;TREYLER", 156, SURFACE2, MAGENTA, WHITE, 12),
        blk("<strong>Serial nomi</strong><br>Drama · 2026 · O'zbek", 60, INK, INK, WHITE, 14),
        blk("👍 1122 &nbsp; ★ 8.3 &nbsp; ⏱ 45 daq", 42, SURFACE, BORDER, WHITE, 10),
        blk("<strong>Qismlar</strong>", 26, INK, INK, WHITE, 11, "left"),
        blk("1-qism &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; BEPUL &nbsp; ▶", 52, SURFACE, SUCCESS, WHITE, 11),
        blk("2-qism &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; 🔒 3 000 so'm", 52, SURFACE, MAGENTA, WHITE, 11),
        blk("3-qism &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; 🔒 3 000 so'm", 52, SURFACE, MAGENTA, WHITE, 11),
        blk("4-qism &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; 🔒 tez kunda", 52, SURFACE, DISABLED, MUTED, 11),
        blk("<strong>BUTUN PREMYERA — 15 000</strong>", 50, MAGENTA, MAGENTA, WHITE, 12),
        blk("", 66, INK, INK),
    ], MAGENTA))
    x += PW + gap

    ids.append(phone(x, fy, "S14", "Qism detali — LOCKED", [
        statusbar(),
        blk("←", 38, INK, INK, WHITE, 13, "left"),
        blk("[ cinematic cover ]<br><br>🔒 &nbsp; QULFLANGAN", 210, SURFACE2, MAGENTA, WHITE, 13),
        blk("<strong>2-qism — Yangi boshlanish</strong>", 46, INK, INK, WHITE, 14),
        blk("Serial nomi · 42 daqiqa", 34, INK, INK, MUTED, 11),
        blk("▶ &nbsp;Treylerni ko'rish (bepul)", 50, SURFACE, CYAN, CYAN, 12),
        blk("Bu qismni ko'rish uchun sotib oling", 44, INK, INK, MUTED, 11),
        blk("<strong>OCHISH — 3 000 so'm</strong>", 58, MAGENTA, MAGENTA, WHITE, 14),
        blk("Boshqa variantlar ▾", 40, INK, INK, CYAN, 11),
        blk("Keyingi qismlar: 3 · 4 · 5 →", 56, SURFACE, BORDER, MUTED, 10),
        blk("", 66, INK, INK),
    ], MAGENTA, "Konversiyasi yuqori ekran. Narx va locked — bir qarashda"))
    x += PW + gap

    ids.append(phone(x, fy, "S15", "Sotib olish darajasi", [
        statusbar(),
        blk("✕ &nbsp; <strong>Qanday ochamiz?</strong>", 46, INK, INK, WHITE, 14, "left"),
        blk("", 16, INK, INK),
        blk("<strong>Faqat shu qism</strong><br>2-qism ochiladi<br><strong>3 000 so'm</strong>", 116, SURFACE, CYAN, WHITE, 12),
        blk("<strong>Butun premyera</strong><br>Barcha mavjud qismlar<br><strong>15 000 so'm</strong>", 116, SURFACE, MAGENTA, WHITE, 12),
        blk("<strong>UZCASTING PREMIUM</strong> ⭐<br>Hamma kontent + reklamasiz<br><strong>24 000 / oy</strong>", 128, SURFACE, GOLD, WHITE, 12),
        blk("", 20, INK, INK),
        blk("❓ 15 000 kelajak qismlarga<br>tarqaladimi — aniqlanmagan", 62, DARKGOLD, GOLD, GOLD, 10),
        blk("", 130, INK, INK),
        blk("<strong>DAVOM ETISH</strong>", 52, PURPLE, PURPLE, WHITE, 13),
    ], GOLD, "TZ'da bu ekran yo'q — 13.08 xabaridan kelib chiqdi"))
    x += PW + gap

    ids.append(phone(x, fy, "S16", "To'lov", [
        statusbar(),
        blk("← &nbsp; <strong>To'lov</strong>", 46, INK, INK, WHITE, 14, "left"),
        blk("<strong>Buyurtma</strong><br>Serial nomi · 2-qism<br><strong>3 000 so'm</strong>", 100, SURFACE, BORDER, WHITE, 11),
        blk("<strong>To'lov usulini tanlang</strong>", 34, INK, INK, WHITE, 11, "left"),
        blk("[ Click ] &nbsp;&nbsp; [ Payme ]", 76, SURFACE, PURPLE, WHITE, 11),
        blk("[ Uzum ] &nbsp;&nbsp; [ UZCARD ]", 76, SURFACE, BORDER, WHITE, 11),
        blk("[ Visa ] &nbsp;&nbsp;&nbsp; [ Mastercard ]", 76, SURFACE, BORDER, WHITE, 11),
        blk("🔒 Xavfsiz to'lov · SSL", 40, INK, INK, MUTED, 10),
        blk("⚠ Store qoidalari: raqamli kontent<br>Play/Apple billing orqali sotilishi kerak", 66, DARKRED, DANGER, DANGER, 10),
        blk("", 96, INK, INK),
        blk("<strong>TO'LASH — 3 000 so'm</strong>", 54, PURPLE, PURPLE, WHITE, 13),
    ], PURPLE, "expo-web-browser + deeplink uzcasting:// orqali qaytish"))
    x += PW + gap

    ids.append(phone(x, fy, "S17", "Muvaffaqiyatli", [
        statusbar(),
        blk("", 130, INK, INK),
        blk("✓", 110, INK, SUCCESS, SUCCESS, 40),
        blk("<strong>To'lov muvaffaqiyatli</strong>", 46, INK, INK, WHITE, 16),
        blk("2-qism ochildi", 36, INK, INK, MUTED, 12),
        blk("<strong>Chek</strong><br>Tranzaksiya: #4820193<br>3 000 so'm · Click · 14.08.2026", 96, SURFACE, BORDER, MUTED, 11),
        blk("", 130, INK, INK),
        blk("<strong>KO'RISHNI BOSHLASH</strong>", 54, SUCCESS, SUCCESS, INK, 13),
        blk("Chekni saqlash", 44, INK, INK, CYAN, 11),
        blk("", 78, INK, INK),
    ], SUCCESS, "entitlement yozildi → himoyalangan player ochiladi"))
    x += PW + gap

    ids.append(phone(x, fy, "S18", "Video player", [
        statusbar(),
        blk("", 60, INK, INK),
        blk("[ ▶ &nbsp; VIDEO &nbsp; landscape ]", 240, SURFACE2, PURPLE, MUTED, 12),
        blk("━━━━━━━●━━━━━━ &nbsp; 18:42 / 42:10", 42, SURFACE, BORDER, WHITE, 10),
        blk("⏮ &nbsp;&nbsp; ⏯ &nbsp;&nbsp; ⏭ &nbsp;&nbsp;&nbsp;&nbsp; ⚙ &nbsp; CC &nbsp; ⛶", 54, SURFACE, BORDER, WHITE, 12),
        blk("Sifat: Auto / 1080 / 720 / 480", 40, SURFACE, BORDER, MUTED, 10),
        blk("Subtitrlar: UZ / RU / yo'q", 40, SURFACE, BORDER, MUTED, 10),
        blk("<strong>Keyingi qism →</strong> 3-qism 🔒 3 000", 54, SURFACE, MAGENTA, WHITE, 11),
        blk("Muammo haqida xabar berish", 40, INK, INK, MUTED, 10),
        blk("", 174, INK, INK),
    ], PURPLE, "Access-controlled streaming. DRM yoki signed URL — hal qilinmagan"))

    for a, b in zip(ids, ids[1:]):
        link(a, b, MAGENTA)
    return ids


def screens_profile(fx, fy):
    """07 · Profil / Studio / Premium / Stars."""
    ids = []
    gap = 120
    x = fx

    ids.append(phone(x, fy, "S19", "Xabarlar", [
        statusbar(),
        blk("<strong>Xabarlar</strong> &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; ✎", 46, INK, INK, WHITE, 14, "left"),
        blk("Barchasi | Casting | Tizim", 40, SURFACE, BORDER, MUTED, 10),
        blk("◯ &nbsp;<strong>Casting menejer</strong> &nbsp;&nbsp;&nbsp; ②<br>Arizangiz qabul qilindi...", 88, SURFACE, MAGENTA, WHITE, 11),
        blk("◯ &nbsp;<strong>Ijodkor · Ism</strong><br>Rahmat, ko'rib chiqaman", 88, SURFACE, BORDER, MUTED, 11),
        blk("⚙ &nbsp;<strong>Tizim</strong> &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; ①<br>2-qism ochildi", 88, SURFACE, CYAN, WHITE, 11),
        blk("", 40, INK, INK),
        blk("empty: «Hozircha xabar yo'q»", 60, SURFACE2, BORDER, MUTED, 10),
        blk("", 176, INK, INK),
        tabbar(3),
    ], CYAN))
    x += PW + gap

    ids.append(phone(x, fy, "S20", "Profil", [
        statusbar(),
        blk("( ◯ ) &nbsp;<strong>Ism Familiya</strong><br>+998 91 123 45 67<br>Balans: 12 000 so'm &nbsp;·&nbsp; ID 1870181 ⧉", 118, SURFACE, PURPLE, WHITE, 11),
        blk("💳 &nbsp;Balansni to'ldirish &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; ›", 46, SURFACE, BORDER, WHITE, 11, "left"),
        blk("🎬 &nbsp;Xaridlarim &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; ›", 46, SURFACE, BORDER, WHITE, 11, "left"),
        blk("📄 &nbsp;To'lovlar tarixi &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; ›", 46, SURFACE, BORDER, WHITE, 11, "left"),
        blk("📋 &nbsp;Arizalarim &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; ›", 46, SURFACE, BORDER, WHITE, 11, "left"),
        blk("🖼 &nbsp;Portfolio &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; ›", 46, SURFACE, BORDER, WHITE, 11, "left"),
        blk("⭐ &nbsp;Stars: 240 &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; ›", 46, SURFACE, GOLD, GOLD, 11, "left"),
        blk("🎥 &nbsp;<strong>Creator Studio</strong> &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; ›", 46, SURFACE, GOLD, GOLD, 11, "left"),
        blk("👑 &nbsp;Premium &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; ›", 46, SURFACE, MAGENTA, WHITE, 11, "left"),
        blk("📱 &nbsp;Faol qurilmalar &nbsp;·&nbsp; ⚙ Sozlamalar", 46, SURFACE, BORDER, WHITE, 11, "left"),
        blk("Chiqish", 42, INK, INK, DANGER, 11, "left"),
        tabbar(4),
    ], PURPLE, "Creator Studio — 6-tab emas, Profil ichidagi bo'lim"))
    x += PW + gap

    ids.append(phone(x, fy, "S21", "Creator Studio — 50/50", [
        statusbar(),
        blk("← &nbsp; <strong>Creator Studio</strong>", 46, INK, INK, WHITE, 14, "left"),
        blk("Ko'rishlar<br><strong>89 420</strong>", 76, SURFACE, CYAN, WHITE, 11),
        blk("Xaridlar<br><strong>1 284</strong>", 76, SURFACE, MAGENTA, WHITE, 11),
        blk("<strong>DAROMAD</strong><br>Yalpi sotuv &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; 3 852 000<br>Platforma 50% &nbsp; −1 926 000<br><strong>Sizning ulush &nbsp; 1 926 000</strong>", 128, SURFACE, GOLD, WHITE, 11),
        blk("⚠ Store komissiyasi 15–30% shu<br>bo'linishdan OLDIN yechiladi", 58, DARKRED, DANGER, DANGER, 10),
        blk("<strong>Kontent</strong>", 26, INK, INK, WHITE, 11, "left"),
        blk("2-qism &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; ✓ chop etilgan", 44, SURFACE, SUCCESS, WHITE, 10),
        blk("3-qism &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; ⏳ moderatsiyada", 44, SURFACE, GOLD, WHITE, 10),
        blk("+ &nbsp;KONTENT YUKLASH", 50, PURPLE, PURPLE, WHITE, 12),
        blk("Mavjud balans: <strong>1 926 000 so'm</strong>", 44, INK, INK, GOLD, 11),
        blk("<strong>PUL YECHISH</strong>", 52, GOLD, GOLD, INK, 13),
    ], GOLD))
    x += PW + gap

    ids.append(phone(x, fy, "S22", "Premium tariflar", [
        statusbar(),
        blk("← &nbsp; <strong>UzCasting Premium</strong>", 46, INK, INK, WHITE, 14, "left"),
        blk("Barcha kontent · barcha premyeralar<br>reklamasiz tomosha · premium kontent", 62, SURFACE, MAGENTA, WHITE, 11),
        blk("<strong>1 OY</strong> &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; 24 000 so'm", 68, SURFACE, BORDER, WHITE, 12),
        blk("<strong>3 OY</strong> &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; 49 999 so'm", 68, SURFACE, BORDER, WHITE, 12),
        blk("<strong>6 OY</strong> &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; 99 000 so'm", 68, SURFACE, BORDER, WHITE, 12),
        blk("⭐ <strong>1 YIL — ENG FOYDALI</strong><br>159 900 so'm &nbsp;(oyiga 13 325)", 88, SURFACE, GOLD, GOLD, 12),
        blk("❓ Avtoprodlenie bor-yo'qligi<br>aniqlanmagan", 56, DARKGOLD, GOLD, GOLD, 10),
        blk("", 88, INK, INK),
        blk("<strong>PREMIUM OLISH</strong>", 52, MAGENTA, MAGENTA, WHITE, 13),
        blk("Xaridlar tarixi", 40, INK, INK, CYAN, 11),
    ], MAGENTA))
    x += PW + gap

    ids.append(phone(x, fy, "S23", "Stars — donat va reyting", [
        statusbar(),
        blk("← &nbsp; <strong>UzCasting Stars</strong> ⭐", 46, INK, INK, WHITE, 14, "left"),
        blk("«Sevimli yulduzingizni qo'llab-quvvatlang»", 44, INK, INK, MUTED, 11),
        blk("Balansingiz: <strong>240 ⭐</strong>", 50, SURFACE, GOLD, GOLD, 12),
        blk("<strong>Paketlar</strong>", 26, INK, INK, WHITE, 11, "left"),
        blk("10 ⭐ &nbsp;&nbsp;|&nbsp;&nbsp; 50 ⭐ &nbsp;&nbsp;|&nbsp;&nbsp; 100 ⭐", 60, SURFACE, GOLD, WHITE, 11),
        blk("500 ⭐ &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;|&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; 1 000 ⭐", 60, SURFACE, GOLD, WHITE, 11),
        blk("❓ Kurs aniqlanmagan — 10 ⭐ necha so'm?", 44, DARKGOLD, GOLD, GOLD, 10),
        blk("<strong>Oylik reyting</strong>", 26, INK, INK, WHITE, 11, "left"),
        blk("🥇 Film nomi &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; 48 200 ⭐<br>🥈 Ijodkor ism &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; 31 050 ⭐<br>🥉 Serial nomi &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; 27 900 ⭐", 100, SURFACE, MAGENTA, WHITE, 10),
        blk("🏆 Oylik taqdirlash shousi →", 48, SURFACE, MAGENTA, MAGENTA, 11),
        blk("<strong>STARS SOTIB OLISH</strong>", 52, GOLD, GOLD, INK, 13),
    ], GOLD, "TZ'da butunlay yo'q — 13.08 xabaridan. Yangi quyi tizim"))

    link(ids[1], ids[2], GOLD)
    link(ids[1], ids[3], MAGENTA)
    link(ids[1], ids[4], GOLD)
    return ids


# ====================================================================
# STATIK FRAME'LAR
# ====================================================================
FW = 4400   # keng frame
PAD = 60


def head(fx, fy, title, sub, accent=PURPLE):
    text(fx + PAD, fy + 40, FW - PAD * 2, "<p><strong>%s</strong></p>" % title, 40, WHITE)
    text(fx + PAD, fy + 108, FW - PAD * 2, "<p>%s</p>" % sub, 19, accent)


def f_design(fx, fy, h):
    frame(fx, fy, FW, h, "01 · DIZAYN TIZIMI")
    shape(fx, fy, FW, h, "", fill=PANEL, border=BORDER, bw="1")
    head(fx, fy, "01 · DIZAYN TIZIMI", "ТЗ V2, 18-bet — tasdiqlangan, o'zgartirilmaydi", MAGENTA)
    x = fx + PAD
    y = fy + 180

    text(x, y, 1200, "<p><strong>ТЗ PALITRASI</strong></p>", 22, WHITE)
    sw = [("Deep Black", INK, "Ilova foni", WHITE), ("Midnight", SURFACE, "Kartochka", WHITE),
          ("Neon Purple", PURPLE, "Asosiy CTA", WHITE), ("Magenta", MAGENTA, "Premium / PREMYERA", WHITE),
          ("Electric Cyan", CYAN, "Info / secondary", INK), ("Gold", GOLD, "Verified / pul yechish", INK),
          ("White", WHITE, "Asosiy matn", INK)]
    cw = 290
    for i, (n, hx, role, tc) in enumerate(sw):
        shape(x + i * (cw + 18), y + 50, cw, 180,
              P("<strong>%s</strong>" % n, hx, "", role), fill=hx, border=BORDER, color=tc, size=13)

    y2 = y + 260
    text(x, y2, 2200, "<p><strong>HOSILA TOKENLAR</strong> — bizning yechim, 8 holat uchun zarur</p>", 18, MUTED)
    dv = [("surface-2", SURFACE2, WHITE), ("border", BORDER, WHITE), ("text-muted", MUTED, INK),
          ("text-disabled", DISABLED, WHITE), ("success", SUCCESS, INK), ("danger", DANGER, INK)]
    cw2 = 340
    for i, (n, hx, tc) in enumerate(dv):
        shape(x + i * (cw2 + 18), y2 + 44, cw2, 130, P("<strong>%s</strong>" % n, hx),
              fill=hx, border=BORDER, color=tc, size=13)

    xr = x + 2200
    card(xr, y + 50, 1020, 290, "UI QOIDALARI (ТЗ)",
         ["Spacing — 8–16px tizimi", "Kartochka radius — 14–22px", "Touch target — kamida 44px",
          "Har ekranda BITTA asosiy CTA", "Dark mode birinchi — light tema yo'q",
          "Rasm sifati — kamida 1080px", "Glow me'yorida, ortiqcha gradient emas",
          "Narx va locked — bir qarashda ko'rinadi"], PURPLE, size=14)
    card(xr + 1050, y + 50, 1030, 290, "TIPOGRAFIKA",
         ["ТЗ faqat «hierarchy» deydi — qolgani bizning yechim", "",
          "display &nbsp; 32 / 38", "h1 &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; 24 / 30",
          "h2 &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; 20 / 26", "body &nbsp;&nbsp;&nbsp;&nbsp;&nbsp; 15 / 21",
          "caption &nbsp; 13 / 18", "micro &nbsp;&nbsp;&nbsp; 11 / 14"], CYAN, size=14)

    y3 = y2 + 220
    text(x, y3, 2000, "<p><strong>8 MAJBURIY HOLAT</strong> — ТЗ har bir ekranda talab qiladi</p>", 22, GOLD)
    st = [("loading", CYAN), ("empty", MUTED), ("error", DANGER), ("success", SUCCESS),
          ("locked", GOLD), ("purchased", SUCCESS), ("disabled", DISABLED), ("offline", MUTED)]
    sw3 = 260
    for i, (n, c) in enumerate(st):
        shape(x + i * (sw3 + 16), y3 + 46, sw3, 84, "<p><strong>%s</strong></p>" % n,
              fill=SURFACE2, border=c, color=c, size=15, align="center", valign="middle")
    shape(x + 2240, y3 + 46, 2040, 200,
          P("<strong>Kodda qayerda:</strong>",
            "loading · empty · error · offline · locked → <strong>src/components/states/ScreenState.tsx</strong>",
            "disabled · loading (tugma) → <strong>src/components/ui/Button.tsx</strong>",
            "success · purchased · locked (belgi) → <strong>src/components/ui/Badge.tsx</strong>", "",
            "Palitra manbasi: <strong>tailwind.config.js</strong> + <strong>src/theme/tokens.ts</strong> — ikkalasi ham tuzatiladi"),
          fill=SURFACE, border=BORDER, color=WHITE, size=14)


def f_product(fx, fy, h):
    frame(fx, fy, FW, h, "02 · MAHSULOT VA MONETIZATSIYA")
    shape(fx, fy, FW, h, "", fill=PANEL, border=BORDER, bw="1")
    head(fx, fy, "02 · MAHSULOT VA MONETIZATSIYA", "Buyurtmachi xabari 13.08.2026 — ТЗ raqamlarini bekor qiladi", GOLD)
    x = fx + PAD
    y = fy + 180

    pil = [("CASTING", PURPLE, ["Aktyor, model, bloger professional", "portfolio yuritadi va rollarga", "ariza beradi."]),
           ("PREMYERA", MAGENTA, ["Eksklyuziv seriallar, shou, filmlar.", "Qismlar pullik — monetizatsiya", "markazi."]),
           ("CREATOR ECONOMY", GOLD, ["Ijodkor kontent yuklaydi, sotuvdan", "ulush oladi, pulni yechadi.", "Revenue share 50/50."])]
    for i, (t, c, ls) in enumerate(pil):
        card(x + i * 730, y, 700, 190, t, ls, c, size=14)

    text(x + 2220, y, 2100, "<p><strong>MONETIZATSIYA YADROSI (ТЗ — asosiy stsenariy)</strong></p>", 20, GOLD)
    steps = [("1", "YouTube / IG", "1-qism BEPUL", CYAN), ("2", "CTA", "«Davomini UzCasting'da»", CYAN),
             ("3", "UzCasting", "2-qism sotib olish", MAGENTA), ("4", "To'lov", "Click / Payme / Uzum", MAGENTA),
             ("5", "Entitlement", "→ himoyalangan player", SUCCESS), ("6", "Revenue", "50 / 50", GOLD)]
    sww = 330
    sids = []
    for i, (n, t, s, c) in enumerate(steps):
        sids.append(shape(x + 2220 + i * (sww + 20), y + 46, sww, 130,
                          P("<strong>%s. %s</strong>" % (n, t), s),
                          fill=SURFACE2, border=c, color=WHITE, size=13))
    for a, b in zip(sids, sids[1:]):
        link(a, b, PURPLE)

    y2 = y + 230
    shape(x, y2, FW - PAD * 2, 90,
          P("<strong>⚠ 4 TA PDF'NING HAMMASIDA 5 000 SO'M TURIBDI — BU ESKIRGAN.</strong> "
            "Buyurtmachi 13.08.2026: narx <strong>3 000</strong>ga tushirildi, 3 ta yangi daraja qo'shildi. "
            "V4 maketlari ham eskirgan."),
          fill=DARKGOLD, border=GOLD, color=GOLD, size=16)

    y3 = y2 + 110
    tiers = [("1 · BITTA SERIYA", "3 000 so'm", CYAN,
              ["Faqat tanlangan seriya ochiladi.", "Obuna kerak emas.", "", "⚠ ТЗ'da 5 000 edi."]),
             ("2 · BITTA PREMYERA", "15 000 so'm", MAGENTA,
              ["Tanlangan serialning BARCHA", "mavjud qismlari.", "", "⚠ ТЗ'da yo'q — yangi daraja.",
               "❓ Kelajak qismlarga tarqaladimi?"]),
             ("3 · UZCASTING PREMIUM", "obuna", PURPLE,
              ["1 oy — 24 000", "3 oy — 49 999", "6 oy — 99 000", "1 yil — 159 900 ⭐",
               "", "Hamma kontent · reklamasiz"]),
             ("4 · UZCASTING STARS", "donat", GOLD,
              ["Paketlar: 10 · 50 · 100 · 500 · 1 000", "Ijodkorga VA filmga yuboriladi.",
               "Reyting + oylik taqdirlash shousi.", "", "⚠ Butunlay yangi quyi tizim."])]
    cw = 1040
    for i, (t, price, c, ls) in enumerate(tiers):
        shape(x + i * (cw + 20), y3, cw, 290,
              "<p><strong>%s</strong></p><p>&nbsp;</p><p><strong>%s</strong></p><p>&nbsp;</p>%s" % (t, price, P(*ls)),
              fill=SURFACE, border=c, color=WHITE, size=14)

    y4 = y3 + 310
    shape(x, y4, 2140, 230,
          P("<strong>🔴 RISK: GOOGLE PLAY VA APP STORE QOIDALARI</strong>", "",
            "Seriyalar, premyeralar, obuna va Stars — bularning hammasi <strong>ilova ichidagi raqamli kontent</strong>. "
            "Ikkala store ham buni faqat o'z billingi orqali sotishni talab qiladi: <strong>komissiya 15–30%</strong>.",
            "Click / Payme / Uzum bu uchun <strong>qoidani buzadi</strong> → ilova olib tashlanadi.",
            "Komissiya 50/50 bo'linishidan <strong>OLDIN</strong> yechiladi: 3 000 so'mda ijodkor 1 500 emas, "
            "<strong>≈1 050–1 275</strong> oladi."),
          fill=DARKRED, border=DANGER, color=WHITE, size=14)
    shape(x + 2160, y4, 2120, 230,
          P("<strong>⚠ «REKLAMASIZ TOMOSHA» NIMANI ANGLATADI</strong>", "",
            "Premium — reklamasiz. Demak <strong>qolgan foydalanuvchilarda reklama BOR</strong>.",
            "ТЗ'da reklama haqida bironta so'z yo'q: qaysi tarmoq, qanday formatlar, qayerda ko'rsatiladi — "
            "hech narsa yozilmagan.", "",
            "Bu alohida quyi tizim va alohida kelishuv talab qiladi."),
          fill=DARKGOLD, border=GOLD, color=WHITE, size=14)


# --------------------------------------------------------------------
SCREENS_INV = [
    ("01", "Splash / Launch", "done", "logo va fon sozlangan"),
    ("02", "Onboarding 3–4 slayd", "todo", ""),
    ("03", "Kirish / Ro'yxatdan o'tish", "part", "ekranlar bor, Google yozilgan; client ID kutilmoqda"),
    ("04", "Home", "done", "ijodkorlar boevoy API'da"),
    ("05", "Kategoriyalar (10)", "part", "plitkalar bor, alohida ekran yo'q"),
    ("06", "Aktyorlar katalogi", "todo", ""),
    ("07", "Modellar katalogi", "todo", ""),
    ("08", "Blogerlar katalogi", "todo", ""),
    ("09", "Influencerlar katalogi", "todo", ""),
    ("10", "Musiqachilar katalogi", "todo", "API'da tur yo'q"),
    ("11", "Raqqoslar katalogi", "todo", "API'da tur yo'q"),
    ("12", "Foto/Video katalogi", "todo", "API'da tur yo'q"),
    ("13", "Ijodkor profili", "todo", ""),
    ("14", "Casting lentasi", "done", "ma'lumot vaqtinchalik"),
    ("15", "Casting detali + ariza", "todo", ""),
    ("16", "Premyera katalogi", "done", "ma'lumot vaqtinchalik"),
    ("17", "Qism detali (locked)", "todo", "3 000 / 15 000 / Premium"),
    ("18", "Video player", "todo", "secure streaming"),
    ("19", "To'lov", "todo", "Click/Payme/Uzum/UZCARD/Visa/MC"),
    ("20", "Xabarlar", "part", "bo'sh holat"),
    ("21", "Foydalanuvchi profili", "done", "mehmon holati"),
    ("22", "Creator Studio 50/50", "todo", "withdraw"),
    ("23", "Premium", "todo", "4 tarif"),
    ("24", "Sozlamalar", "todo", ""),
    ("25", "Admin Panel", "out", "web — bizning qism emas"),
    ("26", "Stars / reyting", "new", "ТЗ'da yo'q, 13.08 da qo'shildi"),
]
ST = {"done": (SUCCESS, "TAYYOR"), "part": (GOLD, "QISMAN"), "todo": (BORDER, "BOSHLANMAGAN"),
      "out": (DISABLED, "BIZNIKI EMAS"), "new": (MAGENTA, "YANGI")}


def f_inventory(fx, fy, h):
    frame(fx, fy, FW, h, "08 · EKRANLAR INVENTARI (26)")
    shape(fx, fy, FW, h, "", fill=PANEL, border=BORDER, bw="1")
    head(fx, fy, "08 · EKRANLAR INVENTARI", "ТЗ V3'dagi 26 ekran — eng to'liq ro'yxat · holat bugungi kodga qarab", SUCCESS)
    x, y = fx + PAD, fy + 180
    for i, (n, c) in enumerate([("TAYYOR", SUCCESS), ("QISMAN", GOLD), ("BOSHLANMAGAN", BORDER),
                                ("BIZNIKI EMAS", DISABLED), ("YANGI (ТЗ'dan tashqari)", MAGENTA)]):
        shape(x + i * 560, y, 540, 68, "<p><strong>%s</strong></p>" % n,
              fill=SURFACE2, border=c, color=c, size=15, align="center", valign="middle")
    cw, ch, gx, gy = 500, 150, 24, 22
    for i, (num, name, st, note) in enumerate(SCREENS_INV):
        r, c = divmod(i, 8)
        col, label = ST[st]
        body = "<p><strong>%s · %s</strong></p><p>&nbsp;</p><p>%s</p>" % (num, name, label)
        if note:
            body += "<p>%s</p>" % note
        shape(x + c * (cw + gx), y + 100 + r * (ch + gy), cw, ch, body,
              fill=SURFACE, border=col, color=WHITE, size=13)
    shape(x, y + 100 + 4 * (ch + gy), FW - PAD * 2, 150,
          P("<strong>TAYYORLIK QOIDASI</strong>",
            "Ekran, unga tegishli barcha holatlar qoplanmaguncha tayyor hisoblanmaydi: "
            "<strong>loading · empty · error · success · locked · purchased · disabled · offline</strong>.",
            "Yangi ekran happy path'dan emas, holatlardan boshlanadi. "
            "Matnlar — faqat <strong>t()</strong> orqali. Ranglar — faqat token orqali, komponentda HEX yozilmaydi."),
          fill=SURFACE2, border=CYAN, color=WHITE, size=14)


def f_roadmap(fx, fy, h):
    frame(fx, fy, FW, h, "09 · ROADMAP")
    shape(fx, fy, FW, h, "", fill=PANEL, border=BORDER, bw="1")
    head(fx, fy, "09 · ROADMAP", "Besh bosqich · tartib biznes qiymati bo'yicha", PURPLE)
    x, y = fx + PAD, fy + 180
    stages = [
        ("BOSQICH 0", "FUNDAMENT", SUCCESS, "✅ BAJARILDI",
         ["Expo SDK 57 + expo-router + TS", "NativeWind, ТЗ palitrasi",
          "Provayderlar: TanStack Query, i18n,", "SafeArea, qorong'i tema",
          "5 tabli tab-bar", "Primitivlar: Button, Badge,", "Screen, ScreenState",
          "UZ/RU lokalizatsiya", "Android build o'tadi, tiplar toza"]),
        ("BOSQICH 1", "DIZAYN", MAGENTA, "⬅ ISHLAB CHIQISHNI BLOKLAYDI",
         ["ТЗ: «Figma — final source of truth,", "ishlab chiqish maketlar", "tasdiqlangandan keyin»", "",
          "✅ Yangi.TV tahlili → karta", "⬜ Axborot arxitekturasi", "⬜ Asosiy oqimlar wireframe",
          "⬜ High-fidelity Figma:", "&nbsp;&nbsp; hamma ekran × holatlar", "⬜ Komponentlar, tipografika",
          "⬜ Prototip → tasdiqlash"]),
        ("BOSQICH 2", "BACKEND KONTRAKTI", CYAN, "qisman ketmoqda",
         ["✅ Endpointlar tahlili,", "&nbsp;&nbsp; ijodkorlar katalogi ulandi",
          "⬜ Shaxsiy ma'lumotlar masalasi", "⬜ SecurityConfig tuzatish",
          "⬜ Yo'nalishlar spravochnigi (4/10)", "⬜ API kontrakt: premyeralar,",
          "&nbsp;&nbsp; xarid, entitlement, to'lovlar,", "&nbsp;&nbsp; chat, balans, Studio, Stars",
          "⬜ To'lov webhook va tekshiruv"]),
        ("BOSQICH 3", "ANDROID · UZ/RU", GOLD, "ТЗ bo'yicha 1-bosqich",
         ["⬜ Auth: telefon+OTP yoki Google", "⬜ Home + kataloglar + profil",
          "⬜ Casting: lenta, filtr, detal,", "&nbsp;&nbsp; ariza, status",
          "🔴 Premyera → qism → to'lov →", "&nbsp;&nbsp; entitlement → player",
          "&nbsp;&nbsp; ← MONETIZATSIYA YADROSI", "⬜ Creator Studio: 50/50, yechish",
          "⬜ Xabarlar + push deep-link", "⬜ Premium, Stars, sozlamalar",
          "⬜ QA · Google Play reliz"]),
        ("BOSQICH 4", "iOS · ENGLISH", DISABLED, "ТЗ bo'yicha 2-bosqich",
         ["⬜ iOS build, qurilmalarda tekshirish", "⬜ EN lokal",
          "⬜ Sign in with Apple", "&nbsp;&nbsp; (boshqa sotsiologin bo'lsa majburiy)",
          "⬜ Advanced monetization", "⬜ App Store reliz"]),
    ]
    cw = 840
    for i, (num, title, col, status, items) in enumerate(stages):
        shape(x + i * (cw + 20), y, cw, 620,
              "<p><strong>%s</strong></p><p><strong>%s</strong></p><p>&nbsp;</p><p>%s</p><p>&nbsp;</p>%s"
              % (num, title, status, P(*items)), fill=SURFACE, border=col, color=WHITE, size=14)
    shape(x, y + 650, FW - PAD * 2, 220,
          P("<strong>TEXNIK STEK</strong>",
            "Expo SDK 57 (RN 0.86, React 19.2.3) · expo-router · NativeWind 4.2 + Tailwind 3.4 · TanStack Query · "
            "Zustand · axios · i18next + expo-localization · expo-video · expo-secure-store · expo-web-browser", "",
            "<strong>⚠ ALLAQACHON DUCH KELINGAN TUZOQLAR</strong>",
            "· SDK 56'dan expo-router @react-navigation/* bilan to'g'ridan-to'g'ri mos emas — faqat "
            "<strong>expo-router/react-navigation</strong>, expo-router/js-tabs, js-stack orqali import qilinadi.",
            "· babel.config.js <strong>babel-preset-expo</strong>ni devDependencies'da talab qiladi — SDK 57 shablonida u yo'q.",
            "· app.json → <strong>scheme: \"uzcasting\"</strong> — to'lov webview'idan qaytish uchun majburiy, o'chirilmaydi."),
          fill=SURFACE2, border=PURPLE, color=WHITE, size=14)


def f_backend(fx, fy, h):
    frame(fx, fy, FW, h, "10 · BACKEND VA API")
    shape(fx, fy, FW, h, "", fill=PANEL, border=BORDER, bw="1")
    head(fx, fy, "10 · BACKEND VA API", "Loyiha noldan emas: Spring backend, sayt, Telegram bot va admin panel ishlayapti", CYAN)
    x, y = fx + PAD, fy + 180
    ents = ["User", "CreatorProfile", "Category", "Portfolio", "Casting", "Application", "Content",
            "Series", "Episode", "Purchase", "PaymentTransaction", "Subscription", "RevenueLedger",
            "Payout", "Message", "Notification", "Report"]
    text(x, y, 2000, "<p><strong>ТЗ SUShNOSTLARI</strong></p>", 20, WHITE)
    ew = 240
    for i, e in enumerate(ents):
        shape(x + i * (ew + 8), y + 44, ew, 56, "<p>%s</p>" % e,
              fill=SURFACE2, border=BORDER, color=WHITE, size=12, align="center", valign="middle")

    y2 = y + 130
    card(x, y2, 2140, 300, "✅ ALLAQACHON ISHLAYDI",
         ["Baza: <strong>https://uzcasting.site</strong>", "",
          "GET /api/v1/casting-user/web — anketalar katalogi",
          "GET /api/v1/file/getFile/{id} — foto",
          "GET /api/v1/news",
          "POST /api/v1/auth/google — ID-token → JWT",
          "POST /api/v1/casting-user — anketa yaratish",
          "POST /api/v1/file/upload",
          "GET /api/v1/casting-user/my/ — mening arizalarim", "",
          "Bosh sahifadagi ijodkorlar katalogi haqiqiy API'ga ulangan (src/features/creators)."],
         SUCCESS, size=14)
    card(x + 2160, y2, 2120, 300, "❌ UMUMAN YO'Q",
         ["premyeralar, seriallar, qismlar, treylerlar",
          "qism xaridi, entitlement, to'lov statusi",
          "to'lovlar: Click / Payme / Uzum / UZCARD / Visa",
          "chat va xabarlar",
          "balans, to'lovlar tarixi, pul yechish",
          "Creator Studio: kontent, statistika, 50/50",
          "Premium obuna",
          "oddiy foydalanuvchi avtorizatsiyasi",
          "push bildirishnomalar",
          "Stars: balans, tranzaksiya, reyting", "",
          "Hozircha <strong>src/lib/placeholder.ts</strong>da yashaydi"],
         DANGER, size=14)

    y3 = y2 + 320
    shape(x, y3, 2140, 300,
          P("<strong>🔴 XAVFSIZLIK: KIRISH QOIDALARI BUTUNLAY O'CHIRILGAN</strong>", "",
            "<strong>backend/.../Security/SecurityConfig.java</strong>",
            ".requestMatchers(HttpMethod.GET, \"/**\").permitAll()",
            ".requestMatchers(HttpMethod.DELETE, \"/**\").permitAll()",
            ".requestMatchers(HttpMethod.PUT, \"/**\").permitAll()",
            ".requestMatchers(HttpMethod.POST, \"/**\").permitAll()", "",
            "Tokensiz <strong>barcha yo'llarda barcha metodlar</strong> ruxsat etilgan — DELETE va admin "
            "endpointlar ham. JWT filtr yozilgan va ishlaydi, lekin bu qoidalar uni ma'nosiz qiladi.",
            "<strong>Butun API ochiq, yozishga ham. Klientdagi himoya hech narsani anglatmaydi.</strong>"),
          fill=DARKRED, border=DANGER, color=WHITE, size=14)
    shape(x + 2160, y3, 2120, 300,
          P("<strong>🔴 OCHIQ ENDPOINTDA SHAXSIY MA'LUMOTLAR</strong>", "",
            "GET /api/v1/casting-user/web avtorizatsiyasiz <strong>phone, email, telegram, instagram</strong>, "
            "aniq tug'ilgan sana va tana o'lchovlarini qaytaradi — <strong>voyaga yetmaganlarniki ham</strong> "
            "(chiqishda age: 17 bo'lgan anketa bor).", "",
            "Har qanday brauzer yoki curl'da ko'rinadi. Javob — ro'yxatga 68 KB.", "",
            "<strong>Taklif:</strong> ochiq endpointda faqat vitrina maydonlari, kontaktlar — alohida "
            "avtorizatsiyalangan so'rov orqali. Bu ayni paytda javob hajmini ham kamaytiradi."),
          fill=DARKRED, border=DANGER, color=WHITE, size=14)

    y4 = y3 + 320
    shape(x, y4, FW - PAD * 2, 160,
          P("<strong>⚠ KATEGORIYALAR MOS KELMAYDI: 10 TADAN 4 TASI</strong>", "",
            "API'da bor: <strong>actor · model · bloger · influencer</strong> &nbsp;&nbsp;|&nbsp;&nbsp; "
            "API'da yo'q: musiqachilar · raqqoslar · foto/video · styling · kurslar · casting e'lonlari "
            "&nbsp;&nbsp;|&nbsp;&nbsp; API'da bor, ТЗ'da yo'q: <strong>euromodel · extra</strong> (massovka)",
            "<strong>Savol:</strong> baza'dagi castingType'ni 10 yo'nalishgacha kengaytiramizmi, yoki ТЗ'ni "
            "haqiqiy turlarga moslashtiramizmi? Kategoriyalar ekraniga va barcha kataloglarga ta'sir qiladi. "
            "Kodda qayd etilgan: <strong>src/features/catalog/categories.ts</strong> — apiType maydoni, null = tur yo'q."),
          fill=SURFACE2, border=GOLD, color=WHITE, size=14)


def f_risks(fx, fy, h):
    frame(fx, fy, FW, h, "11 · RISKLAR VA SAVOLLAR")
    shape(fx, fy, FW, h, "", fill=PANEL, border=BORDER, bw="1")
    head(fx, fy, "11 · RISKLAR VA SAVOLLAR", "Tartiblangan: avval arxitektura yoki iqtisodni buzadiganlari", DANGER)
    x, y = fx + PAD, fy + 180
    blockers = [
        ("🔴 BLOKER 1", "Store billingi", ["Seriyalar, premyeralar, obuna va Stars — ilova ichidagi raqamli kontent.",
                                           "Google/Apple o'z billingini talab qiladi: 15–30%.",
                                           "Click/Payme/Uzum qoidani buzadi → ilova olib tashlanadi.", "",
                                           "Komissiya 50/50 dan OLDIN yechiladi.",
                                           "Aylanma yo'l: faqat saytda sotish, ilovada — ko'rish.",
                                           "Ishlaydi, lekin konversiyani keskin kamaytiradi.", "",
                                           "<strong>To'lov moduli yozilishidan OLDIN hal qilinsin.</strong>"]),
        ("🔴 BLOKER 2", "Butun API ochiq", ["SecurityConfig: barcha metod va yo'llarda permitAll.",
                                            "DELETE va admin endpointlar tokensiz ochiq.", "",
                                            "Tuzatish: permitAll faqat auth, GET news, GET file va",
                                            "vitrinaga qoldiriladi; qolgani .authenticated() + rollar.", "",
                                            "<strong>Backend ishi, lekin hamma bilishi kerak:</strong>",
                                            "<strong>hozircha klientdagi himoya hech narsani anglatmaydi.</strong>"]),
        ("🔴 BLOKER 3", "Voyaga yetmaganlar ma'lumoti", ["Ochiq endpoint telefon, email, aniq tug'ilgan sana va",
                                                          "tana o'lchovlarini qaytaradi — age: 17 anketalar ham.", "",
                                                          "Mobil ilova bu maydonlarni ataylab olmaydi, lekin",
                                                          "muammo backend tomonida — klient hal qila olmaydi.", "",
                                                          "<strong>Bu texnik qarz emas, yuridik risk.</strong>"]),
    ]
    bw = 1420
    for i, (tag, title, ls) in enumerate(blockers):
        shape(x + i * (bw + 20), y, bw, 300,
              "<p><strong>%s · %s</strong></p><p>&nbsp;</p>%s" % (tag, title, P(*ls)),
              fill=DARKRED, border=DANGER, color=WHITE, size=14)

    y2 = y + 330
    text(x, y2, 2600, "<p><strong>BUYURTMACHIGA SAVOLLAR</strong> — javobsiz bu ekranlar loyihalanmaydi</p>", 22, GOLD)
    qs = [("Stars narxi qancha?", "Kurs aytilmagan. 10 Stars — necha so'm?", GOLD),
          ("Stars ijodkor daromadimi?", "Pul qilib yechsa bo'ladimi? 50/50 amal qiladimi, yoki sof reputatsion mexanikami?", GOLD),
          ("15 000 premyera — kelajak qismlarga?", "«mavjud qismlarini» deyilgan. Serial davom etsa — xaridor yana to'laydimi?", GOLD),
          ("Reklama", "Premium «reklamasiz» bo'lsa, qolganlarda bor. Qaysi tarmoq, formatlar, joylar? ТЗ'da yo'q.", GOLD),
          ("Obuna avtoprodleniesi?", "Ha yoki yo'q. To'lov integratsiyasiga va store talablariga ta'sir qiladi.", GOLD),
          ("ТЗ va maketlardagi 5 000", "3 000 yakuniy ekanini va 4 ta PDF eskirgan hisoblanishini tasdiqlash.", GOLD),
          ("«Styling» va «Kurslar»", "Ularga qolgan 7 yo'nalish kabi alohida katalog kerakmi? ТЗ V3'da ular yo'q.", CYAN),
          ("Kategoriyalar: 10 mi yoki 4?", "Baza'dagi castingType'ni 10 gacha kengaytiramizmi, yoki ТЗ'ni faktga moslaymizmi?", CYAN),
          ("Lotin yoki kirill?", "ТЗ hujjatlari kirillda, maket yozuvlari lotinda. Hozir kodda lotin.", CYAN),
          ("Qaytarish (refund)", "ТЗ refund status'ni eslatadi, lekin qoidalarni yozmagan. Sotib olingan seriyani qaytarish mumkinmi?", CYAN),
          ("Kontent himoyasi", "ТЗ «access-controlled streaming» talab qiladi. DRM (Widevine) kerakmi yoki qisqa TTL'li signed URL yetarlimi? Narx va muddat farqi katta.", MAGENTA),
          ("Verified badge", "Uni kim va qanday mezon bo'yicha beradi?", MAGENTA)]
    qw, qh = 1050, 190
    for i, (q, a, col) in enumerate(qs):
        r, c = divmod(i, 4)
        shape(x + c * (qw + 20), y2 + 50 + r * (qh + 20), qw, qh,
              P("<strong>%s</strong>" % q, "", a), fill=SURFACE, border=col, color=WHITE, size=13)

    y3 = y2 + 50 + 3 * (qh + 20)
    shape(x, y3, FW - PAD * 2, 170,
          P("<strong>⚠ V4 MAKETLARI VYORSTKA UCHUN REFERENS BO'LA OLMAYDI</strong>", "",
            "PDF'dagi rasmlar — AI generatsiya qilgan kollajlar. Har bir «ekran» ichiga bir xil mayda 10 ta "
            "telefon to'ri yopishtirilgan, matn o'qib bo'lmaydi, hero-bannerda harflar rasm ustiga chiqib ketgan.",
            "<strong>Ishonchli tarzda faqat shular olinadi:</strong> tab-bar tarkibi, to'q binafsha gamma, "
            "yumaloqlangan kartochkalar, pushti «ПРЕМЬЕРА» bejiligi, oltin pul yechish tugmasi. "
            "Haqiqiy maketlar Figma'da noldan qilinadi — 1-bosqich."),
          fill=SURFACE2, border=GOLD, color=WHITE, size=14)


# ====================================================================
def probe():
    info = call("GET", "")
    print("board:", info.get("name"), "|", info.get("id"))
    items = call("GET", "/items?limit=50")
    d = items.get("data", [])
    print("mavjud elementlar:", len(d))
    for it in d[:50]:
        pos = it.get("position") or {}
        print("  ", it["type"], it["id"], round(pos.get("x", 0)), round(pos.get("y", 0)),
              json.dumps(it.get("data", {}), ensure_ascii=False)[:80])


def build():
    X0 = 0
    y = 0

    # sarlavha
    shape(X0, y, FW, 260, "", fill=INK, border=PURPLE, bw="4")
    text(X0, y + 45, FW, "<p><strong>UZCASTING — MOBIL DIZAYN VA KETMA-KETLIKLAR</strong></p>", 54, WHITE, "center")
    text(X0, y + 130, FW, "<p>22 ta ekran wireframe'i · 5 ta oqim · dizayn tizimi · roadmap · risklar</p>", 24, MUTED, "center")
    text(X0, y + 178, FW, "<p>1-bosqich: Android + UZ/RU &nbsp;·&nbsp; 2-bosqich: iOS + EN &nbsp;·&nbsp; "
                          "Figma — final source of truth</p>", 19, CYAN, "center")
    y += 320

    f_design(X0, y, 1150); y += 1250
    f_product(X0, y, 1180); y += 1280

    flows = [
        ("03 · KETMA-KETLIK 1 — KIRISH", "Splash → Onboarding → Telefon → OTP → Home", PURPLE, screens_auth),
        ("04 · KETMA-KETLIK 2 — HOME → KATALOG → IJODKOR", "Bosh sahifadan ijodkor profiligacha", CYAN, screens_catalog),
        ("05 · KETMA-KETLIK 3 — CASTING", "Lenta → detal → ariza → status", MAGENTA, screens_casting),
        ("06 · KETMA-KETLIK 4 — MONETIZATSIYA YADROSI", "Premyera → qism → daraja → to'lov → entitlement → player", GOLD, screens_money),
        ("07 · KETMA-KETLIK 5 — PROFIL / STUDIO / PREMIUM / STARS", "Xabarlar, profil, 50/50, tariflar, donat", GOLD, screens_profile),
    ]
    for title, sub, col, fn in flows:
        h = 1330
        frame(X0, y, FW, h, title)
        shape(X0, y, FW, h, "", fill=PANEL, border=BORDER, bw="1")
        head(X0, y, title, sub, col)
        fn(X0 + PAD, y + 240)
        y += h + 100

    f_inventory(X0, y, 1080); y += 1180
    f_roadmap(X0, y, 1080); y += 1180
    f_backend(X0, y, 1180); y += 1280
    f_risks(X0, y, 1400)


if __name__ == "__main__":
    mode = sys.argv[1] if len(sys.argv) > 1 else "probe"
    if mode == "probe":
        probe()
    else:
        build()
        print("OK — yaratildi:", len(created), "element")
