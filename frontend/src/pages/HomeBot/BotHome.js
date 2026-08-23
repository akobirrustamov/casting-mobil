import React, { useEffect, useState } from 'react';
import Header from "../BotHeader/BotHeader";
import "react-responsive-modal/styles.css";
import ApiCall, { baseUrl } from '../../config/index';
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
        <div className="min-h-screen flex flex-col bg-[#1b1b1e] text-[#f8f9fa] [font-family:Segoe_UI,Tahoma,Geneva,Verdana,sans-serif] pt-[50px]">
            <Header props={""} />
            <main className="flex-1 p-4 min-[480px]:p-8 max-w-[1400px] mx-auto w-full">
                {loading ? (
                    <div className="flex flex-col justify-center items-center h-[50vh] gap-4">
                        <div className="w-[50px] h-[50px] border-4 border-[rgba(72,149,239,0.2)] border-t-[#4895ef] rounded-full animate-spin"></div>
                        <p>{translations[language].loading}</p>
                    </div>
                ) : error ? (
                    <div className="bg-[rgba(255,50,50,0.15)] p-6 rounded-lg border-l-4 border-[#ff4d4d] my-8 mx-0 text-center">
                        {translations[language].error}: {error}
                    </div>
                ) : (
                    <>
                        <div className="grid grid-cols-1 gap-8 p-2 md:grid-cols-2 md:gap-10 lg:gap-12 min-[1400px]:grid-cols-3">
                            {newsList.slice(0, visibleItems).map((news) => (
                                <article key={news.id} className="group bg-white/5 rounded-xl overflow-hidden transition-all duration-300 shadow-[0_4px_6px_rgba(0,0,0,0.1)] border border-white/5 animate-fadeInUp opacity-0 hover:-translate-y-[5px] hover:shadow-[0_12px_24px_rgba(0,0,0,0.2)] hover:bg-white/[0.08] [&:nth-child(1)]:[animation-delay:0.1s] [&:nth-child(2)]:[animation-delay:0.2s] [&:nth-child(3)]:[animation-delay:0.3s] [&:nth-child(4)]:[animation-delay:0.4s]">
                                    {news.mainPhoto && (
                                        <div className="w-full overflow-hidden relative">
                                            <img
                                                src={`${baseUrl}/api/v1/file/getFile/${news.mainPhoto.id}`}
                                                alt={language === 'uz' ? news.titleUz : news.titleRu}
                                                className="w-full h-full object-cover transition-transform duration-500 bg-[linear-gradient(45deg,#1b1b1e,#2a2a2e)] group-hover:scale-[1.03]"
                                                loading="lazy"
                                            />
                                        </div>
                                    )}

                                    <div className="md:p-8">
                                        <div className="mb-[0.8rem] text-[#4895ef] text-[0.85rem] font-medium flex items-center gap-2">
                                            {/*<span>{formatDate(news.createdAt)}</span>*/}
                                        </div>

                                        <h2
                                            className="text-2xl mb-4 text-[#f8f9fa] leading-[1.3] font-semibold text-center lg:text-[1.7rem]">
                                            {language === 'uz' ? news.titleUz : news.titleRu}
                                        </h2>

                                        <div className="mb-6 leading-[1.6] text-white/80 [&_p]:mb-4">
                                            <p style={{ whiteSpace: "pre-line" }}>{language === 'uz' ? news.descriptionUz : news.descriptionRu}</p>
                                        </div>

                                        {news.link && (
                                            <div className="my-6 mx-0 rounded-lg overflow-hidden">
                                                <div className="relative pb-[56.25%] h-0 overflow-hidden bg-black rounded-lg [&_iframe]:absolute [&_iframe]:top-0 [&_iframe]:left-0 [&_iframe]:w-full [&_iframe]:h-full [&_iframe]:border-none">
                                                    <div
                                                        dangerouslySetInnerHTML={{
                                                            __html: news.link
                                                        }}
                                                    />
                                                </div>
                                            </div>
                                        )}

                                        {news.photos && news.photos.length > 0 && (
                                            <div className="mt-6">
                                                <h4 className="text-[1.1rem] mb-4 text-[#4895ef] font-medium">{translations[language].gallery}</h4>
                                                <div className="grid items-center grid-cols-[repeat(2,minmax(150px,1fr))] gap-[0.8rem] min-[480px]:grid-cols-[repeat(2,minmax(180px,1fr))] md:grid-cols-[repeat(1,minmax(150px,1fr))] md:gap-4">
                                                    {news.photos.map((photo) => (
                                                        <div key={photo.id} className="group/gi h-[150px] overflow-hidden rounded-md relative cursor-pointer">
                                                            <img
                                                                src={`${baseUrl}/api/v1/file/getFile/${photo.id}`}
                                                                alt={translations[language].gallery}
                                                                className="w-full h-full object-cover transition-transform duration-300 bg-[linear-gradient(45deg,#1b1b1e,#2a2a2e)] flex items-center justify-center text-white before:content-[attr(alt)] before:absolute before:p-2 before:text-center group-hover/gi:scale-[1.08]"
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
                            <div className="flex justify-center my-10 mx-0">
                                <button onClick={loadMore} className="bg-[#4895ef] text-white border-none px-[1.8rem] py-[0.8rem] rounded-[50px] text-base font-medium cursor-pointer transition-all duration-300 shadow-[0_4px_12px_rgba(72,149,239,0.3)] hover:bg-[#4cc9f0] hover:-translate-y-0.5 hover:shadow-[0_6px_16px_rgba(72,149,239,0.4)]">
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
                className="fixed bottom-5 right-5 bg-[#2563eb] border-none px-[18px] py-[15px] rounded-full shadow-[0_4px_15px_rgba(37,99,235,0.5)] cursor-pointer z-[1000] transition-all duration-300 hover:scale-[1.15] hover:shadow-[0_6px_20px_rgba(37,99,235,0.7)] hover:bg-[#1d4ed8] after:content-[''] after:absolute after:-top-[10px] after:-left-[10px] after:-right-[10px] after:-bottom-[10px] after:rounded-full after:animate-pulseRing2 after:pointer-events-none"
                aria-label="Pastga o'tish"
            >
                <FaArrowDown className="animate-bounceY [animation-duration:1.5s] text-white text-3xl" /> {/* Kattaroq icon */}
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