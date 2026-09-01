package com.example.backend.Config;

import com.example.backend.Admin.PermissionInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.ResourceResolver;
import org.springframework.web.servlet.resource.ResourceResolverChain;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;



@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final PermissionInterceptor permissionInterceptor;

    /**
     * Ruxsat tekshiruvi so'rov tanasi o'qilishidan oldin ishlashi uchun.
     * Batafsil: {@link PermissionInterceptor}.
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(permissionInterceptor).addPathPatterns("/api/**");
    }

    /**
     * Ruxsat berilgan manbalar. Standarti — hammasi, ya'ni avvalgi
     * xatti-harakat saqlanadi. Ishlab chiqarishda toraytirish uchun:
     * {@code app.cors.allowed-origins=https://uzcasting.site}
     */
    @Value("${app.cors.allowed-origins:*}")
    private String allowedOrigins;

    /**
     * CORS sozlamasi.
     *
     * <h2>⚠️ Nega {@code allowedOriginPatterns}, {@code allowedOrigins} emas</h2>
     * Panel refresh tokenni {@code httpOnly} cookie'da oladi va
     * shuning uchun so'rovlarni {@code withCredentials} bilan
     * yuboradi (§61). Brauzer bunday so'rovda IKKI narsani talab
     * qiladi:
     *
     * <ul>
     *   <li>{@code Access-Control-Allow-Credentials: true};</li>
     *   <li>{@code Access-Control-Allow-Origin} ANIQ manba bo'lsin —
     *       {@code *} qabul qilinmaydi.</li>
     * </ul>
     *
     * {@code allowedOrigins("*")} aynan yulduzcha yuboradi, ya'ni
     * brauzer javobni BLOKLAYDI va klient «server bilan aloqa yo'q»
     * xatosini ko'radi — server esa 200 qaytargan bo'ladi.
     *
     * {@code allowedOriginPatterns} esa so'rovdagi manbani qaytaradi
     * va shart bajariladi.
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns(allowedOrigins.split("\\s*,\\s*"))
                .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
//                .setCacheControl(CacheControl.maxAge(365, TimeUnit.DAYS))
                .resourceChain(false)
                .addResolver(new PushStateResourceResolver());
    }

    private class PushStateResourceResolver implements ResourceResolver {
        private Resource index = new ClassPathResource("/static/index.html");
        private List<String> handledExtensions = Arrays.asList("html", "js", "json", "csv", "css", "png", "svg", "eot", "ttf", "otf", "woff", "appcache", "jpg", "jpeg", "gif", "ico" );
        private List<String> ignoredPaths = Arrays.asList("api");

        @Override
        public Resource resolveResource(HttpServletRequest   request, String requestPath, List<? extends Resource> locations, ResourceResolverChain chain) {
            return resolve(requestPath, locations);
        }

        @Override
        public String resolveUrlPath(String resourcePath, List<? extends Resource> locations, ResourceResolverChain chain) {
            Resource resolvedResource = resolve(resourcePath, locations);
            if (resolvedResource == null) {
                return null;
            }
            try {
                return resolvedResource.getURL().toString();
            } catch (IOException e) {
                return resolvedResource.getFilename();
            }
        }

        private Resource resolve(String requestPath, List<? extends Resource> locations) {
            if (isIgnored(requestPath)) {
                return null;
            }
            if (isHandled(requestPath)) {
                return locations.stream()
                        .map(loc -> createRelative(loc, requestPath))
                        .filter(resource -> resource != null && resource.exists())
                        .findFirst()
                        // ⚠️ `orElse`, `orElseGet` EMAS.
                        //
                        // `orElseGet(null)` — bu «bo'sh bo'lsa SHU
                        // FUNKSIYANI chaqir» degani, va funksiya null.
                        // Ya'ni fayl topilmagan zahoti
                        // `supplier.get()` da NullPointerException
                        // chiqardi.
                        //
                        // Natijada mavjud bo'lmagan har qanday statik
                        // fayl 404 emas, 500 qaytarardi va logga
                        // stack trace yozilardi. `manifest.json`
                        // so'raydigan `logo192.png` ham shular
                        // qatorida — ya'ni buni HAR BIR brauzer
                        // keltirib chiqarardi.
                        .orElse(null);
            }
            return index;
        }

        private Resource createRelative(Resource resource, String relativePath) {
            try {
                return resource.createRelative(relativePath);
            } catch (IOException e) {
                return null;
            }
        }

        /**
         * SPA fallback API yo'llariga TEGMASLIGI kerak.
         *
         * ⚠️ Ilgari bu {@code ignoredPaths.contains(path)} edi, ya'ni faqat
         * AYNAN "api" degan yo'lni tanirdi. Natijada mavjud bo'lmagan API
         * yo'li ({@code /api/v1/app/admin/xato}) 404 emas, {@code index.html}
         * sahifasini 200 bilan qaytarardi.
         *
         * Klient uchun bu chalg'ituvchi: JSON kutgan joyda HTML keladi va
         * xato "yo'q endpoint" emas, "javobni o'qib bo'lmadi" ko'rinishida
         * chiqadi.
         *
         * Endi prefiks bo'yicha tekshiriladi.
         */
        private boolean isIgnored(String path) {
            String normalized = path.startsWith("/") ? path.substring(1) : path;
            return ignoredPaths.stream()
                    .anyMatch(ignored -> normalized.equals(ignored)
                            || normalized.startsWith(ignored + "/"));
        }

        private boolean isHandled(String path) {
            String extension = StringUtils.getFilenameExtension(path);
            return handledExtensions.stream().anyMatch(ext -> ext.equals(extension));
        }
    }

}