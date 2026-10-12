import { Roommate } from '../../shared/models/roommate.model';

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  name: string;
  surname: string;
  email: string;
  password: string;
  birthday: string; // format "2004-02-02"
}

export interface AuthResponse {
  token: string;
  roommate: Roommate;
}

export interface UpdateCredentialsRequest {
  email: string;
  newPassword: string | null; // null = mot de passe inchangé
}

export interface UpdateAvatarRequest {
  color: string | null; // null = couleur par défaut
  image: string | null; // data URL, null = initiales
}
