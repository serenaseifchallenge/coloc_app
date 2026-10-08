import { RoommateSummary } from '../../../shared/models/roommate-summary.model';

export interface PodiumEntry {
  rank: number;
  roommate: RoommateSummary;
  points: number;
}

export interface LastMonthResults {
  monthStart: string;
  podium: PodiumEntry[];
}