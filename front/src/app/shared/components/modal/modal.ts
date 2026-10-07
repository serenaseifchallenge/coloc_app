import { afterNextRender, Component, ElementRef, input, output, viewChild } from '@angular/core';

let nextModalId = 0;

@Component({
  selector: 'app-modal',
  templateUrl: './modal.html',
  styleUrl: './modal.css',
})
export class Modal {
  readonly heading = input.required<string>();
  readonly closed = output<void>();

  readonly headingId = `modal-heading-${nextModalId++}`;
  private readonly dialog = viewChild.required<ElementRef<HTMLDialogElement>>('dialog');

  constructor() {
    afterNextRender(() => this.dialog().nativeElement.showModal());
  }

  close(): void {
    this.dialog().nativeElement.close();
  }

  onDialogClick(event: MouseEvent): void {
    if (event.target === this.dialog().nativeElement) {
      this.close();
    }
  }
}