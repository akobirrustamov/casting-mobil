package com.example.backend.Cms.Service.Storage;

import com.example.backend.Cms.Service.Video.HlsUploadService;
import com.example.backend.Cms.Service.Video.HlsPlaylistService;
import com.example.backend.Cms.Service.Video.PresignedUrlProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.Resource;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Comparator;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * HAQIQIY S3 serveri bilan to'liq sinov.
 *
 * <h2>⚠️ Nima uchun bu test kerak</h2>
 * Qolgan barcha S3 testlari {@code S3Client} ni MOCK qiladi: ular
 * bizning hisob-kitobimizni tekshiradi, lekin S3 ning javobini EMAS.
 *
 * Ya'ni {@code S3StorageService}, {@code S3MultipartUploadService} va
 * {@code HlsUploadService} hech qachon haqiqiy S3 bilan gaplashmagan.
 * Presigned havolalar, multipart yig'ish, {@code ListParts} — hammasi
 * faqat kutilgan xatti-harakat asosida yozilgan.
 *
 * Bu — butun ish ichidagi eng yuqori xavf edi.
 *
 * <h2>MinIO — S3 bilan mos server</h2>
 * Hech narsa sotib olish kerak emas va Timeweb kalitlari
 * ishlatilmaydi (§30). MinIO AWS S3 API'sini amalga oshiradi, ya'ni
 * bu yerda o'tgan kod Timeweb'da ham o'tishi kutiladi.
 *
 * ⚠️ MinIO ishlamayotgan bo'lsa test O'TKAZIB YUBORILADI — CI da u
 * bo'lmasligi mumkin.
 *
 * <h2>Ishga tushirish</h2>
 * <pre>
 *   MINIO_ROOT_USER=testkey MINIO_ROOT_PASSWORD=testsecret123 \
 *     minio server /tmp/minio-data --address :9100
 * </pre>
 */
class S3IntegrationTest {

    private static final String ENDPOINT = "http://localhost:9100";
    private static final String BUCKET = "uzcasting-test";
    private static final String KEY = "testkey";
    private static final String SECRET = "testsecret123";

    private static boolean available;
    private static S3Client s3;
    private static S3Presigner presigner;
    private static S3Properties properties;

    @BeforeAll
    static void setUp() {
        properties = new S3Properties();
        properties.setEndpoint(ENDPOINT);
        properties.setRegion("us-east-1");
        properties.setBucket(BUCKET);
        properties.setAccessKey(KEY);
        properties.setSecretKey(SECRET);
        properties.setUploadUrlTtl(Duration.ofMinutes(10));

        var credentials = StaticCredentialsProvider.create(
                AwsBasicCredentials.create(KEY, SECRET));
        var pathStyle = S3Configuration.builder().pathStyleAccessEnabled(true).build();

        s3 = S3Client.builder()
                .endpointOverride(URI.create(ENDPOINT))
                .region(Region.US_EAST_1)
                .credentialsProvider(credentials)
                .serviceConfiguration(pathStyle)
                .build();

        presigner = S3Presigner.builder()
                .endpointOverride(URI.create(ENDPOINT))
                .region(Region.US_EAST_1)
                .credentialsProvider(credentials)
                .serviceConfiguration(pathStyle)
                .build();

        try {
            s3.createBucket(CreateBucketRequest.builder().bucket(BUCKET).build());
            available = true;
        } catch (S3Exception e) {
            // Bucket allaqachon bor — bu ham ishlaydigan holat.
            available = e.statusCode() == 409;
        } catch (RuntimeException e) {
            available = false;
        }
    }

    private S3StorageService storage() {
        return new S3StorageService(s3, properties);
    }

    private S3MultipartUploadService multipart() {
        return new S3MultipartUploadService(s3, presigner, properties);
    }

    @Nested
    @DisplayName("Oddiy saqlash")
    class Storing {

        @Test
        @DisplayName("Oqim saqlanadi va QAYTA o'qiladi")
        void streamRoundTrip() throws Exception {
            assumeTrue(available, "MinIO ishlamayapti — test o'tkazib yuborildi");

            byte[] data = "salom-dunyo".getBytes(StandardCharsets.UTF_8);
            String key = storage().store(new ByteArrayInputStream(data), "test.mp4", "content");

            assertThat(key).startsWith("/content/").endsWith(".mp4");

            Resource loaded = storage().load(key);
            assertThat(loaded.contentLength()).isEqualTo(data.length);
            try (InputStream in = loaded.getInputStream()) {
                assertThat(in.readAllBytes()).isEqualTo(data);
            }
        }

