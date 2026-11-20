import { useCallback, useEffect, useState } from "react";
import type { FormEvent } from "react";
import { useApi } from "../useApi";
import { extractApiError } from "../utils/apiError";

type TopicInfo = {
    name: string;
    partitions: number;
    replicationFactor: number;
    cleanupPolicy: string | null;
    retentionMs: number | null;
    minInsyncReplicas: number | null;
    system: boolean;
};

type GroupOffsetInfo = {
    topic: string;
    partition: number;
    currentOffset: number;
    endOffset: number;
    lag: number;
};

type ConsumerGroupInfo = {
    groupId: string;
    state: string;
    members: number;
    totalLag: number;
    offsets: GroupOffsetInfo[];
};

const RETENTION_PRESETS = [
    { label: "1 día (86.400.000 ms)", value: "86400000" },
    { label: "3 días (259.200.000 ms)", value: "259200000" },
    { label: "7 días (604.800.000 ms)", value: "604800000" },
    { label: "14 días (1.209.600.000 ms)", value: "1209600000" },
    { label: "30 días (2.592.000.000 ms)", value: "2592000000" },
    { label: "Sin límite (-1)", value: "-1" },
];

function formatRetention(ms: number | null): string {
    if (ms === null || ms === undefined) return "Por defecto";
    if (ms === -1) return "Sin límite (-1)";
    const dayMs = 86_400_000;
    const hourMs = 3_600_000;
    if (ms >= dayMs && ms % dayMs === 0) {
        const days = ms / dayMs;
        return `${days} ${days === 1 ? "día" : "días"}`;
    }
    if (ms >= hourMs && ms % hourMs === 0) {
        const hours = ms / hourMs;
        return `${hours} ${hours === 1 ? "hora" : "horas"}`;
    }
    return `${ms.toLocaleString("es-CL")} ms`;
}

function getGroupStateBadge(state: string): string {
    const upper = (state || "").toUpperCase();
    if (upper === "STABLE") return "bg-green-100 text-green-800 border border-green-200";
    if (upper === "EMPTY") return "bg-gray-200 text-gray-700 border border-gray-300";
    if (upper.includes("REBALANCE")) return "bg-amber-100 text-amber-900 border border-amber-300";
    return "bg-red-100 text-red-800 border border-red-200";
}

