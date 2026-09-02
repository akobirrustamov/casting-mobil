import React, { useEffect, useState } from 'react';
import Header from "../header/Header";
import "react-responsive-modal/styles.css";
import ApiCall, { baseUrl } from '../../config/index';
import { useNavigate, useLocation } from "react-router-dom";
import { Modal } from "react-responsive-modal";
import { FiArrowLeft, FiMail, FiPhone, FiInstagram, FiFacebook, FiX } from 'react-icons/fi';
import { FaTelegramPlane } from 'react-icons/fa';
import { motion } from 'framer-motion';

function Appeal(props) {
    const location = useLocation();
    const castingId = location.state?.castingId;
    const navigate = useNavigate();
    const [casting, setCasting] = useState(null);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState(null);
    const [language, setLanguage] = useState('uz');
    const [selectedImage, setSelectedImage] = useState(null);
    const [isModalOpen, setIsModalOpen] = useState(false);
    const [activeTab, setActiveTab] = useState('basic');

    const openImageModal = (imageUrl) => {
        setSelectedImage(imageUrl);
        setIsModalOpen(true);
    };

    const closeImageModal = () => {
        setIsModalOpen(false);
        setSelectedImage(null);
    };

    useEffect(() => {
        const savedLanguage = localStorage.getItem('selectedLanguage') || 'uz';
        setLanguage(savedLanguage);
        fetchCasting();
    }, []);

    const fetchCasting = async () => {
        setLoading(true);
        try {
            const response = await ApiCall('/api/v1/casting-user/appeal/' + castingId, 'GET');
            if (response.error) {
                setError(response.data);
            } else {
                setCasting(response.data);
            }
        } catch (error) {
            console.error("Error fetching casting:", error);
            setError("Failed to fetch casting application");
        } finally {
            setLoading(false);
        }
    };

    const formatDate = (dateString) => {
        const options = { year: 'numeric', month: 'long', day: 'numeric' };
        return new Date(dateString).toLocaleDateString(undefined, options);
    };

    const getStatusText = (status) => {
        const statusTexts = {
            uz: {
                0: "Ko'rib chiqilmoqda",
                1: "Qabul qilindi",
                2: "Rad etildi"
            },
            ru: {
                0: "На рассмотрении",
                1: "Принято",
                2: "Отклонено"
            }
        };
        return statusTexts[language][status] || "";
    };

    const getStatusClass = (status) => {
        const statusClasses = {
            0: "bg-[rgba(255,212,59,0.15)] text-[#ffd43b] border border-[rgba(255,212,59,0.3)]",
            1: "bg-[rgba(81,207,102,0.15)] text-[#51cf66] border border-[rgba(81,207,102,0.3)]",
            2: "bg-[rgba(255,107,107,0.15)] text-[#ff6b6b] border border-[rgba(255,107,107,0.3)]"
        };
        return statusClasses[status] || "";
    };

    const translations = {
        uz: {
            loading: "Yuklanmoqda...",
            error: "Ariza yuklanmadi",
            back: "Orqaga",
            basicInfo: "Asosiy ma'lumotlar",
            physicalInfo: "Jismoniy tavsif",
            contactInfo: "Aloqa ma'lumotlari",
            gallery: "Galereya",
            name: "Ism",
            castingType: "Casting turi",
            gender: "Jins",
            region: "Hudud",
            nationality: "Millat",
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
            email: "Email",
            phone: "Telefon",
            telegram: "Telegram",
            facebook: "Facebook",
            instagram: "Instagram",
            price: "Narx ($)",
            createdAt: "Ariza sanasi",
            status: "Holat",
            viewFullImage: "To'liq rasmini ko'rish"
        },
        ru: {
            loading: "Загрузка...",
            error: "Заявка не загружена",
            back: "Назад",
            basicInfo: "Основная информация",
            physicalInfo: "Физические характеристики",
            contactInfo: "Контактная информация",
            gallery: "Галерея",
            name: "Имя",
            castingType: "Тип кастинга",
            gender: "Пол",
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
            email: "Email",
            phone: "Телефон",
            telegram: "Telegram",
            facebook: "Facebook",
            instagram: "Instagram",
            price: "Цена ($)",
            createdAt: "Дата заявки",
            status: "Статус",
            viewFullImage: "Посмотреть полное изображение"
        }
    };

    const renderTabContent = () => {
        switch (activeTab) {
            case 'basic':
                return (
                    <div className="grid grid-cols-[repeat(auto-fill,minmax(300px,1fr))] gap-6 max-lg:grid-cols-[repeat(auto-fill,minmax(250px,1fr))] max-[480px]:grid-cols-1">
                        {[
                            { label: translations[language].name, value: casting.name },
                            { label: translations[language].castingType, value: casting.castingType },
                            { label: translations[language].gender, value: casting.gender },
                            { label: translations[language].region, value: casting.region },
                            { label: translations[language].nationality, value: casting.nationality },
                            { label: translations[language].birthday, value: formatDate(casting.birthday) },
                        ].map((item, index) => (
                            <div key={index} className="flex flex-col gap-1 p-4 bg-[#2d2d2d] rounded-lg transition-all duration-300 hover:-translate-y-[3px] hover:shadow-[0_4px_12px_rgba(0,0,0,0.2)]">
                                <span className="font-medium text-[#adb5bd] text-[0.9rem]">{item.label}</span>
                                <span className="font-medium text-[#f8f9fa] text-[1.05rem]">{item.value}</span>
                            </div>
                        ))}
                    </div>
                );
            case 'physical':
                return (
                    <div className="grid grid-cols-[repeat(auto-fill,minmax(300px,1fr))] gap-6 max-lg:grid-cols-[repeat(auto-fill,minmax(250px,1fr))] max-[480px]:grid-cols-1">
                        {[
                            { label: translations[language].age, value: casting.age },
                            { label: translations[language].height, value: casting.height },
                            { label: translations[language].hairColor, value: casting.hairColor },
                            { label: translations[language].eyeColor, value: casting.eyeColor },
                            { label: translations[language].clothSize, value: casting.clothSize },
                            { label: translations[language].shoeSize, value: casting.shoeSize },
                            { label: translations[language].bust, value: casting.bust },
                            { label: translations[language].waist, value: casting.waist },
                            { label: translations[language].son, value: casting.son },
                        ].map((item, index) => (
                            <div key={index} className="flex flex-col gap-1 p-4 bg-[#2d2d2d] rounded-lg transition-all duration-300 hover:-translate-y-[3px] hover:shadow-[0_4px_12px_rgba(0,0,0,0.2)]">
                                <span className="font-medium text-[#adb5bd] text-[0.9rem]">{item.label}</span>
                                <span className="font-medium text-[#f8f9fa] text-[1.05rem]">{item.value}</span>
                            </div>
                        ))}
                    </div>
                );
            case 'contact':
                return (
                    <div className="grid grid-cols-[repeat(auto-fill,minmax(300px,1fr))] gap-6 max-lg:grid-cols-[repeat(auto-fill,minmax(250px,1fr))] max-[480px]:grid-cols-1">
                        <div className="flex flex-col gap-1 p-4 bg-[#2d2d2d] rounded-lg transition-all duration-300 hover:-translate-y-[3px] hover:shadow-[0_4px_12px_rgba(0,0,0,0.2)]">
                            <span className="font-medium text-[#adb5bd] text-[0.9rem]">{translations[language].email}</span>
                            <span className="font-medium text-[#f8f9fa] text-[1.05rem]">
                                <a href={`mailto:${casting.email}`} className="text-[#e9f5ee] no-underline transition-[color,transform] duration-200 hover:text-[#fef3c7] hover:translate-x-0.5">
                                    <FiMail /> {casting.email}
                                </a>
                            </span>
                        </div>
                        <div className="flex flex-col gap-1 p-4 bg-[#2d2d2d] rounded-lg transition-all duration-300 hover:-translate-y-[3px] hover:shadow-[0_4px_12px_rgba(0,0,0,0.2)]">
                            <span className="font-medium text-[#adb5bd] text-[0.9rem]">{translations[language].phone}</span>
                            <span className="font-medium text-[#f8f9fa] text-[1.05rem]">
                                <a href={`tel:${casting.phone}`} className="text-[#e9f5ee] no-underline transition-[color,transform] duration-200 hover:text-[#fef3c7] hover:translate-x-0.5">
                                    <FiPhone /> {casting.phone}
                                </a>
                            </span>
                        </div>
                        {casting.telegram && (
                            <div className="flex flex-col gap-1 p-4 bg-[#2d2d2d] rounded-lg transition-all duration-300 hover:-translate-y-[3px] hover:shadow-[0_4px_12px_rgba(0,0,0,0.2)]">
                                <span className="font-medium text-[#adb5bd] text-[0.9rem]">{translations[language].telegram}</span>
                                <span className="font-medium text-[#f8f9fa] text-[1.05rem]">
                                    <a href={`https://t.me/${casting.telegram}`} target="_blank" rel="noopener noreferrer" className="text-[#e9f5ee] no-underline transition-[color,transform] duration-200 hover:text-[#fef3c7] hover:translate-x-0.5">
                                        <FaTelegramPlane /> {casting.telegram}
                                    </a>
                                </span>
                            </div>
                        )}
                        {casting.facebook && (
                            <div className="flex flex-col gap-1 p-4 bg-[#2d2d2d] rounded-lg transition-all duration-300 hover:-translate-y-[3px] hover:shadow-[0_4px_12px_rgba(0,0,0,0.2)]">
                                <span className="font-medium text-[#adb5bd] text-[0.9rem]">{translations[language].facebook}</span>
                                <span className="font-medium text-[#f8f9fa] text-[1.05rem]">
                                    <a href={casting.facebook} target="_blank" rel="noopener noreferrer" className="text-[#e9f5ee] no-underline transition-[color,transform] duration-200 hover:text-[#fef3c7] hover:translate-x-0.5">
                                        <FiFacebook /> {casting.facebook}
                                    </a>
                                </span>
                            </div>
                        )}
                        {casting.instagram && (
                            <div className="flex flex-col gap-1 p-4 bg-[#2d2d2d] rounded-lg transition-all duration-300 hover:-translate-y-[3px] hover:shadow-[0_4px_12px_rgba(0,0,0,0.2)]">
                                <span className="font-medium text-[#adb5bd] text-[0.9rem]">{translations[language].instagram}</span>
                                <span className="font-medium text-[#f8f9fa] text-[1.05rem]">
                                    <a href={`https://instagram.com/${casting.instagram}`} target="_blank" rel="noopener noreferrer" className="text-[#e9f5ee] no-underline transition-[color,transform] duration-200 hover:text-[#fef3c7] hover:translate-x-0.5">
                                        <FiInstagram /> {casting.instagram}
                                    </a>
                                </span>
                            </div>
                        )}
                        <div className="flex flex-col gap-1 p-4 bg-[#2d2d2d] rounded-lg transition-all duration-300 hover:-translate-y-[3px] hover:shadow-[0_4px_12px_rgba(0,0,0,0.2)]">
                            <span className="font-medium text-[#adb5bd] text-[0.9rem]">{translations[language].price}</span>
                            <span className="font-medium text-[#f8f9fa] text-[1.05rem]">{casting.price} $</span>
                        </div>
                    </div>
                );
            case 'gallery':
                return (
                    <div className="grid grid-cols-[repeat(auto-fill,minmax(200px,1fr))] gap-4 mt-4 max-[480px]:grid-cols-[repeat(auto-fill,minmax(150px,1fr))]">
                        {casting.photos.map((photo, index) => (
                            <motion.div
                                key={index}
                                className="group relative rounded-lg overflow-hidden aspect-[2/3]"
                                whileHover={{ scale: 1.03 }}
                                transition={{ duration: 0.2 }}
                            >
                                <img
                                    src={`${baseUrl}/api/v1/file/getFile/${photo.id}`}
                                    alt={`Gallery ${index + 1}`}
                                    className="w-full h-full object-cover transition-transform duration-300 group-hover:scale-105"
                                    onClick={() => openImageModal(`${baseUrl}/api/v1/file/getFile/${photo.id}`)}
                                    loading="lazy"
                                />
                                <div className="absolute bottom-0 left-0 right-0 bg-[linear-gradient(to_top,rgba(0,0,0,0.8),transparent)] text-white p-4 opacity-0 transition-opacity duration-300 group-hover:opacity-100">
                                    <span>{translations[language].viewFullImage}</span>
                                </div>
                            </motion.div>
                        ))}
                    </div>
                );
            default:
                return null;
        }
    };

    return (
        <div className="min-h-screen bg-[#121212] text-[#f8f9fa] [font-family:Inter,-apple-system,BlinkMacSystemFont,sans-serif]">
            <Header props={""} />
            <main className="p-8 max-w-[1200px] mx-auto max-md:p-6 max-[480px]:p-4">
                {loading ? (
                    <div className="flex flex-col items-center justify-center h-[300px]">
                        <div className="w-[60px] h-[60px] border-[6px] border-[#4361ee] border-t-transparent rounded-full animate-spin"></div>
                        <p className="mt-6 text-[#4895ef] text-[1.2rem]">{translations[language].loading}</p>
                    </div>
                ) : error ? (
                    <div className="bg-[rgba(255,107,107,0.15)] text-[#ff6b6b] p-6 rounded-[10px] text-center max-w-[700px] my-8 mx-auto border-l-[6px] border-[#ff6b6b] shadow-[0_4px_15px_rgba(0,0,0,0.2)]">
                        {translations[language].error}: {error}
                    </div>
                ) : casting === null ? (
                    <div className="text-center p-10 text-[1.3rem] text-[#adb5bd] bg-[#1e1e1e] rounded-[10px] max-w-[700px] my-8 mx-auto shadow-[0_4px_15px_rgba(0,0,0,0.1)]">
                        {translations[language].noApplications}
                    </div>
                ) : (
                    <div>
                        <button
                            className="bg-[#2d2d2d] text-[#f8f9fa] border-none px-6 py-3 rounded-lg text-base cursor-pointer mb-8 transition-all duration-300 flex items-center gap-2 hover:bg-[#444] hover:-translate-y-0.5 hover:shadow-[0_2px_8px_rgba(0,0,0,0.2)]"
                            onClick={() => navigate(-1)}
                        >
                            <FiArrowLeft /> {translations[language].back}
                        </button>

                        <div className="bg-[#1e1e1e] rounded-[14px] overflow-hidden shadow-[0_6px_18px_rgba(0,0,0,0.25)] border border-[#444]">
                            {/* Profile Header */}
                            <div className="flex items-center gap-8 p-8 bg-[linear-gradient(135deg,#4361ee,#3f37c9)] text-white max-md:flex-col max-md:text-center max-md:p-6">
                                {casting.photos && casting.photos.length > 0 && (
                                    <div className="w-[120px] h-[120px] rounded-full overflow-hidden border-4 border-white/20 max-md:w-[100px] max-md:h-[100px]">
                                        <img
                                            src={`${baseUrl}/api/v1/file/getFile/${casting.photos[0].id}`}
                                            alt={casting.name}
                                            className="w-full h-full object-cover"
                                        />
                                    </div>
                                )}
                                <div className="">
                                    <h1 className="m-0 text-[2rem] font-semibold max-md:text-2xl">{casting.name}</h1>
                                    <div className="flex items-center gap-4 my-2 mx-0">
                                        <span className="bg-white/20 px-3 py-1 rounded-[20px] text-[0.9rem]">{casting.castingType}</span>
                                        <span className={`px-[0.9rem] py-[0.4rem] rounded-3xl text-[0.85rem] font-semibold inline-block w-fit ${getStatusClass(casting.status)}`}>
                                            {getStatusText(casting.status)}
                                        </span>
                                    </div>
                                    <div className="text-[0.9rem] opacity-90">
                                        {translations[language].createdAt}: {formatDate(casting.createdAt)}
                                    </div>
                                </div>
                            </div>

                            {/* Navigation Tabs */}
                            <div className="flex border-b border-[#444] px-8 max-md:px-4">
                                <button
                                    className={`px-6 py-4 bg-none border-none font-medium cursor-pointer relative ${activeTab === 'basic' ? "text-[#4361ee] font-semibold after:content-[''] after:absolute after:-bottom-px after:left-0 after:w-full after:h-[3px] after:bg-[#4361ee]" : 'text-[#adb5bd]'}`}
                                    onClick={() => setActiveTab('basic')}
                                >
                                    {translations[language].basicInfo}
                                </button>
                                <button
                                    className={`px-6 py-4 bg-none border-none font-medium cursor-pointer relative ${activeTab === 'physical' ? "text-[#4361ee] font-semibold after:content-[''] after:absolute after:-bottom-px after:left-0 after:w-full after:h-[3px] after:bg-[#4361ee]" : 'text-[#adb5bd]'}`}
                                    onClick={() => setActiveTab('physical')}
                                >
                                    {translations[language].physicalInfo}
                                </button>
                                <button
                                    className={`px-6 py-4 bg-none border-none font-medium cursor-pointer relative ${activeTab === 'contact' ? "text-[#4361ee] font-semibold after:content-[''] after:absolute after:-bottom-px after:left-0 after:w-full after:h-[3px] after:bg-[#4361ee]" : 'text-[#adb5bd]'}`}
                                    onClick={() => setActiveTab('contact')}
                                >
                                    {translations[language].contactInfo}
                                </button>
                                {casting.photos && casting.photos.length > 0 && (
                                    <button
                                        className={`px-6 py-4 bg-none border-none font-medium cursor-pointer relative ${activeTab === 'gallery' ? "text-[#4361ee] font-semibold after:content-[''] after:absolute after:-bottom-px after:left-0 after:w-full after:h-[3px] after:bg-[#4361ee]" : 'text-[#adb5bd]'}`}
                                        onClick={() => setActiveTab('gallery')}
                                    >
                                        {translations[language].gallery}
                                    </button>
                                )}
                            </div>

                            {/* Tab Content */}
                            <div>
                                {renderTabContent()}
                            </div>
                        </div>
                    </div>
                )}

                {/* Image Modal */}
                <Modal
                    open={isModalOpen}
                    onClose={closeImageModal}
                    center
                    classNames={{
                        overlay: 'custom-overlay',
                        modal: 'custom-modal',
                    }}
                    closeIcon={<FiX size={24} color="#fff" />}
                >
                    {selectedImage && (
                        <img
                            src={selectedImage}
                            alt="Full Image"
                           
                        />
                    )}
                </Modal>
            </main>
        </div>
    );
}

export default Appeal;