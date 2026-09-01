/**
 * Yo'riqnoma mazmuni.
 *
 * <h2>⚠️ Nega `i18n.js` da emas</h2>
 * Bu 40 dan ortiq band, har biri sarlavha + tavsif + qadamlar. Ular
 * `i18n.js` ga tekis kalitlar bo'lib tushsa, fayl 300 qatorga o'sardi
 * va yo'riqnomani tahrirlash uchun uchta joyni izlash kerak bo'lardi.
 *
 * Bu yerda esa bir mavzu — bir obyekt, uchala til yonma-yon.
 *
 * <h2>Tuzilishi</h2>
 * Bo'limlar yon menyu bilan BIR XIL tartibda. Admin yo'riqnomada
 * ko'rgan narsasini menyudan darhol topishi kerak.
 *
 * `perm` — shu ishni bajarish uchun kerakli ruxsat. `null` bo'lsa
 * ruxsat talab qilinmaydi. Sahifa shu asosda kimga nima
 * ko'rsatishini hal qiladi.
 */

export const GUIDE = [
  // ─────────────────────────────────────────────── Asosiy
  {
    group: { uz: 'Asosiy', ru: 'Основное', en: 'Main' },
    topics: [
      {
        id: 'dashboard',
        perm: null,
        icon: '▦',
        title: { uz: 'Boshqaruv paneli', ru: 'Панель управления', en: 'Dashboard' },
        what: {
          uz: 'Platformaning umumiy holati: kontent soni, foydalanuvchilar, daromad va oxirgi harakatlar.',
          ru: 'Общее состояние платформы: количество контента, пользователи, доход и последние действия.',
          en: 'Overall platform state: content counts, users, revenue and recent activity.',
        },
        steps: {
          uz: [
            'Panelga kirganingizda birinchi ochiladigan sahifa shu.',
            'Raqamlar bo\'sh bo\'lsa — ma\'lumot hali yo\'q, xato emas.',
          ],
          ru: [
            'Это первая страница после входа в панель.',
            'Пустые цифры — данных ещё нет, это не ошибка.',
          ],
          en: [
            'This is the first page after you sign in.',
            'Empty numbers mean there is no data yet — not an error.',
          ],
        },
      },
      {
        id: 'reports',
        perm: 'REPORT_VIEW',
        icon: '📊',
        title: { uz: 'Hisobotlar', ru: 'Отчёты', en: 'Reports' },
        what: {
          uz: 'Ko\'rishlar, daromad, reklama va bildirishnomalar bo\'yicha statistika.',
          ru: 'Статистика по просмотрам, доходу, рекламе и уведомлениям.',
          en: 'Statistics for views, revenue, advertising and notifications.',
        },
        steps: {
          uz: [
            'Yuqoridan davrni tanlang: hafta, oy yoki o\'z oralig\'ingiz.',
            'Kontent yoki reklama qatoriga bosing — batafsil oyna ochiladi.',
            'Ma\'lumot yo\'q bo\'lsa bo\'sh ko\'rsatiladi. Panel raqamni o\'ylab topmaydi.',
          ],
          ru: [
            'Выберите период сверху: неделя, месяц или свой диапазон.',
            'Нажмите на строку контента или рекламы — откроется подробное окно.',
            'Если данных нет, показывается пусто. Панель не выдумывает цифры.',
          ],
          en: [
            'Pick a period at the top: week, month or a custom range.',
            'Click a content or ad row to open the detailed view.',
            'When there is no data it shows empty. The panel never invents numbers.',
          ],
        },
      },
    ],
  },

  // ─────────────────────────────────────────────── Katalog
  {
    group: { uz: 'Katalog', ru: 'Каталог', en: 'Catalogue' },
    topics: [
      {
        id: 'content-create',
        perm: 'CONTENT_CREATE',
        icon: '🎬',
        title: { uz: 'Yangi kontent qo\'shish', ru: 'Добавить контент', en: 'Add content' },
        what: {
          uz: 'Film, serial, shou, klip yoki podkast yaratish.',
          ru: 'Создание фильма, сериала, шоу, клипа или подкаста.',
          en: 'Create a film, series, show, clip or podcast.',
        },
        steps: {
          uz: [
            '«Kontent» bo\'limiga o\'ting va «Yangi» tugmasini bosing.',
            'Turini tanlang (film, serial…) va tuzilishini: yaxlit yoki qismli.',
            '⚠️ Sarlavha va tavsifni UCHALA tilda to\'ldiring — mobil ilova uch tilda ishlaydi.',
            'Afisha rasmini biriktiring.',
            'Kirish siyosatini tanlang: bepul, premium yoki sotib olinadigan.',
            'Saqlang. Kontent «Qoralama» holatida turadi va ilovada ko\'rinmaydi.',
          ],
          ru: [
            'Откройте раздел «Контент» и нажмите «Новый».',
            'Выберите тип (фильм, сериал…) и структуру: цельный или из серий.',
            '⚠️ Заполните название и описание на ВСЕХ ТРЁХ языках — приложение работает на трёх.',
            'Прикрепите постер.',
            'Выберите политику доступа: бесплатно, премиум или покупка.',
            'Сохраните. Контент будет в статусе «Черновик» и в приложении не появится.',
          ],
          en: [
            'Open “Content” and press “New”.',
            'Pick the type (film, series…) and structure: single or episodic.',
            '⚠️ Fill the title and description in ALL THREE languages — the app runs in three.',
            'Attach a poster image.',
            'Choose the access policy: free, premium or purchase.',
            'Save. The content stays in “Draft” and does not appear in the app.',
          ],
        },
      },
      {
        id: 'content-episodes',
        perm: 'CONTENT_EDIT',
        icon: '🎞',
        title: { uz: 'Fasl va qism qo\'shish', ru: 'Сезоны и серии', en: 'Seasons and episodes' },
        what: {
          uz: 'Serial yoki podkastga qismlar biriktirish.',
          ru: 'Добавление серий к сериалу или подкасту.',
          en: 'Attaching episodes to a series or podcast.',
        },
        steps: {
          uz: [
            'Kontentni oching va «Qismlar» tabiga o\'ting.',
            'Tuzilishi fasllik bo\'lsa avval faslni yarating.',
            '«Yangi qism» — raqami, sarlavhasi (uch tilda) va videosi.',
            'Video bir nechta bo\'lakdan iborat bo\'lsa, ularni tartib bilan qo\'shing.',
            '⚠️ Video yuklangach u DARHOL tayyor bo\'lmaydi — qayta ishlanadi. Holatni media kutubxonasida ko\'rasiz.',
          ],
          ru: [
            'Откройте контент и перейдите на вкладку «Серии».',
            'Если структура с сезонами — сначала создайте сезон.',
            '«Новая серия» — номер, название (на трёх языках) и видео.',
            'Если видео состоит из нескольких частей, добавьте их по порядку.',
            '⚠️ После загрузки видео НЕ готово сразу — оно обрабатывается. Статус виден в медиатеке.',
          ],
          en: [
            'Open the content and go to the “Episodes” tab.',
            'If the structure has seasons, create a season first.',
            '“New episode” — number, title (three languages) and video.',
            'If the video has several parts, add them in order.',
            '⚠️ An uploaded video is NOT ready immediately — it is processed. Watch the status in the media library.',
          ],
        },
      },
      {
        id: 'content-publish',
        perm: 'CONTENT_PUBLISH',
        icon: '✓',
        title: { uz: 'Kontentni chop etish', ru: 'Публикация контента', en: 'Publishing content' },
        what: {
          uz: 'Kontentni ilovada ko\'rinadigan qilish.',
          ru: 'Сделать контент видимым в приложении.',
          en: 'Make content visible in the app.',
        },
        steps: {
          uz: [
            'Kontent muharririda «Chop etish» tabiga o\'ting.',
            'Holatni «Chop etilgan» qiling.',
            'Kelajakdagi sanani belgilasangiz kontent o\'sha vaqtda o\'zi chiqadi.',
            '⚠️ Vaqt Toshkent bo\'yicha hisoblanadi.',
          ],
          ru: [
            'В редакторе контента перейдите на вкладку «Публикация».',
            'Поставьте статус «Опубликовано».',
            'Если указать будущую дату, контент выйдет сам в это время.',
            '⚠️ Время считается по Ташкенту.',
          ],
          en: [
            'In the content editor go to the “Publish” tab.',
            'Set the status to “Published”.',
            'If you set a future date, the content goes live by itself at that time.',
            '⚠️ Times are in Tashkent time.',
          ],
        },
      },
      {
        id: 'creators',
        perm: 'CREATOR_VIEW',
        icon: '★',
        title: { uz: 'Ijodkorlar', ru: 'Креаторы', en: 'Creators' },
        what: {
          uz: 'Aktyorlar, rejissyorlar va boshqa ijodkorlar ro\'yxati.',
          ru: 'Список актёров, режиссёров и других создателей.',
          en: 'Actors, directors and other creators.',
        },
        steps: {
          uz: [
            'Ijodkorni kontentga biriktirish uchun avval uni shu bo\'limda yarating.',
            '⚠️ Ismni uchala tilda kiriting — mobil ilovada uch tilda ko\'rsatiladi.',
            'Keyin kontent muharriridagi «Ijodkorlar» tabida uni tanlang va rolini yozing.',
          ],
          ru: [
            'Чтобы привязать креатора к контенту, сначала создайте его здесь.',
            '⚠️ Имя вводится на трёх языках — в приложении показывается на трёх.',
            'Затем на вкладке «Креаторы» в редакторе выберите его и укажите роль.',
          ],
          en: [
            'To attach a creator to content, create them here first.',
            '⚠️ Enter the name in all three languages — the app shows all three.',
            'Then pick them on the “Creators” tab of the editor and set the role.',
          ],
        },
      },
      {
        id: 'taxonomy',
        perm: 'CATEGORY_VIEW',
        icon: '▤',
        title: { uz: 'Kategoriya va janrlar', ru: 'Категории и жанры', en: 'Categories and genres' },
        what: {
          uz: 'Kontentni guruhlash uchun ro\'yxatlar.',
          ru: 'Списки для группировки контента.',
          en: 'Lists used to group content.',
        },
        steps: {
          uz: [
            'Nom uchala tilda kiritiladi.',
            'Kategoriya — bosh sahifadagi katta bo\'limlar. Janr — filmning turi.',
            '⚠️ Ishlatilayotgan kategoriyani o\'chirib bo\'lmaydi. Avval kontentni boshqasiga o\'tkazing.',
          ],
          ru: [
            'Название вводится на трёх языках.',
            'Категория — крупные разделы главной. Жанр — тип фильма.',
            '⚠️ Используемую категорию удалить нельзя. Сначала перенесите контент.',
          ],
          en: [
            'The name is entered in three languages.',
            'A category is a top-level section of the home screen. A genre is a film type.',
            '⚠️ A category in use cannot be deleted. Move the content first.',
          ],
        },
      },
      {
        id: 'media-upload',
        perm: 'MEDIA_UPLOAD',
        icon: '🖼',
        title: { uz: 'Video va rasm yuklash', ru: 'Загрузка видео и фото', en: 'Uploading video and images' },
        what: {
          uz: 'Media kutubxonasiga fayl qo\'shish.',
          ru: 'Добавление файлов в медиатеку.',
          en: 'Adding files to the media library.',
        },
        steps: {
          uz: [
            '«Media» bo\'limi → «Fayl yuklash».',
            'Video: mp4, mov, webm, m4v. Rasm: jpg, png, webp.',
            '⚠️ mkv va avi qabul qilinadi, LEKIN pleyer ularni ochmaydi. Tomosha uchun mp4 yuklang.',
            'Katta fayl bo\'laklab yuklanadi — sahifani yopsangiz ham davom etadi.',
            'Yuklangach video QAYTA ISHLANADI: sifat variantlari yasaladi. Bu bir necha daqiqa oladi.',
          ],
          ru: [
            'Раздел «Медиа» → «Загрузить файл».',
            'Видео: mp4, mov, webm, m4v. Фото: jpg, png, webp.',
            '⚠️ mkv и avi принимаются, НО плеер их не откроет. Для просмотра загружайте mp4.',
            'Большой файл грузится частями — можно закрыть страницу, загрузка продолжится.',
            'После загрузки видео ОБРАБАТЫВАЕТСЯ: создаются варианты качества. Это занимает несколько минут.',
          ],
          en: [
            '“Media” → “Upload file”.',
            'Video: mp4, mov, webm, m4v. Images: jpg, png, webp.',
            '⚠️ mkv and avi are accepted BUT the player cannot open them. Upload mp4 for viewing.',
            'Large files upload in parts — you can close the page and it continues.',
            'After upload the video is PROCESSED into quality variants. This takes a few minutes.',
          ],
        },
      },
      {
        id: 'media-status',
        perm: 'MEDIA_VIEW',
        icon: '⏳',
        title: { uz: 'Video qayta ishlash holati', ru: 'Статус обработки видео', en: 'Video processing status' },
        what: {
          uz: 'Video tayyor bo\'ldimi yoki yiqildimi — shu yerda ko\'rinadi.',
          ru: 'Готово ли видео или обработка упала — видно здесь.',
          en: 'Whether the video is ready or processing failed.',
        },
        steps: {
          uz: [
            'Media kartochkasidagi nishon holatni ko\'rsatadi: Navbatda → Qayta ishlanmoqda → Video tayyor.',
            'Yiqilgan videolarni topish uchun «Faqat yiqilganlar» belgisini qo\'ying.',
            'Kartochkani bosing — xato sababi va «Qayta urinish» tugmasi ochiladi.',
            '⚠️ «Video tayyor» bo\'lmaguncha foydalanuvchi uni ko\'ra olmaydi.',
          ],
          ru: [
            'Значок на карточке показывает статус: В очереди → Обработка → Видео готово.',
            'Чтобы найти упавшие, поставьте галочку «Только с ошибкой».',
            'Нажмите на карточку — откроется причина ошибки и кнопка «Повторить».',
            '⚠️ Пока не «Видео готово», пользователь его не увидит.',
          ],
          en: [
            'The badge on a card shows the status: Queued → Processing → Video ready.',
            'Tick “Failed only” to find failed videos.',
            'Click a card to see the failure reason and the “Retry” button.',
            '⚠️ Until it says “Video ready”, users cannot watch it.',
          ],
        },
      },
      /*
       * ⚠️ Bu band raqamlarni TAKRORLAMAYDI, ularni tushuntiradi.
       * Aniq o'lchamlar `mediaSpecs.js` da va har bir maydonning
       * YONIDA turadi — u yerda ular kerak bo'lgan onda ko'rinadi.
       * Bu yerda ikkinchi ro'yxat tutilsa, ikkitasi bir-biridan
       * ajralib ketardi va qaysi biri to'g'ri ekani bilinmasdi.
       */
      {
        id: 'media-sizes',
        perm: null,
        icon: '📐',
        title: {
          uz: "Rasm va video o'lchamlari",
          ru: 'Размеры изображений и видео',
          en: 'Image and video sizes',
        },
        what: {
          uz: "Har bir maydon tagida tavsiya etilgan o'lcham yozilgan. Uni bajarish shart emas, lekin bajarilmasa rasm telefonda qirqiladi.",
          ru: 'Под каждым полем написан рекомендуемый размер. Он не обязателен, но при несоблюдении изображение обрежется на телефоне.',
          en: 'Each field shows a recommended size. It is not enforced, but ignoring it means the image gets cropped on the phone.',
        },
        steps: {
          uz: [
            "Maydon tagidagi 📐 yozuvga qarang: «1200×1800 px · 2:3 · JPG/PNG/WebP · ≤2 MB».",
            "⚠️ Server rasmni QAYTA O'LCHAMAYDI — fayl qanday yuklansa, telefonga shundayligicha boradi.",
            "Ilova rasmni ramkaga sig'dirmaydi, ortiqchasini QIRQADI. Nisbat noto'g'ri bo'lsa yuzning chekkasi kesiladi.",
            "Afisha — 1200×1800 (2:3), vertikal Reels uchun 720×1280 (9:16). ⚠️ Kichikroq yuklamang: yopiq kontent ekranida afisha butun ekran kengligida chiziladi.",
            "Reklama banneri — 1280×720 (16:9), muhim narsa markazda. Pastki chap burchakni matn va tugma yopadi.",
            "⚠️ Premyera rasmi banner EMAS: u tik afisha (2:3) bo'lib chiqadi.",
            "Qism kadri — 720×480 (3:2), 16:9 emas: keng kadrning cheti qirqiladi.",
            "Ijodkor surati — 400×400 (1:1), dumaloq qirqiladi, yuz markazda.",
            "Video — 1920×1080 yoki vertikal 1080×1920, MP4 (H.264). 1080p dan kattasi sifat qo'shmaydi.",
          ],
          ru: [
            'Смотрите строку 📐 под полем: «600×900 px · 2:3 · JPG/PNG/WebP · ≤2 MB».',
            '⚠️ Сервер НЕ пережимает изображение — файл уходит на телефон как есть.',
            'Приложение не вписывает картинку в рамку, а ОБРЕЗАЕТ лишнее. При неверной пропорции срежется край лица.',
            'Афиша — 1200×1800 (2:3), для вертикального Reels — 720×1280 (9:16). ⚠️ Меньше не загружайте: на экране закрытого контента афиша рисуется во всю ширину экрана.',
            'Рекламный баннер — 1280×720 (16:9), главное в центре. Левый нижний угол закрывают текст и кнопка.',
            '⚠️ Изображение премьеры — НЕ баннер: оно показывается вертикальной афишей (2:3).',
            'Кадр серии — 720×480 (3:2), а не 16:9: у широкого кадра обрежутся края.',
            'Фото креатора — 400×400 (1:1), обрезается по кругу, лицо по центру.',
            'Видео — 1920×1080 или вертикальное 1080×1920, MP4 (H.264). Больше 1080p качества не добавит.',
          ],
          en: [
            'Look at the 📐 line under the field: “1200×1800 px · 2:3 · JPG/PNG/WebP · ≤2 MB”.',
            '⚠️ The server does NOT resize images — the file reaches the phone exactly as uploaded.',
            'The app does not fit the image into the frame, it CROPS the excess. A wrong ratio cuts off the edge of a face.',
            'Poster — 1200×1800 (2:3); for vertical Reels — 720×1280 (9:16). ⚠️ Do not go smaller: on the locked-content screen the poster is drawn at full screen width.',
            'Ad banner — 1280×720 (16:9), keep the subject centred. The bottom-left corner is covered by text and the button.',
            '⚠️ A premiere image is NOT a banner: it is shown as an upright 2:3 poster.',
            'Episode still — 720×480 (3:2), not 16:9: a wide frame loses its edges.',
            'Creator photo — 400×400 (1:1), cropped to a circle, face centred.',
            'Video — 1920×1080 or vertical 1080×1920, MP4 (H.264). Above 1080p adds no quality.',
          ],
        },
      },
    ],
  },

  // ─────────────────────────────────────────────── Vitrina
  {
    group: { uz: 'Vitrina', ru: 'Витрина', en: 'Storefront' },
    topics: [
      {
        id: 'homepage',
        perm: 'HOMEPAGE_EDIT',
        icon: '▦',
        title: { uz: 'Bosh sahifani sozlash', ru: 'Настройка главной', en: 'Home screen setup' },
        what: {
          uz: 'Ilovaning bosh sahifasidagi bo\'limlar va ularning tartibi.',
          ru: 'Блоки главного экрана приложения и их порядок.',
          en: 'The blocks on the app home screen and their order.',
        },
        steps: {
          uz: [
            'Bo\'limlarni sudrab tartibini o\'zgartiring.',
            'Bo\'limga bosing — ichidagi elementlarni tanlaysiz.',
            '⚠️ Bo\'sh bo\'lim ilovada KO\'RINMAYDI. Sarlavha bo\'lib, ichi bo\'sh qator chiqmaydi.',
          ],
          ru: [
            'Перетаскивайте блоки, чтобы изменить порядок.',
            'Нажмите на блок — выберете, что в нём показывать.',
            '⚠️ Пустой блок в приложении НЕ ПОКАЗЫВАЕТСЯ. Заголовка без содержимого не будет.',
          ],
          en: [
            'Drag blocks to change their order.',
            'Click a block to choose what goes inside it.',
            '⚠️ An empty block does NOT appear in the app. No heading without content.',
          ],
        },
      },
      {
        id: 'ads',
        perm: 'ADVERTISEMENT_VIEW',
        icon: '📢',
        title: { uz: 'Reklama bannerlari', ru: 'Рекламные баннеры', en: 'Ad banners' },
        what: {
          uz: 'Bosh sahifadagi reklama karuseli.',
          ru: 'Рекламная карусель на главной.',
          en: 'The advertising carousel on the home screen.',
        },
        steps: {
          uz: [
            'Banner rasmini va matnini uch tilda kiriting.',
            'Havolani tanlang: tashqi manzil yoki ilova ichidagi kontent.',
            '⚠️ Reklama obunachilarga KO\'RSATILMAYDI — bu ataylab.',
          ],
          ru: [
            'Введите картинку и текст баннера на трёх языках.',
            'Выберите ссылку: внешний адрес или контент внутри приложения.',
            '⚠️ Подписчикам реклама НЕ показывается — так задумано.',
          ],
          en: [
            'Enter the banner image and text in three languages.',
            'Choose the link: an external address or in-app content.',
            '⚠️ Subscribers do NOT see ads — this is intentional.',
          ],
        },
      },
    ],
  },

  // ─────────────────────────────────────────────── Jamoatchilik
  {
    group: { uz: 'Jamoatchilik', ru: 'Сообщество', en: 'Community' },
    topics: [
      {
        id: 'comments',
        perm: 'COMMENT_MODERATE',
        icon: '💬',
        title: { uz: 'Izohlarni moderatsiya qilish', ru: 'Модерация комментариев', en: 'Moderating comments' },
        what: {
          uz: 'Foydalanuvchilar qoldirgan izohlarni tasdiqlash yoki rad etish.',
          ru: 'Одобрение или отклонение комментариев пользователей.',
          en: 'Approving or rejecting user comments.',
        },
        steps: {
          uz: [
            'Yangi izohlar «Kutilmoqda» holatida keladi.',
            'Tasdiqlang yoki rad eting. Rad etilgan izoh ilovada ko\'rinmaydi.',
            'Holat bo\'yicha filtrlab, faqat kutilayotganlarni ko\'rishingiz mumkin.',
          ],
          ru: [
            'Новые комментарии приходят в статусе «Ожидает».',
            'Одобрите или отклоните. Отклонённый в приложении не виден.',
            'Можно отфильтровать по статусу и смотреть только ожидающие.',
          ],
          en: [
            'New comments arrive as “Pending”.',
            'Approve or reject. Rejected comments do not appear in the app.',
            'Filter by status to see only what is waiting.',
          ],
        },
      },
      {
        id: 'notifications',
        perm: 'NOTIFICATION_SEND',
        icon: '🔔',
        title: { uz: 'Bildirishnoma yuborish', ru: 'Отправка уведомлений', en: 'Sending notifications' },
        what: {
          uz: 'Foydalanuvchilarga push-xabar yuborish.',
          ru: 'Отправка push-уведомлений пользователям.',
          en: 'Sending push notifications to users.',
        },
        steps: {
          uz: [
            'Matnni uchala tilda yozing — har kim o\'z tilida oladi.',
            'Auditoriyani tanlang: hamma, obunachilar yoki ma\'lum til.',
            'Avval saqlang, keyin «Yuborish» tugmasini bosing.',
            '⚠️ Yuborilgan xabarni QAYTARIB bo\'lmaydi. Matnni ikki marta tekshiring.',
          ],
          ru: [
            'Напишите текст на трёх языках — каждый получит на своём.',
            'Выберите аудиторию: все, подписчики или конкретный язык.',
            'Сначала сохраните, потом нажмите «Отправить».',
            '⚠️ Отправленное уведомление ОТОЗВАТЬ нельзя. Проверьте текст дважды.',
          ],
          en: [
            'Write the text in three languages — each user gets their own.',
            'Choose the audience: everyone, subscribers or a specific language.',
            'Save first, then press “Send”.',
            '⚠️ A sent notification cannot be recalled. Check the text twice.',
          ],
        },
      },
    ],
  },

  // ─────────────────────────────────────────────── Foydalanuvchilar
  {
    group: { uz: 'Foydalanuvchilar', ru: 'Пользователи', en: 'Users' },
    topics: [
      {
        id: 'users',
        perm: 'USER_VIEW',
        icon: '👤',
        title: { uz: 'Foydalanuvchilar', ru: 'Пользователи', en: 'Users' },
        what: {
          uz: 'Ro\'yxatdan o\'tganlar, ularning obunasi va qurilmalari.',
          ru: 'Зарегистрированные, их подписки и устройства.',
          en: 'Registered users, their subscriptions and devices.',
        },
        steps: {
          uz: [
            'Telefon yoki ism bo\'yicha qidiring.',
            'Qatorga bosing — batafsil sahifa: xaridlar, obuna, qurilmalar.',
            '⚠️ Bloklash foydalanuvchini ilovadan chiqaradi. Sababini yozing — u auditga tushadi.',
          ],
          ru: [
            'Ищите по телефону или имени.',
            'Нажмите на строку — подробная страница: покупки, подписка, устройства.',
            '⚠️ Блокировка выкидывает пользователя из приложения. Укажите причину — она попадёт в аудит.',
          ],
          en: [
            'Search by phone or name.',
            'Click a row for the detail page: purchases, subscription, devices.',
            '⚠️ Blocking signs the user out of the app. Give a reason — it goes into the audit log.',
          ],
        },
      },
      {
        id: 'premium',
        perm: 'USER_PREMIUM_MANAGE',
        icon: '👑',
        title: { uz: 'Obuna berish', ru: 'Выдача подписки', en: 'Granting a subscription' },
        what: {
          uz: 'Foydalanuvchiga qo\'lda premium obuna berish.',
          ru: 'Ручная выдача премиум-подписки пользователю.',
          en: 'Manually granting a premium subscription.',
        },
        steps: {
          uz: [
            'Foydalanuvchi sahifasini oching.',
            '«Obuna berish» — tarif va muddatni tanlang.',
            '⚠️ Bu amal auditga yoziladi: kim, kimga, qachon berdi.',
          ],
          ru: [
            'Откройте страницу пользователя.',
            '«Выдать подписку» — выберите тариф и срок.',
            '⚠️ Действие пишется в аудит: кто, кому и когда выдал.',
          ],
          en: [
            'Open the user page.',
            '“Grant subscription” — pick the plan and duration.',
            '⚠️ This is written to the audit log: who granted what, to whom, and when.',
          ],
        },
      },
      {
        id: 'tariffs',
        perm: 'TARIFF_EDIT',
        icon: '💳',
        title: { uz: 'Tariflar', ru: 'Тарифы', en: 'Plans' },
        what: {
          uz: 'Obuna narxlari va muddatlari.',
          ru: 'Цены и сроки подписок.',
          en: 'Subscription prices and durations.',
        },
        steps: {
          uz: [
            'Tarif nomi va tavsifi uch tilda.',
            'Narx va muddat (oylarda).',
            '⚠️ Mavjud tarif narxini o\'zgartirsangiz, allaqachon sotib olganlarga ta\'sir qilmaydi.',
          ],
          ru: [
            'Название и описание тарифа — на трёх языках.',
            'Цена и срок (в месяцах).',
            '⚠️ Изменение цены не затрагивает тех, кто уже купил.',
          ],
          en: [
            'Plan name and description in three languages.',
            'Price and duration in months.',
            '⚠️ Changing the price does not affect people who already bought it.',
          ],
        },
      },
    ],
  },

  // ─────────────────────────────────────────────── Tizim
  {
    group: { uz: 'Tizim', ru: 'Система', en: 'System' },
    topics: [
      {
        id: 'staff',
        perm: null,
        role: 'ADMIN',
        icon: '👥',
        title: { uz: 'Xodimlar va ruxsatlar', ru: 'Сотрудники и права', en: 'Staff and permissions' },
        what: {
          uz: 'Panelga kiradigan xodimlarni qo\'shish va ularga ruxsat berish.',
          ru: 'Добавление сотрудников в панель и выдача прав.',
          en: 'Adding panel users and granting them permissions.',
        },
        steps: {
          uz: [
            '«Yangi xodim» — telefon, ism va rol.',
            'Rolni tanlagach ruxsatlarni belgilang. Xodim faqat berilgan ruxsatlarni ko\'radi.',
            '⚠️ Xodim o\'zidan yuqori rol yarata olmaydi.',
            'Ruxsat olib tashlansa, xodim o\'sha bo\'limni darhol yo\'qotadi.',
          ],
          ru: [
            '«Новый сотрудник» — телефон, имя и роль.',
            'После выбора роли отметьте права. Сотрудник видит только выданное.',
            '⚠️ Сотрудник не может создать роль выше своей.',
            'При снятии права раздел исчезает у него сразу.',
          ],
          en: [
            '“New staff member” — phone, name and role.',
            'After picking a role, tick the permissions. They see only what you grant.',
            '⚠️ Nobody can create a role higher than their own.',
            'Removing a permission hides that section immediately.',
          ],
        },
      },
      {
        id: 'settings',
        perm: 'SETTINGS_EDIT',
        icon: '⚙️',
        title: { uz: 'Sozlamalar', ru: 'Настройки', en: 'Settings' },
        what: {
          uz: 'Platformaning umumiy qiymatlari: narxlar, chegaralar.',
          ru: 'Общие значения платформы: цены, лимиты.',
          en: 'Platform-wide values: prices, limits.',
        },
        steps: {
          uz: [
            'Qiymatni o\'zgartiring va saqlang.',
            '⚠️ O\'zgarish DARHOL kuchga kiradi va barcha foydalanuvchiga tegadi.',
            'Har o\'zgarish auditga yoziladi.',
          ],
          ru: [
            'Измените значение и сохраните.',
            '⚠️ Изменение вступает в силу СРАЗУ и касается всех пользователей.',
            'Каждое изменение пишется в аудит.',
          ],
          en: [
            'Change a value and save.',
            '⚠️ Changes take effect IMMEDIATELY for all users.',
            'Every change is written to the audit log.',
          ],
        },
      },
      {
        id: 'audit',
        perm: null,
        role: 'ADMIN',
        icon: '📜',
        title: { uz: 'Audit jurnali', ru: 'Журнал аудита', en: 'Audit log' },
        what: {
          uz: 'Panelda kim nima qilgani — o\'zgarmas yozuv.',
          ru: 'Кто что делал в панели — неизменяемая запись.',
          en: 'Who did what in the panel — an immutable record.',
        },
        steps: {
          uz: [
            'Xodim, amal turi yoki sana bo\'yicha qidiring.',
            'Qatorda: kim, nima qildi, qaysi obyektga, qachon.',
            '⚠️ Audit yozuvini o\'chirib bo\'lmaydi — bu ataylab.',
          ],
          ru: [
            'Ищите по сотруднику, типу действия или дате.',
            'В строке: кто, что сделал, с каким объектом и когда.',
            '⚠️ Запись аудита удалить нельзя — так задумано.',
          ],
          en: [
            'Search by staff member, action type or date.',
            'Each row shows who did what, to which object, and when.',
            '⚠️ Audit entries cannot be deleted — by design.',
          ],
        },
      },
    ],
  },
];
