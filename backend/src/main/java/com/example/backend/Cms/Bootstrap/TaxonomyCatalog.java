package com.example.backend.Cms.Bootstrap;

/**
 * Platforma bilan BIRGA keladigan kategoriya va janrlar ro'yxati.
 *
 * <h2>Nega kodda, migratsiyada emas</h2>
 * Sxemani Flyway boshqaradi (ТЗ §91), lekin bu MA'LUMOT — admin uni
 * panelda tahrirlaydi, o'chiradi, tartibini o'zgartiradi. Migratsiyaga
 * yozilsa, keyingi versiyada ro'yxatni yangilash uchun har safar yangi
 * SQL fayl kerak bo'lardi va admin qilgan o'zgarishlar bilan
 * to'qnashardi. Bu yerda esa ro'yxat oddiy jadval:
 * {@link TaxonomyBootstrap} uni bir marta bazaga ko'chiradi.
 *
 * <h2>Kategoriya va janr bir xil narsa EMAS (ТЗ §13)</h2>
 * <ul>
 *   <li><b>Kategoriya</b> — katalog BO'LIMI: kelib chiqishi, auditoriyasi
 *       ("O'zbek kinosi", "Bolalar uchun"). Mobil ilovaning menyusida
 *       chiqadi, shuning uchun ro'yxat QISQA bo'lishi kerak.</li>
 *   <li><b>Janr</b> — uslub TEGI ("Drama", "Triller"). Bitta kontentda
 *       bir nechta bo'lishi mumkin, shuning uchun ro'yxat uzun.</li>
 *   <li><b>Tur</b> ({@code ContentType}) — kontentning SHAKLI. U enum,
 *       bu yerga umuman kirmaydi.</li>
 * </ul>
 *
 * <h2>Janrlar qayerdan olingan</h2>
 * Wikipedia "Category:Film genres" ro'yxati asos qilib olingan:
 * https://en.wikipedia.org/wiki/Category:Film_genres
 *
 * ⚠️ O'sha sahifadagi IKKI YUZDAN ortiq nom to'liq ko'chirilmagan — bu
 * ataylab. Ro'yxatning katta qismi milliy kinoshunoslik atamalari
 * ("Bourekas film", "Chernukha", "Goona-goona epic", "Pornochanchada"),
 * ular uchun na o'zbekcha nom bor, na tomoshabin ularni qidiradi.
 * Janr tanlash oynasida ular faqat kerakli janrni topishga xalaqit
 * berardi. Shuning uchun katalogda tanib olinadigan janrlar qoldirilgan;
 * qolganini admin panel orqali istalgan vaqtda qo'shadi.
 */
public final class TaxonomyCatalog {

    private TaxonomyCatalog() {
    }

    /**
     * Ro'yxat versiyasi.
     *
     * ⚠️ Ro'yxatga yangi satr qo'shilganda BU SON OSHIRILADI — aks holda
     * {@link TaxonomyBootstrap} allaqachon ishlagan bazada qayta
     * ishlamaydi va yangi janrlar hech qachon paydo bo'lmaydi.
     */
    public static final int VERSION = 1;

    /** slug, UZ, RU, EN */
    public static final String[][] CATEGORIES = {
            {"uzbek", "O'zbek kinosi", "Узбекское кино", "Uzbek Cinema"},
            {"foreign", "Xorijiy kino", "Зарубежное кино", "Foreign Cinema"},
            {"turkish", "Turk kinosi", "Турецкое кино", "Turkish Cinema"},
            {"korean", "Koreys kinosi", "Корейское кино", "Korean Cinema"},
            {"indian", "Hind kinosi", "Индийское кино", "Indian Cinema"},
            {"russian", "Rus kinosi", "Российское кино", "Russian Cinema"},
            {"hollywood", "Hollywood", "Голливуд", "Hollywood"},
            {"kids", "Bolalar uchun", "Детям", "For Kids"},
            {"family-viewing", "Oila davrasida", "Семейный просмотр", "Family Viewing"},
            {"cartoons", "Multfilmlar", "Мультфильмы", "Cartoons"},
            {"shows", "Ko'rsatuvlar", "Шоу", "Shows"},
            {"documentary-projects", "Hujjatli loyihalar", "Документальные проекты", "Documentary Projects"},
            {"classics", "Klassika", "Классика", "Classics"},
    };

