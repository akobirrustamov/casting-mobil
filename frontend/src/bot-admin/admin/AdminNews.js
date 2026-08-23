import React, { useState, useEffect } from 'react';
import { useNavigate } from "react-router-dom";
import { AdminPage, AdminPageHeader, AdminError, AdminLoading } from "./AdminLayout";
import { FaPlus, FaTrash, FaTimes, FaSpinner, FaEdit } from 'react-icons/fa';

const baseUrl = "http://localhost:8080"; // поменяй на свой бэкенд адрес

const AdminNews = () => {
    const [newsList, setNewsList] = useState([]);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState(null);
    const [modalVisible, setModalVisible] = useState(false);
    const [deleteModalVisible, setDeleteModalVisible] = useState(false);
    const [currentNews, setCurrentNews] = useState(null);
    const [mode, setMode] = useState('create'); // 'create' or 'edit'
    const navigate = useNavigate();
    const accessToken = localStorage.getItem("access_token");



    const [formData, setFormData] = useState({
        titleUz: '',
        titleRu: '',
        descriptionUz: '',
        descriptionRu: '',
        link: '',
        mainImage: null,
        additionalImages: [],
        mainPhotoPreview: null,
        additionalImagesPreviews: [],
        existingMainPhoto: null,
        existingAdditionalPhotos: []
    });

    // ============== ФУНКЦИИ API ==============
    const apiRequest = async (url, method = "GET", body = null, isJson = true) => {
        try {
            const headers = {};
            if (accessToken) headers["Authorization"] = `Bearer ${accessToken}`;
            if (isJson && body) headers["Content-Type"] = "application/json";

            const response = await fetch(`${baseUrl}${url}`, {
                method,
                headers,
                body: body ? (isJson ? JSON.stringify(body) : body) : null,
            });

            if (!response.ok) {
                throw new Error(`Ошибка ${response.status}`);
            }
            return await response.json();
        } catch (err) {
            console.error("API Error:", err);
            throw err;
        }
    };

    const fetchNews = async () => {
        setLoading(true);
        try {
            const data = await apiRequest("/api/v1/news", "GET");
            setNewsList(data);
        } catch (err) {
            setError("Yangiliklarni yuklab bo'lmadi");
        } finally {
            setLoading(false);
        }
    };

    const uploadImage = async (image, prefix) => {
        const formData = new FormData();
        formData.append("photo", image);
        formData.append("prefix", prefix);

        try {
            const response = await fetch(`${baseUrl}/api/v1/file/upload`, {
                method: "POST",
                headers: accessToken ? { Authorization: `Bearer ${accessToken}` } : {},
                body: formData,
            });

            if (!response.ok) throw new Error("Rasm yuklashda xatolik");
            return await response.json();
        } catch (err) {
            console.error(err);
            throw err;
        }
    };

    useEffect(() => {
        fetchNews();
    }, []);

    // ============== ОБРАБОТЧИКИ ФОРМЫ ==============
    const handleInputChange = (e) => {
        const { name, value } = e.target;
        setFormData(prev => ({
            ...prev,
            [name]: value
        }));
    };

    const handleMainImageChange = (e) => {
        const file = e.target.files[0];
        if (file) {
            setFormData(prev => ({
                ...prev,
                mainImage: file,
                mainPhotoPreview: URL.createObjectURL(file),
                existingMainPhoto: null
            }));
        }
    };

    const handleAdditionalImagesChange = (e) => {
        const files = Array.from(e.target.files);
        if (files.length > 0) {
            const previews = files.map(file => URL.createObjectURL(file));
            setFormData(prev => ({
                ...prev,
                additionalImages: [...prev.additionalImages, ...files],
                additionalImagesPreviews: [...prev.additionalImagesPreviews, ...previews]
            }));
        }
    };

    const removeAdditionalImage = (index) => {
        const newImages = [...formData.additionalImages];
        const newPreviews = [...formData.additionalImagesPreviews];
        const newExisting = [...formData.existingAdditionalPhotos];

        if (index < newExisting.length) {
            newExisting.splice(index, 1);
        } else {
            const adjustedIndex = index - newExisting.length;
            newImages.splice(adjustedIndex, 1);
            newPreviews.splice(adjustedIndex, 1);
        }

        setFormData(prev => ({
            ...prev,
            additionalImages: newImages,
            additionalImagesPreviews: newPreviews,
            existingAdditionalPhotos: newExisting
        }));
    };

    const resetForm = () => {
        setFormData({
            titleUz: '',
            titleRu: '',
            descriptionUz: '',
            descriptionRu: '',
            link: '',
            mainImage: null,
            additionalImages: [],
            mainPhotoPreview: null,
            additionalImagesPreviews: [],
            existingMainPhoto: null,
            existingAdditionalPhotos: []
        });
        setCurrentNews(null);
        setMode('create');
    };

    const openCreateModal = () => {
        resetForm();
        setMode('create');
        setModalVisible(true);
    };

    const openEditModal = (news) => {
        setCurrentNews(news);
        setMode('edit');
        setFormData({
            titleUz: news.titleUz,
            titleRu: news.titleRu,
            descriptionUz: news.descriptionUz,
            descriptionRu: news.descriptionRu,
            link: news.link || '',
            mainImage: null,
            additionalImages: [],
            mainPhotoPreview: news.mainPhoto ? `${baseUrl}/api/v1/file/getFile/${news.mainPhoto.id}` : null,
            additionalImagesPreviews: news.photos?.map(photo =>
                `${baseUrl}/api/v1/file/getFile/${photo.id}`
            ) || [],
            existingMainPhoto: news.mainPhoto,
            existingAdditionalPhotos: news.photos || []
        });
        setModalVisible(true);
    };

    const openDeleteModal = (news) => {
        setCurrentNews(news);
        setDeleteModalVisible(true);
    };

    const closeModal = () => {
        setModalVisible(false);
        setDeleteModalVisible(false);
        resetForm();
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        setLoading(true);

        try {
            let mainPhotoUuid = formData.existingMainPhoto?.id || null;
            const additionalImagesUuids = [...formData.existingAdditionalPhotos.map(p => p.id)];

            if (formData.mainImage) {
                mainPhotoUuid = await uploadImage(formData.mainImage, '/main');
            }

            for (const image of formData.additionalImages) {
                const uuid = await uploadImage(image, '/additional');
                additionalImagesUuids.push(uuid);
            }

            const newsData = {
                titleUz: formData.titleUz,
                titleRu: formData.titleRu,
                descriptionUz: formData.descriptionUz,
                descriptionRu: formData.descriptionRu,
                link: formData.link,
                mainPhoto: mainPhotoUuid,
                photos: additionalImagesUuids
            };

            if (mode === 'create') {
                await apiRequest('/api/v1/news', 'POST', newsData, true);
            } else {
                await apiRequest(`/api/v1/news/${currentNews.id}`, 'PUT', newsData, true);
            }

            fetchNews();
            closeModal();
        } catch (err) {
            setError("Yangilikni saqlashda xatolik");
        } finally {
            setLoading(false);
        }
    };

    const handleDelete = async () => {
        setLoading(true);
        try {
            await apiRequest(`/api/v1/news/${currentNews.id}`, 'DELETE');
            fetchNews();
            closeModal();
        } catch (err) {
            setError("Yangilikni o'chirib bo'lmadi");
        } finally {
            setLoading(false);
        }
    };

    // ============== РЕНДЕР ==============
    return (
        <AdminPage headerProps='admin/news'>
            <AdminPageHeader title="Yangiliklar">
                <button onClick={openCreateModal} className="bg-[#3b82f6] text-white border-none rounded-lg px-4 py-3 font-medium flex items-center gap-2">
                    <FaPlus /> Qo'shish
                </button>
            </AdminPageHeader>

            <AdminError>{error}</AdminError>

            {loading && !newsList.length ? (
                <AdminLoading />
            ) : (
                <div className="grid grid-cols-1 gap-4">
                    {newsList.map((news) => (
                        <div key={news.id} className="bg-[#1a1a1a] rounded-xl p-4 border border-[#333333]">
                            <div className="flex justify-between items-center mb-2">
                                <h3 className="font-semibold text-[1.1rem] text-white">{news.titleUz}</h3>
                                <span className="text-[0.8rem] text-[#a1a1aa]">
                                    {new Date(news.createdAt).toLocaleDateString()}
                                </span>
                            </div>
                            <div className="mt-2 text-[#d4d4d8] text-[0.9rem]">
                                <p>{news.descriptionUz.substring(0, 100)}...</p>
                            </div>
                            <div className="flex justify-end gap-2 mt-4">
                                <button onClick={() => openEditModal(news)}>
                                    <FaEdit /> Tahrirlash
                                </button>
                                <button onClick={() => openDeleteModal(news)} className="bg-[#450a0a] text-[#fca5a5] border-none rounded-md px-3 py-2 text-[0.9rem] flex items-center gap-[0.3rem]">
                                    <FaTrash /> O'chirish
                                </button>
                            </div>
                        </div>
                    ))}
                </div>
            )}

            {/* Модал создания/редактирования */}
            {modalVisible && (
                <div className="fixed inset-0 bg-black/80 flex justify-center items-center z-[1000] p-4">
                    <div className="bg-[#1a1a1a] rounded-xl w-full max-w-[500px] max-h-[90vh] overflow-y-auto p-6 border border-[#333333]">
                        <div className="flex justify-between items-center mb-6">
                            <h2 className="text-xl font-semibold text-white">
                                {mode === 'create' ? 'Yangi Yangilik' : 'Yangilikni Tahrirlash'}
                            </h2>
                            <button onClick={closeModal} className="bg-none border-none text-[#a1a1aa] text-2xl cursor-pointer">
                                <FaTimes />
                            </button>
                        </div>
                        <form onSubmit={handleSubmit}>
                            <div className="mb-4">
                                <label className="block mb-2 text-[#e4e4e7] text-[0.9rem]">Sarlavha (O'zbekcha)</label>
                                <input
                                    type="text"
                                    name="titleUz"
                                    value={formData.titleUz}
                                    onChange={handleInputChange}
                                    className="w-full p-3 bg-[#262626] border border-[#404040] rounded-lg text-white text-base"
                                    required
                                />
                            </div>

                            <div className="mb-4">
                                <label className="block mb-2 text-[#e4e4e7] text-[0.9rem]">Sarlavha (Ruscha)</label>
                                <input
                                    type="text"
                                    name="titleRu"
                                    value={formData.titleRu}
                                    onChange={handleInputChange}
                                    className="w-full p-3 bg-[#262626] border border-[#404040] rounded-lg text-white text-base"
                                    required
                                />
                            </div>

                            <div className="mb-4">
                                <label className="block mb-2 text-[#e4e4e7] text-[0.9rem]">Tavsif (O'zbekcha)</label>
                                <textarea
                                    name="descriptionUz"
                                    value={formData.descriptionUz}
                                    onChange={handleInputChange}
                                    className="w-full p-3 bg-[#262626] border border-[#404040] rounded-lg text-white text-base min-h-[120px] resize-y"
                                    rows={5}
                                    required
                                ></textarea>
                            </div>

                            <div className="mb-4">
                                <label className="block mb-2 text-[#e4e4e7] text-[0.9rem]">Tavsif (Ruscha)</label>
                                <textarea
                                    name="descriptionRu"
                                    value={formData.descriptionRu}
                                    onChange={handleInputChange}
                                    className="w-full p-3 bg-[#262626] border border-[#404040] rounded-lg text-white text-base min-h-[120px] resize-y"
                                    rows={5}
                                    required
                                ></textarea>
                            </div>

                            <div className="mb-4">
                                <label className="block mb-2 text-[#e4e4e7] text-[0.9rem]">YouTube Havolasi</label>
                                <input
                                    type="text"
                                    name="link"
                                    value={formData.link}
                                    onChange={handleInputChange}
                                    className="w-full p-3 bg-[#262626] border border-[#404040] rounded-lg text-white text-base"
                                    placeholder="https://www.youtube.com/embed/..."
                                />
                            </div>

                            <div className="mb-4">
                                <label className="block mb-2 text-[#e4e4e7] text-[0.9rem]">Asosiy Rasm</label>
                                <label className="block p-4 bg-[#262626] border border-dashed border-[#404040] rounded-lg text-center cursor-pointer mb-4">
                                    {formData.mainPhotoPreview ? "Rasmni almashtirish" : "Rasm tanlang"}
                                    <input
                                        type="file"
                                        onChange={handleMainImageChange}
                                        style={{ display: "none" }}
                                        accept="image/*"
                                    />
                                </label>
                                {formData.mainPhotoPreview && (
                                    <div className="grid grid-cols-2 gap-2 mt-2">
                                        <div className="relative rounded-lg overflow-hidden aspect-square">
                                            <img src={formData.mainPhotoPreview} alt="Asosiy rasm" className="w-full h-full object-cover" />
                                        </div>
                                    </div>
                                )}
                            </div>

                            <div className="mb-4">
                                <label className="block mb-2 text-[#e4e4e7] text-[0.9rem]">Qo'shimcha Rasmlar</label>
                                <label className="block p-4 bg-[#262626] border border-dashed border-[#404040] rounded-lg text-center cursor-pointer mb-4">
                                    Rasmlar tanlang
                                    <input
                                        type="file"
                                        onChange={handleAdditionalImagesChange}
                                        style={{ display: "none" }}
                                        accept="image/*"
                                        multiple
                                    />
                                </label>
                                <div className="grid grid-cols-2 gap-2 mt-2">
                                    {formData.additionalImagesPreviews.map((preview, index) => (
                                        <div key={index} className="relative rounded-lg overflow-hidden aspect-square">
                                            <img src={preview} alt={`Qo'shimcha ${index + 1}`} className="w-full h-full object-cover" />
                                            <button
                                                type="button"
                                                onClick={() => removeAdditionalImage(index)}
                                                className="absolute top-1 right-1 bg-[rgba(239,68,68,0.8)] text-white border-none rounded-full w-6 h-6 flex items-center justify-center cursor-pointer"
                                            >
                                                <FaTimes />
                                            </button>
                                        </div>
                                    ))}
                                </div>
                            </div>

                            <div className="flex justify-end gap-3 mt-6">
                                <button type="button" onClick={closeModal} className="bg-[#262626] text-[#e4e4e7] border-none rounded-lg px-5 py-3">
                                    Bekor qilish
                                </button>
                                <button type="submit" className="bg-[#1d4ed8] text-white border-none rounded-lg px-5 py-3 font-medium flex items-center gap-2" disabled={loading}>
                                    {loading ? <FaSpinner className="animate-spin" /> :
                                        (mode === 'create' ? "Saqlash" : "Yangilash")}
                                </button>
                            </div>
                        </form>
                    </div>
                </div>
            )}

            {/* Модал удаления */}
            {deleteModalVisible && (
                <div className="fixed inset-0 bg-black/80 flex justify-center items-center z-[1000] p-4">
                    <div className="bg-[#1a1a1a] rounded-xl p-6 w-[90%] max-w-[400px] border border-[#333333]">
                        <h2 className="text-xl font-semibold text-white">O'chirishni Tasdiqlang</h2>
                        <p className="mb-6 text-[#e4e4e7]">
                            "{currentNews?.titleUz}" yangilikni rostdan ham o'chirmoqchimisiz?
                        </p>
                        <div className="flex justify-end gap-3">
                            <button onClick={closeModal} className="bg-[#262626] text-[#e4e4e7] border-none rounded-lg px-5 py-3">
                                Bekor qilish
                            </button>
                            <button onClick={handleDelete} className="bg-[#7f1d1d] text-[#fca5a5] border-none rounded-lg px-5 py-3" disabled={loading}>
                                {loading ? <FaSpinner className="animate-spin" /> : "O'chirish"}
                            </button>
                        </div>
                    </div>
                </div>
            )}
        </AdminPage>
    );
};

export default AdminNews;
