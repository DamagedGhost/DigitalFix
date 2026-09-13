export default function Reports() {
    const topServices = [
        { name: "Revisión Tablero Principal", requests: 145, percentage: "85%" },
        { name: "Mantenimiento Preventivo", requests: 98, percentage: "60%" },
        { name: "Cambio de Luminarias", requests: 64, percentage: "40%" },
        { name: "Certificación TE1", requests: 30, percentage: "20%" },
    ];

    return (
        <div className="space-y-6">
            <header className="border-b border-gray-300 pb-4">
                <h2 className="text-3xl font-bold text-[#00274C]">Reportería y KPIs</h2>
                <p className="text-gray-600 mt-1">Análisis de rendimiento y demanda de la red</p>
            </header>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                {/* Panel: Tiempo de Resolución */}
                <div className="bg-white p-6 rounded shadow-sm border border-gray-200">
                    <h3 className="font-bold text-[#00274C] mb-4">Tiempo Medio de Resolución (SLA)</h3>
                    <div className="flex items-center justify-center h-32">
                        <div className="text-center">
                            <span className="text-5xl font-black text-[#FDB913]">2.4</span>
                            <span className="text-xl text-gray-500 font-bold ml-1">Horas</span>
                            <p className="text-sm text-green-600 font-bold mt-2">↓ 12% vs mes anterior</p>
                        </div>
                    </div>
                </div>

                {/* Panel: Órdenes por Hora (Simulación) */}
                <div className="bg-white p-6 rounded shadow-sm border border-gray-200">
                    <h3 className="font-bold text-[#00274C] mb-4">Volumen de Órdenes (Últimas 24h)</h3>
                    <div className="flex items-end gap-2 h-32 mt-4">
                        {[4, 7, 5, 12, 18, 24, 15, 8, 3].map((val, idx) => (
                            <div key={idx} className="flex-1 bg-blue-100 rounded-t flex flex-col justify-end group hover:bg-[#FDB913] transition-colors relative">
                                <div className="w-full bg-[#00274C] rounded-t transition-all" style={{ height: `${(val / 24) * 100}%` }}></div>
                                {/* Tooltip simulado */}
                                <span className="absolute -top-6 left-1/2 -translate-x-1/2 text-xs font-bold text-[#00274C] opacity-0 group-hover:opacity-100">
                                    {val}
                                </span>
                            </div>
                        ))}
                    </div>
                    <div className="flex justify-between text-xs text-gray-400 mt-2 font-bold">
                        <span>00:00</span>
                        <span>12:00</span>
                        <span>23:59</span>
                    </div>
                </div>

                {/* Panel: Servicios Top */}
                <div className="bg-white p-6 rounded shadow-sm border border-gray-200 md:col-span-2">
                    <h3 className="font-bold text-[#00274C] mb-4">Servicios Más Requeridos</h3>
                    <div className="space-y-4">
                        {topServices.map((service, idx) => (
                            <div key={idx}>
                                <div className="flex justify-between text-sm font-bold text-gray-700 mb-1">
                                    <span>{service.name}</span>
                                    <span>{service.requests} solicitudes</span>
                                </div>
                                <div className="w-full bg-gray-200 rounded-full h-2.5">
                                    <div className="bg-[#00274C] h-2.5 rounded-full" style={{ width: service.percentage }}></div>
                                </div>
                            </div>
                        ))}
                    </div>
                </div>
            </div>
        </div>
    );
}