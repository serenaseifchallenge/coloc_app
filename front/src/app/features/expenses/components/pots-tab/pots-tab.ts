import { Component, input, output } from '@angular/core';
import { Pot, potTypeLabel } from '../../models/expense.models';
import { EurPipe } from '../../utils/eur.pipe';

@Component({
  selector: 'app-pots-tab',
  imports: [EurPipe],
  templateUrl: './pots-tab.html',
})
export class PotsTab {
  readonly pots = input.required<Pot[]>();
  readonly open = output<Pot>();
  readonly create = output<void>();

  typeLabel(pot: Pot): string {
    return potTypeLabel(pot.type);
  }
}
