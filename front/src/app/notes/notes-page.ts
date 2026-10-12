import { Component, DestroyRef, effect, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

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
  content: string;
  createdAt: Date;
  options: PollOption[];
  multipleAnswers: boolean;
  canDelete: boolean;
}

@Component({
  selector: 'app-notes-page',
  standalone: true,
  imports: [FormsModule],
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
    return this.options.filter(option => option.trim().length > 0).length;
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
    return {
      id: dto.id,
      type: dto.type,
      author: dto.creatorName,
      initials: dto.creatorInitials,
      content: dto.content,
      createdAt: new Date(dto.createdAt),
      multipleAnswers: dto.multipleAnswers,
      canDelete: dto.canDelete,
      options: (dto.options ?? []).map((option: PollOptionDTO) => ({
        id: option.id,
        label: option.label,
        votes: option.voteCount,
        selected: option.votedByCurrentUser
      }))
    };
  }

  openModal(): void {
    this.errorMessage = '';
    this.successMessage = '';
    this.noteType = '';
    this.description = '';
    this.question = '';
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

      if (validOptions.length < 2) {
        this.errorMessage = 'Un sondage doit avoir au moins deux options.';
        return;
      }

      if (new Set(validOptions).size !== validOptions.length) {
        this.errorMessage = 'Les options du sondage doivent être différentes.';
        return;
      }
    }

    const dto: CreateNoteDTO = {
      type: this.noteType,
      content,
      multipleAnswers:
        this.noteType === 'SONDAGE' && this.multipleAnswers,
      options: this.noteType === 'SONDAGE' ? validOptions : []
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
          this.errorMessage = 'Impossible de créer la note.';
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

  toggleVote(note: Note, option: PollOption): void {
    if (
      note.type !== 'SONDAGE' ||
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

    // Mise à jour optimiste de la sélection.
    if (note.multipleAnswers) {
      option.selected = !wasSelected;
    } else {
      note.options.forEach(item => {
        item.selected = false;
      });

      option.selected = !wasSelected;
    }

    // Mise à jour optimiste des compteurs.
    note.options.forEach(item => {
      const previous = previousOptions.find(old => old.id === item.id);

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
          this.pendingVotes.delete(note.id);
        },
        error: () => {
          // Restaurer les données précédentes si le serveur refuse le vote.
          note.options.forEach(item => {
            const previous = previousOptions.find(old => old.id === item.id);

            if (previous) {
              item.selected = previous.selected;
              item.votes = previous.votes;
            }
          });

          this.pendingVotes.delete(note.id);
          this.errorMessage = 'Impossible d’enregistrer votre vote.';
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