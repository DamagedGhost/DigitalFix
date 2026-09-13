import { useMsal } from "@azure/msal-react";
import { InteractionRequiredAuthError } from "@azure/msal-browser";
import { loginRequest } from "./authConfig";

export function useApi() {
    const { instance, accounts } = useMsal();

    const fetchWithToken = async (endpoint: string, options: RequestInit = {}) => {
        const account = accounts[0] || instance.getActiveAccount();
        if (!account) throw new Error("No hay una cuenta activa en DigitalFix");

        let response;
        try {
            response = await instance.acquireTokenSilent({
                ...loginRequest,
                account,
            });
        } catch (error) {
            if (error instanceof InteractionRequiredAuthError) {
                await instance.acquireTokenRedirect(loginRequest);
                throw new Error("Redirigiendo al portal de Microsoft...");
            }
            throw error;
        }

        const headers = new Headers(options.headers);
        headers.set("Authorization", `Bearer ${response.accessToken}`);

        // Aquí apuntamos por defecto a la URL de tu BFF configurada en el .env
        const baseUrl = import.meta.env.VITE_BFF_URL || "http://localhost:8080/api";
        
        return fetch(`${baseUrl}${endpoint}`, {
            ...options,
            headers,
        });
    };

    return { fetchWithToken };
}