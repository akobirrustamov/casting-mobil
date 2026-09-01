import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import ApiCall from '../../config/index';
import Header from "../BotHeader/BotHeader";


// Avval DataForm.css da bo'lgan uslublar — endi Tailwind sinflari.
// Diqqat: sinf nomlari to'liq yozilgan, chunki Tailwind JIT manba matnini
// statik skanerlaydi (dinamik yasalgan nomlarni ko'rmaydi).
const formGroupClass =
    "relative " +
    "[&_label]:block [&_label]:mb-3 [&_label]:font-medium [&_label]:text-white [&_label]:text-[0.9rem] " +
    "[&_input]:w-full [&_input]:px-4 [&_input]:py-[0.85rem] [&_input]:bg-[#2e2e38] [&_input]:border [&_input]:border-[#3d3d47] [&_input]:rounded-lg [&_input]:text-[0.95rem] [&_input]:text-[#f8f9fa] [&_input]:transition-all [&_input]:duration-300 [&_input]:appearance-none " +
    "[&_textarea]:w-full [&_textarea]:px-4 [&_textarea]:py-[0.85rem] [&_textarea]:bg-[#2e2e38] [&_textarea]:border [&_textarea]:border-[#3d3d47] [&_textarea]:rounded-lg [&_textarea]:text-[0.95rem] [&_textarea]:text-[#f8f9fa] [&_textarea]:transition-all [&_textarea]:duration-300 [&_textarea]:appearance-none " +
    "[&_select]:w-full [&_select]:px-4 [&_select]:py-[0.85rem] [&_select]:bg-[#2e2e38] [&_select]:border [&_select]:border-[#3d3d47] [&_select]:rounded-lg [&_select]:text-[0.95rem] [&_select]:text-[#f8f9fa] [&_select]:transition-all [&_select]:duration-300 [&_select]:appearance-none " +
    "[&_input:focus]:border-[#4895ef] [&_input:focus]:outline-none [&_input:focus]:shadow-[0_0_0_3px_rgba(72,149,239,0.2)] " +
    "[&_textarea:focus]:border-[#4895ef] [&_textarea:focus]:outline-none [&_textarea:focus]:shadow-[0_0_0_3px_rgba(72,149,239,0.2)] " +
    "[&_select:focus]:border-[#4895ef] [&_select:focus]:outline-none [&_select:focus]:shadow-[0_0_0_3px_rgba(72,149,239,0.2)] " +
    "[&_select]:bg-select-arrow [&_select]:bg-no-repeat [&_select]:bg-[right_1rem_center] [&_select]:bg-[length:12px] " +
    "[&_option]:bg-[#2e2e38] [&_option]:text-[#f8f9fa]";

const formSectionClass =
    "mb-8 p-6 max-md:p-5 bg-white/[0.03] rounded-xl border border-[#3d3d47] " +
    "transition-all duration-300 animate-formFadeIn opacity-0 hover:border-[#4a4a56] " +
    "[&:nth-child(1)]:[animation-delay:0.1s] [&:nth-child(2)]:[animation-delay:0.2s] " +
    "[&:nth-child(3)]:[animation-delay:0.3s] [&:nth-child(4)]:[animation-delay:0.4s] " +
    "[&>h2]:text-[#4895ef] [&>h2]:text-[1.25rem] [&>h2]:font-semibold [&>h2]:mb-6 " +
    "[&>h2]:flex [&>h2]:items-center [&>h2]:gap-2 [&>h2]:before:content-[''] " +
    "[&>h2]:before:inline-block [&>h2]:before:w-2 [&>h2]:before:h-2 " +
    "[&>h2]:before:bg-[#4895ef] [&>h2]:before:rounded-full";

