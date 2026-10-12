import { Component, computed, input, output } from '@angular/core';
import { Pot, potTypeLabel } from '../../models/expense.models';
import { EurPipe } from '../../utils/eur.pipe';

@Component({
  selector: 'app-pots-tab',
  imports: [EurPipe],
  templateUrl: './pots-tab.html',
})
export class PotsTab {
  readonly pots = input.required<Pot[]>();
  /** Cagnottes en cours, mises en avant. */
  readonly activePots = computed(() => this.pots().filter((pot) => !pot.purchased));
  /** Cagnottes dont l'achat est fait : rangées dans l'historique, toujours consultables. */
  readonly historyPots = computed(() => this.pots().filter((pot) => pot.purchased));
  readonly open = output<Pot>();
  readonly create = output<void>();

  typeLabel(pot: Pot): string {
    return potTypeLabel(pot.type);
  }
}
