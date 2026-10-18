import { useState, useEffect, type FormEvent } from "react";
import { useApi } from "../useApi";

type QueueInfo = {
    name: string;
    messages?: number;
    consumers?: number;
};

export default function AdminRabbitPanel() {
    const { fetchWithToken } = useApi();
    const [statusMsg, setStatusMsg] = useState<{ text: string, type: "success" | "error" | "" }>({ text: "", type: "" });
    const [isLoading, setIsLoading] = useState(false);
    const [queues, setQueues] = useState<QueueInfo[]>([]);
    const [queueName, setQueueName] = useState("");

    const showMessage = (text: string, type: "success" | "error") => {
        setStatusMsg({ text, type });
        setTimeout(() => setStatusMsg({ text: "", type: "" }), 5000);
    };

    // 1. GET /api/v1/admin/queues -> Lista las colas
    const loadQueues = async () => {
        try {
            const response = await fetchWithToken("/api/v1/admin/queues");
            if (response.ok) {
                const data = await response.json();
                // Si el backend devuelve strings, los mapeamos. Si devuelve objetos, los usamos directo.
                const formattedQueues = data.map((q: any) => typeof q === 'string' ? { name: q } : q);
                setQueues(formattedQueues);
            }
        } catch (error) {
            console.error("Error al cargar colas", error);
        }
    };

    useEffect(() => {
        void loadQueues();
    }, []);

    // 2. POST /api/v1/admin/queues -> Crea una cola
    const handleCreateQueue = async (e: FormEvent) => {
        e.preventDefault();
        setIsLoading(true);
        try {
            const response = await fetchWithToken("/api/v1/admin/queues", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                // Tu DTO CreateQueueRequest usualmente espera queueName
                body: JSON.stringify({ queueName }), 
            });
            if (!response.ok) throw new Error(`Error: ${response.status}`);
            showMessage(`Cola '${queueName}' creada exitosamente.`, "success");
            setQueueName("");
            await loadQueues();
        } catch (error) {
            showMessage("Error al crear la cola", "error");
        } finally { setIsLoading(false); }
    };

    // 3. DELETE /api/v1/admin/queues/{name} -> Borra una cola
    const handleDeleteQueue = async (name: string) => {
        if (!confirm(`¿Estás seguro de eliminar la cola ${name}?`)) return;
        setIsLoading(true);
        try {
            const response = await fetchWithToken(`/api/v1/admin/queues/${name}`, { method: "DELETE" });
            if (!response.ok) throw new Error(`Error: ${response.status}`);
            showMessage(`Cola '${name}' eliminada.`, "success");
            await loadQueues();
        } catch (error) {
            showMessage("Error al eliminar la cola", "error");
        } finally { setIsLoading(false); }
    };

    // 4. DELETE /api/v1/admin/queues/{flow}/purge?dlq=true/false -> Purga la cola
    const handlePurgeQueue = async (flow: string, isDlq: boolean) => {
        if (!confirm(`¿Estás seguro de vaciar la cola ${isDlq ? 'muerta (DLQ)' : 'principal'} de ${flow}?`)) return;
        setIsLoading(true);
        try {
            const response = await fetchWithToken(`/api/v1/admin/queues/${flow}/purge?dlq=${isDlq}`, { method: "DELETE" });
            if (!response.ok) throw new Error(`Error: ${response.status}`);
            showMessage(`Cola purgada exitosamente.`, "success");
        } catch (error) {
            showMessage("Error al purgar la cola", "error");
        } finally { setIsLoading(false); }
    };

    // 5. POST /api/v1/admin/queues/{flow}/reprocess?max=100 -> Reprocesa mensajes DLQ
    const handleReprocessDLQ = async (flow: string) => {
        setIsLoading(true);
        try {
            const response = await fetchWithToken(`/api/v1/admin/queues/${flow}/reprocess?max=100`, { method: "POST" });
            if (!response.ok) throw new Error(`Error: ${response.status}`);
            showMessage(`Mensajes de la DLQ reprocesados para el flujo: ${flow}`, "success");
        } catch (error) {
            showMessage("Error al reprocesar la DLQ", "error");
        } finally { setIsLoading(false); }
    };

    // Función auxiliar para extraer el flujo base del nombre de la cola (ej: q.cmd.email -> email)
    const extractFlowName = (queueName: string) => {
        return queueName.replace('q.cmd.', '').replace('.dlq', '');
    };

    return (
        <div className="bg-white rounded shadow-sm border border-gray-200 overflow-hidden mt-6">
            <div className="bg-[#00274C] text-white border-b border-gray-200">
                <h3 className="font-bold flex items-center gap-2 p-4">
                    <span>🐰</span> Consola Administrativa RabbitMQ
                </h3>
            </div>

            <div className="p-6">
                {statusMsg.text && (
                    <div className={`p-3 rounded mb-4 text-sm font-bold ${statusMsg.type === "success" ? "bg-green-100 text-green-800" : "bg-red-100 text-red-800"}`}>
                        {statusMsg.text}
                    </div>
                )}

                <div className="space-y-6">
                    <form onSubmit={handleCreateQueue} className="flex gap-4 items-end bg-gray-50 p-4 rounded border border-gray-200">
                        <div className="flex-1">
                            <label className="block text-sm font-bold text-gray-700 mb-1">Aprovisionar Nueva Cola</label>
                            <input required value={queueName} onChange={(e) => setQueueName(e.target.value)} type="text" className="w-full border border-gray-300 rounded px-3 py-2" placeholder="Ej. q.cmd.email" />
                        </div>
                        <button disabled={isLoading || !queueName} type="submit" className="px-5 py-2 bg-[#FDB913] text-[#00274C] rounded font-bold hover:bg-yellow-500 shadow disabled:opacity-50 transition">
                            Crear Cola
                        </button>
                    </form>

                    <div>
                        <div className="flex justify-between items-center mb-2">
                            <h4 className="font-bold text-[#00274C]">Colas en el Clúster</h4>
                            <button onClick={loadQueues} className="text-xs text-blue-600 hover:underline font-bold">↻ Refrescar Lista</button>
                        </div>
                        
                        <div className="border border-gray-200 rounded overflow-hidden">
                            {queues.length === 0 ? (
                                <p className="p-4 text-sm text-gray-500 text-center">No hay colas registradas.</p>
                            ) : (
                                <ul className="divide-y divide-gray-200 text-sm">
                                    {queues.map((q, idx) => {
                                        const isDlq = q.name.includes('.dlq');
                                        const flowName = extractFlowName(q.name);

                                        return (
                                            <li key={idx} className="p-4 flex flex-col md:flex-row md:justify-between md:items-center hover:bg-gray-50 gap-4">
                                                <div>
                                                    <span className="font-mono font-bold text-[#00274C] text-base">{q.name}</span>
                                                    {isDlq && <span className="ml-2 px-2 py-0.5 bg-red-100 text-red-800 text-xs rounded font-bold">Dead Letter Queue</span>}
                                                </div>
                                                
                                                <div className="flex flex-wrap gap-2">
                                                    <button onClick={() => handlePurgeQueue(flowName, isDlq)} disabled={isLoading} className="px-3 py-1 bg-gray-200 text-gray-700 hover:bg-gray-300 rounded font-bold text-xs transition">
                                                        Vaciar
                                                    </button>
                                                    {isDlq && (
                                                        <button onClick={() => handleReprocessDLQ(flowName)} disabled={isLoading} className="px-3 py-1 bg-blue-100 text-blue-800 hover:bg-blue-200 rounded font-bold text-xs transition">
                                                            Reprocesar DLQ
                                                        </button>
                                                    )}
                                                    <button onClick={() => handleDeleteQueue(q.name)} disabled={isLoading} className="px-3 py-1 bg-red-100 text-red-800 hover:bg-red-200 rounded font-bold text-xs transition">
                                                        Eliminar
                                                    </button>
                                                </div>
                                            </li>
                                        );
                                    })}
                                </ul>
                            )}
                        </div>
                    </div>
                </div>
            </div>
        </div>
    );
}