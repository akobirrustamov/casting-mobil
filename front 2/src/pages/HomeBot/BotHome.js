import React, { useEffect, useState } from 'react';
import Header from "../BotHeader/BotHeader";
import "react-responsive-modal/styles.css";
import ApiCall, { baseUrl } from '../../config/index';
import './BotHome.css';
import { useParams, useNavigate } from "react-router-dom";
import { FaArrowDown } from "react-icons/fa";
import { Modal } from 'react-responsive-modal';
import 'react-responsive-modal/styles.css';

function Home(props) {
    const { userId } = useParams();
    const navigate = useNavigate();
    const [newsList, setNewsList] = useState([]);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState(null);
    const [language, setLanguage] = useState('uz');
    const [visibleItems, setVisibleItems] = useState(4);
    const [isMobile, setIsMobile] = useState(false);
    const [openModal, setOpenModal] = useState(false);
    const [selectedImage, setSelectedImage] = useState(null);

    const handleOpenModal = (imgUrl) => {
        setSelectedImage(imgUrl);
        setOpenModal(true);
    };

    const handleCloseModal = () => {
        setOpenModal(false);
        setSelectedImage(null);
    };

    useEffect(() => {
        const savedLanguage = localStorage.getItem('selectedLanguage') || 'uz';
        setLanguage(savedLanguage);
        checkMobile();
        fetchNews();

        window.addEventListener('resize', checkMobile);
        return () => window.removeEventListener('resize', checkMobile);
    }, []);

    const checkMobile = () => {
        setIsMobile(window.innerWidth < 768);
    };

    const fetchNews = async () => {
        setLoading(true);
        try {
            const response = await ApiCall('/api/v1/news', 'GET');
            if (response.error) {
                setError(response.data);
            } else {
                setNewsList(response.data);
            }
        } catch (error) {
            console.error("Error fetching news:", error);
            setError("Failed to fetch news");
        } finally {
            setLoading(false);
        }
    };

    const formatDate = (dateString) => {
        const options = { year: 'numeric', month: 'short', day: 'numeric' };
        return new Date(dateString).toLocaleDateString(undefined, options);
    };

    const loadMore = () => {
        setVisibleItems(prev => prev + 4);
    };

    const translations = {
        uz: {
            loading: "Yuklanmoqda...",
            error: "Yangiliklar yuklanmadi",
            gallery: "Galereya",
            loadMore: "Ko'proq ko'rish"
        },
        ru: {
            loading: "Загрузка...",
            error: "Новости не загружены",
            gallery: "Галерея",
            loadMore: "Показать больше"
        }
    };

    return (
        <div className="home-container">
            <Header props={""} />
            <main className="news-main">
                {loading ? (
                    <div className="loading-container">
                        <div className="loading-spinner"></div>
                        <p>{translations[language].loading}</p>
                    </div>
                ) : error ? (
                    <div className="error-message">
                        {translations[language].error}: {error}
                    </div>
                ) : (
                    <>
                        <div className="news-grid">
                            {newsList.slice(0, visibleItems).map((news) => (
                                <article key={news.id} className="news-card">
                                    {news.mainPhoto && (
                                        <div className="news-main-image">
                                            <img
                                                src={`${baseUrl}/api/v1/file/getFile/${news.mainPhoto.id}`}
                                                alt={language === 'uz' ? news.titleUz : news.titleRu}
                                                className="news-image"
                                                loading="lazy"
                                            />
                                        </div>
                                    )}

                                    <div className="news-content">
                                        <div className="news-meta">
                                            {/*<span className="news-date">{formatDate(news.createdAt)}</span>*/}
                                        </div>

                                        <h2
                                            className="news-title">
                                            {language === 'uz' ? news.titleUz : news.titleRu}
                                        </h2>

                                        <div className="news-description">
                                            <p style={{ whiteSpace: "pre-line" }}>{language === 'uz' ? news.descriptionUz : news.descriptionRu}</p>
                                        </div>

                                        {news.link && (
                                            <div className="news-video">
                                                <div className="video-container">
                                                    <div
                                                        dangerouslySetInnerHTML={{
                                                            __html: news.link
                                                        }}
                                                    />
                                                </div>
                                            </div>
                                        )}

                                        {news.photos && news.photos.length > 0 && (
                                            <div className="news-gallery">
                                                <h4 className="gallery-title">{translations[language].gallery}</h4>
                                                <div className="gallery-grid1 responsive">
                                                    {news.photos.map((photo) => (
                                                        <div key={photo.id} className="gallery-item">
                                                            <img
                                                                src={`${baseUrl}/api/v1/file/getFile/${photo.id}`}
                                                                alt={translations[language].gallery}
                                                                className="gallery-image"
                                                                loading="lazy"
                                                                onClick={() => handleOpenModal(`${baseUrl}/api/v1/file/getFile/${photo.id}`)}
                                                                style={{ cursor: "pointer" }}
                                                            />

                                                        </div>
                                                    ))}
                                                </div>
                                            </div>
                                        )}
                                    </div>
                                </article>
                            ))}
                        </div>

                        {visibleItems < newsList.length && (
                            <div className="load-more-container">
                                <button onClick={loadMore} className="load-more-btn">
                                    {translations[language].loadMore}
                                </button>
                            </div>
                        )}
                    </>
                )}
            </main>

            {/* Fixed Navigate Button */}
            <button
                onClick={() => navigate(`/data-form/${userId}`)}
                className="fixed-navigate-btn"
                aria-label="Pastga o'tish"
            >
                <FaArrowDown className="animate-bounce text-white text-3xl" /> {/* Kattaroq icon */}
            </button>

            <Modal open={openModal} onClose={handleCloseModal} center>
                {selectedImage && (
                    <img
                        src={selectedImage}
                        alt="Selected"
                        style={{ width: "100%", height: "auto", maxHeight: "90vh", objectFit: "contain" }}
                    />
                )}
            </Modal>


        </div>
    );
}

export default Home;