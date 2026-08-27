package com.example.backend.Cms;

import com.example.backend.Cms.Service.Video.VideoMetadata;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@code ffprobe} JSON chiqishini tahlil qilish.
 *
 * <h2>⚠️ Bu yerda {@code ffprobe} ISHGA TUSHIRILMAYDI</h2>
 * U ishlab chiqish mashinasida o'rnatilmagan bo'lishi mumkin va CI da
 * ham kafolatlanmagan. Tahlil mantig'i esa aynan shu yerda —
 * jarayonni ishga tushirish oddiy protsess chaqiruvi.
 *
 * Quyidagi JSON'lar — {@code ffprobe} ning HAQIQIY chiqish shakli.
 */
class FfprobeParsingTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * ⚠️ Parser paket-xususiy: u ichki detal va API emas.
     * Refleksiya faqat SHU sababdan ishlatiladi.
     */
    private VideoMetadata parse(String json) throws Exception {
        Class<?> parser = Class.forName(
                "com.example.backend.Cms.Service.Video.FfprobeOutputParser");
        Method method = parser.getDeclaredMethod("parse",
                com.fasterxml.jackson.databind.JsonNode.class);
        method.setAccessible(true);
        return (VideoMetadata) method.invoke(null, MAPPER.readTree(json));
    }

    @Nested
    @DisplayName("Oddiy video")
    class Basics {

        @Test
        @DisplayName("O'lcham, davomiylik, kodeklar va fps o'qiladi")
        void readsEverything() throws Exception {
            VideoMetadata meta = parse("""
                {
                  "streams": [
                    {"codec_type":"video","codec_name":"h264",
                     "width":1920,"height":1080,"avg_frame_rate":"25/1"},
                    {"codec_type":"audio","codec_name":"aac"}
                  ],
                  "format": {"duration":"3600.500","bit_rate":"5128000"}
                }
                """);

            assertThat(meta.width()).isEqualTo(1920);
            assertThat(meta.height()).isEqualTo(1080);
            // 3600.5 → yaxlitlanadi.
            assertThat(meta.durationSeconds()).isEqualTo(3601);
            assertThat(meta.fps()).isEqualTo(25.0);
            assertThat(meta.videoCodec()).isEqualTo("h264");
            assertThat(meta.audioCodec()).isEqualTo("aac");
            assertThat(meta.bitrate()).isEqualTo(5_128_000L);
            assertThat(meta.isUsable()).isTrue();
        }

        @Test
        @DisplayName("NTSC kasr chastotasi to'g'ri hisoblanadi")
        void ntscFrameRate() throws Exception {
            // 30000/1001 = 29.97 — eng keng tarqalgan kasr.
            VideoMetadata meta = parse("""
                {"streams":[{"codec_type":"video","codec_name":"h264",
                 "width":1280,"height":720,"avg_frame_rate":"30000/1001"}],
                 "format":{"duration":"60"}}
                """);

            assertThat(meta.fps()).isEqualTo(29.97);
        }

        @Test
        @DisplayName("Ovozsiz video — audio kodeki null, XATO emas")
        void videoWithoutAudio() throws Exception {
            VideoMetadata meta = parse("""
                {"streams":[{"codec_type":"video","codec_name":"h264",
                 "width":640,"height":480}],
                 "format":{"duration":"10"}}
                """);

            assertThat(meta.audioCodec()).isNull();
            assertThat(meta.isUsable()).isTrue();
        }
    }

    @Nested
    @DisplayName("Aylantirilgan video")
    class Rotation {

        /**
         * ⚠️ Telefonda vertikal olingan video faylda GORIZONTAL
         * bo'lib yotadi va 90° belgisi bilan keladi.
         *
         * Belgi e'tiborga olinmasa profil tanlash uni gorizontal deb
         * hisoblardi va natija cho'zilgan yoki noto'g'ri o'lchamda
         * chiqardi — buni faqat tayyor videoni ko'rganda bilinardi.
         */
        @Test
        @DisplayName("Display Matrix bo'yicha o'lchamlar ALMASHADI")
        void sideDataRotationSwapsDimensions() throws Exception {
            VideoMetadata meta = parse("""
                {"streams":[{"codec_type":"video","codec_name":"h264",
                 "width":1920,"height":1080,
                 "side_data_list":[{"side_data_type":"Display Matrix","rotation":-90}]}],
                 "format":{"duration":"30"}}
                """);

            assertThat(meta.width()).isEqualTo(1080);
            assertThat(meta.height()).isEqualTo(1920);
        }

        @Test
        @DisplayName("Eski `tags.rotate` ham qo'llab-quvvatlanadi")
        void legacyRotateTag() throws Exception {
            // ⚠️ ffprobe burchakni IKKI xil joyda beradi. Faqat
            // bittasiga qarash yarim holatlarni o'tkazib yuborardi.
            VideoMetadata meta = parse("""
                {"streams":[{"codec_type":"video","codec_name":"h264",
                 "width":1920,"height":1080,"tags":{"rotate":"270"}}],
                 "format":{"duration":"30"}}
                """);

            assertThat(meta.width()).isEqualTo(1080);
            assertThat(meta.height()).isEqualTo(1920);
        }

        @Test
        @DisplayName("180° da o'lchamlar ALMASHMAYDI")
        void upsideDownKeepsDimensions() throws Exception {
            VideoMetadata meta = parse("""
                {"streams":[{"codec_type":"video","codec_name":"h264",
                 "width":1920,"height":1080,
                 "side_data_list":[{"rotation":180}]}],
                 "format":{"duration":"30"}}
                """);

            assertThat(meta.width()).isEqualTo(1920);
            assertThat(meta.height()).isEqualTo(1080);
        }
    }

    @Nested
    @DisplayName("Muqova rasmi")
    class AttachedPicture {

        /**
         * ⚠️ Bu jimgina va og'ir xato bo'lardi.
         *
         * Albom muqovasi joylashtirilgan faylda IKKITA video oqim
         * bo'ladi: haqiqiy video va {@code mjpeg} formatidagi bitta
         * kadr. Muqova ko'pincha ro'yxatda BIRINCHI turadi.
         *
         * Oddiygina birinchisini olsak, 600×600 muqova o'lchamlari
         * videoning o'lchami deb qabul qilinardi va butun transcoding
         * shu asosda qurilardi — 1080p film 600×600 ga siqilardi.
         */
        @Test
        @DisplayName("Muqova emas, HAQIQIY video oqimi tanlanadi")
        void attachedPictureIsSkipped() throws Exception {
            VideoMetadata meta = parse("""
                {
                  "streams": [
                    {"codec_type":"video","codec_name":"mjpeg",
                     "width":600,"height":600,
                     "disposition":{"attached_pic":1}},
                    {"codec_type":"video","codec_name":"h264",
                     "width":1920,"height":1080,
                     "disposition":{"attached_pic":0}},
                    {"codec_type":"audio","codec_name":"aac"}
                  ],
                  "format": {"duration":"5400"}
                }
                """);

            assertThat(meta.width()).isEqualTo(1920);
            assertThat(meta.height()).isEqualTo(1080);
            assertThat(meta.videoCodec()).isEqualTo("h264");
        }

        @Test
        @DisplayName("Faqat muqova bo'lsa u ishlatiladi — bo'sh natija emas")
        void coverOnlyIsBetterThanNothing() throws Exception {
            VideoMetadata meta = parse("""
                {"streams":[{"codec_type":"video","codec_name":"mjpeg",
                 "width":600,"height":600,"disposition":{"attached_pic":1}}],
                 "format":{"duration":"0"}}
                """);

            assertThat(meta.width()).isEqualTo(600);
        }
    }

    @Nested
    @DisplayName("Noto'g'ri va yetishmayotgan qiymatlar")
    class MissingValues {

        @Test
        @DisplayName("`N/A` davomiyligi — null, NOL emas")
        void notAvailableDurationIsNull() throws Exception {
            // ⚠️ 0 yozish YOLG'ON bo'lardi: «nol soniya» va «noma'lum»
            // butunlay boshqa narsa va ikkinchisi pleyer uchun muhim.
            VideoMetadata meta = parse("""
                {"streams":[{"codec_type":"video","codec_name":"h264",
                 "width":1920,"height":1080}],
                 "format":{"duration":"N/A"}}
                """);

            assertThat(meta.durationSeconds()).isNull();
        }

        @Test
        @DisplayName("Davomiylik oqimdan olinadi, agar konteynerda bo'lmasa")
        void durationFallsBackToStream() throws Exception {
            // `.mkv` bunga moyil: umumiy davomiylik ko'rsatilmaydi.
            VideoMetadata meta = parse("""
                {"streams":[{"codec_type":"video","codec_name":"h264",
                 "width":1920,"height":1080,"duration":"120.0"}],
                 "format":{}}
                """);

            assertThat(meta.durationSeconds()).isEqualTo(120);
        }

        @Test
        @DisplayName("`0/0` chastotasi — null, nolga bo'lish YO'Q")
        void zeroFrameRateDoesNotDivideByZero() throws Exception {
            VideoMetadata meta = parse("""
                {"streams":[{"codec_type":"video","codec_name":"h264",
                 "width":1920,"height":1080,"avg_frame_rate":"0/0"}],
                 "format":{"duration":"10"}}
                """);

            assertThat(meta.fps()).isNull();
        }

        @Test
        @DisplayName("Video oqimsiz fayl ISHLATIB BO'LMAYDI deb belgilanadi")
        void audioOnlyIsNotUsable() throws Exception {
            // Kengaytmasi almashtirilgan audio fayl aynan shunday
            // ko'rinadi. `isUsable()` false bo'lmasa profil tanlash
            // tushunarsiz tarzda yiqilardi.
            VideoMetadata meta = parse("""
                {"streams":[{"codec_type":"audio","codec_name":"mp3"}],
                 "format":{"duration":"180"}}
                """);

            assertThat(meta.isUsable()).isFalse();
            assertThat(meta.width()).isNull();
        }

        @Test
        @DisplayName("Bo'sh yoki buzuq JSON yiqilmaydi")
        void emptyJsonIsHandled() throws Exception {
            assertThat(parse("{}").isUsable()).isFalse();
            assertThat(parse("""
                {"streams":[],"format":{}}
                """).isUsable()).isFalse();
        }
    }
}
