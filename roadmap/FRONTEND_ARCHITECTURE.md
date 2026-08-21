# UZCASTING FRONTEND ARCHITECTURE

> Hozirgi holat + qurilayotgan admin panel arxitekturasi.
> Task ro'yxati → [FRONTEND_ROADMAP.md](./FRONTEND_ROADMAP.md)

Oxirgi yangilanish: 19.08.2026 — admin panel qurildi

---

## 1. Current stack

React 18.3.1 · Create React App 5 (`react-scripts`) · **JavaScript** (TypeScript emas).

```bash
npm install --legacy-peer-deps   # eski dep'lar React 18 bilan peer conflict beradi
npm start                        # dev
npm run build                    # prod → build/
```

⚠️ `--legacy-peer-deps` majburiy: `react-reveal`, `react-bootstrap-carousel` va
`react-dice-roll` React 18 ni peer sifatida qo'llab-quvvatlamaydi.

Build natijasi backend'ning `src/main/resources/static/` ichiga ko'chiriladi —
bitta jar butun saytni beradi.

---

## 2. Routing

`react-router-dom` v6. **Barcha route `src/App.js` ichida** — bitta faylda.

```
/                       public landing
/models                 anketa katalogi
/aadmin/*               sayt admin (src/admin/)
/admin/*                Telegram bot admin (src/bot-admin/)
/bot/:userId · /data-form/:userId · /history/:userId · /appeal/:userId
```

**Yangi admin panel:** `/app/panel/*` prefiksi, `src/adminpanel/routes.jsx` da —
`App.js` shishib ketmasligi uchun.

---

## 3. Layouts

Hozir umumiy layout yo'q — har bir admin sahifa `HeaderAdmin` va `Sidebar`ni
o'zi import qiladi.

