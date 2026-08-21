package com.example.backend.exceptions;

import com.example.backend.DTO.ApiError;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Yagona xato formati (§94). Frontend'ga stacktrace ketmaydi.
 *
 * ⚠️ Mavjud controller'lar hozircha xatolarni o'zlari qaytaradi — bu handler ularni
 * o'zgartirmaydi, faqat ushlanmagan holatlarni qamrab oladi.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiError> handleBusiness(BusinessException ex) {
        return ResponseEntity.status(ex.getStatus())
                .body(new ApiError(ex.getCode(), ex.getMessage()));
    }

    /** Mavjud kod ishlatadi — 401 xatti-harakati saqlanadi. */
    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ApiError> handleInvalidCredentials(InvalidCredentialsException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ApiError("INVALID_CREDENTIALS", ex.getMessage()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ApiError("ACCESS_DENIED", "Ruxsat yo'q"));
    }

    /**
     * Yo'ldagi yoki so'rovdagi parametr TURI mos kelmasa.
     *
     * <h2>Nima uchun qo'shildi</h2>
     * Bu ushlanmaganda har qanday noto'g'ri parametr <b>500</b> qaytarardi.
     * Masalan {@code /api/v1/file/getFile/1} — u yerda UUID kutiladi.
     * Klient uchun 500 «serverda nosozlik» degani, aslida esa so'rov
     * noto'g'ri edi.
     *
     * ⚠️ Bu tekshiruv ruxsatdan OLDIN ishlaydi (Spring argumentlarni
     * controller chaqirilishidan oldin o'giradi). Shuning uchun javobda
     * faqat parametr nomi va kutilgan tur bo'ladi — resurs mavjudligi
     * yoki ruxsat haqida hech nima oshkor qilinmaydi.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String expected = ex.getRequiredType() == null
                ? "boshqa tur" : ex.getRequiredType().getSimpleName();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiError("INVALID_PARAMETER",
                        "'" + ex.getName() + "' parametri noto'g'ri: " + expected + " kutilmoqda"));
    }

    /**
     * Majburiy parametr yoki fayl yuborilmagan, yoki tana o'qib bo'lmadi.
     *
     * Bularning barchasi KLIYENT xatosi, lekin ushlanmaganda 500 qaytardi —
     * ya'ni «serverda nosozlik» deb ko'rinardi va haqiqiy nosozliklardan
     * ajratib bo'lmasdi.
     *
     * ⚠️ Bu ham ruxsatdan OLDIN ishlaydi: Spring argumentlarni controller
     * chaqirilishidan avval hal qiladi. Javobda faqat parametr nomi bo'ladi.
     */
    @ExceptionHandler({
            MissingServletRequestParameterException.class,
            MissingServletRequestPartException.class,
            MultipartException.class,
            HttpMessageNotReadableException.class})
    public ResponseEntity<ApiError> handleBadRequest(Exception ex) {
        String message;
        if (ex instanceof MissingServletRequestParameterException missing) {
            message = "'" + missing.getParameterName() + "' parametri majburiy";
        } else if (ex instanceof MissingServletRequestPartException part) {
            message = "'" + part.getRequestPartName() + "' fayli majburiy";
        } else if (ex instanceof MultipartException) {
            message = "Fayl noto'g'ri yuborildi (multipart/form-data kutilmoqda)";
        } else {
            message = "So'rov tanasini o'qib bo'lmadi";
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiError("BAD_REQUEST", message));
    }

    /**
     * Endpoint bor, lekin bu HTTP metodi qo'llab-quvvatlanmaydi.
     *
     * Ushlanmaganda 500 qaytardi. Masalan {@code DELETE /staff/{id}} —
     * xodimlar ATAYLAB o'chirilmaydi (faolsizlantirish ishlatiladi), va
     * klient buni «server buzuq» emas, «bunday amal yo'q» deb bilishi kerak.
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiError> handleMethodNotAllowed(
            HttpRequestMethodNotSupportedException ex) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(new ApiError("METHOD_NOT_ALLOWED",
                        ex.getMethod() + " bu manzil uchun qo'llab-quvvatlanmaydi"));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex) {
        List<ApiError.FieldErrorItem> fields = ex.getBindingResult().getFieldErrors().stream()
                .map(this::toItem)
                .collect(Collectors.toList());
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(new ApiError("VALIDATION_ERROR", "Kiritilgan ma'lumot noto'g'ri", fields));
    }

    private ApiError.FieldErrorItem toItem(FieldError error) {
        return new ApiError.FieldErrorItem(error.getField(), error.getDefaultMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiError("BAD_REQUEST", ex.getMessage()));
    }

    /** Spring o'zi tashlagan status-xatolar formatini ham bir xil qilamiz. */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiError> handleResponseStatus(ResponseStatusException ex) {
        return ResponseEntity.status(ex.getStatusCode())
                .body(new ApiError("HTTP_ERROR", ex.getReason()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception ex) {
        // Batafsil ma'lumot faqat logga. Klientga umumiy xabar.
        log.error("Kutilmagan xato", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiError("INTERNAL_ERROR", "Ichki xato. Keyinroq urinib ko'ring."));
    }
}
