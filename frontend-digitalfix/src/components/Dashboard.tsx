export default function Dashboard() {
    const mockKpis = [
        { title: "Órdenes Activas", value: "24" },
        { title: "En Terreno", value: "8" },
        { title: "Tiempo Promedio", value: "1.5h" },
    ];

    const mockOrders = [
        { id: "ORD-001", client: "Edificio Central", status: "EN_DESPLAZAMIENTO", tech: "Carlos M." },
        { id: "ORD-002", client: "Sucursal Norte", status: "ASIGNADA", tech: "Ana G." },
        { id: "ORD-003", client: "Galpón 4", status: "EN_EJECUCIÓN", tech: "Luis P." },
    ];

    return (
        <div className="space-y-6">
            {/* Cabecera limpia con separador */}
            <header className="mb-8 border-b border-gray-300 pb-4">
                <h2 className="text-3xl font-bold text-[#00274C]">
                    Panel de Operaciones
                </h2>
                <p className="text-gray-600 mt-1">Métricas en tiempo real de la red eléctrica</p>
            </header>

            {/* Tarjetas de KPI Flat Design */}
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

            {/* Tabla Corporativa de Datos */}
            <div className="mt-8 bg-white rounded shadow-sm border border-gray-200 overflow-hidden">
                <div className="p-5 border-b border-gray-200 bg-gray-50">
                    <h3 className="text-lg font-bold text-[#00274C]">Órdenes Recientes</h3>
                </div>
                <div className="overflow-x-auto">
                    <table className="w-full text-left border-collapse">
                        <thead>
                            {/* Cabecera de tabla azul marino */}
                            <tr className="bg-[#00274C] text-white text-sm uppercase tracking-wider">
                                <th className="p-4 font-semibold">ID Orden</th>
                                <th className="p-4 font-semibold">Cliente</th>
                                <th className="p-4 font-semibold">Técnico</th>
                                <th className="p-4 font-semibold">Estado</th>
                            </tr>
                        </thead>
                        <tbody className="divide-y divide-gray-200 text-gray-800">
                            {mockOrders.map((order) => (
                                <tr key={order.id} className="hover:bg-gray-50 transition duration-150">
                                    <td className="p-4 font-bold text-[#00274C]">{order.id}</td>
                                    <td className="p-4">{order.client}</td>
                                    <td className="p-4">{order.tech}</td>
                                    <td className="p-4">
                                        <span className={`px-3 py-1 rounded text-xs font-bold tracking-wide
                                            ${order.status === 'EN_EJECUCIÓN' ? 'bg-[#FDB913] text-[#00274C]' : 
                                              order.status === 'EN_DESPLAZAMIENTO' ? 'bg-blue-100 text-[#00274C]' : 
                                              'bg-gray-200 text-gray-700'}`}
                                        >
                                            {order.status}
                                        </span>
                                    </td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                </div>
            </div>
        </div>
    );
}