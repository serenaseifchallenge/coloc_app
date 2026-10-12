import { Component, DestroyRef, effect, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { RoommateAvatar } from '../shared/components/roommate-avatar/roommate-avatar';
import { RoommateSummary } from '../shared/models/roommate-summary.model';
import { BackdropClick } from '../shared/directives/backdrop-click';
import {
  NotesService,
  NoteDTO,
  NoteType,
  PollOptionDTO,
  CreateNoteDTO
} from './notes.service';

import { AuthService } from '../core/auth/auth.service';

interface PollOption {
  id: number;
  label: string;
  votes: number;
  selected: boolean;
}

interface Note {
  id: number;
  type: NoteType;
  author: string;
  initials: string;
  roommate: RoommateSummary;
  content: string;
  createdAt: Date;
  pollEndsAt: Date | null;
  options: PollOption[];
  multipleAnswers: boolean;
  canDelete: boolean;
}

@Component({
  selector: 'app-notes-page',
  standalone: true,
  imports: [FormsModule, RoommateAvatar, BackdropClick],
  templateUrl: './notes-page.html',
  styleUrl: './notes-page.css'
})
export class NotesPage {
  private readonly notesService = inject(NotesService);
  private readonly auth = inject(AuthService);
  private readonly destroyRef = inject(DestroyRef);

  searchText = '';
  showModal = false;

  noteType: NoteType | '' = '';
  description = '';
  question = '';
  pollEndsAt = '';
  multipleAnswers = false;
  options: string[] = ['', ''];

  notes: Note[] = [];
  loading = false;
  errorMessage = '';
  successMessage = '';

  currentRoommateId: number | null = null;

  readonly pendingVotes = new Set<number>();

  private loadedRoommateId: number | null = null;

  constructor() {
    effect(() => {
      const user = this.auth.user();

      if (!user || user.id == null) {
        this.currentRoommateId = null;
        this.notes = [];
        this.loadedRoommateId = null;
        return;
      }

      const roommateId = user.id;
      this.currentRoommateId = roommateId;

      if (this.loadedRoommateId === roommateId) {
        return;
      }

      this.loadedRoommateId = roommateId;
      this.loadNotes();
    });

    void this.auth.loadUser();
  }

  get filteredNotes(): Note[] {
    const query = this.searchText.trim().toLowerCase();

    if (!query) {
      return this.notes;
    }

    return this.notes.filter(note =>
      note.content.toLowerCase().includes(query) ||
      note.author.toLowerCase().includes(query) ||
      note.options.some(option =>
        option.label.toLowerCase().includes(query)
      )
    );
  }

  get validOptionCount(): number {
    return this.options.filter(
      option => option.trim().length > 0
    ).length;
  }

  get minPollEndDate(): string {
    const now = new Date();

    const localDate = new Date(
      now.getTime() - now.getTimezoneOffset() * 60_000
    );

    return localDate.toISOString().slice(0, 16);
  }

  get isPollEndInvalid(): boolean {
    if (!this.pollEndsAt) {
      return true;
    }

    const selectedDate = new Date(this.pollEndsAt);

    return (
      Number.isNaN(selectedDate.getTime()) ||
      selectedDate.getTime() <= Date.now()
    );
  }

  private loadNotes(): void {
    if (this.currentRoommateId == null) {
      return;
    }

    this.loading = true;
    this.errorMessage = '';

    this.notesService
      .getNotes()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: dtos => {
          this.notes = dtos.map(dto => this.mapNote(dto));
          this.loading = false;
        },
        error: () => {
          this.loading = false;
          this.errorMessage = 'Impossible de charger les notes.';
        }
      });
  }

  private mapNote(dto: NoteDTO): Note {
    const nameParts = dto.creatorName.trim().split(/\s+/);

    // Le DTO fournit le nom complet, mais pas séparément le prénom
    // et le nom de famille.
    const name = nameParts[0] || '';
    const surname = nameParts.slice(1).join(' ') || name;

    const roommate: RoommateSummary = {
      id: dto.creatorId,
      name,
      surname
    };

    return {
      id: dto.id,
      type: dto.type,
      author: dto.creatorName,
      initials: dto.creatorInitials,
      roommate,
      content: dto.content,
      createdAt: new Date(dto.createdAt),
      pollEndsAt: dto.pollEndsAt
        ? new Date(dto.pollEndsAt)
        : null,
      multipleAnswers: dto.multipleAnswers,
      canDelete: dto.canDelete,
      options: (dto.options ?? []).map(
        (option: PollOptionDTO) => ({
          id: option.id,
          label: option.label,
          votes: option.voteCount,
          selected: option.votedByCurrentUser
        })
      )
    };
  }

  openModal(): void {
    this.errorMessage = '';
    this.successMessage = '';

    this.noteType = '';
    this.description = '';
    this.question = '';
    this.pollEndsAt = '';
    this.multipleAnswers = false;
    this.options = ['', ''];

    this.showModal = true;
  }

  closeModal(): void {
    this.showModal = false;
  }

  addOption(): void {
    this.options.push('');
  }

  removeOption(index: number): void {
    if (this.options.length > 2) {
      this.options.splice(index, 1);
    }
  }

  createNote(): void {
    if (this.currentRoommateId == null) {
      this.errorMessage = 'Veuillez vous connecter pour créer une note.';
      return;
    }

    if (!this.noteType) {
      this.errorMessage = 'Veuillez choisir un type de note.';
      return;
    }

    let content: string;
    let validOptions: string[] = [];
    let pollEndsAtValue: string | null = null;

    if (this.noteType === 'ANNONCE') {
      content = this.description.trim();

      if (!content) {
        this.errorMessage = 'Veuillez saisir le contenu de l’annonce.';
        return;
      }
    } else {
      content = this.question.trim();

      validOptions = this.options
        .map(option => option.trim())
        .filter(option => option.length > 0);

      if (!content) {
        this.errorMessage = 'Veuillez saisir une question.';
        return;
      }

      if (this.isPollEndInvalid) {
        this.errorMessage =
          'Veuillez choisir une date et une heure de fin dans le futur.';
        return;
      }

      if (validOptions.length < 2) {
        this.errorMessage = 'Un sondage doit avoir au moins deux options.';
        return;
      }

      if (
        new Set(
          validOptions.map(option => option.toLocaleLowerCase())
        ).size !== validOptions.length
      ) {
        this.errorMessage =
          'Les options du sondage doivent être différentes.';
        return;
      }

      pollEndsAtValue = this.pollEndsAt.length === 16
        ? `${this.pollEndsAt}:00`
        : this.pollEndsAt;
    }

    const dto: CreateNoteDTO = {
      type: this.noteType,
      content,
      multipleAnswers:
        this.noteType === 'SONDAGE' && this.multipleAnswers,
      options: this.noteType === 'SONDAGE' ? validOptions : [],
      pollEndsAt: pollEndsAtValue
    };

    this.errorMessage = '';
    this.successMessage = '';

    this.notesService
      .createNote(dto)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: createdNote => {
          this.notes = [
            this.mapNote(createdNote),
            ...this.notes
          ];

          this.closeModal();
          this.successMessage = 'Note créée.';
        },
        error: () => {
          this.errorMessage =
            'Impossible de créer la note. Vérifiez les informations saisies.';
        }
      });
  }

  deleteNote(noteId: number): void {
    if (this.currentRoommateId == null) {
      return;
    }

    const note = this.notes.find(item => item.id === noteId);

    if (!note || !note.canDelete) {
      return;
    }

    if (!confirm('Voulez-vous vraiment supprimer cette note ?')) {
      return;
    }

    this.errorMessage = '';
    this.successMessage = '';

    this.notesService
      .deleteNote(noteId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.notes = this.notes.filter(item => item.id !== noteId);
          this.successMessage = 'Note supprimée.';
        },
        error: () => {
          this.errorMessage = 'Impossible de supprimer cette note.';
        }
      });
  }

  isPollClosed(note: Note): boolean {
    return (
      note.type === 'SONDAGE' &&
      (
        note.pollEndsAt === null ||
        note.pollEndsAt.getTime() <= Date.now()
      )
    );
  }

  formatPollEndDate(date: Date): string {
    return new Intl.DateTimeFormat('fr-FR', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    }).format(date);
  }

  toggleVote(note: Note, option: PollOption): void {
    if (
      note.type !== 'SONDAGE' ||
      this.isPollClosed(note) ||
      this.currentRoommateId == null ||
      this.pendingVotes.has(note.id)
    ) {
      return;
    }

    const previousOptions = note.options.map(item => ({
      id: item.id,
      selected: item.selected,
      votes: item.votes
    }));

    const wasSelected = option.selected;

    if (note.multipleAnswers) {
      option.selected = !wasSelected;
    } else {
      note.options.forEach(item => {
        item.selected = false;
      });

      option.selected = !wasSelected;
    }

    note.options.forEach(item => {
      const previous = previousOptions.find(
        old => old.id === item.id
      );

      if (!previous) {
        return;
      }

      if (item.selected !== previous.selected) {
        item.votes = Math.max(
          0,
          previous.votes + (item.selected ? 1 : -1)
        );
      } else {
        item.votes = previous.votes;
      }
    });

    const optionIds = note.options
      .filter(item => item.selected)
      .map(item => item.id);

    this.pendingVotes.add(note.id);
    this.errorMessage = '';
    this.successMessage = '';

    this.notesService
      .vote(note.id, { optionIds })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (updatedDto: NoteDTO) => {
          note.options = (updatedDto.options ?? []).map(item => ({
            id: item.id,
            label: item.label,
            votes: item.voteCount,
            selected: item.votedByCurrentUser
          }));

          note.multipleAnswers = updatedDto.multipleAnswers;

          note.pollEndsAt = updatedDto.pollEndsAt
            ? new Date(updatedDto.pollEndsAt)
            : null;

          this.pendingVotes.delete(note.id);
        },
        error: () => {
          note.options.forEach(item => {
            const previous = previousOptions.find(
              old => old.id === item.id
            );

            if (previous) {
              item.selected = previous.selected;
              item.votes = previous.votes;
            }
          });

          this.pendingVotes.delete(note.id);
          this.errorMessage =
            'Impossible d’enregistrer votre vote. Le sondage est peut-être terminé.';
        }
      });
  }

  getTotalVotes(note: Note): number {
    return note.options.reduce(
      (total, option) => total + option.votes,
      0
    );
  }

  getVotePercentage(note: Note, option: PollOption): number {
    const total = this.getTotalVotes(note);

    return total === 0
      ? 0
      : Math.round((option.votes / total) * 100);
  }

  formatCreatedAt(date: Date): string {
    const today = new Date();

    if (date.toDateString() === today.toDateString()) {
      const time = new Intl.DateTimeFormat('fr-FR', {
        hour: '2-digit',
        minute: '2-digit'
      }).format(date);

      return `Postée aujourd'hui à ${time}`;
    }

    const formattedDate = new Intl.DateTimeFormat('fr-FR', {
      day: '2-digit',
      month: '2-digit',
      year: '2-digit'
    }).format(date);

    return `Postée le ${formattedDate}`;
  }
}