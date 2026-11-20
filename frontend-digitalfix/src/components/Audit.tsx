import { useCallback, useEffect, useMemo, useState } from "react";
import type { FormEvent } from "react";
import { useApi } from "../useApi";
import { extractApiError } from "../utils/apiError";

type AuditRecord = {
    id: number;
    auditId: string;
    eventTimestamp: string | null;
    sourceService: string | null;
    flow: string | null;
    outcome: string;
    reason: string | null;
    orderId: number | null;
    originalEventId: string;
    originalType: string | null;
    traceId: string | null;
    correlationId: string | null;
    receivedAt: string | null;
};

type QueryFilters = {
    outcome: string;
    orderId: string;
};

const FLOW_STYLES: Record<string, { label: string; icon: string; badge: string }> = {
    email: { label: "Email", icon: "📧", badge: "bg-blue-100 text-[#00274C]" },
    dispatch: { label: "Despacho", icon: "🚚", badge: "bg-purple-100 text-purple-800" },
    report: { label: "Reporte PDF", icon: "📄", badge: "bg-amber-100 text-amber-900" },
};

function formatDateTime(value: string | null): string {
    if (!value) return "—";
    const parsed = new Date(value);
    if (Number.isNaN(parsed.getTime())) return value;
    return new Intl.DateTimeFormat("es-CL", {
        dateStyle: "short",
        timeStyle: "medium",
    }).format(parsed);
}

function getRecordDate(record: AuditRecord): Date | null {
    const raw = record.eventTimestamp || record.receivedAt;
    if (!raw) return null;
    const parsed = new Date(raw);
    return Number.isNaN(parsed.getTime()) ? null : parsed;
}

