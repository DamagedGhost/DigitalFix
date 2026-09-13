import { useState } from "react";

export default function Catalog() {
    // Control de pestañas y modal
    const [activeTab, setActiveTab] = useState<"servicios" | "repuestos">("servicios");
    const [isModalOpen, setIsModalOpen] = useState(false);

    // Mock de datos para Servicios
    const mockServices = [
        { id: "SRV-001", name: "Revisión Tablero Principal", description: "Inspección y medición de cargas", price: "$50.000" },
        { id: "SRV-002", name: "Mantenimiento Preventivo", description: "Limpieza y reapriete de conexiones", price: "$75.000" },
        { id: "SRV-003", name: "Cambio de Luminarias", description: "Reemplazo de focos y balastros", price: "$25.000" },
    ];

    // Mock de datos para Repuestos (Stock)
    const mockParts = [
        { id: "REP-001", name: "Interruptor Termomagnético 16A", brand: "Legrand", stock: 45, price: "$5.500" },
        { id: "REP-002", name: "Cable de Cobre 2.5mm (100m)", brand: "Covisa", stock: 12, price: "$42.000" },
        { id: "REP-003", name: "Tablero Eléctrico 12 Polos", brand: "Schneider", stock: 2, price: "$28.000" }, // Bajo stock
    ];

    // Función para dar color al stock
    const getStockBadge = (stock: number) => {
        if (stock > 20) return "bg-green-100 text-green-800 border border-green-200";
        if (stock > 5) return "bg-yellow-100 text-yellow-800 border border-yellow-200";
        return "bg-red-100 text-red-800 border border-red-200 font-bold animate-pulse";
    };

    return (
        <div className="space-y-6">
            {/* Cabecera del Módulo */}
            <header className="flex justify-between items-end border-b border-gray-300 pb-4">
                <div>
                    <h2 className="text-3xl font-bold text-[#00274C]">Catálogo Técnico</h2>
                    <p className="text-gray-600 mt-1">Administración de servicios, repuestos y stock</p>
                </div>
                <button 
                    onClick={() => setIsModalOpen(true)}
                    className="bg-[#00274C] text-white font-bold px-5 py-2 rounded shadow hover:bg-blue-900 transition flex items-center gap-2"
                >
                    <span className="text-[#FDB913] text-xl leading-none">+</span> Nuevo {activeTab === "servicios" ? "Servicio" : "Repuesto"}
                </button>
            </header>

            {/* Sistema de Pestañas (Tabs) */}
            <div className="flex gap-4 border-b border-gray-300">
                <button 
                    onClick={() => setActiveTab("servicios")}
                    className={`px-4 py-2 font-bold text-sm tracking-wide transition-all ${
                        activeTab === "servicios" 
                        ? "text-[#00274C] border-b-4 border-[#FDB913]" 
                        : "text-gray-500 hover:text-[#00274C]"
                    }`}
                >
                    SERVICIOS
                </button>
                <button 
                    onClick={() => setActiveTab("repuestos")}
                    className={`px-4 py-2 font-bold text-sm tracking-wide transition-all ${
                        activeTab === "repuestos" 
                        ? "text-[#00274C] border-b-4 border-[#FDB913]" 
                        : "text-gray-500 hover:text-[#00274C]"
                    }`}
                >
                    REPUESTOS Y STOCK
                </button>
            </div>

            {/* Contenedor de la Tabla */}
            <div className="bg-white rounded shadow-sm border border-gray-200 overflow-hidden">
                <div className="p-4 bg-gray-50 border-b border-gray-200 flex justify-between items-center">
                    <h3 className="font-bold text-[#00274C]">
                        Listado de {activeTab === "servicios" ? "Servicios" : "Repuestos"}
                    </h3>
                    <input 
                        type="text" 
                        placeholder={`Buscar ${activeTab}...`} 
                        className="border border-gray-300 rounded px-3 py-1 text-sm focus:outline-none focus:border-[#00274C]"
                    />
                </div>
                
                <div className="overflow-x-auto">
                    <table className="w-full text-left border-collapse">
                        <thead>
                            <tr className="bg-[#00274C] text-white text-sm uppercase tracking-wider">
                                <th className="p-4 font-semibold w-24">ID</th>
                                <th className="p-4 font-semibold">Nombre</th>
                                {activeTab === "servicios" ? (
                                    <th className="p-4 font-semibold">Descripción</th>
                                ) : (
                                    <th className="p-4 font-semibold">Marca</th>
                                )}
                                {activeTab === "repuestos" && <th className="p-4 font-semibold text-center">Stock</th>}
                                <th className="p-4 font-semibold text-right">Tarifa / Valor</th>
                                <th className="p-4 font-semibold text-center">Acción</th>
                            </tr>
                        </thead>
                        <tbody className="divide-y divide-gray-200 text-gray-800 text-sm">
                            {/* Renderizado condicional según la pestaña activa */}
                            {activeTab === "servicios" ? (
                                mockServices.map((service) => (
                                    <tr key={service.id} className="hover:bg-gray-50 transition duration-150">
                                        <td className="p-4 font-bold text-[#00274C]">{service.id}</td>
                                        <td className="p-4 font-medium">{service.name}</td>
                                        <td className="p-4 text-gray-600">{service.description}</td>
                                        <td className="p-4 text-right font-bold text-gray-700">{service.price}</td>
                                        <td className="p-4 text-center">
                                            <button className="text-[#00274C] hover:text-[#FDB913] font-bold text-xs uppercase tracking-wider transition">Editar</button>
                                        </td>
                                    </tr>
                                ))
                            ) : (
                                mockParts.map((part) => (
                                    <tr key={part.id} className="hover:bg-gray-50 transition duration-150">
                                        <td className="p-4 font-bold text-[#00274C]">{part.id}</td>
                                        <td className="p-4 font-medium">{part.name}</td>
                                        <td className="p-4 text-gray-600">{part.brand}</td>
                                        <td className="p-4 text-center">
                                            <span className={`px-3 py-1 rounded text-xs tracking-wide ${getStockBadge(part.stock)}`}>
                                                {part.stock} unid.
                                            </span>
                                        </td>
                                        <td className="p-4 text-right font-bold text-gray-700">{part.price}</td>
                                        <td className="p-4 text-center">
                                            <button className="text-[#00274C] hover:text-[#FDB913] font-bold text-xs uppercase tracking-wider transition">Editar</button>
                                        </td>
                                    </tr>
                                ))
                            )}
                        </tbody>
                    </table>
                </div>
            </div>

            {/* --- MODAL CREAR/EDITAR --- */}
            {isModalOpen && (
                <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 backdrop-blur-sm">
                    <div className="bg-white rounded shadow-xl w-full max-w-md overflow-hidden border-t-4 border-[#FDB913]">
                        <div className="p-5 border-b border-gray-200 bg-gray-50 flex justify-between items-center">
                            <h3 className="text-xl font-bold text-[#00274C]">
                                Agregar {activeTab === "servicios" ? "Servicio" : "Repuesto"}
                            </h3>
                            <button 
                                onClick={() => setIsModalOpen(false)}
                                className="text-gray-400 hover:text-red-500 font-bold text-xl transition"
                            >
                                ×
                            </button>
                        </div>
                        
                        <div className="p-6 space-y-4">
                            <div>
                                <label className="block text-sm font-bold text-gray-700 mb-1">Nombre</label>
                                <input type="text" className="w-full border border-gray-300 rounded px-3 py-2 focus:outline-none focus:border-[#00274C]" />
                            </div>
                            
                            {activeTab === "repuestos" && (
                                <div className="grid grid-cols-2 gap-4">
                                    <div>
                                        <label className="block text-sm font-bold text-gray-700 mb-1">Stock Inicial</label>
                                        <input type="number" className="w-full border border-gray-300 rounded px-3 py-2 focus:outline-none focus:border-[#00274C]" />
                                    </div>
                                    <div>
                                        <label className="block text-sm font-bold text-gray-700 mb-1">Marca</label>
                                        <input type="text" className="w-full border border-gray-300 rounded px-3 py-2 focus:outline-none focus:border-[#00274C]" />
                                    </div>
                                </div>
                            )}

                            <div>
                                <label className="block text-sm font-bold text-gray-700 mb-1">Valor / Tarifa ($)</label>
                                <input type="number" className="w-full border border-gray-300 rounded px-3 py-2 focus:outline-none focus:border-[#00274C]" />
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
                                Guardar
                            </button>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
}