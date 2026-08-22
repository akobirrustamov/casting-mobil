package com.example.backend.Security;

import com.example.backend.Repository.UserRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Kirish qoidalari.
 *
 * <h2>Asosiy tamoyil</h2>
 * <b>Ochiq endpointlar aniq sanab o'tiladi, qolgan barcha {@code /api/**} yopiq.</b>
 * Ya'ni kelajakda qo'shiladigan har qanday yangi endpoint sukut bo'yicha himoyalangan
 * bo'ladi — uni ochish uchun ataylab shu ro'yxatga yozish kerak.
 *
 * <h2>Nega bu ro'yxat aynan shunday</h2>
 * API'ga uchta klient bog'langan va ularning ehtiyoji har xil:
 * <ul>
 *   <li><b>Telegram bot WebApp</b> ({@code /bot/*}, {@code /data-form/*}) — foydalanuvchi
 *       tizimga KIRMAYDI. U anonim holda anketa yuboradi va rasm yuklaydi, shuning uchun
 *       {@code POST /casting-user} va {@code POST /file/upload} ochiq qolishi SHART.</li>
 *   <li><b>Sayt</b> — public sahifalar (katalog, yangiliklar) + tokenli admin panellar.</li>
 *   <li><b>Mobil ilova</b> — {@code /auth/google} va {@code /casting-user/web}.</li>
 * </ul>
 *
 * <h2>Nima o'zgardi</h2>
 * Avval bu yerda {@code GET/POST/PUT/DELETE "/**" permitAll} turgan edi — ya'ni butun API,
 * jumladan {@code DELETE} va admin amallari, tokensiz ochiq edi. Universitet loyihasidan
 * qolgan qoidalar ({@code /student}, {@code /superadmin/**}, {@code /groups/**},
 * {@code /subject/}) ham olib tashlandi — bu loyihada bunday endpointlar yo'q.
 *
 * <h2>Qolgan ochiq muammo</h2>
 * {@code GET /casting-user/appeal/{id}} va {@code GET /casting-user/my/{telegramId}} shaxsiy
 * ma'lumot (telefon, email, o'lchovlar) qaytaradi va ochiq qolmoqda — ularsiz bot oqimi
 * ishlamaydi. To'g'ri yechim: cheklangan DTO qaytarish. Bu alohida task
 * (roadmap.md → B2), chunki u javob formatini o'zgartiradi.
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final UserRepo userRepo;
    private final MyFilter myFilter;
    private final RestAuthErrorHandler authErrorHandler;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(Customizer.withDefaults())
                // JWT ishlatiladi, brauzer sessiyasi yo'q — CSRF token ma'noga ega emas.
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authErrorHandler)
                        .accessDeniedHandler(authErrorHandler))
                .authorizeHttpRequests(auth -> auth

                        // --- CORS preflight ---
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // --- Avtorizatsiya: tokensiz kirish nuqtalari ---
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/login").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/google").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/refresh").permitAll()
                        // Admin panelga kirish. Faqat LOGIN ochiq - /auth/me token talab qiladi.
                        // Rol tekshiruvi controller ichida: USER admin panelga kira olmaydi.
                        .requestMatchers(HttpMethod.POST, "/api/v1/app/admin/auth/login").permitAll()
                        // Refresh va logout Bearer token TALAB QILMAYDI: ular
                        // httpOnly cookie bilan ishlaydi. Access token muddati
                        // o'tgach yangilash aynan shu holatda kerak bo'ladi (§61).
                        .requestMatchers(HttpMethod.POST, "/api/v1/app/admin/auth/refresh").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/app/admin/auth/logout").permitAll()

                        // --- Yangiliklar: o'qish ochiq, yozish yopiq ---
                        .requestMatchers(HttpMethod.GET, "/api/v1/news").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/news/*").permitAll()

                        // --- Fayllar ---
                        // Rasmlarni ko'rsatish uchun ochiq.
                        .requestMatchers(HttpMethod.GET, "/api/v1/file/getFile/**").permitAll()
                        // Afishalar <img> tegida ko'rsatiladi va mobil ilovada hammaga
                        // ko'rinadi - fayl o'zi ochiq, ro'yxat va yuklash esa yopiq.
                        //
                        // ⚠️ Bu qoida FAQAT rasmlarni ochadi. VIDEO uchun endpoint
                        // ichida entitlement tekshiriladi (AccessService.canReadMedia),
                        // chunki Spring Security fayl TURINI bilmaydi - u faqat URL
                        // ko'radi, media turi esa bazadan aniqlanadi.
                        .requestMatchers(HttpMethod.GET, "/api/v1/app/media/*/raw").permitAll()
                        // Tomosha qarori. Anonim foydalanuvchi ham so'ray oladi -
                        // javob "kiring" bo'ladi, video havolasi esa berilmaydi.
                        //
                        // ⚠️ `**` kerak, `*` EMAS: SINGLE kontent uchun yo'l ikki
                        // darajali — /watch/content/{id}. Bitta yulduzcha uni
                        // qamramaydi va film tomosha qilish 401 qaytarardi.
                        .requestMatchers(HttpMethod.GET, "/api/v1/app/watch/**").permitAll()
                        // Bosh sahifa (§31). Mehmon ham ko'ra oladi - aks holda
                        // odam ilovada nima borligini bilmasdan ro'yxatdan
                        // o'tishi kerak bo'lardi. Tomosha qilish esa baribir
                        // /watch orqali alohida tekshiriladi.
                        //
                        // Token yuborilsa hisobga olinadi: faol obunasi
                        // borlarga reklama qaytarilmaydi.
                        .requestMatchers(HttpMethod.GET, "/api/v1/app/home").permitAll()
                        // ⚠️ Bot foydalanuvchisi anketa rasmini kirmasdan yuklaydi.
                        // Yopilsa Telegram bot oqimi ishlamay qoladi.
                        .requestMatchers(HttpMethod.POST, "/api/v1/file/upload").permitAll()

                        // --- Casting: ochiq qismi ---
                        // Sayt katalogi va mobil ilova.
                        .requestMatchers(HttpMethod.GET, "/api/v1/casting-user/web").permitAll()
                        // ⚠️ Bot: anketa yuborish. Anonim, login yo'q.
                        .requestMatchers(HttpMethod.POST, "/api/v1/casting-user").permitAll()
                        // ⚠️ Bot: "mening arizalarim" va "murojaat" sahifalari.
                        .requestMatchers(HttpMethod.GET, "/api/v1/casting-user/my/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/casting-user/appeal/**").permitAll()

                        // --- Analitika hodisalari ---
                        // ⚠️ OCHIQ: reklama ko'rsatilishi tizimga kirmagan foydalanuvchida
                        // ham qayd etilishi kerak. Token bo'lsa foydalanuvchi aniqlanadi.
                        // Suiiste'mol xavfi bor — rate limiting PHASE 9 da.
                        .requestMatchers(HttpMethod.POST, "/api/v1/app/analytics/events").permitAll()

                        // --- Qolgan barcha API yopiq ---
                        // Bu qatorga tushadiganlar (aniq ro'yxat izoh uchun):
                        //   GET    /api/v1/casting-user              — to'liq ro'yxat, shaxsiy ma'lumot bilan
                        //   GET    /api/v1/casting-user/payed/**
                        //   PUT    /api/v1/casting-user/status/**, /price/**, /web-show/**
                        //   DELETE /api/v1/casting-user/**
                        //   PUT    /api/v1/file/**
                        //   POST/PUT/DELETE /api/v1/news/**
                        //   GET    /api/v1/admin/**
                        //   GET    /api/v1/security, /api/v1/auth/decode
                        //   PUT    /api/v1/auth/password/**          — hisob egallab olishning oldi olindi
                        //   /api/v1/app/admin/**                         — yangi admin panel
                        // ─────────────────────────────────────────────────
                        //  ADMIN MAKONI — Spring Security darajasidagi BAZAVIY rol
                        // ─────────────────────────────────────────────────
                        //
                        // Nega bu kerak, agar har bir endpoint o'zi ham tekshirsa:
                        //
                        // Ichkaridagi tekshiruv YOZILISHI kerak. Kimdir yangi admin
                        // endpoint qo'shib uni yozishni unutsa, `/api/**` qoidasi
                        // faqat AUTENTIFIKATSIYA talab qilardi — ya'ni oddiy USER
                        // tokeni bilan ham unga yetib borish mumkin edi.
                        //
                        // Bu qoida esa avtomatik: yangi endpoint qanday yozilishidan
                        // qat'i nazar, USER tokeni admin makoniga UMUMAN kirmaydi.
                        // Xodim ekanligi routing'dan OLDIN tekshiriladi.
                        //
                        // Bu ichkaridagi ruxsat tekshiruvini ALMASHTIRMAYDI —
                        // u qaysi WORKER nima qila olishini hal qiladi. Bu yerda
                        // faqat «umuman xodimmi» degan savol.
                        //
                        // ⚠️ Rol nomlari eski `UserRoles` enum'idan keladi va
                        // `Role.getAuthority()` ularni shu ko'rinishda qaytaradi.
                        .requestMatchers("/api/v1/app/admin/**").hasAnyAuthority(
                                "ROLE_GIPERSUPERADMIN", "ROLE_SUPERADMIN",
                                "ROLE_ADMIN", "ROLE_WORKER")
                        .requestMatchers("/api/**").authenticated()

                        // --- SPA va statik fayllar ---
                        // WebMvcConfig /api dan boshqa hamma yo'lni index.html ga yo'naltiradi,
                        // shuning uchun React marshrutlari ochiq bo'lishi kerak.
                        .anyRequest().permitAll()
                )
                .addFilterBefore(myFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public UserDetailsService users() {
        return username -> userRepo.findByPhone(username)
                .map(user -> (org.springframework.security.core.userdetails.UserDetails) user)
                .orElseThrow(() -> new UsernameNotFoundException("Foydalanuvchi topilmadi: " + username));
    }

    @Bean
    public static PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
