import React, { useState, useEffect } from 'react';
import ApiCall from "../../config";
import { useNavigate } from "react-router-dom";
import { AdminPage, AdminPageHeader, AdminError, AdminLoading } from "./AdminLayout";
import { FaUser, FaPhone, FaEnvelope, FaTelegram, FaCheckCircle, FaTimesCircle, FaClock, FaMoneyBillWave } from 'react-icons/fa';


// Karta uslubi (avval CastingUserAccepted.css dagi .user-card).
// nth-child animatsiya kechikishi indeks bo'yicha beriladi.
const CARD_DELAYS = [
    "[animation-delay:0.1s]",
    "[animation-delay:0.2s]",
    "[animation-delay:0.3s]",
    "[animation-delay:0.4s]",
    "[animation-delay:0.5s]",
    "[animation-delay:0.6s]",
    "[animation-delay:0.7s]",
    "[animation-delay:0.8s]",
];

const cardClass = (index) =>
    "group relative overflow-hidden cursor-pointer bg-[rgba(26,26,26,0.7)] backdrop-blur-[10px] " +
    "rounded-[20px] p-7 border border-white/10 animate-slideIn " +
    "transition-all duration-[400ms] ease-[cubic-bezier(0.175,0.885,0.32,1.275)] " +
    "before:content-[''] before:absolute before:top-0 before:left-0 before:right-0 before:h-px " +
    "before:bg-[linear-gradient(90deg,transparent,rgba(59,130,246,0.5),transparent)] " +
    "hover:-translate-y-2 hover:scale-[1.01] hover:border-[rgba(59,130,246,0.4)] " +
    "hover:shadow-[0_20px_40px_rgba(0,0,0,0.4),0_0_0_1px_rgba(59,130,246,0.2)] " +
    (CARD_DELAYS[index] || "");

