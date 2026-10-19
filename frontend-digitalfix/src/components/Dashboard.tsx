import { useEffect, useState } from "react";
import { useApi } from "../useApi";
import { useMsal } from "@azure/msal-react";
import AdminRabbitPanel from "./AdminRabbitPanel";

type AppNotification = {
    id: string;
    message: string;
    timestamp: string;
    type: "EMAIL" | "REPORTE" | "DESPACHO" | "SYSTEM" | "DLQ";
};

// Añadimos el tipo de Orden para la tabla resumen
type WorkOrder = {
    id: number;
    clientName: string;
    description: string;
    status: string;
    createdAt: string;
};

export default function Dashboard() {
    const { fetchWithToken } = useApi();
    const { accounts } = useMsal();
    const userRoles = (accounts[0]?.idTokenClaims?.roles as string[]) || [];
    const isAdmin = userRoles.includes("Admin");

    const [notifications, setNotifications] = useState<AppNotification[]>([]);
    const [recentOrders, setRecentOrders] = useState<WorkOrder[]>([]);
    const [metrics, setMetrics] = useState({ total: 0, activas: 0, pendientes: 0 });
    const [isLoading, setIsLoading] = useState(true);

    const loadDashboardData = async () => {
        setIsLoading(true);
        try {
            // 1. Cargar Feed de Notificaciones (RabbitMQ)
            const notifRes = await fetchWithToken("/api/v1/notifications");
            if (notifRes.ok) {
                setNotifications(await notifRes.json());
            } else {
                setNotifications([]); 
            }

            // 2. Cargar Órdenes para calcular métricas en vivo
            const ordersRes = await fetchWithToken("/api/v1/workorders");
            if (ordersRes.ok) {
                const data: WorkOrder[] = await ordersRes.json();
                
                // Calcular KPIs básicos en el frontend
                setMetrics({
                    total: data.length,
                    activas: data.filter(o => o.status === "EN_DESPLAZAMIENTO" || o.status === "EN_EJECUCION").length,
                    pendientes: data.filter(o => o.status === "CREADA" || o.status === "ASIGNADA").length
                });

                // Ordenar por ID descendente y tomar las últimas 4
                const latest = data.sort((a, b) => b.id - a.id).slice(0, 4);
                setRecentOrders(latest);
            }
        } catch (error) {
            console.error("Error al sincronizar el Dashboard:", error);
        } finally {
            setIsLoading(false);
        }
    };

    useEffect(() => {
        void loadDashboardData();
        const handleRefresh = () => void loadDashboardData();
        window.addEventListener('refresh-notifications', handleRefresh);

        const interval = setInterval(() => void loadDashboardData(), 15000);
        return () => {
            clearInterval(interval);
            window.removeEventListener('refresh-notifications', handleRefresh);
        };
    }, [fetchWithToken]);

    const getIcon = (type: string) => {
        switch(type?.toUpperCase()) {
            case "EMAIL": return "📧";
            case "REPORT":
            case "REPORTE": return "📊";
            case "DISPATCH":
            case "DESPACHO": return "🚚";
            case "DLQ":
            case "ERROR": return "⚠️";
            default: return "🔔";
        }
    };

    return (
        <div className="space-y-6">
            <header className="border-b border-gray-300 pb-4">
                <h2 className="text-3xl font-bold text-[#00274C]">Panel de Control</h2>
                <p className="text-gray-600 mt-1">Resumen de operaciones y eventos asíncronos</p>
            </header>

            <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
                
                {/* Columna Izquierda: Métricas, Tabla y RabbitMQ */}
                <div className="lg:col-span-2 space-y-6">
                    
                    {/* Tarjetas de Métricas (Reemplaza al texto feo de Gráficos) */}
                    <div className="grid grid-cols-3 gap-4">
                        <div className="bg-white p-4 rounded shadow-sm border border-gray-200 border-l-4 border-l-blue-500">
                            <p className="text-xs text-gray-500 font-bold uppercase tracking-wider">Total Órdenes</p>
                            <p className="text-3xl font-black text-[#00274C] mt-1">{metrics.total}</p>
                        </div>
                        <div className="bg-white p-4 rounded shadow-sm border border-gray-200 border-l-4 border-l-[#FDB913]">
                            <p className="text-xs text-gray-500 font-bold uppercase tracking-wider">En Terreno</p>
                            <p className="text-3xl font-black text-[#00274C] mt-1">{metrics.activas}</p>
                        </div>
                        <div className="bg-white p-4 rounded shadow-sm border border-gray-200 border-l-4 border-l-red-500">
                            <p className="text-xs text-gray-500 font-bold uppercase tracking-wider">Pendientes</p>
                            <p className="text-3xl font-black text-[#00274C] mt-1">{metrics.pendientes}</p>
                        </div>
                    </div>

                    {/* Tabla de Últimas Órdenes */}
                    <div className="bg-white rounded shadow-sm border border-gray-200 overflow-hidden">
                        <div className="p-4 bg-gray-50 border-b border-gray-200 flex justify-between items-center">
                            <h3 className="font-bold text-[#00274C]">Últimas Órdenes Registradas</h3>
                        </div>
                        <table className="w-full text-left text-sm">
                            <tbody className="divide-y divide-gray-200">
                                {recentOrders.length === 0 ? (
                                    <tr><td className="p-6 text-center text-gray-500">No hay órdenes para mostrar</td></tr>
                                ) : (
                                    recentOrders.map(o => (
                                        <tr key={o.id} className="hover:bg-gray-50 transition">
                                            <td className="p-3 font-bold text-[#00274C] pl-4">#{o.id}</td>
                                            <td className="p-3 font-medium text-gray-800">{o.clientName}</td>
                                            <td className="p-3 text-gray-500 truncate max-w-[150px]">{o.description}</td>
                                            <td className="p-3 text-right pr-4">
                                                <span className="bg-gray-200 text-gray-700 px-2 py-1 rounded text-xs font-bold tracking-wide">
                                                    {o.status.replace("_", " ")}
                                                </span>
                                            </td>
                                        </tr>
                                    ))
                                )}
                            </tbody>
                        </table>
                    </div>
                    
                    {/* Panel de RabbitMQ (Solo Admin) */}
                    {isAdmin && <AdminRabbitPanel />}
                </div>

                {/* Columna Derecha: Feed de Notificaciones Asíncronas */}
                <div className="bg-white rounded shadow-sm border border-gray-200 flex flex-col h-[600px]">
                    <div className="p-4 bg-gray-50 border-b border-gray-200 flex justify-between items-center">
                        <h3 className="font-bold text-[#00274C]">🔔 Eventos Asíncronos</h3>
                        <button onClick={loadDashboardData} className="text-xs text-blue-600 hover:underline font-bold">↻ Refrescar</button>
                    </div>
                    <div className="p-4 flex-1 overflow-y-auto space-y-3">
                        {isLoading && notifications.length === 0 ? (
                            <p className="text-sm text-gray-500 text-center mt-4 font-medium">Sincronizando eventos...</p>
                        ) : notifications.length === 0 ? (
                            <p className="text-sm text-gray-500 text-center mt-4 font-medium">No hay notificaciones recientes.</p>
                        ) : (
                            notifications.map(note => {
                                const isDlq = note.type === "DLQ" || note.message?.includes("[DLQ]");
                                return (
                                    <div key={note.id} className={`p-3 rounded shadow-sm text-sm hover:shadow transition border-l-4 ${
                                        isDlq
                                            ? "bg-red-50 border-red-600 text-red-900"
                                            : "bg-blue-50 border-[#00274C]"
                                    }`}>
                                        <div className="flex items-center gap-2 mb-1">
                                            <span>{getIcon(note.type)}</span>
                                            <span className={`font-bold ${isDlq ? "text-red-700" : "text-gray-700"}`}>{note.type}</span>
                                            <span className="text-xs text-gray-500 ml-auto">
                                                {new Date(note.timestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                                            </span>
                                        </div>
                                        <p className={`leading-snug ${isDlq ? "text-red-800 font-medium" : "text-gray-700"}`}>{note.message}</p>
                                    </div>
                                );
                            })
                        )}
                    </div>
                </div>

            </div>
        </div>
    );
}