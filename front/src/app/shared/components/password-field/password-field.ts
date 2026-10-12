import { Component, ElementRef, inject, signal } from '@angular/core';

/**
 * Ajoute un bouton « œil » à un champ mot de passe.
 * Utilisation : <app-password-field><input type="password" ... /></app-password-field>
 */
@Component({
  selector: 'app-password-field',
  template: `
    <ng-content />
    <button
      type="button"
      class="eye"
      (click)="toggle()"
      [attr.aria-label]="visible() ? 'Masquer le mot de passe' : 'Afficher le mot de passe'"
      [attr.aria-pressed]="visible()"
    >
      @if (visible()) {
        <!-- œil barré : le mot de passe est visible, cliquer pour le masquer -->
        <svg viewBox="0 0 24 24" width="18" height="18" aria-hidden="true">
          <path d="M2 12c1-2.5 5-7 10-7s9 4.5 10 7c-1 2.5-5 7-10 7S3 14.5 2 12Z" />
          <circle cx="12" cy="12" r="3" />
        </svg>
      } @else {
        <!-- œil ouvert : le mot de passe est masqué, cliquer pour l'afficher -->
        <svg viewBox="0 0 24 24" width="18" height="18" aria-hidden="true">
          <path d="M3 3l18 18M10.6 10.6a2 2 0 0 0 2.8 2.8M9.9 5.1A9.8 9.8 0 0 1 12 5c5 0 9 4.5 10 7a12.6 12.6 0 0 1-2.6 3.6M6.6 6.6C4.3 8 2.7 10.2 2 12c1 2.5 5 7 10 7a9.6 9.6 0 0 0 4.4-1.1" />
        </svg>
      }
    </button>
  `,
  styles: `
    :host { position: relative; display: block; }
    .eye {
      position: absolute;
      top: 50%;
      right: 6px;
      display: grid;
      place-items: center;
      width: 30px;
      height: 30px;
      padding: 0;
      border: none;
      border-radius: 6px;
      background: none;
      color: var(--muted);
      cursor: pointer;
      transform: translateY(-50%);
    }
    .eye:hover { color: var(--text); }
    .eye:focus-visible { outline: 2px solid var(--primary); }
    svg { fill: none; stroke: currentColor; stroke-width: 1.8; stroke-linecap: round; stroke-linejoin: round; }
  `,
})
export class PasswordField {
  private readonly host = inject<ElementRef<HTMLElement>>(ElementRef);
  protected readonly visible = signal(false);

  protected toggle(): void {
    const input = this.host.nativeElement.querySelector('input');
    if (!input) return;
    this.visible.update((v) => !v);
    input.type = this.visible() ? 'text' : 'password';
  }
}