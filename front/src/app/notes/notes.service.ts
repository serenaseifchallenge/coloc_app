import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
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
}

export interface CreateNoteDTO {
  type: NoteType;
  content: string;
  creatorId: number;
  sharedHouseId: number;
  multipleAnswers: boolean;
  options: string[];
}

export interface VoteDTO {
  roommateId: number;
  optionIds: number[];
}

@Injectable({
  providedIn: 'root'
})
export class NotesService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = '/api/notes';

  getNotes(
    sharedHouseId: number,
    currentRoommateId: number
  ): Observable<NoteDTO[]> {
    const params = new HttpParams()
      .set('sharedHouseId', sharedHouseId)
      .set('currentRoommateId', currentRoommateId);

    return this.http.get<NoteDTO[]>(this.apiUrl, { params });
  }

  createNote(dto: CreateNoteDTO): Observable<NoteDTO> {
    return this.http.post<NoteDTO>(this.apiUrl, dto);
  }

  deleteNote(
    noteId: number,
    currentRoommateId: number
  ): Observable<void> {
    const params = new HttpParams()
      .set('currentRoommateId', currentRoommateId);

    return this.http.delete<void>(
      `${this.apiUrl}/${noteId}`,
      { params }
    );
  }

  vote(noteId: number, dto: VoteDTO): Observable<NoteDTO> {
    return this.http.put<NoteDTO>(
      `${this.apiUrl}/${noteId}/vote`,
      dto
    );
  }
}