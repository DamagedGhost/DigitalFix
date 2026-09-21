import { useState, useEffect } from "react";
import type { FormEvent } from "react";
import { useApi } from "../useApi";

// Ajusta estas propiedades para que coincidan exactamente con la entidad Java de tu compañero
type CatalogItem = {
    id: number;
    name: string;
    description: string;
    price: number;
    stock: number;
};

export default function Catalog() {
    const { fetchWithToken } = useApi();
    const [items, setItems] = useState<CatalogItem[]>([]);
    const [isLoading, setIsLoading] = useState(true);
    const [error, setError] = useState("");
    const [isModalOpen, setIsModalOpen] = useState(false);
    const [isSaving, setIsSaving] = useState(false);

    // Estados del formulario
    const [name, setName] = useState("");
    const [description, setDescription] = useState("");
    const [price, setPrice] = useState("");
    const [stock, setStock] = useState("");

    const loadCatalog = async () => {
        setIsLoading(true);
        setError("");
        try {

            const response = await fetchWithToken("/catalog");
            if (!response.ok) {
                if (response.status === 403) throw new Error("No tienes permisos para ver el catálogo.");
                throw new Error(`Error al cargar el catálogo (${response.status})`);
            }
            const data = await response.json();
            setItems(data);
        } catch (err) {
            setError(err instanceof Error ? err.message : "Error de conexión");
        } finally {
            setIsLoading(false);
        }
    };

    useEffect(() => {
        void loadCatalog();
    }, [fetchWithToken]);

    const handleCreateItem = async (event: FormEvent) => {
        event.preventDefault();
        setIsSaving(true);
        setError("");
        try {
            const response = await fetchWithToken("/catalog", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ 
                    name, 
                    description, 
                    price: Number(price), 
                    stock: Number(stock) 
                }),
            });
            
            if (!response.ok) throw new Error(`Error al crear el ítem (${response.status})`);
            
            // Limpiar formulario y recargar
            setName("");
            setDescription("");
            setPrice("");
            setStock("");
            setIsModalOpen(false);
            await loadCatalog();
        } catch (err) {
            setError(err instanceof Error ? err.message : "Error al guardar");
        } finally {
            setIsSaving(false);
        }
    };

    return (
        <div className="space-y-6">
            <header className="flex justify-between items-end border-b border-gray-300 pb-4">
                <div>
                    <h2 className="text-3xl font-bold text-[#00274C]">Catálogo de Servicios y Repuestos</h2>
                    <p className="text-gray-600 mt-1">Administración de inventario y tarifas estandarizadas</p>
                </div>
                {/* Más adelante ocultaremos este botón para Clientes y Auditores */}
                <button 
                    onClick={() => setIsModalOpen(true)}
                    className="bg-[#00274C] text-white font-bold px-5 py-2 rounded shadow hover:bg-blue-900 transition flex items-center gap-2"
                >
                    <span className="text-[#FDB913] text-xl leading-none">+</span> Nuevo Ítem
                </button>
            </header>

            {error && <p className="rounded border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700 font-bold">{error}</p>}

            <div className="bg-white rounded shadow-sm border border-gray-200 overflow-hidden">
                <div className="overflow-x-auto">
                    <table className="w-full text-left border-collapse">
                        <thead>
                            <tr className="bg-[#00274C] text-white text-sm uppercase tracking-wider">
                                <th className="p-4 font-semibold w-24">ID</th>
                                <th className="p-4 font-semibold">Nombre</th>
                                <th className="p-4 font-semibold">Descripción</th>
                                <th className="p-4 font-semibold text-right">Precio ($)</th>
                                <th className="p-4 font-semibold text-right">Stock</th>
                            </tr>
                        </thead>
                        <tbody className="divide-y divide-gray-200 text-gray-800 text-sm">
                            {isLoading ? (
                                <tr><td colSpan={5} className="p-8 text-center text-gray-500 font-bold">Cargando inventario...</td></tr>
                            ) : items.length === 0 ? (
                                <tr><td colSpan={5} className="p-8 text-center text-gray-500">No hay ítems registrados en el catálogo.</td></tr>
                            ) : items.map((item) => (
                                <tr key={item.id} className="hover:bg-gray-50 transition duration-150">
                                    <td className="p-4 font-bold text-[#00274C]">CAT-{item.id}</td>
                                    <td className="p-4 font-bold">{item.name}</td>
                                    <td className="p-4 text-gray-600">{item.description}</td>
                                    <td className="p-4 text-right font-mono">${item.price.toLocaleString()}</td>
                                    <td className="p-4 text-right">
                                        <span className={`px-3 py-1 rounded text-xs font-bold ${item.stock > 5 ? 'bg-green-100 text-green-800' : 'bg-red-100 text-red-800'}`}>
                                            {item.stock} un.
                                        </span>
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
                            <h3 className="text-xl font-bold text-[#00274C]">Agregar Ítem al Catálogo</h3>
                            <button onClick={() => setIsModalOpen(false)} className="text-gray-400 hover:text-red-500 font-bold text-xl transition">×</button>
                        </div>
                        
                        <form onSubmit={handleCreateItem} className="p-6 space-y-4">
                            <div>
                                <label className="block text-sm font-bold text-gray-700 mb-1">Nombre</label>
                                <input required value={name} onChange={(e) => setName(e.target.value)} type="text" className="w-full border border-gray-300 rounded px-3 py-2" placeholder="Ej. Cambio de empalme" />
                            </div>
                            <div>
                                <label className="block text-sm font-bold text-gray-700 mb-1">Descripción</label>
                                <textarea required value={description} onChange={(e) => setDescription(e.target.value)} className="w-full border border-gray-300 rounded px-3 py-2" rows={2}></textarea>
                            </div>
                            <div className="grid grid-cols-2 gap-4">
                                <div>
                                    <label className="block text-sm font-bold text-gray-700 mb-1">Precio</label>
                                    <input required value={price} onChange={(e) => setPrice(e.target.value)} type="number" className="w-full border border-gray-300 rounded px-3 py-2" min="0" />
                                </div>
                                <div>
                                    <label className="block text-sm font-bold text-gray-700 mb-1">Stock Inicial</label>
                                    <input required value={stock} onChange={(e) => setStock(e.target.value)} type="number" className="w-full border border-gray-300 rounded px-3 py-2" min="0" />
                                </div>
                            </div>

                            <div className="p-5 border-t border-gray-200 bg-gray-50 flex justify-end gap-3 -mx-6 -mb-6 mt-6">
                                <button type="button" onClick={() => setIsModalOpen(false)} className="px-4 py-2 border border-gray-300 text-gray-700 rounded font-bold hover:bg-gray-100">Cancelar</button>
                                <button disabled={isSaving} type="submit" className="px-4 py-2 bg-[#00274C] text-white rounded font-bold hover:bg-blue-900 disabled:opacity-50">
                                    {isSaving ? "Guardando..." : "Guardar Ítem"}
                                </button>
                            </div>
                        </form>
                    </div>
                </div>
            )}
        </div>
    );
}