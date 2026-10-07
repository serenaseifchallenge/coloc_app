import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

/** Logo coloc : maison indigo avec 3 colocataires + « coloc » (2e o en corail). */
@Component({
  selector: 'app-logo',
  imports: [RouterLink],
  template: `
    <a routerLink="/" class="logo" aria-label="coloc, retour à l'accueil">
      <svg viewBox="0 0 48 44" width="48" height="44" aria-hidden="true">
        <path d="M24 1 46 18v21a4 4 0 0 1-4 4H6a4 4 0 0 1-4-4V18Z" fill="#4f46e5" />
        <circle cx="14" cy="24" r="3.2" fill="#fff" />
        <circle cx="34" cy="24" r="3.2" fill="#fff" />
        <circle cx="24" cy="22" r="3.8" fill="#ff7a59" />
        <path d="M8.5 40v-4.5a5.5 5.5 0 0 1 11 0V40Z" fill="#fff" />
        <path d="M28.5 40v-4.5a5.5 5.5 0 0 1 11 0V40Z" fill="#fff" />
        <path d="M17 40v-6a7 7 0 0 1 14 0v6Z" fill="#ff7a59" />
      </svg>
      <span class="word">col<span class="accent">o</span>c</span>
    </a>
  `,
  styles: `
    .logo { display: inline-flex; align-items: center; gap: 8px; text-decoration: none; }
    .word { font-family: 'Poppins', 'Roboto', sans-serif; font-weight: 700; font-size: 34px; color: #4f46e5; letter-spacing: -0.5px; line-height: 1; }
    .accent { color: #ff7a59; }
  `,
})
export class Logo {}
