
import { Component } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';

type NoteType = 'ANNONCE' | 'SONDAGE';

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
  expiresAt?: string;
  options?: PollOption[];
  multipleAnswers?: boolean;
}

@Component({
  selector: 'app-notes-page',
  imports: [FormsModule, DatePipe],
  templateUrl: './notes-page.html',
  styleUrl: './notes-page.css'
})
export class NotesPage {
  searchText = '';
  showModal = false;
  noteType: NoteType | '' = '';
  description = '';
  question = '';
  multipleAnswers = false;
  options = ['', ''];

  private nextId = 4;

  notes: Note[] = [
    {
      id: 1,
      type: 'ANNONCE',
      author: 'Mila Ambrosi',
      initials: 'MA',
      content: 'Voici une première vue des notes !',
      createdAt: new Date()
    },
    {
      id: 2,
      type: 'SONDAGE',
      author: 'Emma Lehec',
      initials: 'EL',
      content: 'Est-ce que le format vous convient ?',
      createdAt: new Date(),
      expiresAt: '2026-12-10',
      options: [
        {
          id: 1,
          label: 'non pas vraiment',
          votes: 1,
          selected: false
        },
        {
          id: 2,
          label: 'ça peut passer...',
          votes: 3,
          selected: false
        }
      ],
      multipleAnswers: false
    },
    {
      id: 3,
      type: 'ANNONCE',
      author: 'Serena Seif',
      initials: 'SS',
      content: 'Emma le cours à commencer ! Dépêche toi !',
      createdAt: new Date('2026-10-04T15:10:00')
    }
  ];

  get filteredNotes(): Note[] {
    const query = this.searchText.trim().toLowerCase();

    if (!query) {
      return this.notes;
    }

    return this.notes.filter(note =>
      note.content.toLowerCase().includes(query)
      || note.author.toLowerCase().includes(query)
      || (note.options ?? []).some(option =>
        option.label.toLowerCase().includes(query)
      )
    );
  }

  get validOptionCount(): number {
    return this.options.filter(
      option => option.trim().length > 0
    ).length;
  }

  openModal(): void {
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


    deleteNote(noteId: number): void {
    const confirmed = confirm(
        'Voulez-vous vraiment supprimer cette note ?'
    );

    if (!confirmed) {
        return;
    }

    this.notes = this.notes.filter(note => note.id !== noteId);
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
    if (!this.noteType) {
      return;
    }

    const now = new Date();

    if (this.noteType === 'ANNONCE') {
      const content = this.description.trim();

      if (!content) {
        return;
      }

      this.notes.unshift({
        id: this.nextId++,
        type: 'ANNONCE',
        author: 'Mila Ambrosi',
        initials: 'MA',
        content,
        createdAt: now
      });
    } else {
      const question = this.question.trim();

      const validOptions = this.options
        .map(option => option.trim())
        .filter(option => option.length > 0);

      if (!question || validOptions.length < 2) {
        return;
      }

      this.notes.unshift({
        id: this.nextId++,
        type: 'SONDAGE',
        author: 'Mila Ambrosi',
        initials: 'MA',
        content: question,
        createdAt: now,
        options: validOptions.map((label, index) => ({
          id: index + 1,
          label,
          votes: 0,
          selected: false
        })),
        multipleAnswers: this.multipleAnswers
      });
    }

    this.closeModal();
  }

  toggleVote(note: Note, option: PollOption): void {
    if (note.type !== 'SONDAGE' || !note.options) {
      return;
    }

    if (note.multipleAnswers) {
      option.selected = !option.selected;
      option.votes += option.selected ? 1 : -1;
      return;
    }

    const previouslySelected = option.selected;

    for (const item of note.options) {
      if (item.selected) {
        item.votes = Math.max(0, item.votes - 1);
      }

      item.selected = false;
    }

    if (!previouslySelected) {
      option.selected = true;
      option.votes++;
    }
  }

  getTotalVotes(note: Note): number {
    return (note.options ?? []).reduce(
      (total, option) => total + option.votes,
      0
    );
  }

  getVotePercentage(
    note: Note,
    option: PollOption
  ): number {
    const total = this.getTotalVotes(note);

    return total === 0
      ? 0
      : Math.round(option.votes / total * 100);
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