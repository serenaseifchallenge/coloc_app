import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export type NoteType = 'ANNONCE' | 'SONDAGE';

export interface PollOptionDTO {
  id: number;
  label: string;
  voteCount: number;
  votedByCurrentUser: boolean;
}

export interface NoteDTO {
  id: number;
  type: NoteType;
  content: string;
  createdAt: string;
  creatorId: number;
  creatorName: string;
  creatorInitials: string;
  sharedHouseId: number;
  multipleAnswers: boolean;
  options: PollOptionDTO[];
  canDelete: boolean;
  pollEndsAt: string| null;
}

export interface CreateNoteDTO {
  type: NoteType;
  content: string;
  multipleAnswers: boolean;
  options: string[];
  pollEndsAt: string| null;
}

export interface VoteDTO {
  optionIds: number[];
}

@Injectable({
  providedIn: 'root'
})
export class NotesService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = '/api/notes';

  getNotes(): Observable<NoteDTO[]> {
    return this.http.get<NoteDTO[]>(this.apiUrl);
  }

  getNote(noteId: number): Observable<NoteDTO> {
    return this.http.get<NoteDTO>(`${this.apiUrl}/${noteId}`);
  }

  createNote(dto: CreateNoteDTO): Observable<NoteDTO> {
    return this.http.post<NoteDTO>(this.apiUrl, dto);
  }

  deleteNote(noteId: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${noteId}`);
  }

  vote(noteId: number, dto: VoteDTO): Observable<NoteDTO> {
    return this.http.put<NoteDTO>(
      `${this.apiUrl}/${noteId}/vote`,
      dto
    );
  }
}