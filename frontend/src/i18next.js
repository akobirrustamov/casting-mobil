import i18n from 'i18next';
import { initReactI18next } from 'react-i18next';

const savedLng = localStorage.getItem('selectedLanguage') || 'uz';


i18n.on('languageChanged', (lng) => {
    localStorage.setItem('selectedLanguage', lng);
});



const resources = {
    uz: {
        translation: {

            footer: {
                brand: {
                    description: "O'zbekistondagi eng yirik kasting platformasi. Aktyorlar, modellar va ijodkorlarni topishning eng zamonaviy usuli.",
                    online: "Online 24/7"
                },
                contact: {
                    title: "Bog'lanish",
                    address: "Toshkent shahri, Yashnobod tumani",
                    phone: "+998916407314",
                    email: "uzcasting.org@gmail.com"
                },
                social: {
                    title: "Ijtimoiy tarmoqlar",
                    telegram: "Telegram",
                    instagram: "Instagram",
                    youtube: "YouTube",
                    tiktok: "TikTok"
                },
                quick: {
                    title: "Tezkor havolalar",
                    admin: "Admin bilan bog'lanish",
                    adminUser: "@JasMaxStar",
                    register: "Ro'yxatdan o'tish",
                    registerSub: "Telegram bot orqali"
                },
                copyright: {
                    text: "Barcha huquqlar himoyalangan.",
                    partners: "Biz bilan hamkorlikda:"
                }
            },
            header: {
                siteTitle: 'Uzcasting',
                home: 'Bosh sahifa',
                models: 'Modellar',
                register: 'Nomzodlik bildirish'
            },
            common: {
                loading: "Yuklanmoqda...",
                emptyTitle: "Mos keladigan model topilmadi",
                emptySubtitle: "Filtrlarni o‘zgartiring yoki boshqa so‘rov kiriting",
                error: "Yangiliklar yuklanmadi",
                gallery: "Galereya",
                loadMore: "Ko'proq ko'rish",
            },
            hero: {
                title: "Uzcasting — iste'dod va imkon uchrashadigan joy",
                subtitle:
                    "Kastingni keyingi bosqichga olib chiqamiz: murabbiylik, tayyorgarlik va halol hamkorlik. 80+ klip va reklama, 14 ta davlatda suratga olish tajribasi.",
                feature1Title: "Karyera starti",
                feature1Text: "Yulduzlarni topamiz, sahnaga tayyorlaymiz.",
                feature2Title: "Global hamkorlik",
                feature2Text: "Rossiya, Turkiya, Ozarbayjon va boshqalar.",
                feature3Title: "Murabbiylik",
                feature3Text: "Qimmatli yo‘l-yo‘riq va treninglar.",
                btnApplicant: "Men kastinga keldim",
                btnClient: "Men model qidirmoqdaman",
            },
            director: {
                heading: "Producer va Direktor",
                about:
                    "Sattorov Jasur – kino va san’at sohasida 5 yildan ortiq tajribaga ega bo‘lgan professional producer, aktyor va casting direktori. U o‘z faoliyati davomida nafaqat O‘zbekistonda, balki xorijiy mamlakatlarda ham muvaffaqiyatli ishlagan. Jasur Moskva, Sankt-Peterburg, Malayziya va Istanbul kabi yirik madaniy markazlarda turli xil ijodiy loyihalarda ishtirok etib, katta tajriba orttirgan. Uning faoliyati davomida 20–30 dan ortiq musiqiy kliplarning produceri sifatida ishlagani san’at sohasida o‘zining puxta bilim va tajribasini namoyon qiladi. Sattorov Jasur loyihalarda ijodiy yondashuvi, professional tashkiliy qobiliyati va xalqaro tajribasini uyg‘unlashtirib, har bir ishda yuqori sifatni ta’minlashga intiladi.",
            },
            showcase: {
                films: {
                    title: "Faoliyatimizdan namunalar (Film va serialar)",
                    posterCaption: "«Maxsus Bo‘lim» — detektiv / triller",
                    ftitle: "🎬 Film nomi:",
                    fname: "Maxsus bo‘lim (Maxsus Bo‘lim)",
                    genre: "📽 Janr:",
                    genreVal: "Detektiv, triller, siri to‘la drama",
                    producer: "🎬 Produser:",
                    producerVal:
                        "Sattorov Jasur — “Uzcasting” rahbari, produser, shou-biznesda “Jas Max Star” nomi bilan tanilgan",
                    places: "🌍 Suratga olish joylari:",
                    placesVal:
                        "• 🇺🇿 O‘zbekiston (Toshkent, Samarqand)\n• 🇷🇺 Rossiya (Moskva, Sankt-Peterburg)\n• 🇹🇷 Turkiya (Istanbul, Kappadokiya)\n• 🇦🇪 Dubay (BAA)",
                    synopsis: "🧩 Qisqacha mavzusi:",
                    synopsisVal:
                        "Filmda maxsus xizmat agentlari, ikkiyuzlamachilik, siyosiy tuzoqlar va shaxsiy fidoyiliklar orasida kechadigan sirli voqealar tasvirlanadi. Asosiy qahramon — o‘z ortida og‘ir sinovlarni qoldirgan sobiq tergovchi. U yangi topshiriq bilan yashirin “maxsus bo‘lim”ga chaqiriladi va u yerda butun tizimni fosh etuvchi xavfli missiyaga yuz tutadi.",
                    facts: "🎥 Qiziqarli faktlar:",
                    factsVal:
                        "• Rossiyadagi suratga olish jarayonlari texnik va logistika jihatdan eng og‘ir bo‘ldi\n• Filmda haqiqiy maxsus xizmat texnikalari, original maxsus effektlar va xalqaro aktyorlar ishtirok etadi\n• Dubaydagi sahnalarda xalqaro darajadagi ekshn va texnologik yondashuv namoyon bo‘ladi",
                    goal: "🌟 Filmning maqsadi:",
                    goalVal:
                        "• O‘zbekiston kinematografiyasida xalqaro darajadagi detektiv-kriminal yo‘nalishini yuksaltirish\n• Tomoshabinlarga intellektual va dinamik mazmundagi film taqdim etish",
                },
                clips: {
                    title: "Faoliyatimizdan namunalar (Kliplar)",
                    more: "...va boshqa ajoyib kliplar",
                },
            },
            models: {
                title: "Modellar / Aktyorlar",
                found: "Topildi",
                photos: "FOTO",
                contact: {
                    requestTitle: "Model uchun so‘rov",
                    id: "ID",
                    name: "Ism"
                }
            },
            filters: {
                search: "Qidirish",
                searchPlaceholder: "Ism, familiya…",
                gender: "Jins",
                genderAny: "Har qanday",
                genderMale: "Erkak",
                genderFemale: "Ayol",
                age: "Yosh",
                heightFrom: "Bo‘y",
                any: "Har qanday",
                castingType: {
                    label: 'Casting turi',
                    options: {
                        all: 'Barchasi',
                        model: 'Modelyer',
                        euromodel: 'Yevro Modelyer',
                        bloger: 'Bloger',
                        actor: 'Aktyor',
                        extra: 'Aktrisa',
                        influencer: 'Reklama'
                    }
                }
            },
            modal: {
                age: "Yosh",
                nationality: "Fuqaroligi",
                region: "Yashash shahri",
                height: "Bo‘y",
                appearanceType: "Ko‘rinish turi",
                bodyType: "Tana tuzilishi",
                hairColor: "Soch rangi",
                eyeColor: "Ko‘z rangi",
                gender: "Jins",
                gender_female: "Ayol",
                gender_male: "Erkak",
            },
            actions: {
                reset: "Tozalash",
                contact: "Bog‘lanish"
            },
            units: {
                years: "{{count}} yosh",
                cm: "sm"
            }
        },
    },
    ru: {
        translation: {

            footer: {
                brand: {
                    description: "Крупнейшая кастинг-платформа в Узбекистане. Современный способ найти актёров, моделей и творческих людей.",
                    online: "Онлайн 24/7"
                },
                contact: {
                    title: "Контакты",
                    address: "г. Ташкент, Яшнабадский район",
                    phone: "+998916407314",
                    email: "uzcasting.org@gmail.com"
                },
                social: {
                    title: "Социальные сети",
                    telegram: "Телеграм",
                    instagram: "Инстаграм",
                    youtube: "Ютуб",
                    tiktok: "ТикТок"
                },
                quick: {
                    title: "Быстрые ссылки",
                    admin: "Связаться с админом",
                    adminUser: "@JasMaxStar",
                    register: "Регистрация",
                    registerSub: "Через Телеграм-бота"
                },
                copyright: {
                    text: "Все права защищены.",
                    partners: "Сотрудничаем с:"
                }
            },
            header: {
                siteTitle: 'Uzcasting',
                home: 'Главная',
                models: 'Модели',
                register: 'Подать заявку'
            },
            common: {
                emptySubtitle: "Измените фильтры или попробуйте другой запрос",
                emptyTitle: "Подходящих моделей не найдено",
                loading: "Загрузка...",
                error: "Новости не загружены",
                gallery: "Галерея",
                loadMore: "Показать больше",
            },
            hero: {
                title: "Uzcasting — место, где встречаются талант и шанс",
                subtitle:
                    "Поднимем кастинг на новый уровень: менторство, подготовка и честное партнёрство. 80+ клипов и реклам, опыт съёмок в 14 странах.",
                feature1Title: "Старт карьеры",
                feature1Text: "Находим звёзд и готовим к сцене.",
                feature2Title: "Международно",
                feature2Text: "Россия, Турция, Азербайджан и др.",
                feature3Title: "Наставники",
                feature3Text: "Ценные рекомендации и тренинги.",
                btnApplicant: "Я пришёл на кастинг",
                btnClient: "Я ищу модель",
            },
            director: {
                heading: "Продюсер и Режиссёр",
                about:
                    `Сатторов Жасур – профессиональный продюсер, актер и кастинг-директор с более чем 5-летним опытом работы в сфере кино и искусства. В течение своей деятельности он успешно реализовал проекты не только в Узбекистане, но и за рубежом. Жасур принимал участие в различных творческих проектах в таких крупных культурных центрах, как Москва, Санкт-Петербург, Малайзия и Стамбул, накопив значительный опыт.

За время своей карьеры он выступил продюсером более чем 20–30 музыкальных клипов, что демонстрирует его глубокие знания и практический опыт в сфере искусства. Сатторов Жасур в каждом проекте сочетает творческий подход, профессиональные организаторские навыки и международный опыт, стремясь обеспечить высокое качество выполняемой работы.

`,
            },
            showcase: {
                films: {
                    title: "Из нашей деятельности (Фильмы и сериалы)",
                    posterCaption: "«Maxsus Bo‘lim» — детектив / триллер",
                    ftitle: "🎬 Название фильма:",
                    fname: "Mахсус бўлим (Maxsus Bo‘lim)",
                    genre: "📽 Жанр:",
                    genreVal: "Детектив, триллер, драма с тайной",
                    producer: "🎬 Продюсер:",
                    producerVal:
                        "Саттаров Жасур — руководитель «Uzcasting», продюсер, в шоу-бизнесе известен как «Jas Max Star»",
                    places: "🌍 Локации съёмок:",
                    placesVal:
                        "• 🇺🇿 Узбекистан (Ташкент, Самарканд)\n• 🇷🇺 Россия (Москва, Санкт-Петербург)\n• 🇹🇷 Турция (Стамбул, Каппадокия)\n• 🇦🇪 Дубай (ОАЭ)",
                    synopsis: "🧩 Краткий сюжет:",
                    synopsisVal:
                        "История о спецагентах, двойных играх, политических ловушках и личной самоотдаче. Главный герой — бывший следователь с тяжёлым прошлым — получает секретное задание и оказывается в «особом отделе», где ему предстоит миссия, способная обнажить изнанку всей системы.",
                    facts: "🎥 Интересные факты:",
                    factsVal:
                        "• Российские съёмки оказались самыми сложными технически и логистически\n• В фильме используются реальные спецтехнические средства, оригинальные VFX, снимаются актёры международного уровня\n• Дубайские сцены демонстрируют международный экшен и технологичный подход",
                    goal: "🌟 Цель фильма:",
                    goalVal:
                        "• Продвинуть в узбекском кино международный детективно-криминальный жанр\n• Дать зрителю интеллектуальное и динамичное кино",
                },
                clips: {
                    title: "Из нашей деятельности (Клипы)",
                    more: "...и другие классные клипы",
                },
            },
            models: {
                title: 'Models / Actors',
                found: 'Найдено',
                photos: 'ФОТО',
                contact: { requestTitle: 'Заявка на модель', id: 'ID', name: 'Имя' }
            },
            filters: {
                search: 'Поиск',
                searchPlaceholder: 'Имя, фамилия…',
                gender: 'Пол',
                genderAny: 'Любой',
                genderMale: 'Мужской',
                genderFemale: 'Женский',
                age: 'Возраст',
                heightFrom: 'Рост от',
                any: 'Любой',
                castingType: {
                    label: 'Тип кастинга',
                    options: {
                        all: 'Любой',
                        model: 'Модель',
                        euromodel: 'Евро-модель',
                        bloger: 'Блогер',
                        actor: 'Актёр',
                        extra: 'Актриса / массовка',
                        influencer: 'Реклама'
                    }
                }
            },
            modal: {
                age: 'Возраст',
                nationality: 'Гражданство',
                region: 'Город проживания',
                height: 'Рост',
                appearanceType: 'Тип внешности',
                bodyType: 'Телосложение',
                hairColor: 'Цвет волос',
                eyeColor: 'Цвет глаз',
                gender: 'Пол',
                gender_female: "Женщина", // Russian
                gender_male: "Мужчина",
            },
            actions: { reset: 'Сбросить', contact: 'Связаться' },
            units: {
                years_one: "{{count}} год",
                years_few: "{{count}} года",
                years_many: "{{count}} лет",
                years_other: "{{count}} лет",
                cm: "см"
            }


        },
    },
    en: {
        translation: {
            footer: {
                brand: {
                    description: "The largest casting platform in Uzbekistan. The most modern way to find actors, models, and creators.",
                    online: "Online 24/7"
                },
                contact: {
                    title: "Contact",
                    address: "Tashkent city, Yashnabad district",
                    phone: "+998916407314",
                    email: "uzcasting.org@gmail.com"
                },
                social: {
                    title: "Social Media",
                    telegram: "Telegram",
                    instagram: "Instagram",
                    youtube: "YouTube",
                    tiktok: "TikTok"
                },
                quick: {
                    title: "Quick Links",
                    admin: "Contact Admin",
                    adminUser: "@JasMaxStar",
                    register: "Register",
                    registerSub: "Via Telegram bot"
                },
                copyright: {
                    text: "All rights reserved.",
                    partners: "In partnership with:"
                }
            },
            header: {
                siteTitle: 'Uzcasting',
                home: 'Home',
                models: 'Models',
                register: 'Submit Application'
            },
            common: {
                loading: "Loading...",
                error: "Failed to load news",
                gallery: "Gallery",
                loadMore: "Load more",
                emptyTitle: "No matching models found",
                emptySubtitle: "Adjust filters or try a different query",
            },
            hero: {
                title: "Uzcasting — where talent meets opportunity",
                subtitle:
                    "We elevate casting with mentoring, preparation, and fair partnership. 80+ music videos & ads, filming experience across 14 countries.",
                feature1Title: "Career start",
                feature1Text: "We discover stars and prepare them for the stage.",
                feature2Title: "Global partners",
                feature2Text: "Russia, Türkiye, Azerbaijan and more.",
                feature3Title: "Mentorship",
                feature3Text: "Practical guidance and training.",
                btnApplicant: "I came for casting",
                btnClient: "I’m looking for a model",
            },
            director: {
                heading: "Producer & Director",
                about:
                    `Sattorov Jasur is a professional producer, actor, and casting director with more than 5 years of experience in the field of cinema and art. Throughout his career, he has successfully worked not only in Uzbekistan but also abroad. Jasur has taken part in various creative projects in major cultural centers such as Moscow, Saint Petersburg, Malaysia, and Istanbul, gaining significant experience.

During his professional journey, he has produced over 20–30 music videos, which highlights his solid knowledge and expertise in the art industry. In every project, Sattorov Jasur combines a creative approach, strong organizational skills, and international experience, striving to ensure the highest quality in his work.
`,
            },
            showcase: {
                films: {
                    title: "From our work (Films & Series)",
                    posterCaption: "“Maxsus Bo‘lim” — detective / thriller",
                    ftitle: "🎬 Film title:",
                    fname: "Maxsus bo‘lim (Special Unit)",
                    genre: "📽 Genre:",
                    genreVal: "Detective, thriller, mystery drama",
                    producer: "🎬 Producer:",
                    producerVal:
                        "Sattorov Jasur — head of “Uzcasting”, producer, known as “Jas Max Star” in show business",
                    places: "🌍 Filming locations:",
                    placesVal:
                        "• 🇺🇿 Uzbekistan (Tashkent, Samarkand)\n• 🇷🇺 Russia (Moscow, Saint Petersburg)\n• 🇹🇷 Türkiye (Istanbul, Cappadocia)\n• 🇦🇪 Dubai (UAE)",
                    synopsis: "🧩 Logline:",
                    synopsisVal:
                        "A web of covert operations, duplicity, political traps, and personal sacrifice. A former investigator with a heavy past joins a secret “special unit” for a mission that could expose the entire system.",
                    facts: "🎥 Fun facts:",
                    factsVal:
                        "• Russian shooting schedule was the toughest technically and logistically\n• Real special-service gear, practical & original VFX, and international cast\n• Dubai scenes showcase large-scale action and a tech-driven approach",
                    goal: "🌟 Goal:",
                    goalVal:
                        "• Elevate international-level detective/crime genre in Uzbek cinema\n• Offer viewers an intelligent, dynamic film experience",
                },
                clips: {
                    title: "From our work (Music videos)",
                    more: "...and many other great clips",
                },
            },
            models: {
                title: "Models / Actors",
                found: "Found",
                photos: "PHOTOS",
                contact: {
                    requestTitle: "Model request",
                    id: "ID",
                    name: "Name"
                }
            },
            filters: {
                search: "Search",
                searchPlaceholder: "First or last name…",
                gender: "Gender",
                genderAny: "Any",
                genderMale: "Male",
                genderFemale: "Female",
                age: "Age",
                heightFrom: "Height from",
                any: "Any",
                castingType: {
                    label: 'Casting type',
                    options: {
                        all: 'Any',
                        model: 'Model',
                        euromodel: 'Euro model',
                        bloger: 'Blogger',
                        actor: 'Actor',
                        extra: 'Actress / extra',
                        influencer: 'Advertising'
                    }
                }
            },
            modal: {
                age: "Age",
                nationality: "Nationality",
                region: "City of residence",
                height: "Height",
                appearanceType: "Appearance type",
                bodyType: "Body type",
                hairColor: "Hair color",
                eyeColor: "Eye color",
                gender: "Gender",
                gender_female: "Female",  // English
                gender_male: "Male",
            },
            actions: {
                reset: "Reset",
                contact: "Contact"
            },
            units: {
                years_one: "{{count}} year",
                years_other: "{{count}} years",
                cm: "cm"
            }
        },
        footer: {
            brand: {
                description: "O'zbekistondagi eng yirik kasting platformasi. Aktyorlar, modellar va ijodkorlarni topishning eng zamonaviy usuli.",
                online: "Online 24/7"
            },
            contact: {
                title: "Bog'lanish",
                address: "Toshkent shahri, Yashnobod tumani",
                phone: "+998916407314",
                email: "uzcasting.org@gmail.com"
            },
            social: {
                title: "Ijtimoiy tarmoqlar",
                telegram: "Telegram",
                instagram: "Instagram",
                youtube: "YouTube",
                tiktok: "TikTok"
            },
            quick: {
                title: "Tezkor havolalar",
                admin: "Admin bilan bog'lanish",
                adminUser: "@JasMaxStar",
                register: "Ro'yxatdan o'tish",
                registerSub: "Telegram bot orqali"
            },
            copyright: {
                text: "Barcha huquqlar himoyalangan.",
                partners: "Biz bilan hamkorlikda:"
            }
        }
    },
};

i18n
    .use(initReactI18next)
    .init({
        resources,
        lng: savedLng,
        fallbackLng: 'ru',
        interpolation: { escapeValue: false },
    });
export default i18n;