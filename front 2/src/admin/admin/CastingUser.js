import React, { useState, useEffect } from 'react';
import ApiCall from "../../config/index";
import { useNavigate } from "react-router-dom";
import Header from "./HeaderAdmin";
import { FaUser, FaPhone, FaEnvelope, FaTelegram } from 'react-icons/fa';
import './CastingUser.css';

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
            0: "status-pending",
            1: "status-accepted",
            2: "status-rejected"
        };
        return statusClasses[status] || "";
    };

    return (
        <div className="casting-dark">
            <Header props='admin/casting-users' />

            <div className="casting-content">
                <h1 className="casting-title mb-4">Foydalanuvchilar</h1>

                {error && <div className="error-message">{error}</div>}

                {loading && !castingUsers.length ? (
                    <div className="loading-container">
                        <div className="loading-spinner"></div>
                        <span className="loading-text">Yuklanmoqda...</span>
                    </div>
                ) : (
                    <div className="users-grid">
                        {filteredUsers.map((user) => (
                            <div key={user.id} className="user-card">
                                <div className="user-header">
                                    <div>
                                        <h3 className="user-name">
                                            <FaUser className="info-icon" />
                                            {user.name}
                                        </h3>
                                        <p className="user-meta">
                                            {user.castingType} • {user.gender}
                                        </p>
                                    </div>
                                    {/*<span className={`status-badge ${getStatusClass(user.status)}`}>*/}
                                    {/*    {getStatusText(user.status)}*/}
                                    {/*</span>*/}
                                </div>

                                <div className="user-info">
                                    <div className="info-item">
                                        <FaPhone className="info-icon" />
                                        <span>{user.phone}</span>
                                    </div>
                                    <div className="info-item">
                                        <FaEnvelope className="info-icon" />
                                        <span>{user.email}</span>
                                    </div>
                                    {user.telegram && (
                                        <div className="info-item">
                                            <FaTelegram className="info-icon" />
                                            <span>@{user.telegram}</span>
                                        </div>
                                    )}
                                </div>

                                <div className="user-footer">
                                    <span>{formatDate(user.createdAt)}</span>
                                    <div className="footer-buttons">
                                        <button
                                            onClick={() => handleViewDetails(user.id)}
                                            className="btn-details"
                                        >
                                            Batafsil
                                        </button>
                                        <button
                                            onClick={() => confirmToggleWebShow(user.id)}
                                            className={`toggle-btn ${user.isWebShow ? "active-btn" : "inactive-btn"}`}
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
                <div className="modal-overlay">
                    <div className="modal-content">
                        <h3>Tasdiqlaysizmi?</h3>
                        <div className="modal-actions">
                            <button onClick={handleConfirm} className="btn-confirm">Ha</button>
                            <button onClick={handleCancel} className="btn-cancel">Yo'q</button>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
};

export default CastingUser;