export default function Audit() {
    const { fetchWithToken } = useApi();

    // Filtros que se consultan al backend (aplicados al enviar el formulario)
    const [appliedQuery, setAppliedQuery] = useState<QueryFilters>({ outcome: "", orderId: "" });
    const [orderIdInput, setOrderIdInput] = useState("");
    const [outcomeInput, setOutcomeInput] = useState("");

    // Filtros complementarios en cliente (flujo, rango de fechas y texto libre)
    const [flowFilter, setFlowFilter] = useState("");
    const [dateFrom, setDateFrom] = useState("");
    const [dateTo, setDateTo] = useState("");
    const [searchText, setSearchText] = useState("");

    const [records, setRecords] = useState<AuditRecord[]>([]);
    const [isLoading, setIsLoading] = useState(true);
    const [error, setError] = useState("");
    const [lastUpdated, setLastUpdated] = useState<Date | null>(null);
    const [autoRefresh, setAutoRefresh] = useState(true);
    const [selectedRecord, setSelectedRecord] = useState<AuditRecord | null>(null);

    const fetchAuditRecords = useCallback(async (query: QueryFilters) => {
        const params = new URLSearchParams();
        const trimmedOrder = query.orderId.trim();
        const parsedOrderId = trimmedOrder ? Number(trimmedOrder) : NaN;
        const hasValidOrderId = trimmedOrder !== "" && Number.isInteger(parsedOrderId) && parsedOrderId > 0;

        if (trimmedOrder !== "" && !hasValidOrderId) {
            throw new Error("El ID de orden debe ser un número entero positivo.");
        }

        // Si ambos filtros están activos, consultamos por orderId en el backend
        // (el servicio prioriza outcome sobre orderId) y refinamos outcome en cliente.
        if (hasValidOrderId) {
            params.set("orderId", String(parsedOrderId));
        } else if (query.outcome) {
            params.set("outcome", query.outcome);
        }

        const qs = params.toString();
        const endpoint = qs ? `/api/v1/audit?${qs}` : "/api/v1/audit";
        const response = await fetchWithToken(endpoint);

        if (!response.ok) {
            throw new Error(await extractApiError(response, "No se pudieron cargar los eventos de auditoría"));
        }

        const data: unknown = await response.json();
        return Array.isArray(data) ? (data as AuditRecord[]) : [];
    }, [fetchWithToken]);

    const loadAuditRecords = useCallback(async (query: QueryFilters, silent = false) => {
        if (!silent) setIsLoading(true);
        setError("");
        try {
            const list = await fetchAuditRecords(query);
            setRecords(list);
            setLastUpdated(new Date());
        } catch (err) {
            setError(err instanceof Error ? err.message : "Error al consultar auditoría");
        } finally {
            setIsLoading(false);
        }
    }, [fetchAuditRecords]);

    useEffect(() => {
        let cancelled = false;
        const run = async () => {
            try {
                const list = await fetchAuditRecords(appliedQuery);
                if (!cancelled) {
                    setRecords(list);
                    setError("");
                    setLastUpdated(new Date());
                }
            } catch (err) {
                if (!cancelled) {
                    setError(err instanceof Error ? err.message : "Error al consultar auditoría");
                }
            } finally {
                if (!cancelled) setIsLoading(false);
            }
        };
        void run();
        return () => {
            cancelled = true;
        };
    }, [appliedQuery, fetchAuditRecords]);

    useEffect(() => {
        if (!autoRefresh) return;
        const interval = setInterval(() => {
            void loadAuditRecords(appliedQuery, true);
        }, 10000);
        return () => clearInterval(interval);
    }, [autoRefresh, appliedQuery, loadAuditRecords]);

    const handleApplyFilters = (event: FormEvent) => {
        event.preventDefault();
        setIsLoading(true);
        setAppliedQuery({
            outcome: outcomeInput,
            orderId: orderIdInput.trim(),
        });
    };

    const handleClearFilters = () => {
        setOrderIdInput("");
        setOutcomeInput("");
        setFlowFilter("");
        setDateFrom("");
        setDateTo("");
        setSearchText("");
        setIsLoading(true);
        setAppliedQuery({ outcome: "", orderId: "" });
    };

    const filteredRecords = useMemo(() => {
        const search = searchText.trim().toLowerCase();
        const fromDate = dateFrom ? new Date(`${dateFrom}T00:00:00`) : null;
        const toDate = dateTo ? new Date(`${dateTo}T23:59:59.999`) : null;

        return records.filter((rec) => {
            if (appliedQuery.outcome && rec.outcome !== appliedQuery.outcome) {
                return false;
            }
            if (flowFilter && (rec.flow || "").toLowerCase() !== flowFilter) {
                return false;
            }
            if (fromDate || toDate) {
                const recordDate = getRecordDate(rec);
                if (!recordDate) return false;
                if (fromDate && recordDate < fromDate) return false;
                if (toDate && recordDate > toDate) return false;
            }
            if (search) {
                const haystack = [
                    rec.auditId,
                    rec.originalType,
                    rec.sourceService,
                    rec.flow,
                    rec.outcome,
                    rec.reason,
                    rec.traceId,
                    rec.correlationId,
                    rec.originalEventId,
                    rec.orderId !== null ? String(rec.orderId) : "",
                ]
                    .filter(Boolean)
                    .join(" ")
                    .toLowerCase();
                if (!haystack.includes(search)) return false;
            }
            return true;
        });
    }, [records, appliedQuery.outcome, flowFilter, dateFrom, dateTo, searchText]);

    const summary = useMemo(() => {
        let procesados = 0;
        let dlq = 0;
        const uniqueOrders = new Set<number>();

        for (const rec of filteredRecords) {
            if (rec.outcome === "PROCESADO") procesados++;
            if (rec.outcome === "ENVIADO_A_DLQ") dlq++;
            if (rec.orderId !== null && rec.orderId !== undefined) {
                uniqueOrders.add(rec.orderId);
            }
        }

        return {
            total: filteredRecords.length,
            procesados,
            dlq,
            uniqueOrders: uniqueOrders.size,
        };
    }, [filteredRecords]);

    const getFlowMeta = (flow: string | null) => {
        const key = (flow || "").toLowerCase();
        return FLOW_STYLES[key] || {
            label: flow || "N/A",
            icon: "📌",
            badge: "bg-gray-100 text-gray-700",
        };
    };

    const getOutcomeBadge = (outcome: string) => {
        if (outcome === "PROCESADO") {
            return "bg-green-100 text-green-800 border border-green-200";
        }
        if (outcome === "ENVIADO_A_DLQ") {
            return "bg-red-100 text-red-800 border border-red-200";
        }
        return "bg-gray-100 text-gray-700 border border-gray-200";
    };

    const hasActiveFilters = Boolean(
        appliedQuery.orderId || appliedQuery.outcome || flowFilter || dateFrom || dateTo || searchText
    );

    return (
        <div className="space-y-6">
            <header className="border-b border-gray-300 pb-4 flex flex-col md:flex-row md:items-end justify-between gap-4">
                <div>
                    <div className="flex flex-wrap items-center gap-3">
                        <h2 className="text-3xl font-bold text-[#00274C]">Auditoría y Trazabilidad</h2>
                        <span className="px-2.5 py-0.5 rounded-full text-xs font-bold bg-blue-100 text-[#00274C] border border-blue-200">
                            Kafka · audit.events · Solo lectura
                        </span>
                    </div>
                    <p className="text-gray-600 mt-1">
                        Registro inmutable en tiempo real de eventos procesados y desvíos a DLQ
                    </p>
                </div>

                <div className="flex flex-wrap items-center gap-3">
                    {lastUpdated && (
                        <span className="text-xs text-gray-500 font-medium">
                            Actualizado: {lastUpdated.toLocaleTimeString("es-CL")}
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
                        onClick={() => void loadAuditRecords(appliedQuery)}
                        disabled={isLoading}
                        className="bg-[#00274C] text-white text-sm font-bold px-4 py-2 rounded shadow hover:bg-blue-900 transition disabled:opacity-50 cursor-pointer"
                    >
                        {isLoading ? "Sincronizando..." : "↻ Refrescar"}
                    </button>
                </div>
            </header>

            {/* Tarjetas de resumen */}
            <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
                <div className="bg-white p-4 rounded shadow-sm border border-gray-200 border-l-4 border-l-[#00274C]">
                    <p className="text-xs text-gray-500 font-bold uppercase tracking-wider">Eventos Visibles</p>
                    <p className="text-3xl font-black text-[#00274C] mt-1">{summary.total}</p>
                    <p className="text-xs text-gray-400 mt-0.5">de {records.length} cargados (máx. 100)</p>
                </div>
                <div className="bg-white p-4 rounded shadow-sm border border-gray-200 border-l-4 border-l-green-600">
                    <p className="text-xs text-gray-500 font-bold uppercase tracking-wider">Procesados (ACK)</p>
                    <p className="text-3xl font-black text-green-700 mt-1">{summary.procesados}</p>
                    <p className="text-xs text-gray-400 mt-0.5">Ejecución exitosa</p>
                </div>
                <div className="bg-white p-4 rounded shadow-sm border border-gray-200 border-l-4 border-l-red-600">
                    <p className="text-xs text-gray-500 font-bold uppercase tracking-wider">Enviados a DLQ</p>
                    <p className="text-3xl font-black text-red-700 mt-1">{summary.dlq}</p>
                    <p className="text-xs text-gray-400 mt-0.5">Errores trazados</p>
                </div>
                <div className="bg-white p-4 rounded shadow-sm border border-gray-200 border-l-4 border-l-[#FDB913]">
                    <p className="text-xs text-gray-500 font-bold uppercase tracking-wider">Órdenes Trazadas</p>
                    <p className="text-3xl font-black text-[#00274C] mt-1">{summary.uniqueOrders}</p>
                    <p className="text-xs text-gray-400 mt-0.5">IDs únicos en vista</p>
                </div>
            </div>

            {error && (
                <div className="rounded border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-800 flex items-center justify-between gap-4">
                    <span className="font-bold">{error}</span>
                    <button
                        type="button"
                        onClick={() => void loadAuditRecords(appliedQuery)}
                        className="text-xs font-bold underline text-red-700 hover:text-red-900 cursor-pointer"
                    >
                        Reintentar
                    </button>
                </div>
            )}

            {/* Barra de Filtros */}
            <form
                onSubmit={handleApplyFilters}
                className="bg-white p-4 rounded shadow-sm border border-gray-200 space-y-4"
            >
                <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-6 gap-4 items-end">
                    <div>
                        <label className="block text-xs font-bold text-gray-500 uppercase mb-1">
                            ID Orden (API)
                        </label>
                        <input
                            type="number"
                            min="1"
                            value={orderIdInput}
                            onChange={(e) => setOrderIdInput(e.target.value)}
                            placeholder="Ej. 5"
                            className="w-full border border-gray-300 rounded px-3 py-2 text-sm focus:outline-none focus:border-[#00274C]"
                        />
                    </div>
                    <div>
                        <label className="block text-xs font-bold text-gray-500 uppercase mb-1">
                            Resultado (API)
                        </label>
                        <select
                            value={outcomeInput}
                            onChange={(e) => setOutcomeInput(e.target.value)}
                            className="w-full border border-gray-300 rounded px-3 py-2 text-sm focus:outline-none focus:border-[#00274C] bg-white"
                        >
                            <option value="">Todos</option>
                            <option value="PROCESADO">PROCESADO</option>
                            <option value="ENVIADO_A_DLQ">ENVIADO_A_DLQ</option>
                        </select>
                    </div>
                    <div>
                        <label className="block text-xs font-bold text-gray-500 uppercase mb-1">
                            Flujo
                        </label>
                        <select
                            value={flowFilter}
                            onChange={(e) => setFlowFilter(e.target.value)}
                            className="w-full border border-gray-300 rounded px-3 py-2 text-sm focus:outline-none focus:border-[#00274C] bg-white"
                        >
                            <option value="">Todos</option>
                            <option value="email">Email</option>
                            <option value="dispatch">Despacho</option>
                            <option value="report">Reporte PDF</option>
                        </select>
                    </div>
                    <div>
                        <label className="block text-xs font-bold text-gray-500 uppercase mb-1">
                            Fecha Desde
                        </label>
                        <input
                            type="date"
                            value={dateFrom}
                            onChange={(e) => setDateFrom(e.target.value)}
                            className="w-full border border-gray-300 rounded px-3 py-2 text-sm focus:outline-none focus:border-[#00274C]"
                        />
                    </div>
                    <div>
                        <label className="block text-xs font-bold text-gray-500 uppercase mb-1">
                            Fecha Hasta
                        </label>
                        <input
                            type="date"
                            value={dateTo}
                            onChange={(e) => setDateTo(e.target.value)}
                            className="w-full border border-gray-300 rounded px-3 py-2 text-sm focus:outline-none focus:border-[#00274C]"
                        />
                    </div>
                    <div className="flex gap-2">
                        <button
                            type="submit"
                            className="flex-1 bg-[#00274C] text-white font-bold px-4 py-2 rounded shadow hover:bg-blue-900 transition text-sm h-[38px] cursor-pointer"
                        >
                            Filtrar
                        </button>
                        {hasActiveFilters && (
                            <button
                                type="button"
                                onClick={handleClearFilters}
                                className="px-3 py-2 border border-gray-300 text-gray-700 font-bold rounded hover:bg-gray-100 transition text-sm h-[38px] cursor-pointer"
                                title="Limpiar todos los filtros"
                            >
                                Limpiar
                            </button>
                        )}
                    </div>
                </div>

                <div>
                    <input
                        type="text"
                        value={searchText}
                        onChange={(e) => setSearchText(e.target.value)}
                        placeholder="Buscar por tipo de evento (ej. WORKORDER_ASIGNADA), traceId, correlationId, servicio o motivo..."
                        className="w-full border border-gray-300 rounded px-3 py-2 text-sm focus:outline-none focus:border-[#00274C]"
                    />
                </div>
            </form>

            {/* Tabla de Eventos */}
            <div className="bg-white rounded shadow-sm border border-gray-200 overflow-hidden">
                <div className="overflow-x-auto">
                    <table className="w-full text-left border-collapse">
                        <thead>
                            <tr className="bg-[#00274C] text-white text-xs uppercase tracking-wider">
                                <th className="p-4 font-semibold">Fecha / Hora</th>
                                <th className="p-4 font-semibold">Orden</th>
                                <th className="p-4 font-semibold">Flujo</th>
                                <th className="p-4 font-semibold">Evento / Origen</th>
                                <th className="p-4 font-semibold">Resultado</th>
                                <th className="p-4 font-semibold">Motivo / Correlation ID</th>
                                <th className="p-4 font-semibold text-center">Trazabilidad</th>
                            </tr>
                        </thead>
                        <tbody className="divide-y divide-gray-200 text-gray-800 text-sm">
                            {isLoading && records.length === 0 ? (
                                <tr>
                                    <td colSpan={7} className="p-8 text-center text-gray-500 font-medium">
                                        Consultando registros de auditoría desde ms-digitalfix-audit...
                                    </td>
                                </tr>
                            ) : filteredRecords.length === 0 ? (
                                <tr>
                                    <td colSpan={7} className="p-8 text-center text-gray-500">
                                        {hasActiveFilters
                                            ? "No se encontraron eventos de auditoría que coincidan con los filtros aplicados."
                                            : "Aún no hay eventos registrados en audit.events. Crea o actualiza una orden para generar trazabilidad."}
                                    </td>
                                </tr>
                            ) : (
                                filteredRecords.map((ev) => {
                                    const flowMeta = getFlowMeta(ev.flow);
                                    const isDlq = ev.outcome === "ENVIADO_A_DLQ";
                                    return (
                                        <tr
                                            key={ev.id}
                                            className={`transition duration-150 ${isDlq ? "bg-red-50/40 hover:bg-red-50" : "hover:bg-gray-50"}`}
                                        >
                                            <td className="p-4 font-mono text-xs whitespace-nowrap text-gray-700">
                                                <div>{formatDateTime(ev.eventTimestamp || ev.receivedAt)}</div>
                                                <div className="text-[11px] text-gray-400">#{ev.id}</div>
                                            </td>
                                            <td className="p-4 whitespace-nowrap">
                                                {ev.orderId !== null ? (
                                                    <span className="font-bold text-[#00274C]">#{ev.orderId}</span>
                                                ) : (
                                                    <span className="text-gray-400 text-xs">Sin orden</span>
                                                )}
                                            </td>
                                            <td className="p-4 whitespace-nowrap">
                                                <span className={`inline-flex items-center gap-1 px-2.5 py-1 rounded text-xs font-bold ${flowMeta.badge}`}>
                                                    <span>{flowMeta.icon}</span>
                                                    <span>{flowMeta.label}</span>
                                                </span>
                                            </td>
                                            <td className="p-4">
                                                <div className="font-mono text-xs font-bold text-gray-800">
                                                    {ev.originalType || "EVENTO"}
                                                </div>
                                                <div className="text-xs text-gray-500">
                                                    {ev.sourceService || "ms-digitalfix-notify"}
                                                </div>
                                            </td>
                                            <td className="p-4 whitespace-nowrap">
                                                <span className={`px-2.5 py-1 rounded text-xs font-bold ${getOutcomeBadge(ev.outcome)}`}>
                                                    {ev.outcome}
                                                </span>
                                            </td>
                                            <td className="p-4 max-w-xs">
                                                {ev.reason ? (
                                                    <p className="text-xs text-red-700 font-medium line-clamp-2" title={ev.reason}>
                                                        {ev.reason}
                                                    </p>
                                                ) : (
                                                    <span className="font-mono text-xs text-gray-500" title={ev.correlationId || ""}>
                                                        {ev.correlationId || "—"}
                                                    </span>
                                                )}
                                            </td>
                                            <td className="p-4 text-center whitespace-nowrap">
                                                <button
                                                    type="button"
                                                    onClick={() => setSelectedRecord(ev)}
                                                    className="text-[#00274C] hover:text-[#FDB913] font-bold text-xs uppercase tracking-wider transition cursor-pointer"
                                                >
                                                    Ver detalle
                                                </button>
                                            </td>
                                        </tr>
                                    );
                                })
                            )}
                        </tbody>
                    </table>
                </div>
            </div>

            {/* Modal de Detalle del Evento Kafka */}
            {selectedRecord && (
                <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 backdrop-blur-sm p-4">
                    <div className="bg-white rounded shadow-xl w-full max-w-2xl overflow-hidden border-t-4 border-[#FDB913]">
                        <div className="p-5 border-b border-gray-200 bg-gray-50 flex justify-between items-center">
                            <div>
                                <h3 className="text-xl font-bold text-[#00274C]">
                                    Detalle de Evento de Auditoría #{selectedRecord.id}
                                </h3>
                                <p className="text-xs text-gray-500 font-mono mt-0.5">
                                    auditId: {selectedRecord.auditId}
                                </p>
                            </div>
                            <button
                                type="button"
                                onClick={() => setSelectedRecord(null)}
                                className="text-gray-400 hover:text-red-500 font-bold text-2xl leading-none transition cursor-pointer"
                                aria-label="Cerrar"
                            >
                                ×
                            </button>
                        </div>

                        <div className="p-6 space-y-4 text-sm max-h-[75vh] overflow-y-auto">
                            <div className="flex flex-wrap gap-2 items-center">
                                <span className={`px-3 py-1 rounded text-xs font-bold ${getOutcomeBadge(selectedRecord.outcome)}`}>
                                    {selectedRecord.outcome}
                                </span>
                                <span className={`px-3 py-1 rounded text-xs font-bold ${getFlowMeta(selectedRecord.flow).badge}`}>
                                    {getFlowMeta(selectedRecord.flow).icon} {getFlowMeta(selectedRecord.flow).label}
                                </span>
                                {selectedRecord.orderId !== null && (
                                    <span className="px-3 py-1 rounded text-xs font-bold bg-gray-100 text-[#00274C]">
                                        Orden #{selectedRecord.orderId}
                                    </span>
                                )}
                            </div>

                            {selectedRecord.reason && (
                                <div className="p-3 rounded bg-red-50 border border-red-200 text-red-900">
                                    <p className="text-xs font-bold uppercase text-red-700 mb-1">Motivo registrado</p>
                                    <p className="text-sm">{selectedRecord.reason}</p>
                                </div>
                            )}

                            <dl className="grid grid-cols-1 sm:grid-cols-2 gap-4 bg-gray-50 p-4 rounded border border-gray-200">
                                <div>
                                    <dt className="text-xs font-bold uppercase text-gray-500">Tipo de Evento Original</dt>
                                    <dd className="font-mono text-xs text-gray-900 mt-1 break-all">
                                        {selectedRecord.originalType || "—"}
                                    </dd>
                                </div>
                                <div>
                                    <dt className="text-xs font-bold uppercase text-gray-500">Servicio Productor</dt>
                                    <dd className="font-mono text-xs text-gray-900 mt-1">
                                        {selectedRecord.sourceService || "—"}
                                    </dd>
                                </div>
                                <div>
                                    <dt className="text-xs font-bold uppercase text-gray-500">Timestamp del Evento</dt>
                                    <dd className="font-mono text-xs text-gray-900 mt-1">
                                        {formatDateTime(selectedRecord.eventTimestamp)}
                                    </dd>
                                </div>
                                <div>
                                    <dt className="text-xs font-bold uppercase text-gray-500">Persistido en Auditoría</dt>
                                    <dd className="font-mono text-xs text-gray-900 mt-1">
                                        {formatDateTime(selectedRecord.receivedAt)}
                                    </dd>
                                </div>
                                <div className="sm:col-span-2">
                                    <dt className="text-xs font-bold uppercase text-gray-500">Original Event ID</dt>
                                    <dd className="font-mono text-xs text-gray-900 mt-1 break-all">
                                        {selectedRecord.originalEventId || "—"}
                                    </dd>
                                </div>
                                <div>
                                    <dt className="text-xs font-bold uppercase text-gray-500">Trace ID</dt>
                                    <dd className="font-mono text-xs text-gray-900 mt-1 break-all">
                                        {selectedRecord.traceId || "—"}
                                    </dd>
                                </div>
                                <div>
                                    <dt className="text-xs font-bold uppercase text-gray-500">Correlation ID (Partition Key)</dt>
                                    <dd className="font-mono text-xs text-gray-900 mt-1 break-all">
                                        {selectedRecord.correlationId || "—"}
                                    </dd>
                                </div>
                            </dl>
                        </div>

                        <div className="p-4 border-t border-gray-200 bg-gray-50 flex justify-end">
                            <button
                                type="button"
                                onClick={() => setSelectedRecord(null)}
                                className="px-4 py-2 bg-[#00274C] text-white rounded font-bold text-sm hover:bg-blue-900 transition cursor-pointer"
                            >
                                Cerrar
                            </button>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
}
