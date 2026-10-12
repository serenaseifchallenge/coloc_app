import { Directive, ElementRef, inject, output } from '@angular/core';

/**
 * Émet (backdropClick) uniquement si le clic a COMMENCÉ et FINI sur l'élément lui-même
 * (le fond de la pop-up), jamais sur un de ses enfants.
 * → sélectionner du texte dans la pop-up en relâchant la souris dehors ne ferme plus rien.
 */
@Directive({
  selector: '[appBackdropClick]',
  host: {
    '(pointerdown)': 'onPointerDown($event)',
    '(click)': 'onClick($event)',
  },
})
export class BackdropClick {
  private readonly element = inject<ElementRef<HTMLElement>>(ElementRef);

  readonly backdropClick = output<void>();

  /** true si le dernier appui sur le bouton a eu lieu directement sur le fond. */
  private pressedOnBackdrop = false;

  onPointerDown(event: PointerEvent): void {
    this.pressedOnBackdrop = this.isBackdrop(event);
  }

  onClick(event: MouseEvent): void {
    const startedAndEndedOnBackdrop = this.pressedOnBackdrop && this.isBackdrop(event);
    this.pressedOnBackdrop = false;

    if (startedAndEndedOnBackdrop) {
      this.backdropClick.emit();
    }
  }

  private isBackdrop(event: Event): boolean {
    return event.target === this.element.nativeElement;
  }
}