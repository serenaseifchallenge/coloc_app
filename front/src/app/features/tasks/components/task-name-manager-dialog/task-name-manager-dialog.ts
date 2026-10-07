import { Component, DestroyRef, inject, output, signal, viewChild } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Modal } from '../../../../shared/components/modal/modal';
import { TaskService } from '../../services/task.service';

@Component({
  selector: 'app-task-name-manager-dialog',
  imports: [Modal],
  templateUrl: './task-name-manager-dialog.html',
  styleUrl: './task-name-manager-dialog.css',
})
export class TaskNameManagerDialog {
  private readonly taskService = inject(TaskService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly modal = viewChild.required(Modal);

  readonly closed = output<void>();
  readonly taskNameSelected = output<string>();

  readonly taskNames = signal<string[]>([]);
  readonly loading = signal(true);
  readonly deletingName = signal<string | null>(null);
  readonly errorMessage = signal<string | null>(null);

  constructor() {
    this.taskService
      .getTaskNames()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (names) => {
          this.taskNames.set(names);
          this.loading.set(false);
        },
        error: () => {
          this.errorMessage.set('Impossible de charger les suggestions.');
          this.loading.set(false);
        },
      });
  }

  selectTaskName(name: string): void {
    this.taskNameSelected.emit(name);
    this.modal().close();
  }

  deleteTaskName(name: string): void {
    if (!confirm(`Retirer « ${name} » des suggestions de la colocation ?`)) {
      return;
    }

    this.deletingName.set(name);
    this.errorMessage.set(null);

    this.taskService
      .deleteTaskName(name)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.taskNames.update((names) => names.filter((taskName) => taskName !== name));
          this.deletingName.set(null);
        },
        error: () => {
          this.errorMessage.set(`« ${name} » n'a pas pu être retirée.`);
          this.deletingName.set(null);
        },
      });
  }
}