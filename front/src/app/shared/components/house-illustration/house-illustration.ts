import { Component, input } from '@angular/core';

/** Grande illustration : maison saumon avec 3 colocataires. */
@Component({
  selector: 'app-house-illustration',
  template: `
    <svg viewBox="0 0 264 264" [attr.width]="size()" [attr.height]="size()" aria-hidden="true">
      <polygon points="132,10 256,110 256,256 8,256 8,110" fill="#ffb09c" />
      <!-- colocataires sur les côtés -->
      <circle cx="66" cy="150" r="27" fill="#fff" stroke="#4f46e5" stroke-width="4" />
      <circle cx="198" cy="150" r="27" fill="#fff" stroke="#4f46e5" stroke-width="4" />
      <path d="M22 256v-36a44 44 0 0 1 88 0v36" fill="#fff" stroke="#4f46e5" stroke-width="4" />
      <path d="M154 256v-36a44 44 0 0 1 88 0v36" fill="#fff" stroke="#4f46e5" stroke-width="4" />
      <!-- colocataire du milieu -->
      <circle cx="132" cy="116" r="33" fill="#6c63ff" stroke="#4f46e5" stroke-width="4" />
      <path d="M76 256v-50a56 56 0 0 1 112 0v50" fill="#6c63ff" stroke="#4f46e5" stroke-width="4" />
      <!-- contour de la maison par-dessus -->
      <polygon points="132,10 256,110 256,256 8,256 8,110" fill="none" stroke="#4f46e5" stroke-width="8" stroke-linejoin="miter" />
    </svg>
  `,
  styles: `:host { display: inline-block; line-height: 0; } svg { max-width: 100%; height: auto; }`,
})
export class HouseIllustration {
  readonly size = input(260);
}