**Reja:** `AdminLayout` — sidebar (role/permission bo'yicha filtrlanadi) + header +
`<Outlet />`. Sahifa faqat o'z mazmunini yozadi.

---

## 4. Auth

**Hozir:** token `localStorage.access_token`, `Authorization` header'ga
**Bearer prefiksisiz** qo'yiladi (`config/index.js:22`). Backend `JwtService.normalizeToken`
ikkala variantni ham qabul qiladi.

⚠️ **B4 — guard ishlamaydi.** `App.js:31` da `blockedPages = ["/dashboard"]`, lekin
bunday route yo'q. Haqiqiy admin sahifalar `/aadmin/*` — ya'ni `checkSecurity()`
hech qachon ishga tushmaydi va admin sahifalar frontendda qo'riqlanmagan.

**Qurildi:** `AuthContext` — token, user, rol, ruxsatlar. Sahifa yangilanganda
`/api/v1/app/admin/auth/me` bilan profil serverdan tiklanadi (rol yoki ruxsat
o'zgargan bo'lishi mumkin — `localStorage` nusxasiga ishonilmaydi).
Token kaliti alohida: `uzpanel.access_token` — sayt admini bilan aralashmaydi.

Frontend guard — faqat UX. **Haqiqiy himoya backendda**: brauzerda tekshirildi,
WORKER `/app/panel/staff` ga kirsa backend `403 ACCESS_DENIED` qaytaradi.

---

## 5. Permissions

`PermissionGuard` komponenti:

```jsx
<PermissionGuard permission="CONTENT_CREATE">
  <Button>Yangi kontent</Button>
</PermissionGuard>
```

Sidebar menyu ham shu asosda filtrlanadi. Lekin menyuni yashirish **xavfsizlik emas** —
backend baribir 403 qaytaradi (§11).

---

## 6. API layer

**Hozir:** `src/config/index.js` — bitta default export funksiya:

```js
ApiCall(url, method, data, param, isMultipart, onUploadProgress)
  → { error: boolean, data: any }
```

Xatoni `throw` qilmaydi, `{error: true}` qaytaradi. Token'ni `localStorage`dan oladi.

**Reja:** shu pattern saqlanadi (jamoa tanish), lekin `adminpanel/api/`da modul bo'yicha
bo'linadi: `staffApi`, `contentApi`, `mediaApi`... Har biri `client.js` ustida quriladi.
401 → logout, refresh, retry — bitta joyda.

---

## 7. Server state

**Hozir yo'q.** Har bir sahifa `useState` + `useEffect` + `ApiCall` yozadi.
Kesh yo'q, retry yo'q, invalidation yo'q.

**Hozircha:** `adminpanel/api/useApi.js` — minimal hook (loading / error / reload).
Kesh yo'q, lekin sahifalar unga bog'langan, shuning uchun TanStack Query
qo'shilsa faqat shu hook almashadi va sahifalar o'zgarmaydi.

TanStack Query mobil loyihada (`mobile/`) allaqachon ishlatiladi, ya'ni jamoa
uchun yangi emas. Qo'shilganda qaror `roadmap.md → Important Decisions` ga yoziladi.

---

## 8. Forms

Form kutubxonasi yo'q — controlled input'lar qo'lda.
Validatsiya frontend + backend ikkalasida (§52). Xatolar field yonida.

---

## 9. Tables

Reusable table yo'q — har bir admin sahifa o'z `<table>`ini yozadi.

**Reja:** `DataTable` + `Pagination` + `SearchInput` + `FilterPanel`.
Har bir list sahifada majburiy: search · filter · pagination · sorting ·
loading · empty · error · retry (§51).

---

## 10. Media upload

**Hozir:** `ApiCall(..., isMultipart=true, onUploadProgress)` — progress callback bor.

**Reja:** `MediaUploader` komponenti — drag&drop, progress, preview, katta video uchun
chunk/stream. Media Library'dan reuse (`ImagePicker`).

---

## 11. Design system — QURILDI

Manba: `src/adminpanel/theme/panel.css`. **Hex ranglar faqat shu faylda** (§50).

### Palitra — TO'Q KO'K

Buyurtmachi qarori (19.08.2026). Bu mobil ilova palitrasi EMAS: u yerda ТЗ V2
(18-bet) bo'yicha tasdiqlangan to'q binafsha/qora gamma qoladi. Admin panel —
alohida mahsulot yuzasi, shuning uchun ziddiyat yo'q.

| Token | HEX | Roli |
|---|---|---|
| `--p-bg` | `#070E20` | Fon |
| `--p-surface` | `#0C1730` | Kartochka |
| `--p-surface-2` | `#12203F` | Input, ko'tarilgan sirt |
| `--p-border` | `#1E3163` | Chegara |
| `--p-primary` | `#2F6BFF` | Asosiy CTA |
| `--p-accent` | `#38BDF8` | Info |
| `--p-gold` | `#F5C542` | Premium / Reels bejagi |
| `--p-success` / `--p-warning` / `--p-danger` | `#34D399` / `#FBBF24` / `#F87171` | Statuslar |
| `--p-text` / `--p-muted` / `--p-disabled` | `#E9EFFB` / `#8FA3C8` / `#566C99` | Matn |

Komponent klasslari: `.uz-card`, `.uz-btn`, `.uz-input`, `.uz-badge-*`,
`.uz-table`, `.uz-sidebar`, `.uz-nav-item`, `.uz-skeleton`.

### ⚠️ Mavjud sayt CSS'i bilan uchta ziddiyat

Bular topildi va tuzatildi — yangi komponent yozayotganda esda tuting:

1. **`* { font-family: 'Lora', serif !important }`** (`pages/home/home.css:17`)
   butun saytga sizadi. Panel `.uz-panel *` ga `!important` bilan javob beradi.
2. **Tailwind bilan aniqlik teng** — `.uz-btn` va `.lg:hidden` ikkalasi ham (0,1,0),
   panel.css keyinroq yuklanadi va yutadi. Shuning uchun panelda Tailwind'ning
   `lg:hidden` / `fixed` kabi klasslari ISHLATILMAYDI — o'z media so'rovlarimiz bor
   (`.uz-menu-btn`, `.uz-scrim`, `.uz-sidebar`).
3. **Chrome autofill** input'ni oq qiladi — `:-webkit-autofill` da ichki soya bilan bosiladi.

### Responsivlik

Desktop-first. Sinish nuqtasi 1024px: yon panel siljib chiqadigan menyuga aylanadi,
kontent to'liq kenglikka o'tadi, menyu tugmasi paydo bo'ladi. 640px da tipografika
va jadval paddinglari kichrayadi. Keng jadvallar `.uz-table-wrap` ichida gorizontal
siljiydi — sahifa o'zi hech qachon siljimaydi.

⚠️ Brauzerda tasdiqlanmagan — sinov muhitida viewport qotib qolgan edi.

---

## 12. Shared components

```
DataTable · Pagination · SearchInput · FilterPanel · StatusBadge
ConfirmDialog · MediaUploader · ImagePicker · VideoUploader
PermissionGuard · PageHeader · EmptyState · ErrorState · LoadingState
InternalTargetPicker · MoneyInput · SortableList
```

Joylashuv: `src/adminpanel/components/`.

---

## 13. i18n

`i18next` + `react-i18next` mavjud (`src/i18next.js`). Tillar: UZ · RU · EN.
Admin panel matnlari ham `t()` orqali.

---

## 14. Papka rejasi

```
src/
├── admin/            ⚠️ mavjud — casting anketalari admini, TEGILMADI
├── bot-admin/        ⚠️ mavjud — Telegram bot admini, TEGILMADI
├── pages/            ⚠️ mavjud — public sahifalar, TEGILMADI
├── config/index.js   ⚠️ mavjud axios wrapper — sayt uchun, o'z joyida
└── adminpanel/       ← YANGI UZCASTING admin paneli
    ├── PanelApp.jsx        marshrutlar + provayderlar
    ├── i18n.js             UZ/RU/EN, 100+ kalit
    ├── theme/panel.css     dizayn tokenlari (to'q ko'k)
    ├── api/
    │   ├── client.js       axios + 401 ushlash + tokenStore
    │   └── useApi.js       loading/error/reload hook
    ├── auth/
    │   ├── AuthContext.jsx rol, ruxsat, can(), atLeast()
    │   └── Guards.jsx      RequireAuth, RequirePermission
    ├── layout/AdminLayout.jsx
    ├── components/
    │   ├── States.jsx      Loading/Empty/Error/Forbidden
    │   └── Ui.jsx          PageHeader, StatusBadge, SearchInput,
    │                       Pagination, LanguageSwitcher, TableWrap
    └── pages/              Login, Dashboard, Content, Creators,
                            Taxonomy (category+genre), Media, Staff
```

`App.js` ga faqat BITTA qator qo'shildi: `<Route path="/app/panel/*" element={<PanelApp />} />`.
