import { HttpErrorResponse } from '@angular/common/http';

/** Récupère le message d'erreur renvoyé par le back (GlobalExceptionHandler). */
export function apiErrorMessage(error: unknown, fallback = 'Une erreur est survenue, réessayez.'): string {
  if (error instanceof HttpErrorResponse) {
    if (error.status === 0) {
      return 'Impossible de joindre le serveur. Vérifiez que le back est lancé.';
    }
    const message = (error.error as { message?: unknown } | null)?.message;
    if (typeof message === 'string' && message.trim()) {
      return message;
    }
  }
  return fallback;
}
