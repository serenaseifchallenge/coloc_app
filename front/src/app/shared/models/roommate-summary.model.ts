export interface RoommateSummary {
  id: number;
  name: string;
  surname: string;
  /** Couleur choisie (« #6c63ff ») ; absente ou null = couleur par défaut. */
  avatarColor?: string | null;
  /** Photo en data URL ; absente ou null = initiales. */
  avatarImage?: string | null;
}