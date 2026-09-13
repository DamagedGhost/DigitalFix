export default function Audit() {
    const mockEvents = [
        { id: "EV-901", orderId: "ORD-001", action: "ORDEN_CREADA", user: "jperez@cliente.cl", date: "2026-09-11 08:30:12" },
        { id: "EV-902", orderId: "ORD-001", action: "ASIGNACION_TECNICO", user: "admin@digitalfix.cl", date: "2026-09-11 09:15:00" },
        { id: "EV-903", orderId: "ORD-002", action: "CAMBIO_ESTADO", user: "mrodriguez@tech.cl", date: "2026-09-11 10:05:45" },
        { id: "EV-904", orderId: "ORD-002", action: "STOCK_DESCONTADO", user: "sistema", date: "2026-09-11 10:05:46" },
    ];

    return (
        <div className="space-y-6">
            <header className="border-b border-gray-300 pb-4">
                <h2 className="text-3xl font-bold text-[#00274C]">Auditoría y Trazabilidad</h2>
                <p className="text-gray-600 mt-1">Registro inmutable de eventos del sistema</p>
            </header>

            {/* Barra de Filtros (Solo lectura/maqueta visual) */}
            <div className="bg-white p-4 rounded shadow-sm border border-gray-200 flex flex-wrap gap-4 items-end">
                <div className="flex-1 min-w-[200px]">
                    <label className="block text-xs font-bold text-gray-500 uppercase mb-1">Usuario / Actor</label>
                    <input type="text" placeholder="Ej. admin@..." className="w-full border border-gray-300 rounded px-3 py-2 text-sm focus:outline-none focus:border-[#00274C]" />
                </div>
                <div className="flex-1 min-w-[150px]">
                    <label className="block text-xs font-bold text-gray-500 uppercase mb-1">Fecha Desde</label>
                    <input type="date" className="w-full border border-gray-300 rounded px-3 py-2 text-sm focus:outline-none focus:border-[#00274C]" />
                </div>
                <div className="flex-1 min-w-[150px]">
                    <label className="block text-xs font-bold text-gray-500 uppercase mb-1">Fecha Hasta</label>
                    <input type="date" className="w-full border border-gray-300 rounded px-3 py-2 text-sm focus:outline-none focus:border-[#00274C]" />
                </div>
                <div className="flex-1 min-w-[200px]">
                    <label className="block text-xs font-bold text-gray-500 uppercase mb-1">Tipo de Evento</label>
                    <select className="w-full border border-gray-300 rounded px-3 py-2 text-sm focus:outline-none focus:border-[#00274C] bg-white">
                        <option>Todos</option>
                        <option>Creación</option>
                        <option>Cambio de Estado</option>
                        <option>Modificación de Stock</option>
                    </select>
                </div>
                <button className="bg-[#00274C] text-white font-bold px-6 py-2 rounded shadow hover:bg-blue-900 transition h-[38px]">
                    Filtrar
                </button>
            </div>

            {/* Tabla de Eventos */}
            <div className="bg-white rounded shadow-sm border border-gray-200 overflow-hidden">
                <div className="overflow-x-auto">
                    <table className="w-full text-left border-collapse">
                        <thead>
                            <tr className="bg-[#00274C] text-white text-xs uppercase tracking-wider">
                                <th className="p-4 font-semibold">ID Evento</th>
                                <th className="p-4 font-semibold">Fecha / Hora</th>
                                <th className="p-4 font-semibold">ID Orden</th>
                                <th className="p-4 font-semibold">Acción Realizada</th>
                                <th className="p-4 font-semibold">Actor</th>
                            </tr>
                        </thead>
                        <tbody className="divide-y divide-gray-200 text-gray-800 text-sm font-mono">
                            {mockEvents.map((ev) => (
                                <tr key={ev.id} className="hover:bg-gray-50 transition duration-150">
                                    <td className="p-4 text-gray-500">{ev.id}</td>
                                    <td className="p-4">{ev.date}</td>
                                    <td className="p-4 font-bold text-[#00274C]">{ev.orderId}</td>
                                    <td className="p-4">
                                        <span className="bg-gray-200 text-gray-700 px-2 py-1 rounded text-xs font-bold">
                                            {ev.action}
                                        </span>
                                    </td>
                                    <td className="p-4">{ev.user}</td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                </div>
            </div>
        </div>
    );
}