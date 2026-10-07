import { Roommate } from './roommate.model';

export interface SharedHouse {
  id: number;
  name: string;
  address: string;
  description: string;
  invitationCode: string;
  creationDate: string;
  members: Roommate[];
}

export interface SharedHouseRequest {
  name: string;
  address: string;
  description: string;
}