export default function AdminKafkaPanel() {
    const { fetchWithToken } = useApi();

    const [activeTab, setActiveTab] = useState<"topics" | "groups">("topics");
    const [statusMsg, setStatusMsg] = useState<{ text: string; type: "success" | "error" | "" }>({
        text: "",
        type: "",
    });
    const [isBusy, setIsBusy] = useState(false);

    // Estado de tópicos
    const [topics, setTopics] = useState<TopicInfo[]>([]);
    const [isLoadingTopics, setIsLoadingTopics] = useState(true);
    const [topicsError, setTopicsError] = useState("");

    // Formulario de creación de tópico
    const [newName, setNewName] = useState("");
    const [newPartitions, setNewPartitions] = useState("3");
    const [newReplicas, setNewReplicas] = useState("3");
    const [newCleanupPolicy, setNewCleanupPolicy] = useState("delete");
    const [newRetentionMs, setNewRetentionMs] = useState("604800000");
    const [newMinIsr, setNewMinIsr] = useState("2");

    // Modal de edición de tópico
    const [editingTopic, setEditingTopic] = useState<TopicInfo | null>(null);
    const [editPartitions, setEditPartitions] = useState("");
    const [editCleanupPolicy, setEditCleanupPolicy] = useState("delete");
    const [editRetentionMs, setEditRetentionMs] = useState("");
    const [editMinIsr, setEditMinIsr] = useState("");

    // Estado de consumer groups
    const [groups, setGroups] = useState<ConsumerGroupInfo[]>([]);
    const [isLoadingGroups, setIsLoadingGroups] = useState(true);
    const [groupsError, setGroupsError] = useState("");
    const [expandedGroupId, setExpandedGroupId] = useState<string | null>(null);

    const showMessage = (text: string, type: "success" | "error") => {
        setStatusMsg({ text, type });
        setTimeout(() => setStatusMsg({ text: "", type: "" }), 6000);
    };

    const fetchTopicsData = useCallback(async (): Promise<TopicInfo[]> => {
        const response = await fetchWithToken("/api/v1/admin/kafka/topics");
        if (!response.ok) {
            throw new Error(await extractApiError(response, "No se pudieron cargar los tópicos de Kafka"));
        }
        const data: unknown = await response.json();
        return Array.isArray(data) ? (data as TopicInfo[]) : [];
    }, [fetchWithToken]);

    const fetchGroupsData = useCallback(async (): Promise<ConsumerGroupInfo[]> => {
        const response = await fetchWithToken("/api/v1/admin/kafka/groups");
        if (!response.ok) {
            throw new Error(await extractApiError(response, "No se pudieron cargar los consumer groups"));
        }
        const data: unknown = await response.json();
        return Array.isArray(data) ? (data as ConsumerGroupInfo[]) : [];
    }, [fetchWithToken]);

    const loadTopics = useCallback(async () => {
        setIsLoadingTopics(true);
        setTopicsError("");
        try {
            setTopics(await fetchTopicsData());
        } catch (err) {
            setTopicsError(err instanceof Error ? err.message : "Error al cargar tópicos");
        } finally {
            setIsLoadingTopics(false);
        }
    }, [fetchTopicsData]);

    const loadGroups = useCallback(async () => {
        setIsLoadingGroups(true);
        setGroupsError("");
        try {
            const list = await fetchGroupsData();
            setGroups(list);
            if (list.length > 0) {
                setExpandedGroupId((prev) => prev ?? list[0].groupId);
            }
        } catch (err) {
            setGroupsError(err instanceof Error ? err.message : "Error al cargar consumer groups");
        } finally {
            setIsLoadingGroups(false);
        }
    }, [fetchGroupsData]);

    useEffect(() => {
        let cancelled = false;
        const init = async () => {
            try {
                const topicList = await fetchTopicsData();
                if (!cancelled) {
                    setTopics(topicList);
                    setTopicsError("");
                }
            } catch (err) {
                if (!cancelled) {
                    setTopicsError(err instanceof Error ? err.message : "Error al cargar tópicos");
                }
            } finally {
                if (!cancelled) setIsLoadingTopics(false);
            }

            try {
                const groupList = await fetchGroupsData();
                if (!cancelled) {
                    setGroups(groupList);
                    setGroupsError("");
                    if (groupList.length > 0) {
                        setExpandedGroupId((prev) => prev ?? groupList[0].groupId);
                    }
                }
            } catch (err) {
                if (!cancelled) {
                    setGroupsError(err instanceof Error ? err.message : "Error al cargar consumer groups");
                }
            } finally {
                if (!cancelled) setIsLoadingGroups(false);
            }
        };
        void init();
        return () => {
            cancelled = true;
        };
    }, [fetchTopicsData, fetchGroupsData]);

    const handleCreateTopic = async (event: FormEvent) => {
        event.preventDefault();
        setIsBusy(true);
        try {
            const payload = {
                name: newName.trim(),
                partitions: Number(newPartitions),
                replicationFactor: Number(newReplicas),
                cleanupPolicy: newCleanupPolicy,
                retentionMs: Number(newRetentionMs),
                minInsyncReplicas: Number(newMinIsr),
            };
            const response = await fetchWithToken("/api/v1/admin/kafka/topics", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify(payload),
            });
            if (!response.ok) {
                throw new Error(await extractApiError(response, "No se pudo crear el tópico"));
            }
            showMessage(`Tópico '${payload.name}' creado correctamente.`, "success");
            setNewName("");
            await loadTopics();
        } catch (err) {
            showMessage(err instanceof Error ? err.message : "Error al crear tópico", "error");
        } finally {
            setIsBusy(false);
        }
    };

    const openEditModal = (topic: TopicInfo) => {
        setEditingTopic(topic);
        setEditPartitions(String(topic.partitions));
        setEditCleanupPolicy(topic.cleanupPolicy || "delete");
        setEditRetentionMs(topic.retentionMs !== null ? String(topic.retentionMs) : "604800000");
        setEditMinIsr(topic.minInsyncReplicas !== null ? String(topic.minInsyncReplicas) : "2");
    };

    const handleUpdateTopic = async (event: FormEvent) => {
        event.preventDefault();
        if (!editingTopic) return;

        const body: {
            partitions?: number;
            retentionMs?: number;
            cleanupPolicy?: string;
            minInsyncReplicas?: number;
        } = {};

        const parsedPartitions = Number(editPartitions);
        if (parsedPartitions > editingTopic.partitions) {
            body.partitions = parsedPartitions;
        }

        const parsedRetention = Number(editRetentionMs);
        if (!Number.isNaN(parsedRetention) && parsedRetention !== editingTopic.retentionMs) {
            body.retentionMs = parsedRetention;
        }

        if (editCleanupPolicy && editCleanupPolicy !== editingTopic.cleanupPolicy) {
            body.cleanupPolicy = editCleanupPolicy;
        }

        const parsedMinIsr = Number(editMinIsr);
        if (!Number.isNaN(parsedMinIsr) && parsedMinIsr !== editingTopic.minInsyncReplicas) {
            body.minInsyncReplicas = parsedMinIsr;
        }

        if (Object.keys(body).length === 0) {
            showMessage("No hay cambios pendientes para aplicar al tópico.", "error");
            return;
        }

        setIsBusy(true);
        try {
            const response = await fetchWithToken(
                `/api/v1/admin/kafka/topics/${encodeURIComponent(editingTopic.name)}`,
                {
                    method: "PATCH",
                    headers: { "Content-Type": "application/json" },
                    body: JSON.stringify(body),
                }
            );
            if (!response.ok) {
                throw new Error(await extractApiError(response, "No se pudo actualizar el tópico"));
            }
            showMessage(`Tópico '${editingTopic.name}' actualizado correctamente.`, "success");
            setEditingTopic(null);
            await loadTopics();
        } catch (err) {
            showMessage(err instanceof Error ? err.message : "Error al actualizar tópico", "error");
        } finally {
            setIsBusy(false);
        }
    };

    const handleDeleteTopic = async (topicName: string) => {
        if (!confirm(`¿Estás seguro de eliminar el tópico '${topicName}'?`)) return;
        setIsBusy(true);
        try {
            const response = await fetchWithToken(
                `/api/v1/admin/kafka/topics/${encodeURIComponent(topicName)}`,
                { method: "DELETE" }
            );
            if (!response.ok) {
                throw new Error(await extractApiError(response, "No se pudo eliminar el tópico"));
            }
            showMessage(`Tópico '${topicName}' eliminado.`, "success");
            await loadTopics();
        } catch (err) {
            showMessage(err instanceof Error ? err.message : "Error al eliminar tópico", "error");
        } finally {
            setIsBusy(false);
        }
    };

    const handleInspectGroup = async (groupId: string) => {
        setIsBusy(true);
        try {
            const response = await fetchWithToken(
                `/api/v1/admin/kafka/groups/${encodeURIComponent(groupId)}`
            );
            if (!response.ok) {
                throw new Error(await extractApiError(response, `No se pudo inspeccionar el grupo '${groupId}'`));
            }
            const detail = (await response.json()) as ConsumerGroupInfo;
            setGroups((prev) => prev.map((g) => (g.groupId === groupId ? detail : g)));
            setExpandedGroupId(groupId);
            showMessage(`Offsets y lag actualizados para '${groupId}'.`, "success");
        } catch (err) {
            showMessage(err instanceof Error ? err.message : "Error al inspeccionar grupo", "error");
        } finally {
            setIsBusy(false);
        }
    };

    const totalLagAllGroups = groups.reduce((acc, g) => acc + Number(g.totalLag ?? 0), 0);

    return (
        <div className="bg-white rounded shadow-sm border border-gray-200 overflow-hidden mt-6">
            <div className="bg-[#00274C] text-white border-b border-gray-200 flex flex-wrap items-center justify-between gap-2 px-4 py-3">
                <h3 className="font-bold flex items-center gap-2">
                    <span>📡</span> Consola Administrativa Kafka
                </h3>
                <div className="flex gap-2 text-xs">
                    <button
                        type="button"
                        onClick={() => setActiveTab("topics")}
                        className={`px-3 py-1.5 rounded font-bold transition cursor-pointer ${
                            activeTab === "topics"
                                ? "bg-[#FDB913] text-[#00274C]"
                                : "bg-blue-900/60 text-white hover:bg-blue-800"
                        }`}
                    >
                        Tópicos ({topics.length})
                    </button>
                    <button
                        type="button"
                        onClick={() => setActiveTab("groups")}
                        className={`px-3 py-1.5 rounded font-bold transition cursor-pointer ${
                            activeTab === "groups"
                                ? "bg-[#FDB913] text-[#00274C]"
                                : "bg-blue-900/60 text-white hover:bg-blue-800"
                        }`}
                    >
                        Consumer Groups ({groups.length})
                    </button>
                </div>
            </div>

            <div className="p-6 space-y-6">
                {statusMsg.text && (
                    <div
                        className={`p-3 rounded text-sm font-bold ${
                            statusMsg.type === "success"
                                ? "bg-green-100 text-green-800 border border-green-200"
                                : "bg-red-100 text-red-800 border border-red-200"
                        }`}
                    >
                        {statusMsg.text}
                    </div>
                )}

                {activeTab === "topics" ? (
                    <div className="space-y-6">
                        {/* Formulario crear tópico */}
                        <form
                            onSubmit={handleCreateTopic}
                            className="bg-gray-50 p-4 rounded border border-gray-200 space-y-3"
                        >
                            <h4 className="font-bold text-[#00274C] text-sm">Aprovisionar Nuevo Tópico Kafka</h4>
                            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-6 gap-3 items-end">
                                <div className="sm:col-span-2">
                                    <label className="block text-xs font-bold text-gray-600 mb-1">Nombre del Tópico</label>
                                    <input
                                        required
                                        type="text"
                                        value={newName}
                                        onChange={(e) => setNewName(e.target.value)}
                                        placeholder="Ej. workorders.analytics"
                                        className="w-full border border-gray-300 rounded px-3 py-2 text-sm bg-white focus:outline-none focus:border-[#00274C]"
                                    />
                                </div>
                                <div>
                                    <label className="block text-xs font-bold text-gray-600 mb-1">Particiones</label>
                                    <input
                                        required
                                        type="number"
                                        min="1"
                                        max="30"
                                        value={newPartitions}
                                        onChange={(e) => setNewPartitions(e.target.value)}
                                        className="w-full border border-gray-300 rounded px-3 py-2 text-sm bg-white"
                                    />
                                </div>
                                <div>
                                    <label className="block text-xs font-bold text-gray-600 mb-1">Réplicas</label>
                                    <input
                                        required
                                        type="number"
                                        min="1"
                                        max="5"
                                        value={newReplicas}
                                        onChange={(e) => setNewReplicas(e.target.value)}
                                        className="w-full border border-gray-300 rounded px-3 py-2 text-sm bg-white"
                                    />
                                </div>
                                <div>
                                    <label className="block text-xs font-bold text-gray-600 mb-1">Cleanup Policy</label>
                                    <select
                                        value={newCleanupPolicy}
                                        onChange={(e) => setNewCleanupPolicy(e.target.value)}
                                        className="w-full border border-gray-300 rounded px-3 py-2 text-sm bg-white"
                                    >
                                        <option value="delete">delete</option>
                                        <option value="compact">compact</option>
                                        <option value="compact,delete">compact,delete</option>
                                    </select>
                                </div>
                                <div>
                                    <label className="block text-xs font-bold text-gray-600 mb-1">Min ISR</label>
                                    <input
                                        required
                                        type="number"
                                        min="1"
                                        max={Number(newReplicas) || 3}
                                        value={newMinIsr}
                                        onChange={(e) => setNewMinIsr(e.target.value)}
                                        className="w-full border border-gray-300 rounded px-3 py-2 text-sm bg-white"
                                    />
                                </div>
                            </div>
                            <div className="flex flex-col sm:flex-row gap-3 sm:items-end justify-between">
                                <div className="sm:w-72">
                                    <label className="block text-xs font-bold text-gray-600 mb-1">Retención (retention.ms)</label>
                                    <select
                                        value={newRetentionMs}
                                        onChange={(e) => setNewRetentionMs(e.target.value)}
                                        className="w-full border border-gray-300 rounded px-3 py-2 text-sm bg-white"
                                    >
                                        {RETENTION_PRESETS.map((preset) => (
                                            <option key={preset.value} value={preset.value}>
                                                {preset.label}
                                            </option>
                                        ))}
                                    </select>
                                </div>
                                <button
                                    type="submit"
                                    disabled={isBusy || !newName.trim()}
                                    className="px-5 py-2 bg-[#FDB913] text-[#00274C] rounded font-bold text-sm hover:bg-yellow-500 shadow disabled:opacity-50 transition cursor-pointer"
                                >
                                    Crear Tópico
                                </button>
                            </div>
                        </form>

                        {/* Lista de tópicos */}
                        <div>
                            <div className="flex justify-between items-center mb-2">
                                <h4 className="font-bold text-[#00274C]">Tópicos en el Clúster Kafka</h4>
                                <button
                                    type="button"
                                    onClick={() => void loadTopics()}
                                    className="text-xs text-blue-600 hover:underline font-bold cursor-pointer"
                                >
                                    ↻ Refrescar Tópicos
                                </button>
                            </div>

                            {topicsError ? (
                                <div className="p-4 rounded bg-red-50 border border-red-200 text-sm text-red-800">
                                    {topicsError}
                                </div>
                            ) : isLoadingTopics && topics.length === 0 ? (
                                <p className="p-4 text-sm text-gray-500 text-center border border-gray-200 rounded">
                                    Consultando tópicos del clúster Kafka...
                                </p>
                            ) : topics.length === 0 ? (
                                <p className="p-4 text-sm text-gray-500 text-center border border-gray-200 rounded">
                                    No se encontraron tópicos en el clúster.
                                </p>
                            ) : (
                                <div className="border border-gray-200 rounded overflow-hidden">
                                    <ul className="divide-y divide-gray-200 text-sm">
                                        {topics.map((topic) => {
                                            const isDlt = topic.name.endsWith(".DLT");
                                            return (
                                                <li
                                                    key={topic.name}
                                                    className="p-4 flex flex-col md:flex-row md:justify-between md:items-center hover:bg-gray-50 gap-3"
                                                >
                                                    <div>
                                                        <div className="flex flex-wrap items-center gap-2">
                                                            <span className="font-mono font-bold text-[#00274C] text-base">
                                                                {topic.name}
                                                            </span>
                                                            {topic.system ? (
                                                                <span className="px-2 py-0.5 bg-blue-100 text-[#00274C] text-xs rounded font-bold">
                                                                    Sistema
                                                                </span>
                                                            ) : (
                                                                <span className="px-2 py-0.5 bg-gray-100 text-gray-700 text-xs rounded font-bold">
                                                                    Personalizado
                                                                </span>
                                                            )}
                                                            {isDlt && (
                                                                <span className="px-2 py-0.5 bg-red-100 text-red-800 text-xs rounded font-bold">
                                                                    Dead Letter Topic
                                                                </span>
                                                            )}
                                                        </div>
                                                        <div className="text-xs text-gray-600 mt-1.5 flex flex-wrap gap-x-4 gap-y-1">
                                                            <span>Particiones: <strong>{topic.partitions}</strong></span>
                                                            <span>Réplicas: <strong>{topic.replicationFactor}</strong></span>
                                                            <span>Política: <strong className="font-mono">{topic.cleanupPolicy || "delete"}</strong></span>
                                                            <span>Retención: <strong>{formatRetention(topic.retentionMs)}</strong></span>
                                                            <span>Min ISR: <strong>{topic.minInsyncReplicas ?? "—"}</strong></span>
                                                        </div>
                                                    </div>

                                                    <div className="flex flex-wrap gap-2">
                                                        <button
                                                            type="button"
                                                            onClick={() => openEditModal(topic)}
                                                            disabled={isBusy}
                                                            className="px-3 py-1 bg-blue-100 text-blue-800 hover:bg-blue-200 rounded font-bold text-xs transition cursor-pointer disabled:opacity-50"
                                                        >
                                                            Configurar
                                                        </button>
                                                        {!topic.system && (
                                                            <button
                                                                type="button"
                                                                onClick={() => void handleDeleteTopic(topic.name)}
                                                                disabled={isBusy}
                                                                className="px-3 py-1 bg-red-100 text-red-800 hover:bg-red-200 rounded font-bold text-xs transition cursor-pointer disabled:opacity-50"
                                                            >
                                                                Eliminar
                                                            </button>
                                                        )}
                                                    </div>
                                                </li>
                                            );
                                        })}
                                    </ul>
                                </div>
                            )}
                        </div>
                    </div>
                ) : (
                    <div className="space-y-4">
                        <div className="flex flex-wrap justify-between items-center gap-2">
                            <div className="flex flex-wrap items-center gap-3">
                                <h4 className="font-bold text-[#00274C]">Supervisión de Consumer Groups, Offsets y Lag</h4>
                                <span
                                    className={`px-2.5 py-0.5 rounded text-xs font-bold ${
                                        totalLagAllGroups === 0
                                            ? "bg-green-100 text-green-800"
                                            : "bg-amber-100 text-amber-900"
                                    }`}
                                >
                                    Lag total: {totalLagAllGroups}
                                </span>
                            </div>
                            <button
                                type="button"
                                onClick={() => void loadGroups()}
                                className="text-xs text-blue-600 hover:underline font-bold cursor-pointer"
                            >
                                ↻ Refrescar Grupos
                            </button>
                        </div>

                        {groupsError ? (
                            <div className="p-4 rounded bg-red-50 border border-red-200 text-sm text-red-800">
                                {groupsError}
                            </div>
                        ) : isLoadingGroups && groups.length === 0 ? (
                            <p className="p-4 text-sm text-gray-500 text-center border border-gray-200 rounded">
                                Consultando consumer groups en Kafka...
                            </p>
                        ) : groups.length === 0 ? (
                            <p className="p-4 text-sm text-gray-500 text-center border border-gray-200 rounded">
                                No hay consumer groups activos registrados en el clúster.
                            </p>
                        ) : (
                            <div className="space-y-3">
                                {groups.map((group) => {
                                    const isExpanded = expandedGroupId === group.groupId;
                                    return (
                                        <div key={group.groupId} className="border border-gray-200 rounded overflow-hidden">
                                            <div className="p-4 bg-gray-50 flex flex-col sm:flex-row sm:items-center justify-between gap-3">
                                                <div>
                                                    <div className="flex flex-wrap items-center gap-2">
                                                        <span className="font-mono font-bold text-[#00274C]">
                                                            {group.groupId}
                                                        </span>
                                                        <span className={`px-2 py-0.5 rounded text-xs font-bold ${getGroupStateBadge(group.state)}`}>
                                                            {group.state}
                                                        </span>
                                                    </div>
                                                    <div className="text-xs text-gray-600 mt-1 flex flex-wrap gap-4">
                                                        <span>Miembros activos: <strong>{group.members}</strong></span>
                                                        <span>
                                                            Lag acumulado:{" "}
                                                            <strong className={group.totalLag > 0 ? "text-red-700" : "text-green-700"}>
                                                                {group.totalLag}
                                                            </strong>
                                                        </span>
                                                        <span>Particiones: <strong>{group.offsets?.length ?? 0}</strong></span>
                                                    </div>
                                                </div>

                                                <div className="flex items-center gap-2">
                                                    <button
                                                        type="button"
                                                        onClick={() => void handleInspectGroup(group.groupId)}
                                                        disabled={isBusy}
                                                        className="px-3 py-1 bg-blue-100 text-blue-800 hover:bg-blue-200 rounded font-bold text-xs transition cursor-pointer disabled:opacity-50"
                                                    >
                                                        ↻ Inspeccionar
                                                    </button>
                                                    <button
                                                        type="button"
                                                        onClick={() =>
                                                            setExpandedGroupId(isExpanded ? null : group.groupId)
                                                        }
                                                        className="px-3 py-1 bg-gray-200 text-gray-700 hover:bg-gray-300 rounded font-bold text-xs transition cursor-pointer"
                                                    >
                                                        {isExpanded ? "Ocultar offsets" : "Ver offsets"}
                                                    </button>
                                                </div>
                                            </div>

                                            {isExpanded && (
                                                <div className="overflow-x-auto border-t border-gray-200">
                                                    {!group.offsets || group.offsets.length === 0 ? (
                                                        <p className="p-4 text-xs text-gray-500 text-center">
                                                            Este grupo todavía no tiene offsets confirmados.
                                                        </p>
                                                    ) : (
                                                        <table className="w-full text-left text-xs border-collapse">
                                                            <thead>
                                                                <tr className="bg-gray-100 text-gray-600 uppercase">
                                                                    <th className="p-2.5 pl-4 font-bold">Tópico</th>
                                                                    <th className="p-2.5 font-bold text-center">Partición</th>
                                                                    <th className="p-2.5 font-bold text-right">Current Offset</th>
                                                                    <th className="p-2.5 font-bold text-right">End Offset</th>
                                                                    <th className="p-2.5 pr-4 font-bold text-right">Lag</th>
                                                                </tr>
                                                            </thead>
                                                            <tbody className="divide-y divide-gray-200 font-mono">
                                                                {group.offsets.map((off) => (
                                                                    <tr key={`${off.topic}-${off.partition}`} className="hover:bg-gray-50">
                                                                        <td className="p-2.5 pl-4 font-bold text-[#00274C]">
                                                                            {off.topic}
                                                                        </td>
                                                                        <td className="p-2.5 text-center">{off.partition}</td>
                                                                        <td className="p-2.5 text-right">{off.currentOffset}</td>
                                                                        <td className="p-2.5 text-right">{off.endOffset}</td>
                                                                        <td className="p-2.5 pr-4 text-right">
                                                                            <span
                                                                                className={`px-2 py-0.5 rounded font-bold ${
                                                                                    off.lag > 0
                                                                                        ? "bg-red-100 text-red-800"
                                                                                        : "bg-green-100 text-green-800"
                                                                                }`}
                                                                            >
                                                                                {off.lag}
                                                                            </span>
                                                                        </td>
                                                                    </tr>
                                                                ))}
                                                            </tbody>
                                                        </table>
                                                    )}
                                                </div>
                                            )}
                                        </div>
                                    );
                                })}
                            </div>
                        )}
                    </div>
                )}
            </div>

            {/* Modal de configuración de tópico */}
            {editingTopic && (
                <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 backdrop-blur-sm p-4">
                    <form
                        onSubmit={handleUpdateTopic}
                        className="bg-white rounded shadow-xl w-full max-w-lg overflow-hidden border-t-4 border-[#FDB913]"
                    >
                        <div className="p-5 border-b border-gray-200 bg-gray-50 flex justify-between items-center">
                            <div>
                                <h3 className="text-lg font-bold text-[#00274C]">Configurar Tópico</h3>
                                <p className="text-xs font-mono text-gray-500">{editingTopic.name}</p>
                            </div>
                            <button
                                type="button"
                                onClick={() => setEditingTopic(null)}
                                className="text-gray-400 hover:text-red-500 font-bold text-2xl leading-none cursor-pointer"
                            >
                                ×
                            </button>
                        </div>

                        <div className="p-6 space-y-4 text-sm">
                            <div className="grid grid-cols-2 gap-4">
                                <div>
                                    <label className="block text-xs font-bold text-gray-700 mb-1">
                                        Particiones (actual: {editingTopic.partitions})
                                    </label>
                                    <input
                                        type="number"
                                        min={editingTopic.partitions}
                                        max="30"
                                        value={editPartitions}
                                        onChange={(e) => setEditPartitions(e.target.value)}
                                        className="w-full border border-gray-300 rounded px-3 py-2"
                                    />
                                    <p className="text-[11px] text-gray-500 mt-1">
                                        En Kafka solo se puede aumentar el número de particiones.
                                    </p>
                                </div>
                                <div>
                                    <label className="block text-xs font-bold text-gray-700 mb-1">
                                        Min In-Sync Replicas
                                    </label>
                                    <input
                                        type="number"
                                        min="1"
                                        max={Math.max(1, editingTopic.replicationFactor)}
                                        value={editMinIsr}
                                        onChange={(e) => setEditMinIsr(e.target.value)}
                                        className="w-full border border-gray-300 rounded px-3 py-2"
                                    />
                                </div>
                            </div>

                            <div>
                                <label className="block text-xs font-bold text-gray-700 mb-1">Cleanup Policy</label>
                                <select
                                    value={editCleanupPolicy}
                                    onChange={(e) => setEditCleanupPolicy(e.target.value)}
                                    className="w-full border border-gray-300 rounded px-3 py-2 bg-white"
                                >
                                    <option value="delete">delete</option>
                                    <option value="compact">compact</option>
                                    <option value="compact,delete">compact,delete</option>
                                </select>
                            </div>

                            <div>
                                <label className="block text-xs font-bold text-gray-700 mb-1">
                                    Retención en milisegundos (retention.ms)
                                </label>
                                <input
                                    type="number"
                                    min="-1"
                                    value={editRetentionMs}
                                    onChange={(e) => setEditRetentionMs(e.target.value)}
                                    className="w-full border border-gray-300 rounded px-3 py-2 font-mono"
                                />
                                <div className="flex flex-wrap gap-1.5 mt-2">
                                    {RETENTION_PRESETS.map((preset) => (
                                        <button
                                            key={preset.value}
                                            type="button"
                                            onClick={() => setEditRetentionMs(preset.value)}
                                            className="px-2 py-0.5 text-[11px] bg-gray-100 hover:bg-gray-200 text-gray-700 rounded font-medium cursor-pointer"
                                        >
                                            {preset.label.split(" (")[0]}
                                        </button>
                                    ))}
                                </div>
                            </div>
                        </div>

                        <div className="p-4 border-t border-gray-200 bg-gray-50 flex justify-end gap-3">
                            <button
                                type="button"
                                onClick={() => setEditingTopic(null)}
                                className="px-4 py-2 border border-gray-300 text-gray-700 rounded font-bold text-sm hover:bg-gray-100 cursor-pointer"
                            >
                                Cancelar
                            </button>
                            <button
                                type="submit"
                                disabled={isBusy}
                                className="px-4 py-2 bg-[#00274C] text-white rounded font-bold text-sm hover:bg-blue-900 shadow disabled:opacity-50 cursor-pointer"
                            >
                                {isBusy ? "Aplicando..." : "Guardar cambios"}
                            </button>
                        </div>
                    </form>
                </div>
            )}
        </div>
    );
}
