import { RoommateSummary } from './roommate-summary.model';

/** Le colocataire complet, tel que renvoyé par GET /api/me. */
export interface Roommate extends RoommateSummary {
  email: string;
  points: number | null;
  birthday: string; // format "2004-02-02"
  sharedHouseId: number | null;
}
