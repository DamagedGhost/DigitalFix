import { useCallback, useEffect, useMemo, useState } from "react";
import { useApi } from "../useApi";
import { extractApiError } from "../utils/apiError";

type OrdersReport = {
    totalOrdenes: number;
    porEstado: Record<string, number>;
    porTecnico: Record<string, number>;
    generadoEn: string;
};

type StatusMeta = {
    key: string;
    label: string;
    shortLabel: string;
    barClass: string;
    badgeClass: string;
};

const KNOWN_STATUSES: StatusMeta[] = [
    {
        key: "CREADA",
        label: "Creada",
        shortLabel: "Creada",
        barClass: "bg-gray-500",
        badgeClass: "bg-gray-200 text-gray-800",
    },
    {
        key: "ASIGNADA",
        label: "Asignada",
        shortLabel: "Asignada",
        barClass: "bg-blue-600",
        badgeClass: "bg-blue-100 text-[#00274C]",
    },
    {
        key: "EN_DESPLAZAMIENTO",
        label: "En Desplazamiento",
        shortLabel: "Desplaz.",
        barClass: "bg-purple-600",
        badgeClass: "bg-purple-100 text-purple-800",
    },
    {
        key: "EN_EJECUCION",
        label: "En Ejecución",
        shortLabel: "Ejecución",
        barClass: "bg-[#FDB913]",
        badgeClass: "bg-[#FDB913] text-[#00274C]",
    },
    {
        key: "CERRADA",
        label: "Cerrada",
        shortLabel: "Cerrada",
        barClass: "bg-green-600",
        badgeClass: "bg-green-100 text-green-800",
    },
    {
        key: "CANCELADA",
        label: "Cancelada",
        shortLabel: "Cancelada",
        barClass: "bg-red-600",
        badgeClass: "bg-red-100 text-red-800",
    },
];

function formatTimestamp(isoString: string | undefined): string {
    if (!isoString) return "—";
    const parsed = new Date(isoString);
    if (Number.isNaN(parsed.getTime())) return isoString;
    return new Intl.DateTimeFormat("es-CL", {
        dateStyle: "short",
        timeStyle: "medium",
    }).format(parsed);
}