        /**
         * ⚠️ Berilmasa S3 {@code binary/octet-stream} qo'yadi va CDN ham
         * shuni qaytaradi — pleyer playlistni tanimaydi.
         */
        @Test
        @DisplayName("Content-Type S3 da SAQLANADI")
        void contentTypeIsStored() {
            assumeTrue(available, "MinIO ishlamayapti");

            String key = "/videos/1/hls/master.m3u8";
            storage().storeAt(new ByteArrayInputStream("#EXTM3U".getBytes()),
                    key, MediaContentTypes.of(key));

            var head = s3.headObject(HeadObjectRequest.builder()
                    .bucket(BUCKET).key("videos/1/hls/master.m3u8").build());

            assertThat(head.contentType()).isEqualTo("application/vnd.apple.mpegurl");
        }

        /**
         * ⚠️ Boshidagi {@code /} S3 da BO'SH NOMLI papka yasardi:
         * brauzerda ochilardi, lekin konsolda topib bo'lmasdi.
         */
        @Test
        @DisplayName("Kalitdagi boshlang'ich `/` obyekt nomiga TUSHMAYDI")
        void leadingSlashIsStripped() {
            assumeTrue(available, "MinIO ishlamayapti");

            storage().storeAt(new ByteArrayInputStream("x".getBytes()),
                    "/videos/2/probe.txt", "text/plain");

            var listed = s3.listObjectsV2(ListObjectsV2Request.builder()
                    .bucket(BUCKET).prefix("videos/2/").build());

            assertThat(listed.contents()).isNotEmpty();
            assertThat(listed.contents().get(0).key()).isEqualTo("videos/2/probe.txt");
        }

        @Test
        @DisplayName("Mavjudlik va o'chirish ishlaydi")
        void existsAndDelete() {
            assumeTrue(available, "MinIO ishlamayapti");

            String key = storage().store(
                    new ByteArrayInputStream("o'chiriladi".getBytes()), "a.mp4", "content");

            assertThat(storage().exists(key)).isTrue();
            storage().delete(key);
            assertThat(storage().exists(key)).isFalse();

            // ⚠️ Yo'q faylni o'chirish XATO emas — baza yozuvi allaqachon
            // o'chirilgan bo'lishi mumkin.
            storage().delete(key);
        }
    }

    @Nested
    @DisplayName("⚠️ Presigned multipart — brauzer yo'li")
    class Multipart {

        /**
         * Butun ishning MA'NOSI shu yerda: fayl Spring Boot orqali
         * O'TMAYDI, u imzolangan havola bilan to'g'ridan-to'g'ri
         * omborga ketadi.
         *
         * Bu test aynan shuni takrorlaydi: server havola beradi,
         * "brauzer" (bu yerda oddiy HTTP ulanish) baytlarni yuboradi,
         * server esa S3 dan nima kelganini so'rab yig'adi.
         */
        @Test
        @DisplayName("Imzolangan havola bilan yuklash va yig'ish")
        void presignedUploadRoundTrip() throws Exception {
            assumeTrue(available, "MinIO ishlamayapti");

            var service = multipart();
            String storageKey = "/videos/multipart/source.mp4";

            String uploadId = service.begin(storageKey, "kino.mp4");
            assertThat(uploadId).isNotBlank();

            // ⚠️ 5 MB — S3 ning eng kichik bo'lak chegarasi. Undan
            // kichik bo'lak (oxirgisidan tashqari) rad etiladi.
            byte[] first = filled(5 * 1024 * 1024, (byte) 'A');
            byte[] second = filled(1024, (byte) 'B');

            put(service.presignPart(storageKey, uploadId, 1), first);
            put(service.presignPart(storageKey, uploadId, 2), second);

            // Server S3 dan SO'RAYDI — klientga ishonmaydi.
            assertThat(service.receivedParts(storageKey, uploadId))
                    .containsExactly(1, 2);

            long size = service.complete(storageKey, uploadId, 2);
            assertThat(size).isEqualTo(first.length + second.length);

            // Yig'ilgan fayl haqiqatan to'g'ri tartibda.
            try (InputStream in = s3.getObject(GetObjectRequest.builder()
                    .bucket(BUCKET).key("videos/multipart/source.mp4").build())) {
                byte[] all = in.readAllBytes();
                assertThat(all).hasSize(first.length + second.length);
                assertThat(all[0]).isEqualTo((byte) 'A');
                assertThat(all[first.length]).isEqualTo((byte) 'B');
            }
        }

