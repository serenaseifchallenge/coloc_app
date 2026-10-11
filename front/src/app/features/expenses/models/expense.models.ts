import { RoommateSummary } from '../../../shared/models/roommate-summary.model';

export type ExpenseCategory = 'COURANTE' | 'EAU' | 'INTERNET' | 'AUTRE';
export type PaymentMethod = 'VIREMENT' | 'ESPECES' | 'PAYPAL_WERO' | 'AUTRE';
export type PotType = 'VOYAGE' | 'CADEAU' | 'EVENEMENT' | 'MATERIEL' | 'AUTRE';

export const CATEGORY_OPTIONS: { value: ExpenseCategory; label: string }[] = [
  { value: 'COURANTE', label: 'Dépenses courantes' },
  { value: 'EAU', label: 'Facture eau' },
  { value: 'INTERNET', label: 'Facture internet' },
  { value: 'AUTRE', label: 'Autre' },
];

export const METHOD_OPTIONS: { value: PaymentMethod; label: string }[] = [
  { value: 'VIREMENT', label: 'Virement' },
  { value: 'ESPECES', label: 'Espèces' },
  { value: 'PAYPAL_WERO', label: 'PayPal / wero' },
  { value: 'AUTRE', label: 'Autre' },
];

export const POT_TYPE_OPTIONS: { value: PotType; label: string }[] = [
  { value: 'VOYAGE', label: 'Voyage' },
  { value: 'CADEAU', label: 'Cadeau' },
  { value: 'EVENEMENT', label: 'Évènement' },
  { value: 'MATERIEL', label: 'Matériel' },
  { value: 'AUTRE', label: 'Autre' },
];

export function methodLabel(method: PaymentMethod): string {
  return METHOD_OPTIONS.find((option) => option.value === method)?.label ?? method;
}

export function potTypeLabel(type: PotType): string {
  return POT_TYPE_OPTIONS.find((option) => option.value === type)?.label ?? type;
}

export interface Expense {
  id: number;
  name: string;
  amount: number;
  expenseDate: string;
  category: ExpenseCategory;
  payer: RoommateSummary;
  participantIds: number[];
  myShare: number;
  canEdit: boolean;
}

export interface ExpenseRequest {
  name: string;
  amount: number;
  expenseDate: string;
  payerId: number;
  category: ExpenseCategory;
  participantIds: number[];
}

export interface ExpenseSummary {
  month: string;
  monthTotal: number;
  myShare: number;
  myBalance: number;
}

export interface BalanceItem {
  roommateId: number;
  name: string;
  surname: string;
  amount: number;
}

export interface Transfer {
  fromId: number;
  toId: number;
  amount: number;
}

export interface Balances {
  balances: BalanceItem[];
  suggestedTransfers: Transfer[];
}

export interface Reimbursement {
  id: number;
  payer: RoommateSummary;
  receiver: RoommateSummary;
  amount: number;
  method: PaymentMethod;
  reimbursementDate: string;
}

export interface ReimbursementRequest {
  payerId: number;
  receiverId: number;
  amount: number;
  method: PaymentMethod;
  reimbursementDate: string;
}

export interface PotParticipant {
  roommate: RoommateSummary;
  paid: number;
  share: number;
}

export interface PotPayment {
  id: number;
  roommate: RoommateSummary;
  amount: number;
  method: PaymentMethod;
  paymentDate: string;
}

export interface Pot {
  id: number;
  name: string;
  type: PotType;
  targetAmount: number;
  deadline: string | null;
  sharePerPerson: number;
  collected: number;
  percent: number;
  remaining: number;
  remainingPerPerson: number;
  participant: boolean;
  myShare: number | null;
  myPaid: number;
  canEdit: boolean;
  participantIds: number[];
  participants: PotParticipant[];
  payments: PotPayment[];
}

export interface PotRequest {
  name: string;
  type: PotType;
  targetAmount: number;
  deadline: string | null;
  participantIds: number[];
}

export interface PotPaymentRequest {
  amount: number;
  method: PaymentMethod;
  paymentDate: string;
}

/** Une dépense du mois pour laquelle l'utilisateur veut agir sur un virement conseillé. */
export interface TransferView {
  from: RoommateSummary;
  to: RoommateSummary;
  amount: number;
}