export default function Reports() {
    const { fetchWithToken } = useApi();

    const [report, setReport] = useState<OrdersReport | null>(null);
    const [isLoading, setIsLoading] = useState(true);
    const [error, setError] = useState("");
    const [autoRefresh, setAutoRefresh] = useState(true);

    const fetchReportData = useCallback(async (): Promise<OrdersReport> => {
        const response = await fetchWithToken("/api/v1/reports/orders");
        if (!response.ok) {
            throw new Error(await extractApiError(response, "No se pudo cargar el reporte de órdenes"));
        }
        const data = (await response.json()) as OrdersReport;
        return {
            totalOrdenes: Number(data.totalOrdenes ?? 0),
            porEstado: data.porEstado ?? {},
            porTecnico: data.porTecnico ?? {},
            generadoEn: data.generadoEn ?? new Date().toISOString(),
        };
    }, [fetchWithToken]);

    const loadReport = useCallback(async (silent = false) => {
        if (!silent) setIsLoading(true);
        setError("");
        try {
            const parsed = await fetchReportData();
            setReport(parsed);
        } catch (err) {
            setError(err instanceof Error ? err.message : "Error al cargar reportes");
        } finally {
            setIsLoading(false);
        }
    }, [fetchReportData]);

    useEffect(() => {
        let cancelled = false;
        const run = async () => {
            try {
                const parsed = await fetchReportData();
                if (!cancelled) {
                    setReport(parsed);
                    setError("");
                }
            } catch (err) {
                if (!cancelled) {
                    setError(err instanceof Error ? err.message : "Error al cargar reportes");
                }
            } finally {
                if (!cancelled) setIsLoading(false);
            }
        };
        void run();
        return () => {
            cancelled = true;
        };
    }, [fetchReportData]);

    useEffect(() => {
        if (!autoRefresh) return;
        const interval = setInterval(() => {
            void loadReport(true);
        }, 10000);
        return () => clearInterval(interval);
    }, [autoRefresh, loadReport]);

    const statusRows = useMemo(() => {
        const porEstado = report?.porEstado ?? {};
        const knownKeys = new Set(KNOWN_STATUSES.map((s) => s.key));

        const rows = KNOWN_STATUSES.map((meta) => ({
            ...meta,
            count: Number(porEstado[meta.key] ?? 0),
        }));

        // Incluir cualquier estado adicional que devuelva el backend
        for (const [key, value] of Object.entries(porEstado)) {
            if (!knownKeys.has(key)) {
                rows.push({
                    key,
                    label: key.replace(/_/g, " "),
                    shortLabel: key.slice(0, 10),
                    barClass: "bg-[#00274C]",
                    badgeClass: "bg-gray-100 text-gray-800",
                    count: Number(value ?? 0),
                });
            }
        }
        return rows;
    }, [report]);

    const technicianRows = useMemo(() => {
        const porTecnico = report?.porTecnico ?? {};
        return Object.entries(porTecnico)
            .map(([name, count]) => ({ name, count: Number(count ?? 0) }))
            .sort((a, b) => b.count - a.count);
    }, [report]);

    const kpis = useMemo(() => {
        const total = report?.totalOrdenes ?? 0;
        const porEstado = report?.porEstado ?? {};
        const pendientes = Number(porEstado.CREADA ?? 0) + Number(porEstado.ASIGNADA ?? 0);
        const activas = Number(porEstado.EN_DESPLAZAMIENTO ?? 0) + Number(porEstado.EN_EJECUCION ?? 0);
        const cerradas = Number(porEstado.CERRADA ?? 0);
        const canceladas = Number(porEstado.CANCELADA ?? 0);
        const cierrePct = total > 0 ? Math.round((cerradas / total) * 100) : 0;
        const totalConTecnico = technicianRows.reduce((sum, t) => sum + t.count, 0);
        const sinTecnico = Math.max(0, total - totalConTecnico);

        return {
            total,
            pendientes,
            activas,
            cerradas,
            canceladas,
            cierrePct,
            totalConTecnico,
            sinTecnico,
        };
    }, [report, technicianRows]);

    const maxStatusCount = useMemo(
        () => Math.max(1, ...statusRows.map((row) => row.count)),
        [statusRows]
    );

    const maxTechnicianCount = useMemo(
        () => Math.max(1, ...technicianRows.map((row) => row.count)),
        [technicianRows]
    );

    return (
        <div className="space-y-6">
            <header className="border-b border-gray-300 pb-4 flex flex-col md:flex-row md:items-end justify-between gap-4">
                <div>
                    <div className="flex flex-wrap items-center gap-3">
                        <h2 className="text-3xl font-bold text-[#00274C]">Reportería y KPIs en Vivo</h2>
                        <span className="px-2.5 py-0.5 rounded-full text-xs font-bold bg-amber-100 text-amber-900 border border-amber-300">
                            Kafka · workorders.events · Streaming
                        </span>
                    </div>
                    <p className="text-gray-600 mt-1">
                        Agregaciones en tiempo cuasi real consumidas por <code className="font-mono text-xs bg-gray-200 px-1.5 py-0.5 rounded">ms-digitalfix-report</code>
                    </p>
                </div>

                <div className="flex flex-wrap items-center gap-3">
                    {report?.generadoEn && (
                        <span className="text-xs text-gray-500 font-medium">
                            Generado: {formatTimestamp(report.generadoEn)}
                        </span>
                    )}
                    <label className="flex items-center gap-2 text-xs font-bold text-gray-700 bg-white border border-gray-300 rounded px-3 py-2 cursor-pointer select-none">
                        <input
                            type="checkbox"
                            checked={autoRefresh}
                            onChange={(e) => setAutoRefresh(e.target.checked)}
                            className="accent-[#00274C]"
                        />
                        Auto-refresco (10s)
                    </label>
                    <button
                        type="button"
                        onClick={() => void loadReport()}
                        disabled={isLoading}
                        className="bg-[#00274C] text-white text-sm font-bold px-4 py-2 rounded shadow hover:bg-blue-900 transition disabled:opacity-50 cursor-pointer"
                    >
                        {isLoading ? "Actualizando..." : "↻ Actualizar"}
                    </button>
                </div>
            </header>

            {error && (
                <div className="rounded border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-800 flex items-center justify-between gap-4">
                    <span className="font-bold">{error}</span>
                    <button
                        type="button"
                        onClick={() => void loadReport()}
                        className="text-xs font-bold underline text-red-700 hover:text-red-900 cursor-pointer"
                    >
                        Reintentar
                    </button>
                </div>
            )}

            {/* Tarjetas KPI principales */}
            <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
                <div className="bg-white p-5 rounded shadow-sm border border-gray-200 border-l-4 border-l-[#00274C]">
                    <p className="text-xs text-gray-500 font-bold uppercase tracking-wider">Total Órdenes</p>
                    <p className="text-4xl font-black text-[#00274C] mt-1">
                        {isLoading && !report ? "…" : kpis.total}
                    </p>
                    <p className="text-xs text-gray-400 mt-1">Snapshots sincronizados por Kafka</p>
                </div>
                <div className="bg-white p-5 rounded shadow-sm border border-gray-200 border-l-4 border-l-[#FDB913]">
                    <p className="text-xs text-gray-500 font-bold uppercase tracking-wider">En Terreno / Activas</p>
                    <p className="text-4xl font-black text-[#00274C] mt-1">
                        {isLoading && !report ? "…" : kpis.activas}
                    </p>
                    <p className="text-xs text-gray-400 mt-1">En desplazamiento o ejecución</p>
                </div>
                <div className="bg-white p-5 rounded shadow-sm border border-gray-200 border-l-4 border-l-blue-500">
                    <p className="text-xs text-gray-500 font-bold uppercase tracking-wider">Pendientes</p>
                    <p className="text-4xl font-black text-[#00274C] mt-1">
                        {isLoading && !report ? "…" : kpis.pendientes}
                    </p>
                    <p className="text-xs text-gray-400 mt-1">Creadas o asignadas por iniciar</p>
                </div>
                <div className="bg-white p-5 rounded shadow-sm border border-gray-200 border-l-4 border-l-green-600">
                    <p className="text-xs text-gray-500 font-bold uppercase tracking-wider">Cerradas</p>
                    <div className="flex items-baseline gap-2 mt-1">
                        <p className="text-4xl font-black text-green-700">
                            {isLoading && !report ? "…" : kpis.cerradas}
                        </p>
                        <span className="text-sm font-bold text-green-700">({kpis.cierrePct}%)</span>
                    </div>
                    <p className="text-xs text-gray-400 mt-1">
                        {kpis.canceladas > 0 ? `Canceladas: ${kpis.canceladas}` : "Tasa de resolución actual"}
                    </p>
                </div>
            </div>

            {isLoading && !report ? (
                <div className="bg-white p-10 rounded shadow-sm border border-gray-200 text-center text-gray-500 font-medium">
                    Cargando analítica en streaming desde ms-digitalfix-report...
                </div>
            ) : !report || report.totalOrdenes === 0 ? (
                <div className="bg-white p-10 rounded shadow-sm border border-gray-200 text-center space-y-2">
                    <p className="text-lg font-bold text-[#00274C]">Sin órdenes registradas en el modelo de lectura</p>
                    <p className="text-sm text-gray-500 max-w-lg mx-auto">
                        Cuando se cree o actualice una orden de trabajo, <code className="font-mono text-xs bg-gray-100 px-1 rounded">ms-digitalfix-workorders</code> publicará el evento en el tópico <code className="font-mono text-xs bg-gray-100 px-1 rounded">workorders.events</code> y este panel se actualizará automáticamente.
                    </p>
                </div>
            ) : (
                <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
                    {/* Panel 1: Gráfico y desglose por estado */}
                    <div className="bg-white p-6 rounded shadow-sm border border-gray-200 space-y-6">
                        <div className="flex items-center justify-between">
                            <div>
                                <h3 className="font-bold text-[#00274C] text-lg">Órdenes por Estado</h3>
                                <p className="text-xs text-gray-500">Distribución actual del flujo operativo</p>
                            </div>
                            <span className="text-xs font-bold bg-gray-100 text-gray-600 px-2.5 py-1 rounded">
                                Total: {kpis.total}
                            </span>
                        </div>

                        {/* Gráfico de barras verticales */}
                        <div className="bg-gray-50 rounded border border-gray-200 p-4 pt-8">
                            <div className="flex items-end gap-3 h-40">
                                {statusRows.map((row) => {
                                    const heightPct = row.count > 0
                                        ? Math.max(8, Math.round((row.count / maxStatusCount) * 100))
                                        : 0;
                                    return (
                                        <div key={row.key} className="flex-1 flex flex-col items-center justify-end h-full">
                                            <span className="text-xs font-black text-[#00274C] mb-1">
                                                {row.count}
                                            </span>
                                            <div className="w-full max-w-[44px] h-full flex items-end bg-gray-200/70 rounded-t overflow-hidden">
                                                <div
                                                    className={`w-full ${row.barClass} rounded-t transition-all duration-300`}
                                                    style={{ height: `${heightPct}%` }}
                                                    title={`${row.label}: ${row.count}`}
                                                />
                                            </div>
                                        </div>
                                    );
                                })}
                            </div>
                            <div className="flex gap-3 mt-2 pt-2 border-t border-gray-200">
                                {statusRows.map((row) => (
                                    <div key={row.key} className="flex-1 text-center text-[11px] font-bold text-gray-600 truncate" title={row.label}>
                                        {row.shortLabel}
                                    </div>
                                ))}
                            </div>
                        </div>

                        {/* Desglose porcentual */}
                        <div className="space-y-3">
                            {statusRows.map((row) => {
                                const pct = kpis.total > 0 ? Math.round((row.count / kpis.total) * 100) : 0;
                                return (
                                    <div key={row.key}>
                                        <div className="flex items-center justify-between text-sm mb-1">
                                            <span className={`px-2 py-0.5 rounded text-xs font-bold ${row.badgeClass}`}>
                                                {row.label}
                                            </span>
                                            <span className="font-bold text-gray-700">
                                                {row.count} <span className="text-xs font-normal text-gray-500">({pct}%)</span>
                                            </span>
                                        </div>
                                        <div className="w-full bg-gray-200 rounded-full h-2">
                                            <div
                                                className={`${row.barClass} h-2 rounded-full transition-all duration-300`}
                                                style={{ width: `${pct}%` }}
                                            />
                                        </div>
                                    </div>
                                );
                            })}
                        </div>
                    </div>

                    {/* Panel 2: Carga de trabajo por técnico */}
                    <div className="bg-white p-6 rounded shadow-sm border border-gray-200 flex flex-col justify-between space-y-6">
                        <div className="space-y-4">
                            <div className="flex items-center justify-between">
                                <div>
                                    <h3 className="font-bold text-[#00274C] text-lg">Órdenes por Técnico</h3>
                                    <p className="text-xs text-gray-500">Carga asignada y atendida por cuadrilla en terreno</p>
                                </div>
                                <span className="text-xs font-bold bg-blue-50 text-[#00274C] px-2.5 py-1 rounded border border-blue-100">
                                    {technicianRows.length} {technicianRows.length === 1 ? "técnico" : "técnicos"}
                                </span>
                            </div>

                            {technicianRows.length === 0 ? (
                                <div className="p-8 text-center bg-gray-50 rounded border border-dashed border-gray-300 text-sm text-gray-500">
                                    Aún no hay órdenes con técnico asignado en los snapshots de Kafka.
                                </div>
                            ) : (
                                <div className="space-y-4">
                                    {technicianRows.map((tech, index) => {
                                        const barPct = Math.round((tech.count / maxTechnicianCount) * 100);
                                        const sharePct = kpis.totalConTecnico > 0
                                            ? Math.round((tech.count / kpis.totalConTecnico) * 100)
                                            : 0;
                                        return (
                                            <div key={tech.name} className="space-y-1.5">
                                                <div className="flex items-center justify-between text-sm">
                                                    <div className="flex items-center gap-2 min-w-0">
                                                        <span className="w-6 h-6 rounded-full bg-[#00274C] text-[#FDB913] text-xs font-black flex items-center justify-center shrink-0">
                                                            {index + 1}
                                                        </span>
                                                        <span className="font-bold text-gray-800 truncate">{tech.name}</span>
                                                    </div>
                                                    <span className="font-bold text-[#00274C] whitespace-nowrap ml-2">
                                                        {tech.count} {tech.count === 1 ? "orden" : "órdenes"}{" "}
                                                        <span className="text-xs font-normal text-gray-500">({sharePct}%)</span>
                                                    </span>
                                                </div>
                                                <div className="w-full bg-gray-200 rounded-full h-2.5 overflow-hidden">
                                                    <div
                                                        className="bg-[#00274C] h-2.5 rounded-full transition-all duration-300"
                                                        style={{ width: `${barPct}%` }}
                                                    />
                                                </div>
                                            </div>
                                        );
                                    })}
                                </div>
                            )}
                        </div>

                        <div className="pt-4 border-t border-gray-200 flex flex-wrap justify-between gap-2 text-xs text-gray-600 bg-gray-50 -mx-6 -mb-6 px-6 py-4 rounded-b">
                            <span>
                                Con técnico asignado: <strong className="text-[#00274C]">{kpis.totalConTecnico}</strong>
                            </span>
                            <span>
                                Sin técnico asignado: <strong className="text-[#00274C]">{kpis.sinTecnico}</strong>
                            </span>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
}
