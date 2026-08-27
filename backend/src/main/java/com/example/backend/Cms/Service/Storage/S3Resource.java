package com.example.backend.Cms.Service.Storage;

import org.springframework.core.io.AbstractResource;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * S3 obyekti Spring {@code Resource} sifatida.
 *
 * <h2>⚠️ Nega oddiy {@code InputStreamResource} yaramaydi</h2>
 * Video {@code Range} sarlavhasi bilan bo'laklab beriladi
 * ({@code MediaController.rawRange}). Spring buni {@code ResourceRegion}
 * orqali qiladi, u esa oqimni ochib <b>{@code skip(boshlanish)}</b>
 * chaqiradi.
 *
 * Oddiy oqimda {@code skip} — bu baytlarni O'QIB, tashlab yuborish.
 * Ya'ni 2 GB lik videoning oxiriga o'tish uchun S3 dan 2 GB tortilardi.
 * Foydalanuvchi videoni oldinga surganda har safar. Trafik ham, kutish
 * ham qabul qilib bo'lmas darajada bo'lardi.
 *
 * Bu klass {@code skip} ni ushlaydi va o'rniga S3 ga <b>ranged GET</b>
 * yuboradi: server kerakli joydan boshlab beradi, ortiqcha bayt
 * o'tkazilmaydi.
 *
 * <h2>Uzunlik oldindan ma'lum</h2>
 * {@code contentLength()} tarmoqqa chiqmaydi — u {@code HEAD} javobidan
 * olinib, konstruktorga beriladi. Aks holda har bir {@code Range}
 * so'rovi ikkita murojaat qilardi.
 */
public class S3Resource extends AbstractResource {

    private final S3Client s3;
    private final String bucket;
    private final String key;
    private final long length;

    public S3Resource(S3Client s3, String bucket, String key, long length) {
        this.s3 = s3;
        this.bucket = bucket;
        this.key = key;
        this.length = length;
    }

    @Override
    public String getDescription() {
        return "S3[" + bucket + "/" + key + "]";
    }

    @Override
    public long contentLength() {
        return length;
    }

    @Override
    public boolean exists() {
        return true;
    }

    @Override
    public String getFilename() {
        int slash = key.lastIndexOf('/');
        return slash < 0 ? key : key.substring(slash + 1);
    }

    @Override
    public InputStream getInputStream() {
        return new SkipByRange(open(0), 0);
    }

    /** Berilgan bayt o'rnidan boshlab oqim ochadi. */
    private InputStream open(long from) {
        GetObjectRequest.Builder request = GetObjectRequest.builder()
                .bucket(bucket)
                .key(key);
        if (from > 0) {
            request.range("bytes=" + from + "-");
        }
        return s3.getObject(request.build());
    }

    /**
     * {@code skip} ni ranged GET ga aylantiruvchi o'ram.
     *
     * ⚠️ {@code skip} shartnomasi bo'yicha u so'ralgandan KAM baytni
     * o'tkazishi mumkin va chaqiruvchi buni hisobga oladi. Shuning uchun
     * qaytariladigan qiymat har doim haqiqatda o'tkazilgan miqdor.
     */
    private final class SkipByRange extends FilterInputStream {

        private long position;

        private SkipByRange(InputStream in, long position) {
            super(in);
            this.position = position;
        }

        @Override
        public long skip(long n) throws IOException {
            if (n <= 0) {
                return 0;
            }
            long target = Math.min(position + n, length);
            long skipped = target - position;
            if (skipped <= 0) {
                return 0;
            }

            // Eski oqim yopiladi — aks holda ulanish pulida osilib qolardi.
            in.close();
            in = open(target);
            position = target;
            return skipped;
        }

        @Override
        public int read() throws IOException {
            int value = in.read();
            if (value >= 0) {
                position++;
            }
            return value;
        }

        @Override
        public int read(byte[] buffer, int offset, int count) throws IOException {
            int read = in.read(buffer, offset, count);
            if (read > 0) {
                position += read;
            }
            return read;
        }
    }
}
