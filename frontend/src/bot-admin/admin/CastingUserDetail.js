import React, { useEffect, useState } from 'react';
import { AdminPage, AdminPageHeader, AdminLoading } from "./AdminLayout";
import "react-responsive-modal/styles.css";
import ApiCall, { baseUrl } from '../../config/index';
import { useParams, useNavigate } from "react-router-dom";
import { Modal } from "react-responsive-modal";
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
            0: "bg-[rgba(245,158,11,0.15)] text-[#f59e0b] border border-[rgba(245,158,11,0.3)]",
            1: "bg-[rgba(16,185,129,0.15)] text-[#10b981] border border-[rgba(16,185,129,0.3)]",
            2: "bg-[rgba(239,68,68,0.15)] text-[#ef4444] border border-[rgba(239,68,68,0.3)]"
        };
        return statusClasses[status] || "";
    };

    const getStatusIcon = (status) => {
        if (status === 0) return <FaClock className="text-[0.9rem]" />;
        if (status === 1) return <FaCheck className="text-[0.9rem]" />;
        return <FaTimes className="text-[0.9rem]" />;
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
        <AdminPage headerProps={""}>
                <AdminPageHeader title="Ariza tafsilotlari">
                    <button className="inline-flex items-center bg-white/5 backdrop-blur-[10px] text-[#3b82f6] border border-white/10 rounded-xl px-5 py-3 mb-8 font-medium cursor-pointer transition-all duration-300 hover:bg-[rgba(59,130,246,0.1)] hover:-translate-x-[5px]" onClick={() => navigate("/admin/casting-users")}>
                        <FaArrowLeft className="mr-2" /> Orqaga
                    </button>
                </AdminPageHeader>
                {loading && !casting ? (
                    <AdminLoading />
                ) : error ? (
                    <div className="flex items-center bg-[linear-gradient(90deg,rgba(239,68,68,0.15),rgba(239,68,68,0.1))] text-[#fca5a5] p-6 rounded-2xl my-8 border-l-4 border-[#ef4444] backdrop-blur-[10px] animate-shake">
                        <div className="text-[2rem] mr-4">⚠️</div>
                        <div>
                            <h3 className="mt-0 mb-2 mx-0 text-[1.2rem]">Xatolik yuz berdi</h3>
                            <p className="m-0 opacity-90">{error}</p>
                        </div>
                    </div>
                ) : casting === null ? (
                    <div className="flex items-center bg-[linear-gradient(90deg,rgba(239,68,68,0.15),rgba(239,68,68,0.1))] text-[#fca5a5] p-6 rounded-2xl my-8 border-l-4 border-[#ef4444] backdrop-blur-[10px] animate-shake">
                        <div className="text-[2rem] mr-4">🔍</div>
                        <div>
                            <h3 className="mt-0 mb-2 mx-0 text-[1.2rem]">Ma'lumotlar topilmadi</h3>
                            <p className="m-0 opacity-90">So'ralgan foydalanuvchi ma'lumotlari topilmadi</p>
                        </div>
                    </div>
                ) : (
                    <>
                        <div className="flex items-center mb-10 bg-white/[0.03] rounded-[20px] p-8 border border-white/10 max-md:flex-col max-md:text-center max-md:p-6">
                            <div className="w-[100px] h-[100px] rounded-full overflow-hidden mr-6 border-[3px] border-[rgba(59,130,246,0.3)] flex-shrink-0 max-md:mr-0 max-md:mb-4">
                                {casting.photos && casting.photos.length > 0 ? (
                                    <img
                                        src={`${baseUrl}/api/v1/file/getFile/${casting.photos[0].id}`}
                                        alt={casting.name}
                                        className="w-full h-full object-cover"
                                    />
                                ) : (
                                    <div className="w-full h-full flex items-center justify-center bg-[rgba(59,130,246,0.2)] text-[2.5rem]">
                                        <FaUser />
                                    </div>
                                )}
                            </div>
                            <div className="flex-1">
                                <h1 className="text-[2rem] font-bold mt-0 mb-3 mx-0 bg-[linear-gradient(90deg,#3b82f6,#10b981)] bg-clip-text text-transparent max-[480px]:text-2xl">{casting.name}</h1>
                                <div className="flex flex-wrap gap-3 items-center max-md:justify-center">
                                    <span className="bg-white/10 px-[0.8rem] py-[0.4rem] rounded-lg text-[0.9rem]">{casting.castingType}</span>
                                    <span className="bg-white/10 px-[0.8rem] py-[0.4rem] rounded-lg text-[0.9rem]">{casting.gender}</span>
                                    <span className={`inline-flex items-center gap-2 px-4 py-2 rounded-full text-[0.85rem] font-medium ${getStatusClass(casting.status)}`}>
                                        {getStatusIcon(casting.status)}
                                        {getStatusText(casting.status)}
                                    </span>
                                </div>
                            </div>
                        </div>

                        <div className="flex flex-col gap-6 mb-8">
                            <div className="bg-[rgba(26,26,26,0.7)] backdrop-blur-[10px] rounded-[20px] p-7 border border-white/10 transition-all duration-300 hover:-translate-y-[5px] hover:shadow-[0_10px_30px_rgba(0,0,0,0.3)]">
                                <h2 className="flex items-center text-[1.35rem] font-semibold text-white mt-0 mb-6 mx-0 pb-3 border-b border-white/10">
                                    <FaUser className="text-[#3b82f6] mr-3 text-[1.1rem]" />
                                    Asosiy ma'lumotlar
                                </h2>
                                <div className="grid grid-cols-[repeat(auto-fill,minmax(280px,1fr))] gap-4 max-md:grid-cols-1">
                                    <div className="flex items-center bg-white/5 p-4 rounded-xl transition-all duration-300 hover:bg-white/[0.08] hover:translate-x-[5px]">
                                        <div className="text-[1.1rem] text-[#3b82f6] mr-3 w-6 text-center"><FaMapMarkerAlt /></div>
                                        <div className="flex flex-col">
                                            <span className="text-[0.85rem] text-[#a1a1aa] mb-1">Hudud</span>
                                            <span className="text-base font-medium text-white">{casting.region}</span>
                                        </div>
                                    </div>
                                    <div className="flex items-center bg-white/5 p-4 rounded-xl transition-all duration-300 hover:bg-white/[0.08] hover:translate-x-[5px]">
                                        <div className="text-[1.1rem] text-[#3b82f6] mr-3 w-6 text-center"><FaGlobe /></div>
                                        <div className="flex flex-col">
                                            <span className="text-[0.85rem] text-[#a1a1aa] mb-1">Millat</span>
                                            <span className="text-base font-medium text-white">{casting.nationality}</span>
                                        </div>
                                    </div>
                                    <div className="flex items-center bg-white/5 p-4 rounded-xl transition-all duration-300 hover:bg-white/[0.08] hover:translate-x-[5px]">
                                        <div className="text-[1.1rem] text-[#3b82f6] mr-3 w-6 text-center"><FaBirthdayCake /></div>
                                        <div className="flex flex-col">
                                            <span className="text-[0.85rem] text-[#a1a1aa] mb-1">Tug'ilgan sana</span>
                                            <span className="text-base font-medium text-white">{formatDate(casting.birthday)}</span>
                                        </div>
                                    </div>
                                </div>
                            </div>

                            <div className="bg-[rgba(26,26,26,0.7)] backdrop-blur-[10px] rounded-[20px] p-7 border border-white/10 transition-all duration-300 hover:-translate-y-[5px] hover:shadow-[0_10px_30px_rgba(0,0,0,0.3)]">
                                <h2 className="flex items-center text-[1.35rem] font-semibold text-white mt-0 mb-6 mx-0 pb-3 border-b border-white/10">
                                    <FaRulerVertical className="text-[#3b82f6] mr-3 text-[1.1rem]" />
                                    Jismoniy tavsif
                                </h2>
                                <div className="grid grid-cols-[repeat(auto-fill,minmax(280px,1fr))] gap-4 max-md:grid-cols-1">
                                    <div className="flex items-center bg-white/5 p-4 rounded-xl transition-all duration-300 hover:bg-white/[0.08] hover:translate-x-[5px]">
                                        <div className="text-[1.1rem] text-[#3b82f6] mr-3 w-6 text-center">🎂</div>
                                        <div className="flex flex-col">
                                            <span className="text-[0.85rem] text-[#a1a1aa] mb-1">Yosh</span>
                                            <span className="text-base font-medium text-white">{casting.age}</span>
                                        </div>
                                    </div>
                                    <div className="flex items-center bg-white/5 p-4 rounded-xl transition-all duration-300 hover:bg-white/[0.08] hover:translate-x-[5px]">
                                        <div className="text-[1.1rem] text-[#3b82f6] mr-3 w-6 text-center">📏</div>
                                        <div className="flex flex-col">
                                            <span className="text-[0.85rem] text-[#a1a1aa] mb-1">Bo'y</span>
                                            <span className="text-base font-medium text-white">{casting.height} sm</span>
                                        </div>
                                    </div>
                                    <div className="flex items-center bg-white/5 p-4 rounded-xl transition-all duration-300 hover:bg-white/[0.08] hover:translate-x-[5px]">
                                        <div className="text-[1.1rem] text-[#3b82f6] mr-3 w-6 text-center"><FaPalette /></div>
                                        <div className="flex flex-col">
                                            <span className="text-[0.85rem] text-[#a1a1aa] mb-1">Soch rangi</span>
                                            <span className="text-base font-medium text-white">{casting.hairColor}</span>
                                        </div>
                                    </div>
                                    <div className="flex items-center bg-white/5 p-4 rounded-xl transition-all duration-300 hover:bg-white/[0.08] hover:translate-x-[5px]">
                                        <div className="text-[1.1rem] text-[#3b82f6] mr-3 w-6 text-center"><FaEye /></div>
                                        <div className="flex flex-col">
                                            <span className="text-[0.85rem] text-[#a1a1aa] mb-1">Ko'z rangi</span>
                                            <span className="text-base font-medium text-white">{casting.eyeColor}</span>
                                        </div>
                                    </div>
                                    <div className="flex items-center bg-white/5 p-4 rounded-xl transition-all duration-300 hover:bg-white/[0.08] hover:translate-x-[5px]">
                                        <div className="text-[1.1rem] text-[#3b82f6] mr-3 w-6 text-center"><FaTshirt /></div>
                                        <div className="flex flex-col">
                                            <span className="text-[0.85rem] text-[#a1a1aa] mb-1">Kiyim o'lchami</span>
                                            <span className="text-base font-medium text-white">{casting.clothSize}</span>
                                        </div>
                                    </div>
                                    <div className="flex items-center bg-white/5 p-4 rounded-xl transition-all duration-300 hover:bg-white/[0.08] hover:translate-x-[5px]">
                                        <div className="text-[1.1rem] text-[#3b82f6] mr-3 w-6 text-center"><FaShoePrints /></div>
                                        <div className="flex flex-col">
                                            <span className="text-[0.85rem] text-[#a1a1aa] mb-1">Oyoq o'lchami</span>
                                            <span className="text-base font-medium text-white">{casting.shoeSize}</span>
                                        </div>
                                    </div>
                                    <div className="flex items-center bg-white/5 p-4 rounded-xl transition-all duration-300 hover:bg-white/[0.08] hover:translate-x-[5px]">
                                        <div className="text-[1.1rem] text-[#3b82f6] mr-3 w-6 text-center">📐</div>
                                        <div className="flex flex-col">
                                            <span className="text-[0.85rem] text-[#a1a1aa] mb-1">Ko'krak</span>
                                            <span className="text-base font-medium text-white">{casting.bust} sm</span>
                                        </div>
                                    </div>
                                    <div className="flex items-center bg-white/5 p-4 rounded-xl transition-all duration-300 hover:bg-white/[0.08] hover:translate-x-[5px]">
                                        <div className="text-[1.1rem] text-[#3b82f6] mr-3 w-6 text-center">📐</div>
                                        <div className="flex flex-col">
                                            <span className="text-[0.85rem] text-[#a1a1aa] mb-1">Bel</span>
                                            <span className="text-base font-medium text-white">{casting.waist} sm</span>
                                        </div>
                                    </div>
                                    <div className="flex items-center bg-white/5 p-4 rounded-xl transition-all duration-300 hover:bg-white/[0.08] hover:translate-x-[5px]">
                                        <div className="text-[1.1rem] text-[#3b82f6] mr-3 w-6 text-center">📐</div>
                                        <div className="flex flex-col">
                                            <span className="text-[0.85rem] text-[#a1a1aa] mb-1">Son</span>
                                            <span className="text-base font-medium text-white">{casting.son} sm</span>
                                        </div>
                                    </div>
                                </div>
                            </div>

                            <div className="bg-[rgba(26,26,26,0.7)] backdrop-blur-[10px] rounded-[20px] p-7 border border-white/10 transition-all duration-300 hover:-translate-y-[5px] hover:shadow-[0_10px_30px_rgba(0,0,0,0.3)]">
                                <h2 className="flex items-center text-[1.35rem] font-semibold text-white mt-0 mb-6 mx-0 pb-3 border-b border-white/10">
                                    <FaPaperPlane className="text-[#3b82f6] mr-3 text-[1.1rem]" />
                                    Aloqa ma'lumotlari
                                </h2>
                                <div className="grid grid-cols-[repeat(auto-fill,minmax(280px,1fr))] gap-4 max-md:grid-cols-1">
                                    <div className="flex items-center bg-white/5 p-4 rounded-xl transition-all duration-300 hover:bg-white/[0.08] hover:translate-x-[5px]">
                                        <div className="text-[1.1rem] text-[#3b82f6] mr-3 w-6 text-center"><FaEnvelope /></div>
                                        <div className="flex flex-col">
                                            <span className="text-[0.85rem] text-[#a1a1aa] mb-1">Email</span>
                                            <span className="text-base font-medium text-white">{casting.email || "Ko'rsatilmagan"}</span>
                                        </div>
                                    </div>
                                    <div className="flex items-center bg-white/5 p-4 rounded-xl transition-all duration-300 hover:bg-white/[0.08] hover:translate-x-[5px]">
                                        <div className="text-[1.1rem] text-[#3b82f6] mr-3 w-6 text-center"><FaPhone /></div>
                                        <div className="flex flex-col">
                                            <span className="text-[0.85rem] text-[#a1a1aa] mb-1">Telefon</span>
                                            <span className="text-base font-medium text-white">{casting.phone}</span>
                                        </div>
                                    </div>
                                    <div className="flex items-center bg-white/5 p-4 rounded-xl transition-all duration-300 hover:bg-white/[0.08] hover:translate-x-[5px]">
                                        <div className="text-[1.1rem] text-[#3b82f6] mr-3 w-6 text-center"><FaPaperPlane /></div>
                                        <div className="flex flex-col">
                                            <span className="text-[0.85rem] text-[#a1a1aa] mb-1">Telegram</span>
                                            <span className="text-base font-medium text-white">{casting.telegram || "Ko'rsatilmagan"}</span>
                                        </div>
                                    </div>
                                    <div className="flex items-center bg-white/5 p-4 rounded-xl transition-all duration-300 hover:bg-white/[0.08] hover:translate-x-[5px]">
                                        <div className="text-[1.1rem] text-[#3b82f6] mr-3 w-6 text-center"><FaFacebook /></div>
                                        <div className="flex flex-col">
                                            <span className="text-[0.85rem] text-[#a1a1aa] mb-1">Facebook</span>
                                            <span className="text-base font-medium text-white">{casting.facebook || "Ko'rsatilmagan"}</span>
                                        </div>
                                    </div>
                                    <div className="flex items-center bg-white/5 p-4 rounded-xl transition-all duration-300 hover:bg-white/[0.08] hover:translate-x-[5px]">
                                        <div className="text-[1.1rem] text-[#3b82f6] mr-3 w-6 text-center"><FaInstagram /></div>
                                        <div className="flex flex-col">
                                            <span className="text-[0.85rem] text-[#a1a1aa] mb-1">Instagram</span>
                                            <span className="text-base font-medium text-white">{casting.instagram || "Ko'rsatilmagan"}</span>
                                        </div>
                                    </div>
                                    <div className="flex items-center bg-white/5 p-4 rounded-xl transition-all duration-300 hover:bg-white/[0.08] hover:translate-x-[5px]">
                                        <div className="text-[1.1rem] text-[#3b82f6] mr-3 w-6 text-center"><FaDollarSign /></div>
                                        <div className="flex flex-col">
                                            <span className="text-[0.85rem] text-[#a1a1aa] mb-1">Narx</span>
                                            <span className="text-base font-semibold text-[#10b981]">
                                                {casting.price ? `$${casting.price}` : "Belgilanmagan"}
                                            </span>
                                        </div>
                                    </div>
                                </div>
                            </div>

                            {casting.photos && casting.photos.length > 0 && (
                                <div className="bg-[rgba(26,26,26,0.7)] backdrop-blur-[10px] rounded-[20px] p-7 border border-white/10 transition-all duration-300 hover:-translate-y-[5px] hover:shadow-[0_10px_30px_rgba(0,0,0,0.3)]">
                                    <h2 className="flex items-center text-[1.35rem] font-semibold text-white mt-0 mb-6 mx-0 pb-3 border-b border-white/10">
                                        <FaImages className="text-[#3b82f6] mr-3 text-[1.1rem]" />
                                        Galereya
                                    </h2>
                                    <div className="grid place-items-center grid-cols-[repeat(auto-fit,minmax(180px,1fr))] gap-4 my-4 mx-auto max-w-[1000px] w-full max-md:grid-cols-2 max-[480px]:grid-cols-1">
                                        {casting.photos.map((photo, index) => (
                                            <div>
                                                <div key={index} className="group flex flex-col items-center gap-[6px] aspect-[3/4] rounded-xl overflow-hidden relative transition-all duration-300 mb-[10px] hover:scale-105 hover:shadow-[0_10px_25px_rgba(0,0,0,0.3)]">
                                                    <div className="relative w-full">
                                                        <img
                                                            src={`${baseUrl}/api/v1/file/getFile/${photo.id}`}
                                                            alt={`Gallery ${index + 1}`}
                                                            className="w-full h-full rounded-lg cursor-pointer object-cover transition-transform duration-300 hover:scale-[1.02]"
                                                            onClick={() =>
                                                                openImageModal(`${baseUrl}/api/v1/file/getFile/${photo.id}`)
                                                            }
                                                        />
                                                    </div>


                                                </div>
                                                {/* Pastdagi tugmalar */}
                                                <div className="flex justify-center gap-[10px] w-full">
                                                    {/* <button
                                                        className="bg-[#007bff] text-white px-3 py-1.5 rounded-md border-none cursor-pointer text-sm transition-colors duration-300 hover:bg-[#0056b3]"
                                                        onClick={() =>
                                                            openImageModal(`${baseUrl}/api/v1/file/getFile/${photo.id}`)
                                                        }
                                                    >
                                                        👁 Ko‘rish
                                                    </button> */}

                                                    {/*<button*/}
                                                    {/*    className={`photo-toggle-btn ${photo.isWebShow ? "active-btn" : "inactive-btn"*/}
                                                    {/*        }`}*/}
                                                    {/*    onClick={() => confirmTogglePhoto(photo)}*/}
                                                    {/*>*/}
                                                    {/*    {photo.isWebShow ? "Faol" : "Nofaol"}*/}
                                                    {/*</button>*/}
                                                </div>
                                            </div>
                                        ))}
                                    </div>

                                </div>
                            )}
                        </div>

                        <div className="flex flex-wrap gap-4 mt-8 max-md:flex-col">
                            {(casting.status === 1 && casting.secondChan == 0) && (
                                <button
                                    className="inline-flex items-center px-6 py-[0.9rem] rounded-xl font-medium cursor-pointer border-none transition-all duration-300 text-base disabled:opacity-60 disabled:cursor-not-allowed enabled:hover:-translate-y-[3px] enabled:hover:shadow-[0_5px_15px_rgba(0,0,0,0.3)] max-md:w-full max-md:justify-center bg-[linear-gradient(90deg,#065f46,#10b981)] text-white enabled:hover:bg-[linear-gradient(90deg,#047857,#059669)]"
                                    onClick={hasPayment}
                                    disabled={loading}
                                >
                                    <FaMoneyBillWave className="mr-2" />
                                    To'lov qildi
                                </button>
                            )}

                            {casting.status === 0 && (
                                <>
                                    <button
                                        className="inline-flex items-center px-6 py-[0.9rem] rounded-xl font-medium cursor-pointer border-none transition-all duration-300 text-base disabled:opacity-60 disabled:cursor-not-allowed enabled:hover:-translate-y-[3px] enabled:hover:shadow-[0_5px_15px_rgba(0,0,0,0.3)] max-md:w-full max-md:justify-center bg-[linear-gradient(90deg,#1d4ed8,#3b82f6)] text-white enabled:hover:bg-[linear-gradient(90deg,#1e40af,#2563eb)]"
                                        onClick={() => setIsPriceModalOpen(true)}
                                        disabled={loading}
                                    >
                                        <FaCheck className="mr-2" />
                                        Qabul qilish
                                    </button>

                                    <button
                                        className="inline-flex items-center px-6 py-[0.9rem] rounded-xl font-medium cursor-pointer border-none transition-all duration-300 text-base disabled:opacity-60 disabled:cursor-not-allowed enabled:hover:-translate-y-[3px] enabled:hover:shadow-[0_5px_15px_rgba(0,0,0,0.3)] max-md:w-full max-md:justify-center bg-[linear-gradient(90deg,#075985,#0ea5e9)] text-white enabled:hover:bg-[linear-gradient(90deg,#0c4a6e,#0284c7)]"
                                        onClick={() => setIsPriceModalOpen(true)}
                                        disabled={loading}
                                    >
                                        <FaDollarSign className="mr-2" />
                                        Narx belgilash
                                    </button>

                                    <button
                                        className="inline-flex items-center px-6 py-[0.9rem] rounded-xl font-medium cursor-pointer border-none transition-all duration-300 text-base disabled:opacity-60 disabled:cursor-not-allowed enabled:hover:-translate-y-[3px] enabled:hover:shadow-[0_5px_15px_rgba(0,0,0,0.3)] max-md:w-full max-md:justify-center bg-[linear-gradient(90deg,#7f1d1d,#ef4444)] text-white enabled:hover:bg-[linear-gradient(90deg,#991b1b,#dc2626)]"
                                        onClick={handleReject}
                                        disabled={loading}
                                    >
                                        <FaTimes className="mr-2" />
                                        Rad qilish
                                    </button>
                                </>
                            )}

                            <button
                                className="inline-flex items-center px-6 py-[0.9rem] rounded-xl font-medium cursor-pointer border-none transition-all duration-300 text-base disabled:opacity-60 disabled:cursor-not-allowed enabled:hover:-translate-y-[3px] enabled:hover:shadow-[0_5px_15px_rgba(0,0,0,0.3)] max-md:w-full max-md:justify-center bg-[linear-gradient(90deg,#7f1d1d,#ef4444)] text-white ml-auto enabled:hover:bg-[linear-gradient(90deg,#991b1b,#dc2626)]"
                                onClick={handleDelete}
                                disabled={loading}
                            >
                                <FaTrash className="mr-2" />
                                O'chirish
                            </button>
                        </div>
                    </>
                )}
                {/* Active/Inactive tasdiqlash modal */}
                <Modal
                    open={confirmPhotoModal.show}
                    onClose={() => setConfirmPhotoModal({ show: false, attachment: null })}
                    center
                    classNames={{
                        overlay: "",
                        modal: "!bg-[#1a1a1a] !text-white !rounded-xl !p-8 !max-w-[400px] !w-[90%]"
                    }}
                >


                    <div className="text-center">
                        <h3 className="mb-4 text-[1.3rem] text-[#3b82f6]">Rasmni yangilash</h3>
                        <p className="mb-6 text-[#d1d5db]">
                            Ushbu rasmni {confirmPhotoModal.attachment?.isWebShow ? "Nofaol" : "Faol"} qilishni tasdiqlaysizmi?
                        </p>
                        <div className="flex gap-4 justify-center max-md:flex-col">
                            <button className="relative z-[10000] inline-flex items-center px-6 py-3 rounded-[10px] font-medium cursor-pointer border-none transition-all duration-300 bg-[linear-gradient(90deg,#065f46,#10b981)] text-white enabled:hover:bg-[linear-gradient(90deg,#047857,#059669)]" onClick={handleTogglePhoto}>
                                Ha
                            </button>
                            <button
                                className="relative z-[10000] inline-flex items-center px-6 py-3 rounded-[10px] font-medium cursor-pointer border-none transition-all duration-300 bg-white/10 text-[#a1a1aa] enabled:hover:bg-white/20 enabled:hover:text-white"
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
                        overlay: "",
                        modal: "!bg-transparent !border-none !rounded-none !p-0 !max-w-[90vw] !w-auto"
                    }}
                >

                    {selectedImage && (
                        <div className="relative inline-block">
                            <img
                                src={selectedImage}
                                alt="To'liq rasm"
                                className="max-w-[90vw] max-h-[80vh] rounded-xl shadow-[0_20px_40px_rgba(0,0,0,0.5)]"
                            />
                            <button className="absolute -top-[15px] -right-[15px] w-10 h-10 rounded-full bg-[#ef4444] text-white border-none flex items-center justify-center cursor-pointer text-[1.2rem] transition-all duration-300 hover:scale-110 hover:bg-[#dc2626]" onClick={closeImageModal}>
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
                        overlay: '!bg-black/85 backdrop-blur-[5px]',
                        modal: '!bg-[#1a1a1a] !border !border-white/10 !rounded-[20px] !p-8 !max-w-[500px] !w-[90%]'
                    }}
                >
                    <div className="text-center">
                        <h2 className="flex items-center justify-center !text-white !mb-2 text-2xl">
                            <FaDollarSign className="text-[#10b981] mr-2" />
                            Narx belgilash
                        </h2>
                        <p className="text-[#a1a1aa] mb-6">Foydalanuvchi uchun narxni dollar ($) da kiriting</p>
                        <div className="relative mb-6">
                            <span className="absolute left-4 top-1/2 -translate-y-1/2 text-[#a1a1aa] font-medium text-[1.1rem]">$</span>
                            <input
                                type="number"
                                value={price}
                                onChange={(e) => setPrice(e.target.value)}
                                className="w-full py-4 pr-4 pl-10 bg-white/5 border border-white/10 rounded-xl text-white text-[1.1rem] transition-all duration-300 focus:outline-none focus:border-[#3b82f6] focus:shadow-[0_0_0_3px_rgba(59,130,246,0.2)]"
                                placeholder="Narxni kiriting"
                                min="0"
                            />
                        </div>
                        <div className="flex gap-4 justify-center max-md:flex-col">
                            <button
                                className="relative z-[10000] inline-flex items-center px-6 py-3 rounded-[10px] font-medium cursor-pointer border-none transition-all duration-300 bg-[linear-gradient(90deg,#065f46,#10b981)] text-white enabled:hover:bg-[linear-gradient(90deg,#047857,#059669)]"
                                onClick={handleAccept}
                                disabled={loading}
                            >
                                {loading ? (
                                    <>
                                        <div className="w-4 h-4 border-2 border-transparent border-t-white rounded-full animate-spin mr-2"></div>
                                        Saqlanmoqda...
                                    </>
                                ) : (
                                    <>
                                        <FaCheck className="mr-2" />
                                        Saqlash va Qabul qilish
                                    </>
                                )}
                            </button>
                            <button
                                className="relative z-[10000] inline-flex items-center px-6 py-3 rounded-[10px] font-medium cursor-pointer border-none transition-all duration-300 bg-white/10 text-[#a1a1aa] enabled:hover:bg-white/20 enabled:hover:text-white"
                                onClick={() => setIsPriceModalOpen(false)}
                                disabled={loading}
                            >
                                <FaTimes className="mr-2" />
                                Bekor qilish
                            </button>
                        </div>
                    </div>
                </Modal>
        </AdminPage>
    );
}

export default CastingUserDetail;