function DataForm() {
    const { userId } = useParams();
    const navigate = useNavigate();
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState(null);
    const [success, setSuccess] = useState(false);
    const [imagePreviews, setImagePreviews] = useState([]);
    const [language, setLanguage] = useState('uz');

    useEffect(() => {
        const savedLanguage = localStorage.getItem('selectedLanguage') || 'uz';
        setLanguage(savedLanguage);
    }, []);

    const translations = {
        uz: {
            formTitle: "Casting Ro'yxatdan O'tish Formasi",
            formSubtitle: "Iltimos, barcha kerakli maydonlarni to'ldiring",
            basicInfo: "Asosiy Ma'lumotlar",
            physicalChars: "Jismoniy Tavsif",
            contactInfo: "Aloqa Ma'lumotlari",
            photos: "Rasmlar",
            submit: "Ariza Yuborish",
            success: "Ro'yxatdan o'tish muvaffaqiyatli yakunlandi! Yo'naltirilmoqda...",
            error: "Xatolik yuz berdi",
            requiredField: "Majburiy maydon",
            castingType: "Casting Turi",
            selectType: "Turini tanlang",
            actor: "Aktyor",
            extra: "Aktrisa",
            model: "Modelyer",
            euromodel: "Yevro Modelyer",
            bloger: "Bloger",
            influencer: "Reklama",
            gender: "Jins",
            selectGender: "Jinsni tanlang",
            male: "Erkak",
            female: "Ayol",
            fullName: "To'liq Ism",
            region: "Viloyat",
            nationality: "Millati",
            birthday: "Tug'ilgan sana",
            age: "Yosh",
            height: "Bo'y (sm)",
            hairColor: "Soch rangi",
            eyeColor: "Ko'z rangi",
            clothSize: "Kiyim o'lchami",
            shoeSize: "Oyoq kiyim o'lchami",
            bust: "Ko'krak (sm)",
            waist: "Bel (sm)",
            son: "Son (sm)",
            email: "Elektron pochta",
            phone: "Telefon raqam",
            telegram: "Telegram username",
            facebook: "Facebook",
            instagram: "Instagram",
            price: "Kutilayotgan narx ($)",
            uploadPhotos: "Rasmlar yuklash (bir nechta rasm yuklash mumkin)",
            photoHint: "Iltimos, yuzingiz va butun tanaingiz ko'rinadigan aniq rasmlarni yuklang(6 tadan kam bo'lmasin)",
            // ⚠️ Raqamlar mobil ilovadagi HAQIQIY ramkadan olingan:
            // kartochka 3:4 (`CreatorCard` aspectRatio 0.75), anketa
            // galereyasi esa 4:5 (`creator/[id]` Gallery). Ikkalasi ham
            // ortiqchasini QIRQADI - shuning uchun tik rasm va markazdagi
            // yuz talab qilinadi.
            photoSize: "📐 Tik (vertikal) rasm: 1200×1600 px · 3:4 · JPG / PNG · ≤3 MB. Rasm ilovada qirqiladi — yuzni markazda saqlang, chekkaga yozuv qo'ymang.",
            remove: "×",
            loading: "Ma'lumotlar yuklanmoqda...",
            uploadingPhotos: "Rasmlar yuklanmoqda..."
        },
        ru: {
            formTitle: "Форма регистрации на кастинг",
            formSubtitle: "Пожалуйста, заполните все обязательные поля",
            basicInfo: "Основная информация",
            physicalChars: "Физические характеристики",
            contactInfo: "Контактная информация",
            photos: "Фотографии",
            submit: "Отправить заявку",
            success: "Регистрация прошла успешно! Перенаправление...",
            error: "Произошла ошибка",
            requiredField: "Обязательное поле",
            castingType: "Тип кастинга",
            selectType: "Выберите тип",
            actor: "Актер",
            extra: "Актриса",
            model: "Модельер",
            euromodel: "Евро-модельер",
            bloger: "Блогер",
            influencer: "Инфлюенсер",
            gender: "Пол",
            selectGender: "Выберите пол",
            male: "Мужской",
            female: "Женский",
            fullName: "Полное имя",
            region: "Регион",
            nationality: "Национальность",
            birthday: "Дата рождения",
            age: "Возраст",
            height: "Рост (см)",
            hairColor: "Цвет волос",
            eyeColor: "Цвет глаз",
            clothSize: "Размер одежды",
            shoeSize: "Размер обуви",
            bust: "Грудь (см)",
            waist: "Талия (см)",
            son: "Бедра (см)",
            email: "Электронная почта",
            phone: "Номер телефона",
            telegram: "Telegram username",
            facebook: "Facebook",
            instagram: "Instagram",
            price: "Ожидаемая цена ($)",
            uploadPhotos: "Загрузить фотографии (можно несколько)",
            photoHint: "Пожалуйста, загрузите чёткие фотографии, на которых видно ваше лицо и всё тело (не менее 6 штук)",
            photoSize: "📐 Вертикальное фото: 1200×1600 px · 3:4 · JPG / PNG · ≤3 MB. В приложении фото обрезается — держите лицо по центру и не размещайте надписи по краям.",
            remove: "×",
            loading: "Данные загружаются...",
            uploadingPhotos: "Фотографии загружаются..."
        }
    };

    const [formData, setFormData] = useState({
        telegramId: userId || '',
        castingType: '',
        gender: '',
        name: '',
        region: '',
        nationality: '',
        birthday: '',
        age: '',
        height: '',
        hairColor: '',
        eyeColor: '',
        clothSize: '',
        shoeSize: '',
        bust: '',
        waist: '',
        son: '',
        email: '',
        phone: '',
        telegram: '',
        facebook: '',
        instagram: '',
        price: '',
        photos: []
    });

    const isFormValid = () => {
        const requiredFields = [
            formData.castingType,
            formData.gender,
            formData.name,
            formData.region,
            formData.nationality,
            formData.age,
            formData.height,
            formData.hairColor,
            formData.eyeColor,
            formData.email,
            formData.phone
        ];
        const allFieldsFilled = requiredFields.every(field => field && field.trim() !== '');
        const hasEnoughPhotos = formData.photos.length >= 6;
        return allFieldsFilled && hasEnoughPhotos;
    };

    const handleChange = (e) => {
        const { name, value } = e.target;
        setFormData(prev => ({
            ...prev,
            [name]: value
        }));
    };

    const [currentFileInput, setCurrentFileInput] = useState(0);
    const MAX_PHOTOS = 6;

    const [uploadProgress, setUploadProgress] = useState({
        isUploading: false,
        message: '',
        progress: 0
    });

    const handleImageUpload = async (e, index) => {
        const file = e.target.files[0];
        if (!file) return;

        setUploadProgress({
            isUploading: true,
            message: translations[language].uploadingPhotos,
            progress: 0
        });

        // Create preview
        const preview = URL.createObjectURL(file);
        const newPreviews = [...imagePreviews];
        newPreviews[index] = preview;
        setImagePreviews(newPreviews);

        try {
            // Upload to backend
            const uploadData = new FormData();
            uploadData.append('photo', file);
            uploadData.append('prefix', '/users/' + userId);

            const response = await ApiCall('/api/v1/file/upload', 'POST', uploadData, null, true, (progressEvent) => {
                const progress = Math.round((progressEvent.loaded * 100) / progressEvent.total);
                setUploadProgress(prev => ({
                    ...prev,
                    progress
                }));
            });

            // Update photos array
            const newPhotos = [...formData.photos];
            newPhotos[index] = response.data;
            setFormData(prev => ({
                ...prev,
                photos: newPhotos
            }));

            // Move to next input if not last
            if (currentFileInput < MAX_PHOTOS - 1) {
                setCurrentFileInput(currentFileInput + 1);
            }
        } catch (error) {
            console.error("Image upload error:", error);
            setError("Rasmlarni yuklashda xatolik yuz berdi");
        } finally {
            setUploadProgress({
                isUploading: false,
                message: '',
                progress: 0
            });
        }
    };

    const removeImage = (index) => {
        const newPreviews = [...imagePreviews];
        newPreviews.splice(index, 1);
        setImagePreviews(newPreviews);

        const newPhotos = [...formData.photos];
        newPhotos.splice(index, 1);
        setFormData(prev => ({
            ...prev,
            photos: newPhotos
        }));

        // Adjust current file input if needed
        if (index <= currentFileInput) {
            setCurrentFileInput(Math.max(0, currentFileInput - 1));
        }
    };
    // const handleImageUpload = async (e) => {
    //     const files = Array.from(e.target.files);
    //     const previews = files.map(file => URL.createObjectURL(file));
    //     setImagePreviews(prev => [...prev, ...previews]);
    //
    //     const uploadedIds = [];
    //     for (const file of files) {
    //         try {
    //             const uploadData = new FormData();
    //             uploadData.append('photo', file);
    //             uploadData.append('prefix', '/users/' + userId);
    //             const response = await ApiCall('/api/v1/file/upload', 'POST', uploadData, null, true);
    //             uploadedIds.push(response.data);
    //         } catch (error) {
    //             console.error("Image upload error:", error);
    //             setError("Rasmlarni yuklashda xatolik yuz berdi");
    //         }
    //     }
    //
    //     setFormData(prev => ({
    //         ...prev,
    //         photos: [...prev.photos, ...uploadedIds]
    //     }));
    // };

    // const removeImage = (index) => {
    //     const newPreviews = [...imagePreviews];
    //     newPreviews.splice(index, 1);
    //     setImagePreviews(newPreviews);
    //
    //     const newPhotos = [...formData.photos];
    //     newPhotos.splice(index, 1);
    //     setFormData(prev => ({
    //         ...prev,
    //         photos: newPhotos
    //     }));
    // };

    const handleSubmit = async (e) => {
        e.preventDefault();
        setLoading(true);
        setError(null);
        setUploadProgress({
            isUploading: true,
            message: translations[language].loading,
            progress: 0
        });

        if (formData.photos.length < 6) {
            setError("Kamida 6 ta rasm yuklashingiz shart!");
            setLoading(false);
            setUploadProgress({
                isUploading: false,
                message: '',
                progress: 0
            });
            return;
        }

        try {
            const payload = {
                ...formData,
                age: parseInt(formData.age),
                height: parseInt(formData.height),
                price: parseFloat(formData.price) || 0,
                birthday: formData.birthday ? new Date(formData.birthday).toISOString() : null,
                status: 0,
                createdAt: new Date().toISOString()
            };

            const response = await ApiCall('/api/v1/casting-user', 'POST', payload, null, true);

            if (response.error) {
                setError(response.data?.message || "Formani yuborishda xatolik");
            } else {
                setSuccess(true);
                setTimeout(() => {
                    navigate(`/history/${userId}`);
                }, 2000);
            }
        } catch (error) {
            console.error("Submit error:", error);
            setError("Formani yuborishda xatolik yuz berdi");
        } finally {
            setLoading(false);
            setUploadProgress({
                isUploading: false,
                message: '',
                progress: 0
            });
        }
    };

    return (
        <div className="min-h-screen bg-[#1b1b1e] text-[#f8f9fa] [font-family:Inter,-apple-system,BlinkMacSystemFont,sans-serif] pt-[60px]">


            {(uploadProgress.isUploading || loading) && (
                <div className="fixed inset-0 bg-white/90 flex items-center justify-center z-[1000]">
                    <div className="text-center p-[30px] bg-white rounded-[10px] shadow-[0_4px_20px_rgba(0,0,0,0.1)] max-w-[400px] w-[90%]">
                        <div className="w-[60px] h-[60px] border-[6px] border-[#4caf50] border-t-transparent rounded-full animate-spin mx-auto mb-5"></div>
                        <p>{uploadProgress.message}</p>
                        {uploadProgress.progress > 0 && (
                            <div className="w-full h-[10px] bg-[#f0f0f0] rounded-[5px] mt-5 overflow-hidden">
                                <div
                                    className="h-full bg-[#4caf50] transition-[width] duration-300"
                                    style={{ width: `${uploadProgress.progress}%` }}
                                ></div>
                            </div>
                        )}
                    </div>
                </div>
            )}

            <Header props={"data-form"} />
            <div className="max-w-[900px] my-4 mx-auto p-8 bg-[#25252d] rounded-2xl shadow-[0_4px_6px_rgba(0,0,0,0.1)] text-[#f8f9fa] [font-family:Inter,-apple-system,BlinkMacSystemFont,sans-serif] max-md:p-6 max-md:m-0 max-md:rounded-none max-[480px]:p-4">

                <div className="text-center mb-10">
                    <h1 className="text-[2rem] font-bold mb-2 bg-[linear-gradient(90deg,#4361ee,#4895ef)] bg-clip-text text-transparent max-md:text-[1.75rem]">{translations[language].formTitle}</h1>
                    <p className="text-white opacity-80 text-base">{translations[language].formSubtitle}</p>
                </div>

                {error && (
                    <div className="bg-[rgba(247,37,133,0.1)] text-[#f72585] p-4 rounded-lg mb-6 border-l-4 border-[#f72585]">
                        {translations[language].error}: {error}
                    </div>
                )}

                {success && (
                    <div className="bg-[rgba(76,201,240,0.1)] text-[#4cc9f0] p-4 rounded-lg mb-6 border-l-4 border-[#4cc9f0]">
                        {translations[language].success}
                    </div>
                )}

                <form onSubmit={handleSubmit}>
                    <input type="hidden" name="telegramId" value={formData.telegramId} />
                    <div className={formSectionClass}>
                        <h2>{translations[language].basicInfo}</h2>
                        <div className="grid grid-cols-[repeat(auto-fit,minmax(250px,1fr))] gap-6 mb-6 max-md:grid-cols-1 max-md:gap-4">
                            <div className={formGroupClass}>
                                <label>{translations[language].castingType}*</label>
                                <select
                                    name="castingType"
                                    value={formData.castingType}
                                    onChange={handleChange}
                                    required
                                >
                                    <option disabled hidden value="">{translations[language].selectType}</option>
                                    <option value="model">{translations[language].model}</option>
                                    <option value="euromodel">{translations[language].euromodel}</option>
                                    <option value="bloger">{translations[language].bloger}</option>
                                    <option value="actor">{translations[language].actor}</option>
                                    <option value="extra">{translations[language].extra}</option>
                                    <option value="influencer">{translations[language].influencer}</option>
                                </select>
                            </div>

                            <div className={formGroupClass}>
                                <label>{translations[language].gender}*</label>
                                <select
                                    name="gender"
                                    value={formData.gender}
                                    onChange={handleChange}
                                    required
                                >
                                    <option disabled hidden value="">{translations[language].selectGender}</option>
                                    <option value="male">{translations[language].male}</option>
                                    <option value="female">{translations[language].female}</option>
                                </select>
                            </div>
                        </div>

                        <div className="grid grid-cols-[repeat(auto-fit,minmax(250px,1fr))] gap-6 mb-6 max-md:grid-cols-1 max-md:gap-4">
                            <div className={formGroupClass}>
                                <label>{translations[language].fullName}*</label>
                                <input
                                    type="text"
                                    name="name"
                                    value={formData.name}
                                    onChange={handleChange}
                                    required
                                />
                            </div>

                            <div className={formGroupClass}>
                                <label>{translations[language].region}*</label>
                                <input
                                    type="text"
                                    name="region"
                                    value={formData.region}
                                    onChange={handleChange}
                                    required
                                />
                            </div>
                        </div>

                        <div className="grid grid-cols-[repeat(auto-fit,minmax(250px,1fr))] gap-6 mb-6 max-md:grid-cols-1 max-md:gap-4">
                            <div className={formGroupClass}>
                                <label>{translations[language].nationality}*</label>
                                <input
                                    type="text"
                                    name="nationality"
                                    value={formData.nationality}
                                    onChange={handleChange}
                                    required
                                />
                            </div>

                            <div className={formGroupClass}>
                                <label>{translations[language].birthday}</label>
                                <input
                                    type="date"
                                    name="birthday"
                                    value={formData.birthday}
                                    onChange={handleChange}
                                />
                            </div>
                        </div>
                    </div>
                    <div className={formSectionClass}>
                        <h2>{translations[language].physicalChars}</h2>
                        <div className="grid grid-cols-[repeat(auto-fit,minmax(250px,1fr))] gap-6 mb-6 max-md:grid-cols-1 max-md:gap-4">
                            <div className={formGroupClass}>
                                <label>{translations[language].age}*</label>
                                <input
                                    type="number"
                                    name="age"
                                    value={formData.age}
                                    onChange={handleChange}
                                    min="1"
                                    required
                                />
                            </div>

                            <div className={formGroupClass}>
                                <label>{translations[language].height}*</label>
                                <input
                                    type="number"
                                    name="height"
                                    value={formData.height}
                                    onChange={handleChange}
                                    min="1"
                                    required
                                />
                            </div>
                        </div>

                        <div className="grid grid-cols-[repeat(auto-fit,minmax(250px,1fr))] gap-6 mb-6 max-md:grid-cols-1 max-md:gap-4">
                            <div className={formGroupClass}>
                                <label>{translations[language].hairColor}*</label>
                                <input
                                    type="text"
                                    name="hairColor"
                                    value={formData.hairColor}
                                    onChange={handleChange}
                                    required
                                />
                            </div>

                            <div className={formGroupClass}>
                                <label>{translations[language].eyeColor}*</label>
                                <input
                                    type="text"
                                    name="eyeColor"
                                    value={formData.eyeColor}
                                    onChange={handleChange}
                                    required
                                />
                            </div>
                        </div>

                        <div className="grid grid-cols-[repeat(auto-fit,minmax(250px,1fr))] gap-6 mb-6 max-md:grid-cols-1 max-md:gap-4">
                            <div className={formGroupClass}>
                                <label>{translations[language].clothSize}</label>
                                <input
                                    type="text"
                                    name="clothSize"
                                    value={formData.clothSize}
                                    onChange={handleChange}
                                />
                            </div>

                            <div className={formGroupClass}>
                                <label>{translations[language].shoeSize}</label>
                                <input
                                    type="text"
                                    name="shoeSize"
                                    value={formData.shoeSize}
                                    onChange={handleChange}
                                />
                            </div>
                        </div>

                        {/* Agar gender male bo‘lmasa, bust va son inputlarini chiqaramiz */}
                        {formData.gender !== 'male' && (
                            <>
                                <div className="grid grid-cols-[repeat(auto-fit,minmax(250px,1fr))] gap-6 mb-6 max-md:grid-cols-1 max-md:gap-4">
                                    <div className={formGroupClass}>
                                        <label>{translations[language].bust}</label>
                                        <input
                                            type="text"
                                            name="bust"
                                            value={formData.bust}
                                            onChange={handleChange}
                                        />
                                    </div>

                                    <div className={formGroupClass}>
                                        <label>{translations[language].son}</label>
                                        <input
                                            type="text"
                                            name="son"
                                            value={formData.son}
                                            onChange={handleChange}
                                        />
                                    </div>
                                </div>
                            </>
                        )}

                        <div className={formGroupClass}>
                            <label>{translations[language].waist}</label>
                            <input
                                type="text"
                                name="waist"
                                value={formData.waist}
                                onChange={handleChange}
                            />
                        </div>
                    </div>
                    <div className={formSectionClass}>
                        <h2>{translations[language].contactInfo}</h2>
                        <div className="grid grid-cols-[repeat(auto-fit,minmax(250px,1fr))] gap-6 mb-6 max-md:grid-cols-1 max-md:gap-4">
                            <div className={formGroupClass}>
                                <label>{translations[language].email}*</label>
                                <input
                                    type="email"
                                    name="email"
                                    value={formData.email}
                                    onChange={handleChange}
                                    required
                                />
                            </div>

                            <div className={formGroupClass}>
                                <label>{translations[language].phone}*</label>
                                <input
                                    type="tel"
                                    name="phone"
                                    value={formData.phone}
                                    onChange={handleChange}
                                    required
                                />
                            </div>
                        </div>

                        <div className="grid grid-cols-[repeat(auto-fit,minmax(250px,1fr))] gap-6 mb-6 max-md:grid-cols-1 max-md:gap-4">
                            <div className={formGroupClass}>
                                <label>{translations[language].telegram}</label>
                                <input
                                    type="text"
                                    name="telegram"
                                    value={formData.telegram}
                                    onChange={handleChange}
                                />
                            </div>

                            <div className={formGroupClass}>
                                <label>{translations[language].facebook}</label>
                                <input
                                    type="text"
                                    name="facebook"
                                    value={formData.facebook}
                                    onChange={handleChange}
                                />
                            </div>
                        </div>

                        <div className={formGroupClass}>
                            <label>{translations[language].instagram}</label>
                            <input
                                type="text"
                                name="instagram"
                                value={formData.instagram}
                                onChange={handleChange}
                            />
                        </div>


                    </div>
                    <div className={formSectionClass}>
                        <h2>{translations[language].photos}</h2>
                        <div className={formGroupClass}>
                            <label>{translations[language].uploadPhotos}*</label>
                            <p className="text-[0.8rem] text-white opacity-70 mt-2 leading-[1.4]">{translations[language].photoHint}</p>
                            <p className="text-[0.8rem] text-white opacity-70 mt-2 leading-[1.4]">{translations[language].photoSize}</p>

                            {/* Render file inputs dynamically */}
                            {Array.from({ length: MAX_PHOTOS }).map((_, index) => (
                                <div key={index} >
                                    <label className="block p-4 bg-[#262626] border border-dashed border-[#404040] rounded-lg text-center cursor-pointer mb-4">
                                        {imagePreviews[index] ? (
                                            <div className="group relative w-[100px] h-[100px] m-[5px] rounded-lg overflow-hidden shadow-[0_4px_6px_rgba(0,0,0,0.1)] transition-all duration-300 hover:-translate-y-[3px] hover:shadow-[0_6px_12px_rgba(0,0,0,0.15)] [&_img]:w-full [&_img]:h-full [&_img]:object-cover">
                                                <img src={imagePreviews[index]} alt={`Preview ${index}`} />
                                                <button
                                                    type="button"
                                                    onClick={(e) => {
                                                        e.preventDefault();
                                                        removeImage(index);
                                                    }}
                                                    className="absolute top-2 right-2 bg-[#f72585] text-white border-none w-6 h-6 rounded-full flex items-center justify-center cursor-pointer text-[0.9rem] p-0 transition-all duration-300 opacity-0 group-hover:opacity-100 hover:bg-[#d32f2f] hover:scale-110"
                                                >
                                                    {translations[language].remove}
                                                </button>
                                            </div>
                                        ) : (
                                            <div>
                                                {translations[language].uploadPhotos} {index + 1}
                                            </div>
                                        )}
                                        <input
                                            type="file"
                                            onChange={(e) => handleImageUpload(e, index)}
                                            accept="image/*"
                                            style={{ display: 'none' }}
                                            disabled={index !== currentFileInput}
                                        />
                                    </label>
                                </div>
                            ))}
                        </div>
                    </div>

                    <div className="flex justify-center mt-10">
                        <button
                            type="submit"
                            className="bg-[linear-gradient(135deg,#4361ee,#4895ef)] text-white border-none px-10 py-4 text-base font-semibold rounded-lg cursor-pointer transition-all duration-300 shadow-[0_4px_12px_rgba(67,97,238,0.3)] relative overflow-hidden disabled:bg-[#666] disabled:bg-none disabled:shadow-none disabled:cursor-not-allowed max-md:w-full max-md:p-4"
                            disabled={!isFormValid() || loading}
                        >
                            {loading ? '...' : translations[language].submit}
                        </button>

                    </div>
                </form>
            </div>
        </div>

    );
}

export default DataForm;