const CastingUser = () => {
    const [castingUsers, setCastingUsers] = useState([]);
    const [filteredUsers, setFilteredUsers] = useState([]);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState(null);
    const [statusFilter, setStatusFilter] = useState("all");
    const navigate = useNavigate();
    const accessToken = localStorage.getItem("access_token");



    useEffect(() => {
        fetchCastingUsers();
    }, []);

    useEffect(() => {
        filterUsers();
    }, [statusFilter, castingUsers]);

    const fetchCastingUsers = async () => {
        setLoading(true);
        try {
            const response = await ApiCall('/api/v1/casting-user', 'GET');
            if (response.error) {
                setError(response.data);
            } else {
                setCastingUsers(response.data);
            }
        } catch (error) {
            console.error("Casting foydalanuvchilarni yuklashda xatolik:", error);
            setError("Ma'lumotlarni yuklashda xatolik yuz berdi");
        } finally {
            setLoading(false);
        }
    };

    const filterUsers = () => {
        if (!Array.isArray(castingUsers)) {
            console.error("castingUsers is not an array:", castingUsers);
            setFilteredUsers([]);
            return;
        }
        setFilteredUsers(
            castingUsers.filter(user => String(user.status) !== "1")
        );
    };


    const formatDate = (dateString) => {
        const options = { year: 'numeric', month: 'short', day: 'numeric' };
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

    const getStatusIcon = (status) => {
        if (status === 0) return <FaClock className="text-[0.9rem]" />;
        if (status === 1) return <FaCheckCircle className="text-[0.9rem]" />;
        return <FaTimesCircle className="text-[0.9rem]" />;
    };

    const getStatusClass = (status) => {
        const statusClasses = {
            0: "bg-[rgba(245,158,11,0.15)] text-[#f59e0b] border border-[rgba(245,158,11,0.3)]",
            1: "bg-[rgba(16,185,129,0.15)] text-[#10b981] border border-[rgba(16,185,129,0.3)]",
            2: "bg-[rgba(239,68,68,0.15)] text-[#ef4444] border border-[rgba(239,68,68,0.3)]"
        };
        return statusClasses[status] || "";
    };

    const handleViewDetails = (castingUserId) => {
        navigate(`/admin/casting-users/${castingUserId}`);
    };

    return (
        <AdminPage headerProps='admin/casting-users'>
                <AdminPageHeader title="Qabul qilinganlar">
                    <div className="flex gap-4">
                        <div className="bg-white/5 backdrop-blur-[10px] border border-white/10 rounded-2xl px-6 py-4 flex flex-col items-center transition-all duration-300 hover:-translate-y-[5px] hover:shadow-[0_10px_25px_rgba(59,130,246,0.15)]">
                            <span className="text-[2rem] font-bold text-[#3b82f6]">{filteredUsers.length}</span>
                            <span className="text-[0.85rem] text-[#a1a1aa]">Jami qabul qilinganlar</span>
                        </div>
                    </div>
                </AdminPageHeader>

                <AdminError>{error}</AdminError>

                {loading && !castingUsers.length ? (
                    <AdminLoading />
                ) : (
                    <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 wide:grid-cols-4 gap-6">
                        {filteredUsers.map((user, index) => (
                            <div
                                key={user.id}
                                onClick={() => handleViewDetails(user.id)}
                                className={cardClass(index)}
                            >
                                <div className="absolute top-0 left-0 right-0 h-full rounded-[20px] bg-[radial-gradient(circle_at_50%_0%,rgba(59,130,246,0.1),transparent_60%)] opacity-0 transition-opacity duration-[400ms] group-hover:opacity-100"></div>
                                <div className="flex justify-between items-start mb-6 relative z-[2]">
                                    <div className="flex-1">
                                        <div className="text-[0.8rem] text-[#a1a1aa] mb-1">#{user.id}</div>
                                        <h3 className="text-[1.25rem] font-bold text-white flex items-center gap-3 mt-0 mb-2 mx-0">
                                            <FaUser className="text-[#3b82f6] flex-shrink-0 text-base" />
                                            {user.name}
                                        </h3>
                                        <p className="text-[0.9rem] text-[#a1a1aa] m-0">
                                            {user.castingType} • {user.gender}
                                        </p>
                                    </div>
                                    <span className={`flex items-center gap-2 text-[0.85rem] font-semibold px-4 py-2 rounded-full whitespace-nowrap ${getStatusClass(user.status)}`}>
                                        {getStatusIcon(user.status)}
                                        {getStatusText(user.status)}
                                    </span>
                                </div>

                                <div className="flex flex-col gap-4 mb-7 relative z-[2]">
                                    <div className="flex items-center gap-3 text-[0.95rem] text-[#d4d4d8] transition-colors duration-300 group-hover:text-white">
                                        <FaPhone className="text-[#3b82f6] flex-shrink-0 text-base" />
                                        <span>{user.phone}</span>
                                    </div>
                                    {user.email && (
                                        <div className="flex items-center gap-3 text-[0.95rem] text-[#d4d4d8] transition-colors duration-300 group-hover:text-white">
                                            <FaEnvelope className="text-[#3b82f6] flex-shrink-0 text-base" />
                                            <span>{user.email}</span>
                                        </div>
                                    )}
                                    {user.telegram && (
                                        <div className="flex items-center gap-3 text-[0.95rem] text-[#d4d4d8] transition-colors duration-300 group-hover:text-white">
                                            <FaTelegram className="text-[#3b82f6] flex-shrink-0 text-base" />
                                            <span>@{user.telegram}</span>
                                        </div>
                                    )}
                                </div>

                                <div className="flex justify-between items-center text-[0.85rem] text-[#a1a1aa] relative z-[2] flex-wrap sm:flex-nowrap gap-2">
                                    <span className="bg-white/5 px-[0.8rem] py-[0.4rem] rounded-lg">{formatDate(user.createdAt)}</span>
                                    <div className={`flex items-center gap-2 px-[0.8rem] py-[0.4rem] rounded-lg font-medium ${user.secondChan === 0 ? 'bg-[rgba(239,68,68,0.15)] text-[#ef4444]' : 'bg-[rgba(16,185,129,0.15)] text-[#10b981]'}`}>
                                        <FaMoneyBillWave className="text-[0.9rem]" />
                                        {user.secondChan === 0 ? "To'lov qilmagan" : "To'lov qilgan"}
                                    </div>
                                    <span className="text-[#3b82f6] font-semibold no-underline transition-all duration-300 flex items-center gap-1 group-hover:gap-2">Batafsil &rarr;</span>
                                </div>
                            </div>
                        ))}
                    </div>
                )}

                {!loading && filteredUsers.length === 0 && (
                    <div className="text-center px-8 py-16 bg-white/[0.03] rounded-[20px] border border-dashed border-white/10 animate-fadeIn">
                        <div className="text-[4rem] mb-6 opacity-70">👥</div>
                        <h3 className="text-2xl text-white mt-0 mb-4 mx-0">Hozircha qabul qilinganlar mavjud emas</h3>
                        <p className="text-[#a1a1aa] m-0 text-[1.1rem]">Qabul qilinganlar ro'yxati shu yerda ko'rinadi</p>
                    </div>
                )}
        </AdminPage>
    );
};

export default CastingUser;