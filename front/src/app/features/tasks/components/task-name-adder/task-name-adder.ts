import { HttpErrorResponse } from '@angular/common/http';
import { Component, DestroyRef, inject, output, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { AutoFocus } from '../../../../shared/directives/auto-focus';
import { TaskService } from '../../services/task.service';

@Component({
  selector: 'app-task-name-adder',
  imports: [AutoFocus],
  templateUrl: './task-name-adder.html',
  styleUrl: './task-name-adder.css',
})
export class TaskNameAdder {
  private readonly taskService = inject(TaskService);
  private readonly destroyRef = inject(DestroyRef);

  readonly added = output<string>();

  readonly adding = signal(false);
  readonly saving = signal(false);
  readonly errorMessage = signal<string | null>(null);

  start(): void {
    this.errorMessage.set(null);
    this.adding.set(true);
  }

  cancel(event?: Event): void {
    event?.preventDefault();
    this.adding.set(false);
    this.errorMessage.set(null);
  }

  submit(rawName: string): void {
    const name = rawName.trim();
    if (!name) {
      this.cancel();
      return;
    }
    if (this.saving()) {
      return;
    }

    this.saving.set(true);
    this.errorMessage.set(null);

    this.taskService
      .addTaskName(name)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (response) => {
          this.saving.set(false);
          this.adding.set(false);
          this.added.emit(response.name);
        },
        error: (error: HttpErrorResponse) => {
          this.saving.set(false);
          this.errorMessage.set(
            error.status === 409
              ? 'Cette tâche récurrente existe déjà.'
              : "La tâche récurrente n'a pas pu être ajoutée.",
          );
        },
      });
  }
}