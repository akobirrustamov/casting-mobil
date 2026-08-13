import React, { useState } from 'react';


import { useNavigate } from "react-router-dom";
import Header from "../pages/header/Header";
import Footer from "../pages/footer/Footer";
import ApiCall from "../config/index"
const LoginStudent = () => {
    const [adminData, setAdminData] = useState({ phone: '', password: '', rememberMe: false });

    const handleAdminChange = (e) => {
        const { name, value } = e.target;
        setAdminData({ ...adminData, [name]: value });
    };
    const navigate = useNavigate();

    const handleAdminSubmit = async (e) => {
        e.preventDefault();
        try {
            const response = await ApiCall('/api/v1/auth/login', 'POST', adminData, null, false); // false indicates not using multipart/form-data
            localStorage.removeItem("access_token");
            localStorage.removeItem("refresh_token");
            if (response.data.refresh_token) {
                localStorage.setItem("access_token", response.data.access_token);
                localStorage.setItem("refresh_token", response.data.refresh_token);
            } else {
                localStorage.setItem("access_token", response.data.access_token);
            }
            const roles = response.data.roles || [];
            if (roles[0].name === "ROLE_SUPER_ADMIN" && response.error === false) {
                navigate("/dashboard");
            }
            if (roles[0].name === "ROLE_ADMIN" && response.error === false) {
                navigate("/admin/home")
            }

            if (roles[0].name === "ROLE_AGENT" && response.error === false) {
                navigate("/agent/home")
            }


        } catch (error) {
            if (error.response && (error.response.status === 401 || error.response.status === 403)) {
                console.error("Login or password is wrong");
            }
        }
    };
    return (
        <div className="min-h-screen bg-black selection:bg-primary/10 selection:text-primary dark:bg-gray-900">
            <section className="pt-24 mt-0 sm:pt-36 md:pt-40 lg:pt-28">
                <div className="mx-auto px-4 sm:px-12 xl:max-w-6xl xl:px-0 mb-4" >
                    <div>
                        <div aria-hidden="true"
                            className="absolute inset-0 top-60 grid grid-cols-2 -space-x-52 opacity-50 dark:opacity-30">
                            <div
                                className="h-60 bg-gradient-to-br from-primary to-purple-400 blur-[106px] dark:from-blue-700"></div>
                            <div
                                className="h-40 bg-gradient-to-r from-cyan-600 to-sky-500 blur-[106px] dark:to-indigo-600"></div>
                        </div>
                        <div className="items-center gap-12 lg:flex justify-center">

                            <div
                                className="relative lg:w-1/2 rounded-3xl border border-gray-100 bg-white p-4 shadow-2xl shadow-gray-600/10 dark:border-gray-700 dark:bg-gray-800 dark:shadow-none sm:p-8">
                                <div className="flex m-auto text-center">
                                    {/*<img className="mt-4 w-44" src={bmti} alt="tailus stats and login components" />*/}
                                    {/*<img className="mt-8 w-32 h-20" src={km} alt="tailus stats and login components" />*/}
                                </div>

                                <h2 className="text-center text-2xl font-bold text-gray-800 dark:text-white">Tizimga kirish.</h2>

                                <form onSubmit={handleAdminSubmit} className="mt-4 p-10 pt-0">
                                    <div>

                                        <label htmlFor="phone"
                                            className="my-2 mb-2 block text-gray-600 dark:text-gray-300">Admin login <span
                                                className="text-xl text-red-500 dark:text-red-400">*</span></label>
                                        <input type="text" name="phone" id="phone" autoComplete="name"
                                            value={adminData.phone} onChange={handleAdminChange}
                                            className="peer block w-full rounded-lg border border-gray-200 bg-transparent px-4 py-2 text-gray-600 transition-shadow duration-300 invalid:ring-2 invalid:ring-red-400 focus:ring-2 dark:border-gray-700" />
                                        <span
                                            className="mt-1 hidden text-sm text-red-500 peer-invalid:block"></span>
                                    </div>

                                    <div className="mt-4">
                                        <label htmlFor="password"
                                            className="mb-2 block text-gray-600 dark:text-gray-300">Parol <span
                                                className="text-xl text-red-500 dark:text-red-400">*</span></label>
                                        <input type="password" name="password" id="password" autoComplete="tel"
                                            value={adminData.password} onChange={handleAdminChange}
                                            className="peer block w-full rounded-lg border border-gray-200 bg-transparent px-4 py-2 text-gray-600 transition-shadow duration-300 invalid:ring-2 invalid:ring-red-400 focus:ring-2 dark:border-gray-700" />
                                        <span
                                            className="mt-1 hidden text-sm text-red-500 peer-invalid:block"></span>
                                    </div>

                                    <button type="submit"
                                        className="relative mt-6 flex h-12 w-full items-center justify-center px-6 bg-blue-800 rounded-3xl hover:bg-black text-white text-xl font-semibold">
                                        Kirish
                                    </button>
                                </form>

                            </div>
                        </div>
                    </div>
                </div>
            </section>
        </div>
    );
};

export default LoginStudent;
