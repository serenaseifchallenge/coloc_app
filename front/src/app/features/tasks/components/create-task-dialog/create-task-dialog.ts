import { Component, computed, DestroyRef, inject, input, OnInit, output, signal, viewChild } from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { catchError, of } from 'rxjs';
import { Modal } from '../../../../shared/components/modal/modal';
import { RoommatePicker } from '../../../../shared/components/roommate-picker/roommate-picker';
import { RoommateService } from '../../../../shared/services/roommate.service';
import { Task } from '../../models/task.model';
import { TaskService } from '../../services/task.service';
import { todayIso } from '../../utils/task-status';
import { TaskNamePicker } from '../task-name-picker/task-name-picker';

@Component({
  selector: 'app-create-task-dialog',
  imports: [ReactiveFormsModule, Modal, RoommatePicker, TaskNamePicker],
  templateUrl: './create-task-dialog.html',
  styleUrl: './create-task-dialog.css',
})
export class CreateTaskDialog implements OnInit {
  private readonly taskService = inject(TaskService);
  private readonly roommateService = inject(RoommateService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly formBuilder = inject(NonNullableFormBuilder);
  private readonly modal = viewChild.required(Modal);

  readonly initialName = input('');
  readonly created = output<Task>();
  readonly closed = output<void>();

  readonly taskNames = signal<string[]>([]);
  readonly roommates = toSignal(this.roommateService.getRoommates().pipe(catchError(() => of([]))), {
    initialValue: [],
  });

  readonly today = todayIso();
  readonly submitting = signal(false);
  readonly errorMessage = signal<string | null>(null);

  readonly form = this.formBuilder.group({
    name: ['', [Validators.required, Validators.maxLength(150), Validators.pattern(/\S/)]],
    deadline: [''],
    assigneeId: this.formBuilder.control<number | null>(null),
    points: [10, [Validators.required, Validators.min(1), Validators.max(100)]],
  });

  readonly nameValue = toSignal(this.form.controls.name.valueChanges, {
    initialValue: this.form.controls.name.value,
  });

  readonly filteredTaskNames = computed(() => {
    const query = this.nameValue().trim().toLowerCase();
    const names = this.taskNames();
    const isExactSuggestion = names.some((name) => name.toLowerCase() === query);

    if (!query || isExactSuggestion) {
      return names;
    }
    return names.filter((name) => name.toLowerCase().includes(query));
  });

  ngOnInit(): void {
    this.form.controls.name.setValue(this.initialName());
    this.loadTaskNames();
  }

  selectTaskName(name: string): void {
    this.form.controls.name.setValue(name);
  }

  onTaskNameAdded(name: string): void {
    this.loadTaskNames();
    this.selectTaskName(name);
  }

  selectAssignee(roommateId: number | null): void {
    this.form.controls.assigneeId.setValue(roommateId);
  }

  hasError(controlName: 'name' | 'points'): boolean {
    const control = this.form.controls[controlName];
    return control.invalid && control.touched;
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const { name, deadline, assigneeId, points } = this.form.getRawValue();
    this.submitting.set(true);
    this.errorMessage.set(null);

    this.taskService
      .createTask({ name: name.trim(), deadline: deadline || null, assigneeId, points })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (task) => {
          this.created.emit(task);
          this.modal().close();
        },
        error: () => {
          this.errorMessage.set("La tâche n'a pas pu être créée. Vérifie les champs et réessaie.");
          this.submitting.set(false);
        },
      });
  }

  private loadTaskNames(): void {
    this.taskService
      .getTaskNames()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (names) => this.taskNames.set(names),
        error: () => this.taskNames.set([]),
      });
  }
}