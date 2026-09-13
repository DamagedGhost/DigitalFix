import { BrowserRouter as Router, Routes, Route, Link, Navigate } from "react-router-dom";
import { useMsal, useIsAuthenticated } from "@azure/msal-react";
import { InteractionStatus } from "@azure/msal-browser";
import Dashboard from "./components/Dashboard";
import Login from "./components/Login";
import Ordenes from "./components/Ordenes";
import Catalog from "./components/Catalog";
import Reports from "./components/Reports";
import Audit from "./components/Audit";

export default function App() {
    const { instance, accounts, inProgress } = useMsal();
    const isAuthenticated = useIsAuthenticated();

    const handleLogout = () => {
        if (inProgress === InteractionStatus.None) {
            instance.logoutRedirect({ postLogoutRedirectUri: "/" }).catch((e) => console.error(e));
        }
    };

    // Extraer datos del usuario y roles desde los claims del token
    const currentUser = accounts[0];
    const userRoles = currentUser?.idTokenClaims?.roles || [];

    // Pantalla de carga mientras MSAL resuelve la redirección
    if (inProgress !== InteractionStatus.None && !isAuthenticated) {
        return <div className="min-h-screen bg-gray-100 flex items-center justify-center text-[#00274C] font-bold">Verificando sesión...</div>;
    }

    return (
        <Router>
            {/* Si no está autenticado, forzamos la vista de Login */}
            {!isAuthenticated ? (
                <Routes>
                    <Route path="*" element={<Login />} />
                </Routes>
            ) : (
                /* Si está autenticado, cargamos el Layout Corporativo (Guards) */
                <div className="min-h-screen bg-gray-100 font-sans">
                    <nav className="bg-[#00274C] text-white shadow-md sticky top-0 z-20">
                        <div className="max-w-7xl mx-auto px-6 py-4 flex justify-between items-center">
                            <div className="flex items-center gap-3">
                                <div className="w-10 h-10 bg-[#FDB913] rounded-full flex items-center justify-center text-[#FDB913] text-2xl" aria-hidden="true">⚡</div>
                                <span className="font-bold text-2xl tracking-wide">
                                    Digital<span className="text-[#FDB913]">Fix</span>
                                </span>
                            </div>
                            <div className="flex gap-6 font-medium items-center">
                                <Link to="/dashboard" className="hover:text-[#FDB913] transition">Dashboard</Link>
                                <Link to="/ordenes" className="hover:text-[#FDB913] transition">Órdenes</Link>
                                <Link to="/catalog" className="hover:text-[#FDB913] transition">Catálogo</Link>
                                <Link to="/reports" className="hover:text-[#FDB913] transition">Reportería</Link>
                                <Link to="/audit" className="hover:text-[#FDB913] transition">Auditoría</Link>
                                
                                <div className="border-l border-gray-500 pl-6 flex items-center gap-4">
                                    <div className="text-sm text-right hidden sm:block">
                                        <p className="font-bold">{currentUser?.name}</p>
                                        <p className="text-xs text-[#FDB913]">
                                            {userRoles.length > 0 ? userRoles.join(", ") : "Usuario"}
                                        </p>
                                    </div>
                                    <button 
                                        onClick={handleLogout}
                                        className="bg-[#FDB913] text-[#00274C] text-sm font-bold px-4 py-2 rounded shadow hover:bg-yellow-500 transition"
                                    >
                                        Salir
                                    </button>
                                </div>
                            </div>
                        </div>
                    </nav>

                    <main className="max-w-7xl mx-auto p-6 mt-4">
                        <Routes>
                            <Route path="/" element={<Navigate to="/dashboard" />} />
                            <Route path="/dashboard" element={<Dashboard />} />
                            <Route path="/ordenes" element={<Ordenes />} />
                            <Route path="/catalog" element={<Catalog />} />
                            <Route path="/reports" element={<Reports />} />
                            <Route path="/audit" element={<Audit />} />
                        </Routes>
                    </main>
                </div>
            )}
        </Router>
    );
}