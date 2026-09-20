import { useState, useEffect } from "react";
import { useApi } from "../useApi";

type WorkOrderStatus = "CREADA" | "ASIGNADA" | "EN_DESPLAZAMIENTO" | "EN_EJECUCION" | "CERRADA" | "CANCELADA";

type WorkOrder = {
    id: number;
    clientName: string;
    technicianName: string | null;
    description: string;
    status: WorkOrderStatus;
    createdAt: string;
};

export default function Dashboard() {
    const { fetchWithToken } = useApi();
    const [orders, setOrders] = useState<WorkOrder[]>([]);
    const [isLoading, setIsLoading] = useState(true);
    const [error, setError] = useState("");

    // Mocks de KPIs
    const mockKpis = [
        { title: "Órdenes Activas", value: "24" },
        { title: "En Terreno", value: "8" },
        { title: "Tiempo Promedio", value: "1.5h" },
    ];

    useEffect(() => {
        const loadRecentOrders = async () => {
            try {
                const response = await fetchWithToken("/workorders");
                if (!response.ok) throw new Error(`Error al obtener órdenes (${response.status})`);
                
                const data: WorkOrder[] = await response.json();
                // Invertimos para mostrar las más nuevas primero y cortamos en 5
                const recentOrders = data.reverse().slice(0, 5); 
                setOrders(recentOrders);
            } catch (err) {
                setError(err instanceof Error ? err.message : "Error de conexión");
            } finally {
                setIsLoading(false);
            }
        };

        void loadRecentOrders();
    }, [fetchWithToken]);

    const getStatusBadge = (status: WorkOrderStatus) => {
        const styles: Record<WorkOrderStatus, string> = {
            "CREADA": "bg-gray-200 text-gray-700",
            "ASIGNADA": "bg-blue-100 text-[#00274C]",
            "EN_DESPLAZAMIENTO": "bg-purple-100 text-purple-800",
            "EN_EJECUCION": "bg-[#FDB913] text-[#00274C]",
            "CERRADA": "bg-green-100 text-green-800",
            "CANCELADA": "bg-red-100 text-red-800",
        };
        return styles[status] || "bg-gray-200 text-gray-700";
    };

    return (
        <div className="space-y-6">
            <header className="mb-8 border-b border-gray-300 pb-4">
                <h2 className="text-3xl font-bold text-[#00274C]">
                    Panel de Operaciones
                </h2>
                <p className="text-gray-600 mt-1">Métricas en tiempo real de la red eléctrica</p>
            </header>

            <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
                {mockKpis.map((kpi, index) => (
                    <div 
                        key={index} 
                        className="bg-white p-6 rounded shadow-sm border-l-4 border-[#FDB913] hover:shadow-md transition"
                    >
                        <h3 className="text-sm font-bold text-gray-500 uppercase tracking-wider">{kpi.title}</h3>
                        <p className="text-4xl font-extrabold text-[#00274C] mt-2">{kpi.value}</p>
                    </div>
                ))}
            </div>

            <div className="mt-8 bg-white rounded shadow-sm border border-gray-200 overflow-hidden">
                <div className="p-5 border-b border-gray-200 bg-gray-50 flex justify-between items-center">
                    <h3 className="text-lg font-bold text-[#00274C]">Órdenes Recientes</h3>
                    {isLoading && <span className="text-sm text-gray-500 animate-pulse font-bold">Actualizando...</span>}
                </div>
                
                {error && (
                    <div className="p-4 text-center text-red-600 font-bold bg-red-50 border-b border-red-100">
                        {error}
                    </div>
                )}

                <div className="overflow-x-auto">
                    <table className="w-full text-left border-collapse">
                        <thead>
                            <tr className="bg-[#00274C] text-white text-sm uppercase tracking-wider">
                                <th className="p-4 font-semibold">ID Orden</th>
                                <th className="p-4 font-semibold">Cliente</th>
                                <th className="p-4 font-semibold">Técnico</th>
                                <th className="p-4 font-semibold">Estado</th>
                            </tr>
                        </thead>
                        <tbody className="divide-y divide-gray-200 text-gray-800">
                            {!isLoading && orders.length === 0 ? (
                                <tr>
                                    <td colSpan={4} className="p-6 text-center text-gray-500">
                                        No hay órdenes recientes en la base de datos.
                                    </td>
                                </tr>
                            ) : (
                                orders.map((order) => (
                                    <tr key={order.id} className="hover:bg-gray-50 transition duration-150">
                                        <td className="p-4 font-bold text-[#00274C]">#{order.id}</td>
                                        <td className="p-4">{order.clientName}</td>
                                        <td className="p-4 text-gray-600 italic">
                                            {order.technicianName || "Por asignar"}
                                        </td>
                                        <td className="p-4">
                                            <span className={`px-3 py-1 rounded text-xs font-bold tracking-wide ${getStatusBadge(order.status)}`}>
                                                {order.status.replace("_", " ")}
                                            </span>
                                        </td>
                                    </tr>
                                ))
                            )}
                        </tbody>
                    </table>
                </div>
            </div>
        </div>
    );
}