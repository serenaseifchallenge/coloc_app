import { Component, input, output } from '@angular/core';

export interface TabOption {
  value: string;
  label: string;
}

@Component({
  selector: 'app-tab-group',
  templateUrl: './tab-group.html',
  styleUrl: './tab-group.css',
  host: { role: 'tablist' },
})
export class TabGroup<T extends TabOption> {
  readonly options = input.required<readonly T[]>();
  readonly selected = input.required<T>();
  readonly selectedChange = output<T>();

  isSelected(option: T): boolean {
    return option.value === this.selected().value;
  }
}