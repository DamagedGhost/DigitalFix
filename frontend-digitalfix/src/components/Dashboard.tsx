import { useEffect, useState } from "react";
import { useApi } from "../useApi";
import { useMsal } from "@azure/msal-react";
import AdminRabbitPanel from "./AdminRabbitPanel";

type AppNotification = {
    id: string;
    message: string;
    timestamp: string;
    type: "EMAIL" | "REPORT" | "DISPATCH" | "SYSTEM";
};

export default function Dashboard() {
    const { fetchWithToken } = useApi();
    const { accounts } = useMsal();
    const userRoles = (accounts[0]?.idTokenClaims?.roles as string[]) || [];
    const isAdmin = userRoles.includes("Admin");

    const [notifications, setNotifications] = useState<AppNotification[]>([]);
    const [isLoading, setIsLoading] = useState(true);

    const loadNotifications = async () => {
        setIsLoading(true);
        try {
            // Consumo de la ruta explícita v1 del microservicio de notificaciones o auditoría
            const response = await fetchWithToken("/api/v1/notifications"); 
            
            if (response.ok) {
                setNotifications(await response.json());
            } else {
                // Si el backend da 404 u otro error, registramos en consola y dejamos vacío
                console.warn(`Endpoint no disponible o error HTTP: ${response.status}`);
                setNotifications([]); 
            }
        } catch (error) {
            console.error("Fallo de red al intentar cargar las notificaciones:", error);
            setNotifications([]); 
        } finally {
            setIsLoading(false);
        }
    };

    useEffect(() => {
        void loadNotifications();
        
        const handleRefresh = () => void loadNotifications();
        window.addEventListener('refresh-notifications', handleRefresh);

        // Polling ligero para simular real-time si no hay WebSockets configurados
        const interval = setInterval(() => void loadNotifications(), 15000);
        return () => {
            clearInterval(interval);
            window.removeEventListener('refresh-notifications', handleRefresh);
        };
    }, [fetchWithToken]);

    const getIcon = (type: string) => {
        switch(type) {
            case "EMAIL": return "📧";
            case "REPORT": return "📊";
            case "DISPATCH": return "🚚";
            default: return "🔔";
        }
    };

    return (
        <div className="space-y-6">
            <header className="border-b border-gray-300 pb-4">
                <h2 className="text-3xl font-bold text-[#00274C]">Panel de Control</h2>
                <p className="text-gray-600 mt-1">Resumen de operaciones y eventos del sistema</p>
            </header>

            <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
                {/* Métricas o contenido principal del Dashboard iría aquí */}
                <div className="lg:col-span-2 space-y-6">
                    <div className="bg-white p-6 rounded shadow-sm border border-gray-200 h-64 flex items-center justify-center text-gray-400 font-bold">
                        [Gráficos y Métricas Operativas]
                    </div>
                    
                    {/* Renderizado Condicional RBAC para el panel de RabbitMQ */}
                    {isAdmin && <AdminRabbitPanel />}
                </div>

                {/* Feed de Notificaciones Asíncronas */}
                <div className="bg-white rounded shadow-sm border border-gray-200 flex flex-col h-[500px]">
                    <div className="p-4 bg-gray-50 border-b border-gray-200 flex justify-between items-center">
                        <h3 className="font-bold text-[#00274C]">🔔 Eventos Asíncronos</h3>
                        <button onClick={loadNotifications} className="text-xs text-blue-600 hover:underline">Actualizar</button>
                    </div>
                    <div className="p-4 flex-1 overflow-y-auto space-y-3">
                        {isLoading && notifications.length === 0 ? (
                            <p className="text-sm text-gray-500 text-center mt-4">Sincronizando eventos...</p>
                        ) : notifications.length === 0 ? (
                            <p className="text-sm text-gray-500 text-center mt-4">No hay notificaciones recientes.</p>
                        ) : (
                            notifications.map(note => (
                                <div key={note.id} className="p-3 bg-blue-50 border-l-4 border-[#00274C] rounded shadow-sm text-sm">
                                    <div className="flex items-center gap-2 mb-1">
                                        <span>{getIcon(note.type)}</span>
                                        <span className="font-bold text-gray-700">{note.type}</span>
                                        <span className="text-xs text-gray-500 ml-auto">
                                            {new Date(note.timestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                                        </span>
                                    </div>
                                    <p className="text-gray-700">{note.message}</p>
                                </div>
                            ))
                        )}
                    </div>
                </div>
            </div>
        </div>
    );
}