    /** slug, UZ, RU, EN */
    public static final String[][] GENRES = {
            {"drama", "Drama", "Драма", "Drama"},
            {"comedy", "Komediya", "Комедия", "Comedy"},
            {"melodrama", "Melodrama", "Мелодрама", "Melodrama"},
            {"romance", "Romantika", "Романтика", "Romance"},
            {"romantic-comedy", "Romantik komediya", "Романтическая комедия", "Romantic Comedy"},
            {"comedy-drama", "Tragikomediya", "Трагикомедия", "Comedy-drama"},
            {"action", "Jangari", "Боевик", "Action"},
            {"adventure", "Sarguzasht", "Приключения", "Adventure"},
            {"thriller", "Triller", "Триллер", "Thriller"},
            {"psychological-thriller", "Psixologik triller", "Психологический триллер", "Psychological Thriller"},
            {"political-thriller", "Siyosiy triller", "Политический триллер", "Political Thriller"},
            {"horror", "Qo'rqinchli", "Ужасы", "Horror"},
            {"mystery", "Sirli", "Мистика", "Mystery"},
            {"detective", "Detektiv", "Детектив", "Detective"},
            {"crime", "Jinoyat", "Криминал", "Crime"},
            {"gangster", "Gangster filmi", "Гангстерский", "Gangster"},
            {"heist", "O'g'irlik filmi", "Ограбление", "Heist"},
            {"prison", "Qamoqxona filmi", "Тюремный", "Prison Film"},
            {"legal-drama", "Sud dramasi", "Судебная драма", "Legal Drama"},
            {"medical-drama", "Tibbiy drama", "Медицинская драма", "Medical Drama"},
            {"social-drama", "Ijtimoiy drama", "Социальная драма", "Social Drama"},
            {"family", "Oilaviy", "Семейный", "Family"},
            {"children", "Bolalar filmi", "Детский", "Children's Film"},
            {"teen", "O'smirlar filmi", "Подростковый", "Teen Film"},
            {"coming-of-age", "Voyaga yetish", "Взросление", "Coming-of-age"},
            {"animation", "Multfilm", "Мультфильм", "Animation"},
            {"fantasy", "Fentezi", "Фэнтези", "Fantasy"},
            {"science-fiction", "Ilmiy fantastika", "Научная фантастика", "Science Fiction"},
            {"post-apocalyptic", "Post-apokaliptik", "Постапокалипсис", "Post-apocalyptic"},
            {"superhero", "Superqahramon", "Супергерои", "Superhero"},
            {"monster", "Yirtqichlar haqida", "Монстры", "Monster Movie"},
            {"disaster", "Falokat filmi", "Фильм-катастрофа", "Disaster"},
            {"survival", "Omon qolish", "Выживание", "Survival"},
            {"war", "Harbiy", "Военный", "War Film"},
            {"historical", "Tarixiy", "Исторический", "Historical"},
            {"epic", "Epik", "Эпос", "Epic"},
            {"biography", "Biografik", "Биографический", "Biographical"},
            {"documentary", "Hujjatli", "Документальный", "Documentary"},
            {"docudrama", "Hujjatli drama", "Докудрама", "Docudrama"},
            {"mockumentary", "Psevdohujjatli", "Псевдодокументальный", "Mockumentary"},
            {"musical", "Musiqiy", "Мюзикл", "Musical"},
            {"concert-film", "Konsert filmi", "Концертный фильм", "Concert Film"},
            {"dance", "Raqs filmi", "Танцевальный", "Dance Film"},
            {"sports", "Sport filmi", "Спортивный", "Sports Film"},
            {"martial-arts", "Jang san'ati", "Боевые искусства", "Martial Arts"},
            {"spy", "Josuslik filmi", "Шпионский", "Spy Film"},
            {"western", "Vestern", "Вестерн", "Western"},
            {"road-movie", "Yo'l filmi", "Роуд-муви", "Road Movie"},
            {"satire", "Satira", "Сатира", "Satire"},
            {"parody", "Parodiya", "Пародия", "Parody"},
            {"tragedy", "Fojia", "Трагедия", "Tragedy"},
            {"mythology", "Mifologik", "Мифологический", "Mythological"},
            {"noir", "Fil'm-nuar", "Фильм-нуар", "Film Noir"},
            {"art-film", "Mualliflik kinosi", "Авторское кино", "Art Film"},
            {"experimental", "Eksperimental", "Экспериментальный", "Experimental"},
            {"short-film", "Qisqa metrajli", "Короткометражный", "Short Film"},
    };
}
