export async function extractApiError(response: Response, fallbackMessage: string): Promise<string> {
    try {
        const text = await response.text();
        if (!text) {
            return `${fallbackMessage} (${response.status})`;
        }
        try {
            const parsed = JSON.parse(text) as { message?: string; detail?: string; error?: string };
            if (parsed.message) return parsed.message;
            if (parsed.detail) return parsed.detail;
            if (parsed.error) return `${parsed.error} (${response.status})`;
        } catch {
            return text.length > 200 ? `${fallbackMessage} (${response.status})` : text;
        }
    } catch {
        // Ignorar fallo leyendo cuerpo
    }
    return `${fallbackMessage} (${response.status})`;
}