        /**
         * ⚠️ Bekor qilinmagan multipart bo'laklari bucketda qoladi,
         * ro'yxatda KO'RINMAYDI va ular uchun pul olinaveradi.
         */
        @Test
        @DisplayName("Bekor qilingandan keyin bo'laklar QOLMAYDI")
        void abortRemovesParts() throws Exception {
            assumeTrue(available, "MinIO ishlamayapti");

            var service = multipart();
            String storageKey = "/videos/aborted/source.mp4";

            String uploadId = service.begin(storageKey, "kino.mp4");
            put(service.presignPart(storageKey, uploadId, 1), filled(5 * 1024 * 1024, (byte) 'C'));

            service.abort(storageKey, uploadId);

            // Endi bu yuklash mavjud emas — ro'yxat so'rovi xato beradi.
            assertThat(s3.listObjectsV2(ListObjectsV2Request.builder()
                    .bucket(BUCKET).prefix("videos/aborted/").build()).contents())
                    .isEmpty();
        }

        @Test
        @DisplayName("Bo'lak yetishmasa yig'ish RAD etiladi")
        void incompleteUploadIsRejected() throws Exception {
            assumeTrue(available, "MinIO ishlamayapti");

            var service = multipart();
            String storageKey = "/videos/incomplete/source.mp4";
            String uploadId = service.begin(storageKey, "kino.mp4");

            put(service.presignPart(storageKey, uploadId, 1), filled(5 * 1024 * 1024, (byte) 'D'));

            // Ikkita kutilgan, bittasi kelgan.
            org.assertj.core.api.Assertions
                    .assertThatThrownBy(() -> service.complete(storageKey, uploadId, 2))
                    .hasMessageContaining("to'liq emas");

            service.abort(storageKey, uploadId);
        }
    }

    @Nested
    @DisplayName("HLS papkasini yuklash")
    class HlsUpload {

        /**
         * ⚠️ {@code master.m3u8} ENG OXIRIDA yuklanadi.
         *
         * Uning paydo bo'lishi «video tayyor» degani. Birinchi
         * yuklansa, segmentlar hali kelmagan paytda pleyer uni o'qib,
         * mavjud bo'lmagan fayllarni so'rardi.
         */
        @Test
        @DisplayName("Butun papka yuklanadi va Content-Type to'g'ri")
        void directoryIsUploaded() throws Exception {
            assumeTrue(available, "MinIO ishlamayapti");

            Path dir = Files.createTempDirectory("hls-upload");
            try {
                Files.createDirectories(dir.resolve("720p"));
                Files.writeString(dir.resolve("master.m3u8"), "#EXTM3U\n720p/index.m3u8\n");
                Files.writeString(dir.resolve("720p/index.m3u8"), "#EXTM3U\n");
                Files.write(dir.resolve("720p/init.mp4"), new byte[]{1, 2, 3});
                Files.write(dir.resolve("720p/segment_00001.m4s"), new byte[]{4, 5, 6});

                String masterKey = new HlsUploadService(storage()).upload(dir, 77L);
                assertThat(masterKey).isEqualTo("/videos/77/hls/master.m3u8");

                var listed = s3.listObjectsV2(ListObjectsV2Request.builder()
                        .bucket(BUCKET).prefix("videos/77/hls/").build());

                assertThat(listed.contents())
                        .extracting(o -> o.key().replace("videos/77/hls/", ""))
                        .containsExactlyInAnyOrder(
                                "master.m3u8", "720p/index.m3u8",
                                "720p/init.mp4", "720p/segment_00001.m4s");

                // ⚠️ Segment turi — usiz CDN `octet-stream` qaytaradi.
                var head = s3.headObject(HeadObjectRequest.builder()
                        .bucket(BUCKET).key("videos/77/hls/720p/segment_00001.m4s").build());
                assertThat(head.contentType()).isEqualTo("video/iso.segment");

            } finally {
                deleteTree(dir);
            }
        }

