import { Component } from '@angular/core';

/** Les deux vagues (saumon + violet) à droite des pages connexion et inscription. */
@Component({
  selector: 'app-auth-waves',
  template: `
    <svg viewBox="0 0 100 100" preserveAspectRatio="none" aria-hidden="true">
      <path class="salmon" d="M76 0C44 22 38 62 60 100H100V0Z" />
      <path class="purple" d="M81 0C55 26 52 66 68 100H100V0Z" />
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
