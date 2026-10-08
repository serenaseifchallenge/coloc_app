import { Component, input, output } from '@angular/core';
import { TaskNameAdder } from '../task-name-adder/task-name-adder';

@Component({
  selector: 'app-task-name-picker',
  imports: [TaskNameAdder],
  templateUrl: './task-name-picker.html',
  styleUrl: './task-name-picker.css',
})
export class TaskNamePicker {
  readonly names = input.required<string[]>();
  readonly selectedName = input('');
  readonly nameSelected = output<string>();
  readonly nameAdded = output<string>();
}