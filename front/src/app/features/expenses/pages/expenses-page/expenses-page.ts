import { Component, computed, DestroyRef, inject, signal, ViewEncapsulation } from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { catchError, of } from 'rxjs';
import { AuthService } from '../../../../core/auth/auth.service';
import { apiErrorMessage } from '../../../../core/http/api-error';
import { TabGroup, TabOption } from '../../../../shared/components/tab-group/tab-group';
import { RoommateSummary } from '../../../../shared/models/roommate-summary.model';
import { RoommateService } from '../../../../shared/services/roommate.service';
import { BalancesTab } from '../../components/balances-tab/balances-tab';
import { ExpenseFormDialog } from '../../components/expense-form-dialog/expense-form-dialog';
import { ExpensesTab } from '../../components/expenses-tab/expenses-tab';
import { PotDetail } from '../../components/pot-detail/pot-detail';
import { PotFormDialog } from '../../components/pot-form-dialog/pot-form-dialog';
import { PotPaymentDialog } from '../../components/pot-payment-dialog/pot-payment-dialog';
import { PotsTab } from '../../components/pots-tab/pots-tab';
import { ReimbursementDialog } from '../../components/reimbursement-dialog/reimbursement-dialog';
import {
  Balances,
  Expense,
  ExpenseSummary,
  Pot,
  Reimbursement,
  TransferView,
} from '../../models/expense.models';
import { ExpenseService } from '../../services/expense.service';
import { PotService } from '../../services/pot.service';
import { addMonths, monthIso } from '../../utils/money';

interface ExpenseTab extends TabOption {
  value: 'expenses' | 'balances' | 'pots';
}

const TABS: ExpenseTab[] = [
  { value: 'expenses', label: 'Dépenses' },
  { value: 'balances', label: 'Soldes' },
  { value: 'pots', label: 'Cagnottes' },
];

@Component({
  selector: 'app-expenses-page',
  imports: [
    TabGroup,
    ExpensesTab,
    BalancesTab,
    PotsTab,
    PotDetail,
    ExpenseFormDialog,
    ReimbursementDialog,
    PotFormDialog,
    PotPaymentDialog,
  ],
  templateUrl: './expenses-page.html',
  styleUrl: './expenses-page.css',
  // Les styles « exp-* » sont partagés avec les composants du module (fenêtres, onglets...)
  encapsulation: ViewEncapsulation.None,
})
export class ExpensesPage {
  private readonly expenseService = inject(ExpenseService);
  private readonly potService = inject(PotService);
  private readonly roommateService = inject(RoommateService);
  private readonly auth = inject(AuthService);
  private readonly destroyRef = inject(DestroyRef);

  readonly tabs = TABS;
  readonly activeTab = signal<ExpenseTab>(TABS[0]);
  readonly month = signal(monthIso(new Date()));

  readonly expenses = signal<Expense[]>([]);
  readonly summary = signal<ExpenseSummary | null>(null);
  readonly balances = signal<Balances | null>(null);
  readonly reimbursements = signal<Reimbursement[]>([]);
  readonly pots = signal<Pot[]>([]);
  readonly loadingExpenses = signal(true);
  readonly errorMessage = signal<string | null>(null);

  readonly expenseDialog = signal<{ expense: Expense | null } | null>(null);
  readonly reimbursementDialog = signal<TransferView | null>(null);
  readonly potDialog = signal<{ pot: Pot | null } | null>(null);
  readonly paymentPotId = signal<number | null>(null);
  readonly selectedPotId = signal<number | null>(null);

  readonly meId = computed(() => this.auth.user()?.id ?? -1);

  private readonly loadedRoommates = toSignal(
    this.roommateService.getRoommates().pipe(catchError(() => of([] as RoommateSummary[]))),
    { initialValue: [] as RoommateSummary[] },
  );

  /** Les colocataires, avec l'utilisateur connecté toujours inclus. */
  readonly members = computed<RoommateSummary[]>(() => {
    const list = this.loadedRoommates();
    const me = this.auth.user();
    return me && !list.some((roommate) => roommate.id === me.id) ? [...list, me] : list;
  });

  readonly selectedPot = computed(() => this.pots().find((pot) => pot.id === this.selectedPotId()) ?? null);
  readonly paymentPot = computed(() => this.pots().find((pot) => pot.id === this.paymentPotId()) ?? null);
  readonly showPotDetail = computed(() => this.activeTab().value === 'pots' && this.selectedPot() !== null);

  constructor() {
    this.refresh();
  }

  selectTab(tab: ExpenseTab): void {
    this.activeTab.set(tab);
    this.selectedPotId.set(null);
  }

  changeMonth(delta: number): void {
    this.month.set(addMonths(this.month(), delta));
    this.loadExpenses();
  }

  refresh(): void {
    this.loadExpenses();
    this.loadBalances();
    this.loadPots();
  }

  openPotFromSummary(pot: Pot): void {
    this.activeTab.set(TABS[2]);
    this.selectedPotId.set(pot.id);
  }

  openPotCreation(): void {
    this.potDialog.set({ pot: null });
  }

  onPotDeleted(): void {
    this.selectedPotId.set(null);
    this.refresh();
  }

  private loadExpenses(): void {
    const month = this.month();
    this.loadingExpenses.set(true);

    this.expenseService
      .getExpenses(month)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (expenses) => {
          this.expenses.set(expenses);
          this.loadingExpenses.set(false);
        },
        error: (error: unknown) => {
          this.errorMessage.set(apiErrorMessage(error, 'Impossible de charger les dépenses.'));
          this.loadingExpenses.set(false);
        },
      });

    this.expenseService
      .getSummary(month)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({ next: (summary) => this.summary.set(summary), error: () => this.summary.set(null) });
  }

  private loadBalances(): void {
    this.expenseService
      .getBalances()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (balances) => this.balances.set(balances),
        error: (error: unknown) => this.errorMessage.set(apiErrorMessage(error, 'Impossible de charger les soldes.')),
      });

    this.expenseService
      .getReimbursements(10)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({ next: (list) => this.reimbursements.set(list), error: () => this.reimbursements.set([]) });
  }

  private loadPots(): void {
    this.potService
      .getPots()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (pots) => this.pots.set(pots),
        error: (error: unknown) => this.errorMessage.set(apiErrorMessage(error, 'Impossible de charger les cagnottes.')),
      });
  }
}
