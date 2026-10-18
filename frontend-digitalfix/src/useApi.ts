import { useCallback } from "react";
import { useMsal } from "@azure/msal-react";
import { InteractionRequiredAuthError } from "@azure/msal-browser";
import { loginRequest } from "./authConfig";

export function useApi() {
    const { instance, accounts } = useMsal();

    const fetchWithToken = useCallback(async (endpoint: string, options: RequestInit = {}) => {
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

        const configuredBaseUrl = (import.meta.env.VITE_BFF_URL || "").replace(/\/$/, "");
        let path = endpoint.startsWith("/") ? endpoint : `/${endpoint}`;
        if (!configuredBaseUrl && !path.startsWith("/api")) {
            path = `/api${path}`;
        }
        
        const fetchResponse = await fetch(`${configuredBaseUrl}${path}`, {
            ...options,
            headers,
        });

        // Manejo centralizado de errores 403 y 401
        if (fetchResponse.status === 401 || fetchResponse.status === 403) {
            throw new Error("Tu rol no tiene permiso para esta acción");
        }

        return fetchResponse;
    }, [accounts, instance]);

    return { fetchWithToken };
}