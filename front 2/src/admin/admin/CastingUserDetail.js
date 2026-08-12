import React, { useEffect, useState } from 'react';
import Header from "./HeaderAdmin";
import "react-responsive-modal/styles.css";
import ApiCall, { baseUrl } from '../../config/index';
import { useParams, useNavigate } from "react-router-dom";
import { Modal } from "react-responsive-modal";
import './CastingUserDetail.css';
import {
    FaArrowLeft,
    FaCheck,
    FaTimes,
    FaDollarSign,
    FaTrash,
    FaUser,
    FaRulerVertical,
    FaPalette,
    FaEye,
    FaTshirt,  // Using FaTshirt instead of FaShirt
    FaShoePrints,
    FaMapMarkerAlt,
    FaGlobe,
    FaBirthdayCake,
    FaPhone,
    FaEnvelope,
    FaPaperPlane,
    FaFacebook,
    FaInstagram,
    FaImages,
    FaMoneyBillWave,
    FaClock
} from 'react-icons/fa';

function CastingUserDetail() {
    const { castingUserId } = useParams();
    const navigate = useNavigate();
    const accessToken = localStorage.getItem("access_token");
    const checkSecurity = () => {
        const accessToken = localStorage.getItem("access_token");

        if (!accessToken) {
            navigate("/admin/login");
        }
    };
    useEffect(() => {
        checkSecurity()
    }, []);

    const [casting, setCasting] = useState(null);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState(null);
    const [selectedImage, setSelectedImage] = useState(null);
    const [isModalOpen, setIsModalOpen] = useState(false);
    const [isPriceModalOpen, setIsPriceModalOpen] = useState(false);
    const [price, setPrice] = useState('');
    const [confirmPhotoModal, setConfirmPhotoModal] = useState({ show: false, attachment: null });
    const confirmTogglePhoto = (attachment) => {
        setConfirmPhotoModal({ show: true, attachment });
    };


    const handleTogglePhoto = async () => {
        if (!confirmPhotoModal) return;
        console.log(confirmPhotoModal.attachment.id);

        try {
            await ApiCall(`/api/v1/file/${confirmPhotoModal.attachment.id}`, "PUT");
            // Frontend state yangilash
            setCasting(prev => ({
                ...prev,
                photos: prev.photos.map(p =>
                    p.id === confirmPhotoModal.attachment.id
                        ? { ...p, isWebShow: !p.isWebShow }
                        : p
                )
            }));
        } catch (error) {
            console.error("Rasmni yangilashda xatolik:", error);
            setError("Rasmni yangilashda xatolik yuz berdi");
        } finally {
            setConfirmPhotoModal({ show: false, attachment: null });
        }
    };

    useEffect(() => {
        fetchCasting();
    }, []);

    const fetchCasting = async () => {
        setLoading(true);
        try {
            const response = await ApiCall('/api/v1/casting-user/appeal/' + castingUserId, 'GET');
            console.log(response.data);

            if (response.error) {
                setError(response.data);
            } else {
                setCasting(response.data);
                if (response.data.price) {
                    setPrice(response.data.price);
                }
            }
        } catch (error) {
            console.error("Castingni yuklashda xatolik:", error);
            setError("Ma'lumotlarni yuklashda xatolik yuz berdi");
        } finally {
            setLoading(false);
        }
    };

    const formatDate = (dateString) => {
        const options = { year: 'numeric', month: 'long', day: 'numeric' };
        return new Date(dateString).toLocaleDateString('uz-UZ', options);
    };

    const getStatusText = (status) => {
        const statusTexts = {
            0: "Ko'rib chiqilmoqda",
            1: "Qabul qilindi",
            2: "Rad etildi"
        };
        return statusTexts[status] || "";
    };

    const getStatusClass = (status) => {
        const statusClasses = {
            0: "status-pending",
            1: "status-approved",
            2: "status-rejected"
        };
        return statusClasses[status] || "";
    };

    const getStatusIcon = (status) => {
        if (status === 0) return <FaClock className="status-icon" />;
        if (status === 1) return <FaCheck className="status-icon" />;
        return <FaTimes className="status-icon" />;
    };

    const openImageModal = (imageUrl) => {
        setSelectedImage(imageUrl);
        setIsModalOpen(true);
    };

    const closeImageModal = () => {
        setIsModalOpen(false);
        setSelectedImage(null);
    };

    const handleAccept = async () => {
        if (!price) {
            alert("Iltimos, narx kiriting!");
            return;
        }

        setLoading(true);
        try {
            await ApiCall(`/api/v1/casting-user/price/${castingUserId}/${price}`, 'PUT');
            await ApiCall(`/api/v1/casting-user/status/${castingUserId}/1/${price}`, 'PUT');
            alert("Foydalanuvchi qabul qilindi va narx saqlandi.");
            fetchCasting();
            setIsPriceModalOpen(false);
        } catch (error) {
            console.error("Qabul qilishda xatolik:", error);
            setError("Qabul qilishda xatolik yuz berdi");
        } finally {
            setLoading(false);
        }
    };

    const handleReject = async () => {
        if (!window.confirm("Haqiqatan ham ushbu foydalanuvchini rad qilmoqchimisiz?")) {
            return;
        }

        setLoading(true);
        try {
            await ApiCall(`/api/v1/casting-user/status/${castingUserId}/2/0`, 'PUT');
            alert("Foydalanuvchi rad etildi.");
            fetchCasting();
        } catch (error) {
            console.error("Rad qilishda xatolik:", error);
            setError("Rad qilishda xatolik yuz berdi");
        } finally {
            setLoading(false);
        }
    };
    const [confirmModal, setConfirmModal] = useState({ show: false, userId: null });

    const confirmToggleWebShow = (userId) => {
        setConfirmModal({ show: true, userId });
    };

    const handleConfirm = async () => {
        if (!confirmModal.userId) return;
        try {
            await ApiCall(`/api/v1/casting-user/web-show/${confirmModal.userId}`, "PUT");
            setCasting(prev =>
                prev.id === confirmModal.userId
                    ? { ...prev, isWebShow: !prev.isWebShow }
                    : prev
            );
        } catch (error) {
            console.error("isWebShow yangilashda xatolik:", error);
        } finally {
            setConfirmModal({ show: false, userId: null });
        }
    };

    const handleCancel = () => {
        setConfirmModal({ show: false, userId: null });
    };


    const handleDelete = async () => {
        if (!window.confirm("Ushbu foydalanuvchini butunlay o'chirmoqchimisiz?")) return;

        setLoading(true);
        try {
            await ApiCall(`/api/v1/casting-user/${castingUserId}`, 'DELETE');
            alert("Foydalanuvchi muvaffaqiyatli o'chirildi.");
            navigate(-1);
        } catch (error) {
            console.error("O'chirishda xatolik:", error);
            setError("O'chirishda xatolik yuz berdi");
        } finally {
            setLoading(false);
        }
    };

    const hasPayment = async () => {
        if (!window.confirm("Ushbu foydalanuvchini rosttan ham to'lov qildimi?")) return;
        setLoading(true);
        try {
            await ApiCall(`/api/v1/casting-user/payed/${castingUserId}`, 'GET');
            navigate(-1);
        } catch (error) {
            setError("To'lovni tasdiqlashda xatolik yuz berdi");
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="casting-detail-dark">
            <Header props={""} />

            <div className="casting-detail-content">
                <button className="back-button" onClick={() => navigate(-1)}>
                    <FaArrowLeft className="button-icon" /> Orqaga
                </button>

                {loading && !casting ? (
                    <div className="loading-container">
                        <div className="loading-spinner"></div>
                        <p className="loading-text">Yuklanmoqda...</p>
                    </div>
                ) : error ? (
                    <div className="error-message">
                        <div className="error-icon">⚠️</div>
                        <div className="error-content">
                            <h3>Xatolik yuz berdi</h3>
                            <p>{error}</p>
                        </div>
                    </div>
                ) : casting === null ? (
                    <div className="error-message">
                        <div className="error-icon">🔍</div>
                        <div className="error-content">
                            <h3>Ma'lumotlar topilmadi</h3>
                            <p>So'ralgan foydalanuvchi ma'lumotlari topilmadi</p>
                        </div>
                    </div>
                ) : (
                    <>
                        <div className="user-profile-header">
                            <div className="profile-avatar">
                                {casting.photos && casting.photos.length > 0 ? (
                                    <img
                                        src={`${baseUrl}/api/v1/file/getFile/${casting.photos[0].id}`}
                                        alt={casting.name}
                                    />
                                ) : (
                                    <div className="avatar-placeholder">
                                        <FaUser />
                                    </div>
                                )}
                            </div>
                            <div className="profile-info ">
                                <h1 className="user-name">{casting.name}</h1>
                                <div className="user-meta">
                                    {/*<span className="user-type">{casting.castingType}</span>*/}
                                    {/*<span className="user-gender">{casting.gender}</span>*/}
                                    {/*<span className={`status-badge ${getStatusClass(casting.status)}`}>*/}
                                    {/*    {getStatusIcon(casting.status)}*/}
                                    {/*    {getStatusText(casting.status)}*/}
                                    {/*</span>*/}
                                </div>

                                {/* 🔘 isWebShow tugmasi */}
                                <button
                                    onClick={() => confirmToggleWebShow(casting.id)}
                                    className={`toggle-btn ${casting.isWebShow ? "active-btn" : "inactive-btn"}`}
                                >
                                    {casting.isWebShow ? "Saytda Faol" : "Saytda Nofaol"}
                                </button>
                            </div>
                        </div>



                        <div className="info-cards-container">
                            {/*<div className="info-card">*/}
                            {/*    <h2 className="section-title">*/}
                            {/*        <FaUser className="section-icon" />*/}
                            {/*        Asosiy ma'lumotlar*/}
                            {/*    </h2>*/}
                            {/*    <div className="info-grid">*/}
                            {/*        <div className="info-item">*/}
                            {/*            <div className="info-icon"><FaMapMarkerAlt /></div>*/}
                            {/*            <div className="info-content">*/}
                            {/*                <span className="info-label">Hudud</span>*/}
                            {/*                <span className="info-value">{casting.region}</span>*/}
                            {/*            </div>*/}
                            {/*        </div>*/}
                            {/*        <div className="info-item">*/}
                            {/*            <div className="info-icon"><FaGlobe /></div>*/}
                            {/*            <div className="info-content">*/}
                            {/*                <span className="info-label">Millat</span>*/}
                            {/*                <span className="info-value">{casting.nationality}</span>*/}
                            {/*            </div>*/}
                            {/*        </div>*/}
                            {/*        <div className="info-item">*/}
                            {/*            <div className="info-icon"><FaBirthdayCake /></div>*/}
                            {/*            <div className="info-content">*/}
                            {/*                <span className="info-label">Tug'ilgan sana</span>*/}
                            {/*                <span className="info-value">{formatDate(casting.birthday)}</span>*/}
                            {/*            </div>*/}
                            {/*        </div>*/}
                            {/*    </div>*/}
                            {/*</div>*/}

                            {/*<div className="info-card">*/}
                            {/*    <h2 className="section-title">*/}
                            {/*        <FaRulerVertical className="section-icon" />*/}
                            {/*        Jismoniy tavsif*/}
                            {/*    </h2>*/}
                            {/*    <div className="info-grid">*/}
                            {/*        <div className="info-item">*/}
                            {/*            <div className="info-icon">🎂</div>*/}
                            {/*            <div className="info-content">*/}
                            {/*                <span className="info-label">Yosh</span>*/}
                            {/*                <span className="info-value">{casting.age}</span>*/}
                            {/*            </div>*/}
                            {/*        </div>*/}
                            {/*        <div className="info-item">*/}
                            {/*            <div className="info-icon">📏</div>*/}
                            {/*            <div className="info-content">*/}
                            {/*                <span className="info-label">Bo'y</span>*/}
                            {/*                <span className="info-value">{casting.height} sm</span>*/}
                            {/*            </div>*/}
                            {/*        </div>*/}
                            {/*        <div className="info-item">*/}
                            {/*            <div className="info-icon"><FaPalette /></div>*/}
                            {/*            <div className="info-content">*/}
                            {/*                <span className="info-label">Soch rangi</span>*/}
                            {/*                <span className="info-value">{casting.hairColor}</span>*/}
                            {/*            </div>*/}
                            {/*        </div>*/}
                            {/*        <div className="info-item">*/}
                            {/*            <div className="info-icon"><FaEye /></div>*/}
                            {/*            <div className="info-content">*/}
                            {/*                <span className="info-label">Ko'z rangi</span>*/}
                            {/*                <span className="info-value">{casting.eyeColor}</span>*/}
                            {/*            </div>*/}
                            {/*        </div>*/}
                            {/*        <div className="info-item">*/}
                            {/*            <div className="info-icon"><FaTshirt /></div>*/}
                            {/*            <div className="info-content">*/}
                            {/*                <span className="info-label">Kiyim o'lchami</span>*/}
                            {/*                <span className="info-value">{casting.clothSize}</span>*/}
                            {/*            </div>*/}
                            {/*        </div>*/}
                            {/*        <div className="info-item">*/}
                            {/*            <div className="info-icon"><FaShoePrints /></div>*/}
                            {/*            <div className="info-content">*/}
                            {/*                <span className="info-label">Oyoq o'lchami</span>*/}
                            {/*                <span className="info-value">{casting.shoeSize}</span>*/}
                            {/*            </div>*/}
                            {/*        </div>*/}
                            {/*        <div className="info-item">*/}
                            {/*            <div className="info-icon">📐</div>*/}
                            {/*            <div className="info-content">*/}
                            {/*                <span className="info-label">Ko'krak</span>*/}
                            {/*                <span className="info-value">{casting.bust} sm</span>*/}
                            {/*            </div>*/}
                            {/*        </div>*/}
                            {/*        <div className="info-item">*/}
                            {/*            <div className="info-icon">📐</div>*/}
                            {/*            <div className="info-content">*/}
                            {/*                <span className="info-label">Bel</span>*/}
                            {/*                <span className="info-value">{casting.waist} sm</span>*/}
                            {/*            </div>*/}
                            {/*        </div>*/}
                            {/*        <div className="info-item">*/}
                            {/*            <div className="info-icon">📐</div>*/}
                            {/*            <div className="info-content">*/}
                            {/*                <span className="info-label">Son</span>*/}
                            {/*                <span className="info-value">{casting.son} sm</span>*/}
                            {/*            </div>*/}
                            {/*        </div>*/}
                            {/*    </div>*/}
                            {/*</div>*/}

                            {/*<div className="info-card">*/}
                            {/*    <h2 className="section-title">*/}
                            {/*        <FaPaperPlane className="section-icon" />*/}
                            {/*        Aloqa ma'lumotlari*/}
                            {/*    </h2>*/}
                            {/*    <div className="info-grid">*/}
                            {/*        <div className="info-item">*/}
                            {/*            <div className="info-icon"><FaEnvelope /></div>*/}
                            {/*            <div className="info-content">*/}
                            {/*                <span className="info-label">Email</span>*/}
                            {/*                <span className="info-value">{casting.email || "Ko'rsatilmagan"}</span>*/}
                            {/*            </div>*/}
                            {/*        </div>*/}
                            {/*        <div className="info-item">*/}
                            {/*            <div className="info-icon"><FaPhone /></div>*/}
                            {/*            <div className="info-content">*/}
                            {/*                <span className="info-label">Telefon</span>*/}
                            {/*                <span className="info-value">{casting.phone}</span>*/}
                            {/*            </div>*/}
                            {/*        </div>*/}
                            {/*        <div className="info-item">*/}
                            {/*            <div className="info-icon"><FaPaperPlane /></div>*/}
                            {/*            <div className="info-content">*/}
                            {/*                <span className="info-label">Telegram</span>*/}
                            {/*                <span className="info-value">{casting.telegram || "Ko'rsatilmagan"}</span>*/}
                            {/*            </div>*/}
                            {/*        </div>*/}
                            {/*        <div className="info-item">*/}
                            {/*            <div className="info-icon"><FaFacebook /></div>*/}
                            {/*            <div className="info-content">*/}
                            {/*                <span className="info-label">Facebook</span>*/}
                            {/*                <span className="info-value">{casting.facebook || "Ko'rsatilmagan"}</span>*/}
                            {/*            </div>*/}
                            {/*        </div>*/}
                            {/*        <div className="info-item">*/}
                            {/*            <div className="info-icon"><FaInstagram /></div>*/}
                            {/*            <div className="info-content">*/}
                            {/*                <span className="info-label">Instagram</span>*/}
                            {/*                <span className="info-value">{casting.instagram || "Ko'rsatilmagan"}</span>*/}
                            {/*            </div>*/}
                            {/*        </div>*/}
                            {/*        <div className="info-item">*/}
                            {/*            <div className="info-icon"><FaDollarSign /></div>*/}
                            {/*            <div className="info-content">*/}
                            {/*                <span className="info-label">Narx</span>*/}
                            {/*                <span className="info-value price-value">*/}
                            {/*                    {casting.price ? `$${casting.price}` : "Belgilanmagan"}*/}
                            {/*                </span>*/}
                            {/*            </div>*/}
                            {/*        </div>*/}
                            {/*    </div>*/}
                            {/*</div>*/}

                            {casting.photos && casting.photos.length > 0 && (
                                <div className="info-card">
                                    <h2 className="section-title">
                                        <FaImages className="section-icon" />
                                        Galereya
                                    </h2>
                                    <div className="gallery-grid">
                                        {casting.photos.map((photo, index) => (
                                            <div>
                                                <div key={index} className="gallery-item">
                                                    <div className="gallery-image-container">
                                                        <img
                                                            src={`${baseUrl}/api/v1/file/getFile/${photo.id}`}
                                                            alt={`Gallery ${index + 1}`}
                                                            onClick={() =>
                                                                openImageModal(`${baseUrl}/api/v1/file/getFile/${photo.id}`)
                                                            }
                                                        />
                                                    </div>


                                                </div>
                                                {/* Pastdagi tugmalar */}
                                                <div className="gallery-actions">
                                                    {/* <button
                                                        className="view-btn"
                                                        onClick={() =>
                                                            openImageModal(`${baseUrl}/api/v1/file/getFile/${photo.id}`)
                                                        }
                                                    >
                                                        👁 Ko‘rish
                                                    </button> */}

                                                    <button
                                                        className={`photo-toggle-btn ${photo.isWebShow ? "active-btn" : "inactive-btn"
                                                            }`}
                                                        onClick={() => confirmTogglePhoto(photo)}
                                                    >
                                                        {photo.isWebShow ? "Faol" : "Nofaol"}
                                                    </button>
                                                </div>
                                            </div>
                                        ))}
                                    </div>

                                </div>
                            )}
                        </div>

                        {/*<div className="action-buttons-container">*/}
                        {/*    {(casting.status === 1 && casting.secondChan == 0) && (*/}
                        {/*        <button*/}
                        {/*            className="action-btn payment-btn"*/}
                        {/*            onClick={hasPayment}*/}
                        {/*            disabled={loading}*/}
                        {/*        >*/}
                        {/*            <FaMoneyBillWave className="button-icon" />*/}
                        {/*            To'lov qildi*/}
                        {/*        </button>*/}
                        {/*    )}*/}

                        {/*    {casting.status === 0 && (*/}
                        {/*        <>*/}
                        {/*            <button*/}
                        {/*                className="action-btn accept-btn"*/}
                        {/*                onClick={() => setIsPriceModalOpen(true)}*/}
                        {/*                disabled={loading}*/}
                        {/*            >*/}
                        {/*                <FaCheck className="button-icon" />*/}
                        {/*                Qabul qilish*/}
                        {/*            </button>*/}

                        {/*            <button*/}
                        {/*                className="action-btn price-btn"*/}
                        {/*                onClick={() => setIsPriceModalOpen(true)}*/}
                        {/*                disabled={loading}*/}
                        {/*            >*/}
                        {/*                <FaDollarSign className="button-icon" />*/}
                        {/*                Narx belgilash*/}
                        {/*            </button>*/}

                        {/*            <button*/}
                        {/*                className="action-btn reject-btn"*/}
                        {/*                onClick={handleReject}*/}
                        {/*                disabled={loading}*/}
                        {/*            >*/}
                        {/*                <FaTimes className="button-icon" />*/}
                        {/*                Rad qilish*/}
                        {/*            </button>*/}
                        {/*        </>*/}
                        {/*    )}*/}

                        {/*    <button*/}
                        {/*        className="action-btn delete-btn"*/}
                        {/*        onClick={handleDelete}*/}
                        {/*        disabled={loading}*/}
                        {/*    >*/}
                        {/*        <FaTrash className="button-icon" />*/}
                        {/*        O'chirish*/}
                        {/*    </button>*/}
                        {/*</div>*/}
                    </>
                )}
                {/* Active/Inactive tasdiqlash modal */}
                <Modal
                    open={confirmPhotoModal.show}
                    onClose={() => setConfirmPhotoModal({ show: false, attachment: null })}
                    center
                    classNames={{
                        overlay: "confirm-overlay", // faqat confirm uchun
                        modal: "confirm-modal"
                    }}
                >


                    <div className="confirm-modal-content">
                        <h3>Rasmni yangilash</h3>
                        <p>
                            Ushbu rasmni {confirmPhotoModal.attachment?.isWebShow ? "Nofaol" : "Faol"} qilishni tasdiqlaysizmi?
                        </p>
                        <div className="modal-action-buttons">
                            <button className="modal-btn confirm-btn" onClick={handleTogglePhoto}>
                                Ha
                            </button>
                            <button
                                className="modal-btn cancel-btn"
                                onClick={() => setConfirmPhotoModal({ show: false, attachment: null })}
                            >
                                Yo‘q
                            </button>
                        </div>
                    </div>
                </Modal>


                {/* Rasm modal oynasi */}
                <Modal
                    open={isModalOpen}
                    onClose={() => setIsModalOpen(false)}
                    center
                    classNames={{
                        overlay: "image-overlay",   // faqat rasm uchun
                        modal: "image-modal"
                    }}
                >

                    {selectedImage && (
                        <div className="image-modal-content">
                            <img
                                src={selectedImage}
                                alt="To'liq rasm"
                            />
                            <button className="modal-close-btn" onClick={closeImageModal}>
                                <FaTimes />
                            </button>
                        </div>
                    )}
                </Modal>

                {/* Narx modal oynasi */}
                <Modal
                    open={isPriceModalOpen}
                    onClose={() => setIsPriceModalOpen(false)}
                    center
                    classNames={{
                        overlay: 'modal-overlay',
                        modal: 'price-modal'
                    }}
                >
                    <div className="price-modal-content">
                        <h2 className="modal-title">
                            <FaDollarSign className="modal-title-icon" />
                            Narx belgilash
                        </h2>
                        <p className="modal-description">Foydalanuvchi uchun narxni dollar ($) da kiriting</p>
                        <div className="price-input-container">
                            <span className="currency-symbol">$</span>
                            <input
                                type="number"
                                value={price}
                                onChange={(e) => setPrice(e.target.value)}
                                className="price-input"
                                placeholder="Narxni kiriting"
                                min="0"
                            />
                        </div>
                        <div className="modal-action-buttons">
                            <button
                                className="modal-btn confirm-btn"
                                onClick={handleAccept}
                                disabled={loading}
                            >
                                {loading ? (
                                    <>
                                        <div className="button-spinner"></div>
                                        Saqlanmoqda...
                                    </>
                                ) : (
                                    <>
                                        <FaCheck className="button-icon" />
                                        Saqlash va Qabul qilish
                                    </>
                                )}
                            </button>
                            <button
                                className="modal-btn cancel-btn"
                                onClick={() => setIsPriceModalOpen(false)}
                                disabled={loading}
                            >
                                <FaTimes className="button-icon" />
                                Bekor qilish
                            </button>
                        </div>
                    </div>
                </Modal>
                {confirmModal.show && (
                    <div className="modal-overlay">
                        <div className="modal-content">
                            <h3>Tasdiqlaysizmi?</h3>
                            <div className="modal-actions">
                                <button onClick={handleConfirm} className="btn-confirm">Ha</button>
                                <button onClick={handleCancel} className="btn-cancel">Yo‘q</button>
                            </div>
                        </div>
                    </div>
                )}

            </div>
        </div>
    );
}

export default CastingUserDetail;