import React, { useEffect, useState } from 'react';
import Header from "../BotHeader/BotHeader";
import "react-responsive-modal/styles.css";
import ApiCall, { baseUrl } from '../../config/index';
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
            0: "bg-[rgba(248,150,30,0.2)] text-[#f8961e]",
            1: "bg-[rgba(76,201,240,0.2)] text-[#4cc9f0]",
            2: "bg-[rgba(247,37,133,0.2)] text-[#f72585]"
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
        <div className="min-h-screen bg-[#1b1b1e] text-[#f8f9fa] [font-family:Inter,-apple-system,BlinkMacSystemFont,sans-serif]">
            <Header props={"history"} />

            <main className="max-w-[1200px] mx-auto p-8 max-md:p-6 max-[480px]:p-4">
                <div className="mb-10 text-center animate-fadeInOpacity">
                    <h1 className="text-[2.2rem] mb-2 bg-[linear-gradient(90deg,#4361ee,#4895ef)] bg-clip-text text-transparent font-bold max-[480px]:text-[1.8rem]">{translations[language].myApplications}</h1>
                </div>

                {loading ? (
                    <div className="flex flex-col items-center justify-center h-[200px] animate-fadeInOpacity">
                        <div className="w-[50px] h-[50px] border-4 border-[rgba(67,97,238,0.2)] border-t-[#4361ee] rounded-full animate-spin"></div>
                        <p>{translations[language].loading}</p>
                    </div>
                ) : error ? (
                    <div className="bg-[rgba(247,37,133,0.1)] text-[#f72585] p-6 rounded-lg text-center max-w-[600px] mx-auto border-l-4 border-[#f72585] animate-fadeInOpacity">
                        {translations[language].error}: {error}
                    </div>
                ) : casting.length === 0 ? (
                    <div className="text-center p-8 text-[#e9ecef] text-[1.1rem] bg-[#25252d] rounded-lg max-w-[600px] mx-auto animate-fadeInOpacity">
                        {translations[language].noApplications}
                    </div>
                ) : (
                    <div className="grid grid-cols-[repeat(auto-fill,minmax(350px,1fr))] gap-6 max-md:grid-cols-1">
                        {casting.map((application) => (
                            <div
                                key={application.id}
                                className="group bg-[#25252d] rounded-xl overflow-hidden shadow-[0_4px_20px_rgba(0,0,0,0.3)] transition-all duration-300 ease-[cubic-bezier(0.25,0.8,0.25,1)] cursor-pointer animate-cardEnter opacity-0 hover:-translate-y-[5px] hover:scale-[1.02] hover:shadow-[0_10px_30px_rgba(0,0,0,0.4)]"
                                onClick={() => handleCardClick(application.id)}
                            >
                                {application.photos && application.photos.length > 0 && (
                                    <div className="h-[200px] overflow-hidden relative after:content-[''] after:absolute after:bottom-0 after:left-0 after:w-full after:h-[40%] after:bg-[linear-gradient(transparent,rgba(0,0,0,0.7))]">
                                        <img
                                            src={`${baseUrl}/api/v1/file/getFile/${application.photos[0].id}`}
                                            alt={application.name}
                                            className="w-full h-full object-cover transition-transform duration-500 group-hover:scale-105"
                                        />
                                    </div>
                                )}
                                <div className="p-6 relative max-[480px]:p-5">
                                    <h3 className="mt-0 mb-4 mx-0 text-[#f8f9fa] text-[1.3rem] font-semibold max-[480px]:text-[1.1rem]">{application.name}</h3>

                                    <div className="grid gap-4 mb-6">
                                        <div className="flex justify-between items-center">
                                            <span className="font-medium text-[#e9ecef] text-[0.9rem]">{translations[language].castingType}</span>
                                            <span className="font-medium text-[#f8f9fa]">{application.castingType}</span>
                                        </div>

                                        <div className="flex justify-between items-center">
                                            <span className="font-medium text-[#e9ecef] text-[0.9rem]">{translations[language].status}</span>
                                            <span className={`px-3 py-[0.35rem] rounded-[20px] text-[0.8rem] font-semibold ${getStatusClass(application.status)}`}>
                                                {getStatusText(application.status)}
                                            </span>
                                        </div>

                                        <div className="flex justify-between items-center">
                                            <span className="font-medium text-[#e9ecef] text-[0.9rem]">{translations[language].date}</span>
                                            <span className="font-medium text-[#f8f9fa]">{formatDate(application.createdAt)}</span>
                                        </div>
                                    </div>

                                    <button
                                        className="w-full p-3 bg-[linear-gradient(135deg,#4361ee,#4895ef)] text-white border-none rounded-lg text-base font-medium cursor-pointer transition-all duration-300 ease-[cubic-bezier(0.25,0.8,0.25,1)] shadow-[0_4px_12px_rgba(67,97,238,0.3)] hover:bg-[linear-gradient(135deg,#4895ef,#4361ee)] hover:-translate-y-0.5 hover:shadow-[0_6px_16px_rgba(67,97,238,0.4)]"
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