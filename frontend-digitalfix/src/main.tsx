import React from "react";
import ReactDOM from "react-dom/client";
import { PublicClientApplication, EventType } from "@azure/msal-browser";
import type { EventMessage, AuthenticationResult } from "@azure/msal-browser";
import { MsalProvider } from "@azure/msal-react";
import { msalConfig } from "./authConfig";
import App from "./App";
import './index.css';

const msalInstance = new PublicClientApplication(msalConfig);

async function main() {
    await msalInstance.initialize();

    await msalInstance.handleRedirectPromise().catch((error) => {
        if (error?.errorCode === "no_token_request_cache_error") {
            Object.keys(localStorage)
                .filter((k) => k.startsWith("msal."))
                .forEach((k) => localStorage.removeItem(k));
        } else {
            console.error(error);
        }
    });

    if (
        !msalInstance.getActiveAccount() &&
        msalInstance.getAllAccounts().length > 0
    ) {
        msalInstance.setActiveAccount(msalInstance.getAllAccounts()[0]);
    }

    msalInstance.addEventCallback((event: EventMessage) => {
        if (event.eventType === EventType.LOGIN_SUCCESS && event.payload) {
            const payload = event.payload as AuthenticationResult;
            msalInstance.setActiveAccount(payload.account);
        }
    });

    ReactDOM.createRoot(document.getElementById("root")!).render(
        <React.StrictMode>
            <MsalProvider instance={msalInstance}>
                <App />
            </MsalProvider>
        </React.StrictMode>,
    );
}

main();