        @Test
        @DisplayName("master.m3u8 bo'lmasa ANIQ xato beriladi")
        void missingMasterIsRejected() throws Exception {
            assumeTrue(available, "MinIO ishlamayapti");

            Path dir = Files.createTempDirectory("hls-no-master");
            try {
                Files.createDirectories(dir.resolve("720p"));
                Files.writeString(dir.resolve("720p/index.m3u8"), "#EXTM3U\n");

                org.assertj.core.api.Assertions
                        .assertThatThrownBy(() -> new HlsUploadService(storage()).upload(dir, 78L))
                        .hasMessageContaining("master.m3u8");
            } finally {
                deleteTree(dir);
            }
        }
    }

    // ------------------------------------------------------- yordamchilar

    private static byte[] filled(int size, byte value) {
        byte[] data = new byte[size];
        java.util.Arrays.fill(data, value);
        return data;
    }

    /** Imzolangan havolaga baytlarni yuboradi — brauzer qiladigan ish. */
    private static void put(String presignedUrl, byte[] body) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(presignedUrl).openConnection();
        connection.setDoOutput(true);
        connection.setRequestMethod("PUT");
        connection.setFixedLengthStreamingMode(body.length);
        try (OutputStream out = connection.getOutputStream()) {
            out.write(body);
        }
        int code = connection.getResponseCode();
        connection.disconnect();

