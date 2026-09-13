import { useState } from "react";

export default function Ordenes() {
    const [ordenes, setOrders] = useState([
        { id: "ORD-001", client: "Edificio Central", service: "Revisión Tablero Principal", status: "CREADA", date: "2026-09-11" },
        { id: "ORD-002", client: "Sucursal Norte", service: "Cambio de Luminarias", status: "ASIGNADA", date: "2026-09-10" },
        { id: "ORD-003", client: "Galpón 4", service: "Mantenimiento Preventivo", status: "EN_EJECUCIÓN", date: "2026-09-09" },
    ]);

    // Estado para controlar la visibilidad del modal
    const [isModalOpen, setIsModalOpen] = useState(false);

    const getStatusBadge = (status: string) => {
        const styles: Record<string, string> = {
            "CREADA": "bg-gray-200 text-gray-700",
            "ASIGNADA": "bg-blue-100 text-[#00274C]",
            "EN_DESPLAZAMIENTO": "bg-purple-100 text-purple-800",
            "EN_EJECUCIÓN": "bg-[#FDB913] text-[#00274C]", 
            "CERRADA": "bg-green-100 text-green-800",
            "CANCELADA": "bg-red-100 text-red-800",
        };
        return styles[status] || "bg-gray-100 text-gray-600";
    };

    return (
        <div className="space-y-6">
            <header className="flex justify-between items-end border-b border-gray-300 pb-4">
                <div>
                    <h2 className="text-3xl font-bold text-[#00274C]">Órdenes de Trabajo</h2>
                    <p className="text-gray-600 mt-1">Gestión y seguimiento de mantenciones en terreno</p>
                </div>
                {/* Botón que abre el modal */}
                <button 
                    onClick={() => setIsModalOpen(true)}
                    className="bg-[#00274C] text-white font-bold px-5 py-2 rounded shadow hover:bg-blue-900 transition flex items-center gap-2"
                >
                    <span className="text-[#FDB913] text-xl leading-none">+</span> Nueva Orden
                </button>
            </header>

            <div className="bg-white rounded shadow-sm border border-gray-200 overflow-hidden">
                <div className="p-4 bg-gray-50 border-b border-gray-200 flex justify-between items-center">
                    <h3 className="font-bold text-[#00274C]">Listado de Órdenes</h3>
                    <input 
                        type="text" 
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
                            {ordenes.map((orden) => (
                                <tr key={orden.id} className="hover:bg-gray-50 transition duration-150">
                                    <td className="p-4 font-bold text-[#00274C]">{orden.id}</td>
                                    <td className="p-4 text-gray-600">{orden.date}</td>
                                    <td className="p-4 font-medium">{orden.client}</td>
                                    <td className="p-4">{orden.service}</td>
                                    <td className="p-4">
                                        <span className={`px-3 py-1 rounded text-xs font-bold tracking-wide ${getStatusBadge(orden.status)}`}>
                                            {orden.status}
                                        </span>
                                    </td>
                                    <td className="p-4 text-center">
                                        <button className="text-[#00274C] hover:text-[#FDB913] font-bold text-xs uppercase tracking-wider transition">
                                            Gestionar
                                        </button>
                                    </td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                </div>
            </div>

            {/* --- MODAL DE NUEVA ORDEN --- */}
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
                        
                        <div className="p-6 space-y-4">
                            <div>
                                <label className="block text-sm font-bold text-gray-700 mb-1">Cliente / Inmueble</label>
                                <input type="text" className="w-full border border-gray-300 rounded px-3 py-2 focus:outline-none focus:border-[#00274C]" placeholder="Ej. Edificio Las Palmas" />
                            </div>
                            
                            <div>
                                <label className="block text-sm font-bold text-gray-700 mb-1">Servicio Requerido</label>
                                <select className="w-full border border-gray-300 rounded px-3 py-2 focus:outline-none focus:border-[#00274C] bg-white">
                                    <option value="">Seleccione un servicio del catálogo...</option>
                                    <option value="1">Revisión Tablero Principal</option>
                                    <option value="2">Mantenimiento Preventivo</option>
                                    <option value="3">Cambio de Luminarias</option>
                                </select>
                            </div>

                            <div>
                                <label className="block text-sm font-bold text-gray-700 mb-1">Descripción del Problema</label>
                                <textarea className="w-full border border-gray-300 rounded px-3 py-2 focus:outline-none focus:border-[#00274C]" rows={3} placeholder="Detalle los síntomas o requerimientos..."></textarea>
                            </div>
                        </div>

                        <div className="p-5 border-t border-gray-200 bg-gray-50 flex justify-end gap-3">
                            <button 
                                onClick={() => setIsModalOpen(false)}
                                className="px-4 py-2 border border-gray-300 text-gray-700 rounded font-bold hover:bg-gray-100 transition"
                            >
                                Cancelar
                            </button>
                            <button className="px-4 py-2 bg-[#00274C] text-white rounded font-bold hover:bg-blue-900 shadow transition">
                                Guardar Orden
                            </button>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
}