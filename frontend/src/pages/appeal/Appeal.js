import React, { useEffect, useState } from 'react';
import Header from "../header/Header";
import "react-responsive-modal/styles.css";
import ApiCall, { baseUrl } from '../../config/index';
import './Appeal.css';
import { useParams, useNavigate, useLocation } from "react-router-dom";
import { Modal } from "react-responsive-modal";
import { FiArrowLeft, FiMail, FiPhone, FiInstagram, FiFacebook, FiX } from 'react-icons/fi';
import { FaTelegramPlane } from 'react-icons/fa';
import { motion } from 'framer-motion';

function Appeal(props) {
    const { userId } = useParams();
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
            0: "status-pending",
            1: "status-approved",
            2: "status-rejected"
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
                    <div className="info-grid">
                        {[
                            { label: translations[language].name, value: casting.name },
                            { label: translations[language].castingType, value: casting.castingType },
                            { label: translations[language].gender, value: casting.gender },
                            { label: translations[language].region, value: casting.region },
                            { label: translations[language].nationality, value: casting.nationality },
                            { label: translations[language].birthday, value: formatDate(casting.birthday) },
                        ].map((item, index) => (
                            <div key={index} className="info-item">
                                <span className="info-label">{item.label}</span>
                                <span className="info-value">{item.value}</span>
                            </div>
                        ))}
                    </div>
                );
            case 'physical':
                return (
                    <div className="info-grid">
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
                            <div key={index} className="info-item">
                                <span className="info-label">{item.label}</span>
                                <span className="info-value">{item.value}</span>
                            </div>
                        ))}
                    </div>
                );
            case 'contact':
                return (
                    <div className="info-grid">
                        <div className="info-item">
                            <span className="info-label">{translations[language].email}</span>
                            <span className="info-value">
                                <a href={`mailto:${casting.email}`} className="contact-link">
                                    <FiMail className="contact-icon" /> {casting.email}
                                </a>
                            </span>
                        </div>
                        <div className="info-item">
                            <span className="info-label">{translations[language].phone}</span>
                            <span className="info-value">
                                <a href={`tel:${casting.phone}`} className="contact-link">
                                    <FiPhone className="contact-icon" /> {casting.phone}
                                </a>
                            </span>
                        </div>
                        {casting.telegram && (
                            <div className="info-item">
                                <span className="info-label">{translations[language].telegram}</span>
                                <span className="info-value">
                                    <a href={`https://t.me/${casting.telegram}`} target="_blank" rel="noopener noreferrer" className="contact-link">
                                        <FaTelegramPlane className="contact-icon" /> {casting.telegram}
                                    </a>
                                </span>
                            </div>
                        )}
                        {casting.facebook && (
                            <div className="info-item">
                                <span className="info-label">{translations[language].facebook}</span>
                                <span className="info-value">
                                    <a href={casting.facebook} target="_blank" rel="noopener noreferrer" className="contact-link">
                                        <FiFacebook className="contact-icon" /> {casting.facebook}
                                    </a>
                                </span>
                            </div>
                        )}
                        {casting.instagram && (
                            <div className="info-item">
                                <span className="info-label">{translations[language].instagram}</span>
                                <span className="info-value">
                                    <a href={`https://instagram.com/${casting.instagram}`} target="_blank" rel="noopener noreferrer" className="contact-link">
                                        <FiInstagram className="contact-icon" /> {casting.instagram}
                                    </a>
                                </span>
                            </div>
                        )}
                        <div className="info-item">
                            <span className="info-label">{translations[language].price}</span>
                            <span className="info-value">{casting.price} $</span>
                        </div>
                    </div>
                );
            case 'gallery':
                return (
                    <div className="gallery-grid">
                        {casting.photos.map((photo, index) => (
                            <motion.div
                                key={index}
                                className="gallery-item"
                                whileHover={{ scale: 1.03 }}
                                transition={{ duration: 0.2 }}
                            >
                                <img
                                    src={`${baseUrl}/api/v1/file/getFile/${photo.id}`}
                                    alt={`Gallery ${index + 1}`}
                                    onClick={() => openImageModal(`${baseUrl}/api/v1/file/getFile/${photo.id}`)}
                                    loading="lazy"
                                />
                                <div className="gallery-overlay">
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
        <div className="appeal-container">
            <Header props={""} />
            <main className="appeal-main">
                {loading ? (
                    <div className="loading-container">
                        <div className="loading-spinner"></div>
                        <p>{translations[language].loading}</p>
                    </div>
                ) : error ? (
                    <div className="error-message">
                        {translations[language].error}: {error}
                    </div>
                ) : casting === null ? (
                    <div className="no-applications">
                        {translations[language].noApplications}
                    </div>
                ) : (
                    <div className="appeal-detail">
                        <button
                            className="back-button"
                            onClick={() => navigate(-1)}
                        >
                            <FiArrowLeft /> {translations[language].back}
                        </button>

                        <div className="appeal-card">
                            {/* Profile Header */}
                            <div className="profile-header">
                                {casting.photos && casting.photos.length > 0 && (
                                    <div className="profile-avatar">
                                        <img
                                            src={`${baseUrl}/api/v1/file/getFile/${casting.photos[0].id}`}
                                            alt={casting.name}
                                        />
                                    </div>
                                )}
                                <div className="profile-info">
                                    <h1>{casting.name}</h1>
                                    <div className="profile-meta">
                                        <span className="casting-type">{casting.castingType}</span>
                                        <span className={`status-badge ${getStatusClass(casting.status)}`}>
                                            {getStatusText(casting.status)}
                                        </span>
                                    </div>
                                    <div className="profile-date">
                                        {translations[language].createdAt}: {formatDate(casting.createdAt)}
                                    </div>
                                </div>
                            </div>

                            {/* Navigation Tabs */}
                            <div className="tabs-container">
                                <button
                                    className={`tab-button ${activeTab === 'basic' ? 'active' : ''}`}
                                    onClick={() => setActiveTab('basic')}
                                >
                                    {translations[language].basicInfo}
                                </button>
                                <button
                                    className={`tab-button ${activeTab === 'physical' ? 'active' : ''}`}
                                    onClick={() => setActiveTab('physical')}
                                >
                                    {translations[language].physicalInfo}
                                </button>
                                <button
                                    className={`tab-button ${activeTab === 'contact' ? 'active' : ''}`}
                                    onClick={() => setActiveTab('contact')}
                                >
                                    {translations[language].contactInfo}
                                </button>
                                {casting.photos && casting.photos.length > 0 && (
                                    <button
                                        className={`tab-button ${activeTab === 'gallery' ? 'active' : ''}`}
                                        onClick={() => setActiveTab('gallery')}
                                    >
                                        {translations[language].gallery}
                                    </button>
                                )}
                            </div>

                            {/* Tab Content */}
                            <div className="tab-content">
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
                            className="modal-image"
                        />
                    )}
                </Modal>
            </main>
        </div>
    );
}

export default Appeal;