        assertThat(code)
                .as("imzolangan havola rad etildi — imzo yoki sarlavhalar noto'g'ri")
                .isBetween(200, 299);
    }

    @Nested
    @DisplayName("Segment uchun imzolangan havola (§4.10)")
    class SignedUrls {

        /**
         * @param window kesh oynasi — shu ichida bir xil kalit uchun
         *               AYNAN bir xil satr qaytishi kerak
         */
        private PresignedUrlProvider provider(Duration window) {
            PresignedUrlProvider p = new PresignedUrlProvider(presigner, properties);
            ReflectionTestUtils.setField(p, "ttl", Duration.ofHours(4));
            ReflectionTestUtils.setField(p, "window", window);
            return p;
        }

        /**
         * ⚠️ ENG MUHIM TEKSHIRUV: havola HAQIQATDAN ochiladimi.
         *
         * Qolgan hamma narsa to'g'ri bo'lib, faqat imzo noto'g'ri
         * bo'lsa — pleyer har segmentda 403 olardi va video umuman
         * ochilmasdi. Buni faqat haqiqiy server ayta oladi.
         */
        @Test
        @DisplayName("Imzolangan havola faylni QAYTARADI")
        void signedUrlActuallyFetches() throws Exception {
            assumeTrue(available, "MinIO ishlamayapti");

            byte[] body = "segment-mazmuni".getBytes(StandardCharsets.UTF_8);
            String key = "/videos/410/hls/720p/segment_00001.m4s";
            new S3StorageService(s3, properties)
                    .storeAt(new ByteArrayInputStream(body), key, MediaContentTypes.of(key));

            assertThat(fetch(provider(Duration.ofHours(1)).sign(key))).isEqualTo(body);
        }

        /**
         * ⚠️ CDN uchun HAL QILUVCHI xususiyat.
         *
         * S3 imzosi {@code X-Amz-Date} ni o'z ichiga oladi, ya'ni har
         * chaqiruv boshqa satr berardi. CDN uchun bu boshqa manzil
         * degani: 3000 tomoshabin bitta filmni ko'rsa, kesh umuman
         * ishlamay, butun trafik omborga tushardi.
         */
        @Test
        @DisplayName("Bir oyna ichida havola O'ZGARMAYDI")
        void stableWithinWindow() {
            assumeTrue(available, "MinIO ishlamayapti");

            PresignedUrlProvider p = provider(Duration.ofHours(1));
            String key = "/videos/410/hls/720p/segment_00002.m4s";

            assertThat(p.sign(key)).isEqualTo(p.sign(key));
        }

        /**
         * Turli kalitlar chalkashib ketmasligi kerak — aks holda
         * pleyer har segmentda bir xil faylni olardi.
         */
        @Test
        @DisplayName("Har kalit o'z havolasini oladi")
        void keysDoNotCollide() {
            assumeTrue(available, "MinIO ishlamayapti");

            PresignedUrlProvider p = provider(Duration.ofHours(1));

            assertThat(p.sign("/videos/410/a.m4s"))
                    .isNotEqualTo(p.sign("/videos/410/b.m4s"));
        }

        /**
         * ⚠️ Kesh CHEKLANMAGAN bo'lsa xotirani yeb qo'yardi: ikki
         * soatlik filmda 1200 ta segment, har foydalanuvchi uchun.
         *
         * Oyna almashgach eski yozuvlar tashlanadi — buni havola
         * o'zgargani bilan tekshirish mumkin.
         */
        @Test
        @DisplayName("Oyna almashsa havola yangilanadi")
        void refreshesAfterWindow() throws Exception {
            assumeTrue(available, "MinIO ishlamayapti");

            // Bir soniyalik oyna — chegara albatta kesib o'tiladi.
            PresignedUrlProvider p = provider(Duration.ofSeconds(1));
            String key = "/videos/410/hls/720p/segment_00003.m4s";

            String first = p.sign(key);
            Thread.sleep(1100);
            String second = p.sign(key);

            assertThat(second).isNotEqualTo(first);
        }
    }

    @Nested
    @DisplayName("⚠️ Uchma-uch: playlist → imzolangan segment (§4.10)")
    class EndToEnd {

        /**
         * ⚠️ Bu yerda ZANJIR sinaladi, bo'laklar emas.
         *
         * Playlist qayta yozilishi mock ombor bilan, imzolash esa
         * alohida sinalgan. Ikkalasi birga ishlaydimi — hech qayerda
         * tekshirilmagan edi, va aynan shu joyda jimgina buzilish
         * bo'lardi: playlist chiroyli qaytadi, HTTP 200 keladi,
         * segment esa 403 bilan yopiladi va «video ochilmadi» degan
         * sababsiz nosozlik chiqadi.
         */
        @Test
        @DisplayName("Playlistdagi segment havolasi HAQIQATDAN ochiladi")
        void rewrittenSegmentIsFetchable() throws Exception {
            assumeTrue(available, "MinIO ishlamayapti");

            String dir = "/videos/e2e-410/hls/720p";
            byte[] segment = "haqiqiy-segment".getBytes(StandardCharsets.UTF_8);

            S3StorageService storage = new S3StorageService(s3, properties);
            storage.storeAt(new ByteArrayInputStream("""
                    #EXTM3U
                    #EXT-X-MAP:URI="init.mp4"
                    #EXTINF:6.000,
                    segment_00001.m4s
                    #EXT-X-ENDLIST
                    """.getBytes(StandardCharsets.UTF_8)),
                    dir + "/index.m3u8", MediaContentTypes.of("index.m3u8"));
            storage.storeAt(new ByteArrayInputStream(segment),
                    dir + "/segment_00001.m4s", MediaContentTypes.of("segment_00001.m4s"));

            PresignedUrlProvider signer = new PresignedUrlProvider(presigner, properties);
            ReflectionTestUtils.setField(signer, "ttl", Duration.ofHours(4));
            ReflectionTestUtils.setField(signer, "window", Duration.ofHours(1));

            String playlist = new HlsPlaylistService(storage)
                    .rewrite(dir + "/index.m3u8", signer::sign);

            // Playlistdan segment havolasini AYNAN pleyer kabi ajratamiz.
            String segmentUrl = playlist.lines()
                    .filter(line -> line.contains("segment_00001.m4s"))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("segment qatori yo'q"));

            assertThat(segmentUrl).startsWith("http");
            assertThat(fetch(segmentUrl)).isEqualTo(segment);
        }
    }

    /** Imzolangan havola bo'yicha faylni o'qiydi. */
    private static byte[] fetch(String presignedUrl) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(presignedUrl).openConnection();
        connection.setRequestMethod("GET");

        int code = connection.getResponseCode();
        assertThat(code)
                .as("imzolangan havola rad etildi — pleyer segmentni ololmasdi")
                .isBetween(200, 299);

        try (InputStream in = connection.getInputStream()) {
            return in.readAllBytes();
        } finally {
            connection.disconnect();
        }
    }

    private static void deleteTree(Path dir) {
        try (Stream<Path> walk = Files.walk(dir)) {
            walk.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException ignored) {
                    // test tozalashi
                }
            });
        } catch (IOException ignored) {
            // test tozalashi
        }
    }
}
