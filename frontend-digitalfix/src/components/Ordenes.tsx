import { useEffect, useState } from "react";
import type { FormEvent } from "react";
import { useApi } from "../useApi";

// Cambio mayor: Reemplazo de funciones estaticas con datos simulados
// por llamadas API real usando useApi y fetchWithToken
// Interaccion con backend real

type WorkOrderStatus = "CREADA" | "ASIGNADA" | "EN_DESPLAZAMIENTO" | "EN_EJECUCION" | "CERRADA" | "CANCELADA";

type WorkOrder = {
    id: number;
    clientName: string;
    technicianName: string | null;
    description: string;
    status: WorkOrderStatus;
    createdAt: string;
};

const statuses: WorkOrderStatus[] = [
    "CREADA",
    "ASIGNADA",
    "EN_DESPLAZAMIENTO",
    "EN_EJECUCION",
    "CERRADA",
    "CANCELADA",
];

export default function Ordenes() {
    const { fetchWithToken } = useApi();
    const [ordenes, setOrdenes] = useState<WorkOrder[]>([]);
    const [isLoading, setIsLoading] = useState(true);
    const [error, setError] = useState("");
    const [clientName, setClientName] = useState("");
    const [description, setDescription] = useState("");
    const [selectedOrder, setSelectedOrder] = useState<WorkOrder | null>(null);
    const [selectedStatus, setSelectedStatus] = useState<WorkOrderStatus>("CREADA");
    const [technicianName, setTechnicianName] = useState("");
    const [isSaving, setIsSaving] = useState(false);
    const [searchTerm, setSearchTerm] = useState("");

    const [isModalOpen, setIsModalOpen] = useState(false);

    const loadOrders = async () => {
        setIsLoading(true);
        setError("");
        try {
            const response = await fetchWithToken("/workorders");
            if (!response.ok) throw new Error(`No se pudieron cargar las órdenes (${response.status})`);
            setOrdenes(await response.json());
        } catch (loadError) {
            setError(loadError instanceof Error ? loadError.message : "No se pudieron cargar las órdenes");
        } finally {
            setIsLoading(false);
        }
    };

    useEffect(() => {
        void loadOrders();
    }, []);

    const createOrder = async (event: FormEvent) => {
        event.preventDefault();
        setIsSaving(true);
        setError("");
        try {
            const response = await fetchWithToken("/workorders", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ clientName, description }),
            });
            if (!response.ok) throw new Error(`No se pudo crear la orden (${response.status})`);
            setClientName("");
            setDescription("");
            setIsModalOpen(false);
            await loadOrders();
        } catch (saveError) {
            setError(saveError instanceof Error ? saveError.message : "No se pudo crear la orden");
        } finally {
            setIsSaving(false);
        }
    };

    const updateOrder = async (event: FormEvent) => {
        event.preventDefault();
        if (!selectedOrder) return;
        setIsSaving(true);
        setError("");
        try {
            const response = await fetchWithToken(`/workorders/${selectedOrder.id}/status`, {
                method: "PUT",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ status: selectedStatus, technicianName: technicianName || null }),
            });
            if (!response.ok) throw new Error(`No se pudo actualizar la orden (${response.status})`);
            setSelectedOrder(null);
            await loadOrders();
        } catch (updateError) {
            setError(updateError instanceof Error ? updateError.message : "No se pudo actualizar la orden");
        } finally {
            setIsSaving(false);
        }
    };

    const openOrder = (order: WorkOrder) => {
        setSelectedOrder(order);
        setSelectedStatus(order.status);
        setTechnicianName(order.technicianName || "");
    };

    const getStatusBadge = (status: WorkOrderStatus) => {
        const styles: Record<WorkOrderStatus, string> = {
            "CREADA": "bg-gray-200 text-gray-700",
            "ASIGNADA": "bg-blue-100 text-[#00274C]",
            "EN_DESPLAZAMIENTO": "bg-purple-100 text-purple-800",
            "EN_EJECUCION": "bg-[#FDB913] text-[#00274C]",
            "CERRADA": "bg-green-100 text-green-800",
            "CANCELADA": "bg-red-100 text-red-800",
        };
        return styles[status];
    };

    const formatDate = (date: string) => new Intl.DateTimeFormat("es-CL", {
        dateStyle: "short",
    }).format(new Date(date));

    const filteredOrders = ordenes.filter((order) => {
        const search = searchTerm.toLowerCase();
        return order.clientName.toLowerCase().includes(search)
            || order.description.toLowerCase().includes(search)
            || order.id.toString().includes(search);
    });

    return (
        <div className="space-y-6">
            <header className="flex justify-between items-end border-b border-gray-300 pb-4">
                <div>
                    <h2 className="text-3xl font-bold text-[#00274C]">Órdenes de Trabajo</h2>
                    <p className="text-gray-600 mt-1">Gestión y seguimiento de mantenciones en terreno</p>
                </div>
                <button 
                    onClick={() => setIsModalOpen(true)}
                    className="bg-[#00274C] text-white font-bold px-5 py-2 rounded shadow hover:bg-blue-900 transition flex items-center gap-2"
                >
                    <span className="text-[#FDB913] text-xl leading-none">+</span> Nueva Orden
                </button>
            </header>

            {error && <p className="rounded border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">{error}</p>}

            <div className="bg-white rounded shadow-sm border border-gray-200 overflow-hidden">
                <div className="p-4 bg-gray-50 border-b border-gray-200 flex justify-between items-center">
                    <h3 className="font-bold text-[#00274C]">Listado de Órdenes</h3>
                    <input 
                        type="text" 
                        value={searchTerm}
                        onChange={(event) => setSearchTerm(event.target.value)}
                        placeholder="Buscar orden o cliente..." 
                        className="border border-gray-300 rounded px-3 py-1 text-sm focus:outline-none focus:border-[#00274C]"
                    />
                </div>
                <div className="overflow-x-auto">
                    <table className="w-full text-left border-collapse">
                        <thead>
                            <tr className="bg-[#00274C] text-white text-sm uppercase tracking-wider">
                                <th className="p-4 font-semibold w-24">ID</th>
                                <th className="p-4 font-semibold">Fecha</th>
                                <th className="p-4 font-semibold">Cliente</th>
                                <th className="p-4 font-semibold">Servicio</th>
                                <th className="p-4 font-semibold">Estado</th>
                                <th className="p-4 font-semibold text-center">Acción</th>
                            </tr>
                        </thead>
                        <tbody className="divide-y divide-gray-200 text-gray-800 text-sm">
                            {isLoading ? (
                                <tr><td colSpan={6} className="p-8 text-center text-gray-500">Cargando órdenes...</td></tr>
                            ) : filteredOrders.length === 0 ? (
                                <tr><td colSpan={6} className="p-8 text-center text-gray-500">No hay órdenes registradas.</td></tr>
                            ) : filteredOrders.map((orden) => (
                                <tr key={orden.id} className="hover:bg-gray-50 transition duration-150">
                                    <td className="p-4 font-bold text-[#00274C]">#{orden.id}</td>
                                    <td className="p-4 text-gray-600">{formatDate(orden.createdAt)}</td>
                                    <td className="p-4 font-medium">{orden.clientName}</td>
                                    <td className="p-4">{orden.description}</td>
                                    <td className="p-4">
                                        <span className={`px-3 py-1 rounded text-xs font-bold tracking-wide ${getStatusBadge(orden.status)}`}>
                                            {orden.status.replace("_", " ")}
                                        </span>
                                    </td>
                                    <td className="p-4 text-center">
                                        <button onClick={() => openOrder(orden)} className="text-[#00274C] hover:text-[#FDB913] font-bold text-xs uppercase tracking-wider transition">
                                            Gestionar
                                        </button>
                                    </td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                </div>
            </div>

            {isModalOpen && (
                <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 backdrop-blur-sm">
                    <div className="bg-white rounded shadow-xl w-full max-w-lg overflow-hidden border-t-4 border-[#FDB913]">
                        <div className="p-5 border-b border-gray-200 bg-gray-50 flex justify-between items-center">
                            <h3 className="text-xl font-bold text-[#00274C]">Crear Nueva Orden</h3>
                            <button 
                                onClick={() => setIsModalOpen(false)}
                                className="text-gray-400 hover:text-red-500 font-bold text-xl transition"
                            >
                                ×
                            </button>
                        </div>
                        
                        <form onSubmit={createOrder} className="p-6 space-y-4">
                            <div>
                                <label className="block text-sm font-bold text-gray-700 mb-1">Cliente / Inmueble</label>
                                <input required value={clientName} onChange={(event) => setClientName(event.target.value)} type="text" className="w-full border border-gray-300 rounded px-3 py-2 focus:outline-none focus:border-[#00274C]" placeholder="Ej. Edificio Las Palmas" />
                            </div>
                            
                            <div>
                                <label className="block text-sm font-bold text-gray-700 mb-1">Descripción del Problema</label>
                                <textarea required value={description} onChange={(event) => setDescription(event.target.value)} className="w-full border border-gray-300 rounded px-3 py-2 focus:outline-none focus:border-[#00274C]" rows={3} placeholder="Detalle los síntomas o requerimientos..."></textarea>
                            </div>

                            <div className="p-5 border-t border-gray-200 bg-gray-50 flex justify-end gap-3 -mx-6 -mb-6">
                            <button 
                                onClick={() => setIsModalOpen(false)}
                                className="px-4 py-2 border border-gray-300 text-gray-700 rounded font-bold hover:bg-gray-100 transition"
                            >
                                Cancelar
                            </button>
                            <button disabled={isSaving} type="submit" className="px-4 py-2 bg-[#00274C] text-white rounded font-bold hover:bg-blue-900 shadow transition disabled:opacity-50">
                                {isSaving ? "Guardando..." : "Guardar Orden"}
                            </button>
                            </div>
                        </form>
                    </div>
                </div>
            )}

            {selectedOrder && (
                <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 backdrop-blur-sm">
                    <form onSubmit={updateOrder} className="bg-white rounded shadow-xl w-full max-w-lg overflow-hidden border-t-4 border-[#FDB913]">
                        <div className="p-5 border-b border-gray-200 bg-gray-50 flex justify-between items-center">
                            <h3 className="text-xl font-bold text-[#00274C]">Gestionar orden #{selectedOrder.id}</h3>
                            <button type="button" onClick={() => setSelectedOrder(null)} className="text-gray-400 hover:text-red-500 font-bold text-xl transition">×</button>
                        </div>
                        <div className="p-6 space-y-4">
                            <p className="text-sm text-gray-600">{selectedOrder.clientName}: {selectedOrder.description}</p>
                            <div>
                                <label className="block text-sm font-bold text-gray-700 mb-1">Estado</label>
                                <select value={selectedStatus} onChange={(event) => setSelectedStatus(event.target.value as WorkOrderStatus)} className="w-full border border-gray-300 rounded px-3 py-2 bg-white">
                                    {statuses.map((status) => <option key={status} value={status}>{status.replace("_", " ")}</option>)}
                                </select>
                            </div>
                            <div>
                                <label className="block text-sm font-bold text-gray-700 mb-1">Técnico</label>
                                <input value={technicianName} onChange={(event) => setTechnicianName(event.target.value)} className="w-full border border-gray-300 rounded px-3 py-2" placeholder="Nombre del técnico" />
                            </div>
                        </div>
                        <div className="p-5 border-t border-gray-200 bg-gray-50 flex justify-end gap-3">
                            <button type="button" onClick={() => setSelectedOrder(null)} className="px-4 py-2 border border-gray-300 text-gray-700 rounded font-bold hover:bg-gray-100 transition">Cancelar</button>
                            <button disabled={isSaving} type="submit" className="px-4 py-2 bg-[#00274C] text-white rounded font-bold hover:bg-blue-900 shadow transition disabled:opacity-50">{isSaving ? "Guardando..." : "Guardar cambios"}</button>
                        </div>
                    </form>
                </div>
            )}
        </div>
    );
}