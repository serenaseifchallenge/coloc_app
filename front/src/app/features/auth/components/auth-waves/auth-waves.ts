import { Component } from '@angular/core';

/** Les deux vagues (saumon + violet) à droite des pages connexion et inscription. */
@Component({
  selector: 'app-auth-waves',
  template: `
    <svg viewBox="0 0 100 100" preserveAspectRatio="none" aria-hidden="true">
      <path class="salmon" d="M70 0C50 40 50 85 58 100H100V0Z" />
      <path class="purple" d="M76 0C60 40 58 80 64 100H100V0Z" />
    </svg>
  `,
  styles: `
    :host { position: absolute; inset: 0; z-index: 0; pointer-events: none; }
    svg { display: block; width: 100%; height: 100%; }
    .salmon { fill: var(--salmon); }
    .purple { fill: var(--primary); }
  `,
})
export class AuthWaves {}
