import React, { useState, useEffect } from 'react';
import ApiCall from "../../config";
import { useNavigate } from "react-router-dom";
import Header from "./HeaderAdmin";
import { FaUser, FaPhone, FaEnvelope, FaTelegram, FaCheckCircle, FaTimesCircle, FaClock, FaMoneyBillWave } from 'react-icons/fa';
import './CastingUserAccepted.css';

const CastingUser = () => {
    const [castingUsers, setCastingUsers] = useState([]);
    const [filteredUsers, setFilteredUsers] = useState([]);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState(null);
    const [statusFilter, setStatusFilter] = useState("all");
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
            console.log(response.data);

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
            castingUsers.filter(user => String(user.status) === "1")
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
        if (status === 0) return <FaClock className="status-icon" />;
        if (status === 1) return <FaCheckCircle className="status-icon" />;
        return <FaTimesCircle className="status-icon" />;
    };

    const getStatusClass = (status) => {
        const statusClasses = {
            0: "status-pending",
            1: "status-accepted",
            2: "status-rejected"
        };
        return statusClasses[status] || "";
    };

    const handleViewDetails = (castingUserId) => {
        navigate(`/admin/casting-users/${castingUserId}`);
    };

    return (
        <div className="casting-dark">
            <Header props='admin/casting-users' />

            <div className="casting-content">
                <div className="page-header">
                    <h1 className="casting-title">Qabul qilinganlar</h1>
                    <div className="stats-container">
                        <div className="stat-card">
                            <span className="stat-number">{filteredUsers.length}</span>
                            <span className="stat-label">Jami qabul qilinganlar</span>
                        </div>
                    </div>
                </div>

                {error && (
                    <div className="error-message">
                        {error}
                    </div>
                )}

                {loading && !castingUsers.length ? (
                    <div className="loading-container">
                        <div className="loading-spinner"></div>
                        <span className="loading-text">Yuklanmoqda...</span>
                    </div>
                ) : (
                    <div className="users-grid">
                        {filteredUsers.map((user) => (
                            <div
                                key={user.id}
                                onClick={() => handleViewDetails(user.id)}
                                className="user-card"
                            >
                                <div className="card-glow"></div>
                                <div className="user-header">
                                    <div className="user-identity">
                                        <div className="user-id">#{user.id}</div>
                                        <h3 className="user-name">
                                            <FaUser className="info-icon" />
                                            {user.name}
                                        </h3>
                                        <p className="user-meta">
                                            {user.castingType} • {user.gender}
                                        </p>
                                    </div>
                                    <span className={`status-badge ${getStatusClass(user.status)}`}>
                                        {getStatusIcon(user.status)}
                                        {getStatusText(user.status)}
                                    </span>
                                </div>

                                <div className="user-info">
                                    <div className="info-item">
                                        <FaPhone className="info-icon" />
                                        <span>{user.phone}</span>
                                    </div>
                                    {user.email && (
                                        <div className="info-item">
                                            <FaEnvelope className="info-icon" />
                                            <span>{user.email}</span>
                                        </div>
                                    )}
                                    {user.telegram && (
                                        <div className="info-item">
                                            <FaTelegram className="info-icon" />
                                            <span>@{user.telegram}</span>
                                        </div>
                                    )}
                                </div>

                                <div className="user-footer">
                                    <span className="created-date">{formatDate(user.createdAt)}</span>
                                    <div className={`payment-status ${user.secondChan === 0 ? 'not-paid' : 'paid'}`}>
                                        <FaMoneyBillWave className="payment-icon" />
                                        {user.secondChan === 0 ? "To'lov qilmagan" : "To'lov qilgan"}
                                    </div>
                                    <span className="details-link">Batafsil &rarr;</span>
                                </div>
                            </div>
                        ))}
                    </div>
                )}

                {!loading && filteredUsers.length === 0 && (
                    <div className="empty-state">
                        <div className="empty-icon">👥</div>
                        <h3>Hozircha qabul qilinganlar mavjud emas</h3>
                        <p>Qabul qilinganlar ro'yxati shu yerda ko'rinadi</p>
                    </div>
                )}
            </div>
        </div>
    );
};

export default CastingUser;