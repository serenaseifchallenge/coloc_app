import { Component, input, output } from '@angular/core';

@Component({
  selector: 'app-task-name-picker',
  templateUrl: './task-name-picker.html',
  styleUrl: './task-name-picker.css',
})
export class TaskNamePicker {
  readonly names = input.required<string[]>();
  readonly selectedName = input('');
  readonly nameSelected = output<string>();
}