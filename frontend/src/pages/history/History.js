import React, { useEffect, useState } from 'react';
import Header from "../BotHeader/BotHeader";
import "react-responsive-modal/styles.css";
import ApiCall, { baseUrl } from '../../config/index';
import './History.css';
import { useParams, useNavigate } from "react-router-dom";

function History(props) {
    const { userId } = useParams();
    const navigate = useNavigate();

    const [casting, setCasting] = useState([]);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState(null);
    const [language, setLanguage] = useState('uz');

    useEffect(() => {
        const savedLanguage = localStorage.getItem('selectedLanguage') || 'uz';
        setLanguage(savedLanguage);
        fetchCasting();
    }, []);

    const fetchCasting = async () => {
        setLoading(true);
        try {
            const response = await ApiCall('/api/v1/casting-user/my/' + userId, 'GET');
            if (response.error) {
                setError(response.data);
            } else {
                setCasting(response.data);
            }
        } catch (error) {
            console.error("Error fetching casting:", error);
            setError("Failed to fetch casting applications");
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

    const handleCardClick = (castingId) => {
        navigate(`/appeal/${userId}`, { state: { castingId } });
    };

    const translations = {
        uz: {
            loading: "Yuklanmoqda...",
            error: "Arizalar yuklanmadi",
            noApplications: "Sizda arizalar mavjud emas",
            castingType: "Casting Turi",
            status: "Holat",
            date: "Sana",
            viewDetails: "Batafsil",
            myApplications: "Mening Arizalarim"
        },
        ru: {
            loading: "Загрузка...",
            error: "Заявки не загружены",
            noApplications: "У вас нет заявок",
            castingType: "Тип кастинга",
            status: "Статус",
            date: "Дата",
            viewDetails: "Подробнее",
            myApplications: "Мои Заявки"
        }
    };

    return (
        <div className="history-container">
            <Header props={"history"} />

            <main className="history-main">
                <div className="history-header">
                    <h1>{translations[language].myApplications}</h1>
                </div>

                {loading ? (
                    <div className="loading-container">
                        <div className="loading-spinner"></div>
                        <p>{translations[language].loading}</p>
                    </div>
                ) : error ? (
                    <div className="error-message">
                        {translations[language].error}: {error}
                    </div>
                ) : casting.length === 0 ? (
                    <div className="no-applications">
                        {translations[language].noApplications}
                    </div>
                ) : (
                    <div className="applications-grid">
                        {casting.map((application) => (
                            <div
                                key={application.id}
                                className="application-card"
                                onClick={() => handleCardClick(application.id)}
                            >
                                {application.photos && application.photos.length > 0 && (
                                    <div className="application-image">
                                        <img
                                            src={`${baseUrl}/api/v1/file/getFile/${application.photos[0].id}`}
                                            alt={application.name}
                                        />
                                    </div>
                                )}
                                <div className="application-content">
                                    <h3>{application.name}</h3>

                                    <div className="application-meta">
                                        <div className="meta-item">
                                            <span className="meta-label">{translations[language].castingType}</span>
                                            <span className="meta-value">{application.castingType}</span>
                                        </div>

                                        <div className="meta-item">
                                            <span className="meta-label">{translations[language].status}</span>
                                            <span className={`meta-value status-badge ${getStatusClass(application.status)}`}>
                                                {getStatusText(application.status)}
                                            </span>
                                        </div>

                                        <div className="meta-item">
                                            <span className="meta-label">{translations[language].date}</span>
                                            <span className="meta-value">{formatDate(application.createdAt)}</span>
                                        </div>
                                    </div>

                                    <button
                                        className="view-details-btn"
                                        onClick={(e) => {
                                            e.stopPropagation();
                                            handleCardClick(application.id);
                                        }}
                                    >
                                        {translations[language].viewDetails}
                                    </button>
                                </div>
                            </div>
                        ))}
                    </div>
                )}
            </main>
        </div>
    );
}

export default History;