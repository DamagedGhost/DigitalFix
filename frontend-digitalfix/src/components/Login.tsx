import { useMsal } from "@azure/msal-react";
import { InteractionStatus } from "@azure/msal-browser";
import { loginRequest } from "../authConfig";

export default function Login() {
    const { instance, inProgress } = useMsal();

    const handleLogin = () => {
        if (inProgress === InteractionStatus.None) {
            // Inicia el flujo de redirección hacia Microsoft
            instance.loginRedirect(loginRequest).catch((e) => console.error(e));
        }
    };

    return (
        <div className="min-h-screen bg-gray-100 flex flex-col justify-center items-center">
            <div className="bg-white p-10 rounded shadow-md w-full max-w-md border-t-4 border-[#FDB913]">
                <div className="flex justify-center items-center gap-3 mb-8">
                    <div className="w-10 h-10 bg-[rgb(0,39,76)] rounded-full flex items-center justify-center text-[#FDB913] text-2xl" aria-hidden="true">⚡</div>
                    <h1 className="font-bold text-3xl tracking-wide text-[rgb(0,39,76)]">
                        Digital<span className="text-[#FDB913]">Fix</span>
                    </h1>
                </div>
                
                <p className="text-center text-gray-600 mb-8 font-medium">
                    Plataforma para órdenes de trabajo de mantención eléctrica.
                </p>

                <button 
                    onClick={handleLogin}
                    disabled={inProgress !== InteractionStatus.None}
                    className="w-full bg-[#00274C] text-white font-bold py-3 px-4 rounded shadow hover:bg-blue-900 transition flex justify-center items-center gap-2 disabled:opacity-50"
                >
                    {inProgress !== InteractionStatus.None ? (
                        <span>Conectando con Azure...</span>
                    ) : (
                        <span>Iniciar sesión con Microsoft</span>
                    )}
                </button>
            </div>
        </div>
    );
}