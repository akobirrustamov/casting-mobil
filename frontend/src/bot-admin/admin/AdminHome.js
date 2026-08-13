import React, { useState, useEffect } from 'react';
import ApiCall from "../../config";
import Header from "./HeaderAdmin";
import { useNavigate } from 'react-router-dom';

const AdminHome = () => {
    const navigate = useNavigate()
    const [stats, setStats] = useState(null);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState(null);
    const accessToken = localStorage.getItem("access_token");



    useEffect(() => {

    }, []);

    useEffect(() => {
        fetchStatistics();
    }, []);

    const fetchStatistics = async () => {
        setLoading(true);
        try {
            const response = await ApiCall('/api/v1/admin/statistic', 'GET');
            if (response.error) {
                setError(response.data);
            } else {
                setStats(response.data);
            }
        } catch (error) {
            console.error("Statistikani olishda xatolik:", error);
            setError("Statistikani olishda xatolik yuz berdi.");
        } finally {
            setLoading(false);
        }
    };

    const calculateDonutSegments = () => {
        if (!stats) return [];

        const total = stats.totalCount || 1;
        const accepted = (stats.acceptedCount / total) * 360;
        const pending = (stats.pendingCount / total) * 360;
        const rejected = (stats.rejectedCount / total) * 360;

        return [
            { degrees: accepted, color: '#4895ef' },
            { degrees: pending, color: '#4cc9f0' },
            { degrees: rejected, color: '#ef4444' }
        ];
    };

    const donutSegments = calculateDonutSegments();

    return (
        <div className="min-h-screen bg-black text-white">
            <Header props='admin' />
            <div className="container mx-auto px-4 py-8">
                <h1 className="text-3xl font-bold text-center mb-8 text-blue-400">Admin Statistikasi</h1>

                {loading ? (
                    <div className="flex justify-center items-center h-64">
                        <div className="animate-spin rounded-full h-12 w-12 border-t-2 border-b-2 border-blue-500"></div>
                    </div>
                ) : error ? (
                    <div className="bg-red-900 text-red-200 p-4 rounded-lg text-center max-w-md mx-auto">
                        {error}
                    </div>
                ) : stats && (
                    <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6 justify-center max-w-4xl mx-auto">
                        {/* Umumiy topshirganlar - Donut chart */}
                        <div className="bg-gray-900 rounded-xl p-6 shadow-lg">
                            <h2 className="text-xl font-semibold mb-4 text-center text-blue-300">Umumiy topshirganlar</h2>
                            <div className="relative w-48 h-48 mx-auto mb-4">
                                <div className="absolute inset-0 rounded-full bg-gray-800"></div>
                                {donutSegments.map((segment, index) => (
                                    segment.degrees > 0 && (
                                        <div
                                            key={index}
                                            className="absolute inset-0 rounded-full"
                                            style={{
                                                clipPath: `conic-gradient(from ${index === 0 ? 0 : donutSegments.slice(0, index).reduce((a, b) => a + b.degrees, 0)}deg, ${segment.color} 0deg, ${segment.color} ${segment.degrees}deg, transparent ${segment.degrees}deg)`
                                            }}
                                        ></div>
                                    )
                                ))}
                                <div className="absolute inset-6 rounded-full bg-gray-900 flex items-center justify-center">
                                    <span className="text-2xl font-bold">{stats.totalCount}</span>
                                </div>
                            </div>
                            <div className="space-y-2 mt-4">
                                <div className="flex items-center">
                                    <div className="w-4 h-4 rounded-full bg-[#4895ef] mr-2"></div>
                                    <span>Qabul qilingan</span>
                                </div>
                                <div className="flex items-center">
                                    <div className="w-4 h-4 rounded-full bg-[#4cc9f0] mr-2"></div>
                                    <span>Ko'rib chiqilmoqda</span>
                                </div>
                                <div className="flex items-center">
                                    <div className="w-4 h-4 rounded-full bg-[#ef4444] mr-2"></div>
                                    <span>Rad etilgan</span>
                                </div>
                            </div>
                        </div>

                        {/* Holatlar bo'yicha statistikalar */}
                        <div className="bg-gray-900 rounded-xl p-6 shadow-lg">
                            <h2 className="text-xl font-semibold mb-4 text-center text-blue-300">Holatlar bo'yicha statistikalar</h2>

                            <div className="mb-6">
                                <div className="flex justify-between mb-1">
                                    <h3 className="text-sm">Qabul qilinganlar</h3>
                                    <span className="text-sm font-medium">{stats.acceptedCount}</span>
                                </div>
                                <div className="w-full bg-gray-700 rounded-full h-2.5">
                                    <div
                                        className="bg-[#4895ef] h-2.5 rounded-full"
                                        style={{ width: `${(stats.acceptedCount / stats.totalCount) * 100}%` }}
                                    ></div>
                                </div>
                            </div>

                            <div className="mb-6">
                                <div className="flex justify-between mb-1">
                                    <h3 className="text-sm">Ko'rib chiqilayotganlar</h3>
                                    <span className="text-sm font-medium">{stats.pendingCount}</span>
                                </div>
                                <div className="w-full bg-gray-700 rounded-full h-2.5">
                                    <div
                                        className="bg-[#4cc9f0] h-2.5 rounded-full"
                                        style={{ width: `${(stats.pendingCount / stats.totalCount) * 100}%` }}
                                    ></div>
                                </div>
                            </div>

                            <div className="mb-6">
                                <div className="flex justify-between mb-1">
                                    <h3 className="text-sm">Rad etilganlar</h3>
                                    <span className="text-sm font-medium">{stats.rejectedCount}</span>
                                </div>
                                <div className="w-full bg-gray-700 rounded-full h-2.5">
                                    <div
                                        className="bg-[#ef4444] h-2.5 rounded-full"
                                        style={{ width: `${(stats.rejectedCount / stats.totalCount) * 100}%` }}
                                    ></div>
                                </div>
                            </div>
                        </div>

                        {/* 1 kunlik topshirganlar */}
                        <div className="bg-gray-900 rounded-xl p-6 shadow-lg flex flex-col items-center">
                            <h2 className="text-xl font-semibold mb-4 text-center text-blue-300">Kunlik topshirganlar</h2>
                            <div className="w-32 h-48 flex items-end justify-center mb-4">
                                <div
                                    className="w-16 bg-gradient-to-t from-blue-500 to-blue-300 rounded-t-lg transition-all duration-500"
                                    style={{
                                        height: `${Math.min(100, (stats.dailyCount / Math.max(1, stats.totalCount)) * 100)}%`
                                    }}
                                ></div>
                            </div>
                            <div className="text-3xl font-bold text-blue-400">{stats.dailyCount}</div>
                            <p className="text-gray-400 mt-2">bugun</p>
                        </div>

                    </div>
                )
                }
            </div >
        </div >
    );
};

export default AdminHome;