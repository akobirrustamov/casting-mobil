import React, { useState, useEffect } from 'react';
import ApiCall from "../../config/index";
import { useNavigate } from "react-router-dom";
import Header from "./HeaderAdmin";
import { FaUser, FaPhone, FaEnvelope, FaTelegram } from 'react-icons/fa';

const CastingUser = () => {
    const [castingUsers, setCastingUsers] = useState([]);
    const [filteredUsers, setFilteredUsers] = useState([]);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState(null);
    const [confirmModal, setConfirmModal] = useState({ show: false, userId: null });
    const navigate = useNavigate();

    useEffect(() => {
        const accessToken = localStorage.getItem("access_token");
        if (!accessToken) navigate("/aadmin/login");
        fetchCastingUsers();
    }, []);

    useEffect(() => {
        filterUsers();
    }, [castingUsers]);

    const fetchCastingUsers = async () => {
        setLoading(true);
        try {
            const response = await ApiCall('/api/v1/casting-user', 'GET');
            console.log("API response:", response.data);

            if (response.error) {
                setError(response.data);
            } else {
                // faqat massivni state ga yozamiz
                const users = Array.isArray(response.data)
                    ? response.data
                    : response.data.data || response.data.content || [];

                setCastingUsers(users);
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
        setFilteredUsers(castingUsers);
    };


    // Rest of the component remains the same...
    const confirmToggleWebShow = (userId) => {
        setConfirmModal({ show: true, userId });
    };

    const handleConfirm = async () => {
        if (!confirmModal.userId) return;
        try {
            await ApiCall(`/api/v1/casting-user/web-show/${confirmModal.userId}`, "PUT");
            setCastingUsers(prev =>
                prev.map(user =>
                    user.id === confirmModal.userId ? { ...user, isWebShow: !user.isWebShow } : user
                )
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

    const handleViewDetails = (castingUserId) => {
        navigate(`/aadmin/casting-users/${castingUserId}`);
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

    const getStatusClass = (status) => {
        const statusClasses = {
            0: "bg-[#422006] text-[#f59e0b]",
            1: "bg-[#052e16] text-[#10b981]",
            2: "bg-[#450a0a] text-[#ef4444]"
        };
        return statusClasses[status] || "";
    };

    return (
        <div className="[font-family:Inter,-apple-system,sans-serif] bg-black text-white min-h-screen pt-[70px]">
            <Header props='admin/casting-users' />

            <div className="p-4 max-w-[1600px] mx-auto">
                <h1 className="text-2xl font-bold text-white mb-4">Foydalanuvchilar</h1>

                {error && <div className="bg-[#450a0a] text-[#fca5a5] p-4 rounded-lg mb-6 border-l-4 border-[#ef4444]">{error}</div>}

                {loading && !castingUsers.length ? (
                    <div className="flex justify-center items-center h-[200px]">
                        <div className="w-10 h-10 border-4 border-[rgba(59,130,246,0.2)] border-t-[#3b82f6] rounded-full animate-spin"></div>
                        <span className="ml-4 text-[#a1a1aa]">Yuklanmoqda...</span>
                    </div>
                ) : (
                    <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
                        {filteredUsers.map((user) => (
                            <div key={user.id} className="cursor-pointer bg-[#1a1a1a] rounded-xl p-5 border border-[#333333] transition-all duration-300 hover:-translate-y-[3px] hover:shadow-[0_4px_20px_rgba(59,130,246,0.2)] hover:border-[#3b82f6]">
                                <div className="flex justify-between items-start mb-4">
                                    <div>
                                        <h3 className="text-[1.1rem] font-semibold text-white flex items-center gap-2">
                                            <FaUser className="text-[#3b82f6] flex-shrink-0" />
                                            {user.name}
                                        </h3>
                                        <p className="text-[0.85rem] text-[#a1a1aa] mt-1">
                                            {user.castingType} • {user.gender}
                                        </p>
                                    </div>
                                    {/*<span className={`text-xs font-medium px-3 py-1 rounded-full ${getStatusClass(user.status)}`}>*/}
                                    {/*    {getStatusText(user.status)}*/}
                                    {/*</span>*/}
                                </div>

                                <div className="flex flex-col gap-3 mb-5">
                                    <div className="flex items-center gap-2 text-[0.9rem] text-[#d4d4d8]">
                                        <FaPhone className="text-[#3b82f6] flex-shrink-0" />
                                        <span>{user.phone}</span>
                                    </div>
                                    <div className="flex items-center gap-2 text-[0.9rem] text-[#d4d4d8]">
                                        <FaEnvelope className="text-[#3b82f6] flex-shrink-0" />
                                        <span>{user.email}</span>
                                    </div>
                                    {user.telegram && (
                                        <div className="flex items-center gap-2 text-[0.9rem] text-[#d4d4d8]">
                                            <FaTelegram className="text-[#3b82f6] flex-shrink-0" />
                                            <span>@{user.telegram}</span>
                                        </div>
                                    )}
                                </div>

                                <div className="flex justify-between items-center text-[0.8rem] text-[#a1a1aa]">
                                    <span>{formatDate(user.createdAt)}</span>
                                    <div className="flex gap-[10px] items-center">
                                        <button
                                            onClick={() => handleViewDetails(user.id)}
                                            className="bg-[#007bff] text-white border-none px-3 py-1.5 rounded-md cursor-pointer transition-all duration-300 hover:opacity-90"
                                        >
                                            Batafsil
                                        </button>
                                        <button
                                            onClick={() => confirmToggleWebShow(user.id)}
                                            className={`px-3 py-1.5 rounded-md text-[13px] cursor-pointer transition-all duration-300 hover:opacity-90 border-none text-white ${user.isWebShow ? "bg-[#28a745]" : "bg-[#dc3545]"}`}
                                        >
                                            {user.isWebShow ? "Faol" : "Nofaol"}
                                        </button>
                                    </div>
                                </div>
                            </div>
                        ))}
                    </div>
                )}
            </div>

            {confirmModal.show && (
                <div className="fixed top-0 left-0 w-full h-full bg-black/70 flex items-center justify-center z-[1000]">
                    <div className="bg-[#1a1a1a] text-white p-8 rounded-xl max-w-[400px] w-[90%] text-center shadow-[0_8px_30px_rgba(0,0,0,0.5)] animate-modalScale">
                        <h3 className="mb-6 text-[1.3rem] text-[#3b82f6]">Tasdiqlaysizmi?</h3>
                        <div className="flex justify-center gap-4">
                            <button onClick={handleConfirm} className="bg-[#28a745] text-white border-none px-[1.2rem] py-[0.6rem] rounded-lg cursor-pointer font-medium transition-colors duration-300 hover:bg-[#218838]">Ha</button>
                            <button onClick={handleCancel} className="bg-[#dc3545] text-white border-none px-[1.2rem] py-[0.6rem] rounded-lg cursor-pointer font-medium transition-colors duration-300 hover:bg-[#c82333]">Yo'q</button>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
};

export default CastingUser;