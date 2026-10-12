import { Component, DestroyRef, OnInit, computed, inject, input, output, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { AuthService } from '../../../../core/auth/auth.service';
import { apiErrorMessage } from '../../../../core/http/api-error';
import { SharedHouseService } from '../../../../core/shared-house/shared-house.service';
import { Modal } from '../../../../shared/components/modal/modal';
import { AVATAR_COLORS, RoommateAvatar } from '../../../../shared/components/roommate-avatar/roommate-avatar';
import { Roommate } from '../../../../shared/models/roommate.model';

/** Taille finale de la photo (carré, en pixels) : assez pour un avatar, et léger à stocker. */
const PHOTO_SIZE = 256;
/** Diamètre du cercle de recadrage à l'écran (doit correspondre au CSS .crop-area). */
const CROP_SIZE = 220;
const MAX_ZOOM = 3;

/** Photo en cours de recadrage : l'image chargée, le zoom et le décalage (en pixels écran). */
interface CropState {
  img: HTMLImageElement;
  url: string;
  zoom: number;
  x: number;
  y: number;
}

/** Pop-up « Photo de profil » : choisir une couleur OU une photo, que l'on peut recadrer et zoomer. */
@Component({
  selector: 'app-edit-avatar-dialog',
  imports: [Modal, RoommateAvatar],
  templateUrl: './edit-avatar-dialog.html',
  styleUrl: './edit-avatar-dialog.css',
})
export class EditAvatarDialog implements OnInit {
  private readonly auth = inject(AuthService);
  private readonly houses = inject(SharedHouseService);
  private readonly destroyRef = inject(DestroyRef);

  readonly roommate = input.required<Roommate>();
  readonly closed = output<void>();

  protected readonly colors = AVATAR_COLORS;
  protected readonly maxZoom = MAX_ZOOM;
  protected readonly color = signal<string | null>(null);
  protected readonly image = signal<string | null>(null);
  protected readonly crop = signal<CropState | null>(null);
  protected readonly saving = signal(false);
  protected readonly error = signal<string | null>(null);

  /** Glisser-déposer en cours : point de départ du pointeur et position de l'image à ce moment-là. */
  private drag: { pointerX: number; pointerY: number; startX: number; startY: number } | null = null;

  /** La couleur affichée : celle choisie, sinon la couleur par défaut du colocataire. */
  protected readonly currentColor = computed(
    () => this.color() ?? AVATAR_COLORS[this.roommate().id % AVATAR_COLORS.length],
  );

  /** Aperçu en direct, avant d'enregistrer. */
  protected readonly preview = computed(() => ({
    ...this.roommate(),
    avatarColor: this.color(),
    avatarImage: this.image(),
  }));

  /** Position et taille de l'image dans le cercle de recadrage (pour le template). */
  protected readonly cropStyle = computed(() => {
    const c = this.crop();
    if (!c) return null;
    const { width, height } = displayedSize(c);
    return {
      width: `${width}px`,
      height: `${height}px`,
      transform: `translate(${c.x - width / 2}px, ${c.y - height / 2}px)`,
    };
  });

  constructor() {
    this.destroyRef.onDestroy(() => this.releaseCrop());
  }

  ngOnInit(): void {
    this.color.set(this.roommate().avatarColor ?? null);
    this.image.set(this.roommate().avatarImage ?? null);
  }

  /** Choisir une couleur remplace la photo : l'avatar redevient les initiales sur cette couleur. */
  protected pickColor(color: string): void {
    this.cancelCrop();
    this.color.set(color);
    this.image.set(null);
  }

  protected removePhoto(): void {
    this.cancelCrop();
    this.image.set(null);
  }

  /** Une photo choisie dans la galerie s'ouvre d'abord dans le recadrage. */
  protected onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    input.value = ''; // permet de rechoisir la même photo plus tard
    if (!file) return;

    if (!file.type.startsWith('image/')) {
      this.error.set('Choisissez une image (JPG, PNG…).');
      return;
    }
    this.error.set(null);
    const url = URL.createObjectURL(file);
    const img = new Image();
    img.onload = () => {
      this.releaseCrop();
      this.crop.set({ img, url, zoom: 1, x: 0, y: 0 });
    };
    img.onerror = () => {
      URL.revokeObjectURL(url);
      this.error.set('Impossible de lire cette image. Essayez une photo au format JPG ou PNG.');
    };
    img.src = url;
  }

  // ---------- Recadrage : déplacer (souris ou doigt) et zoomer ----------

  protected startDrag(event: PointerEvent): void {
    const c = this.crop();
    if (!c) return;
    (event.target as HTMLElement).setPointerCapture(event.pointerId);
    this.drag = { pointerX: event.clientX, pointerY: event.clientY, startX: c.x, startY: c.y };
  }

  protected moveDrag(event: PointerEvent): void {
    const c = this.crop();
    if (!c || !this.drag) return;
    const x = this.drag.startX + event.clientX - this.drag.pointerX;
    const y = this.drag.startY + event.clientY - this.drag.pointerY;
    this.crop.set(clamp({ ...c, x, y }));
  }

  protected endDrag(): void {
    this.drag = null;
  }

  protected setZoom(value: string | number): void {
    const c = this.crop();
    if (!c) return;
    const zoom = Math.min(MAX_ZOOM, Math.max(1, Number(value)));
    this.crop.set(clamp({ ...c, zoom }));
  }

  /** Molette de la souris : zoom avant / arrière. */
  protected onWheel(event: WheelEvent): void {
    const c = this.crop();
    if (!c) return;
    event.preventDefault();
    this.setZoom(c.zoom - event.deltaY * 0.002);
  }

  /** « Utiliser cette photo » : on découpe le cercle visible en une image de 256 × 256. */
  protected applyCrop(): void {
    const c = this.crop();
    if (!c) return;
    const canvas = document.createElement('canvas');
    canvas.width = PHOTO_SIZE;
    canvas.height = PHOTO_SIZE;
    const ctx = canvas.getContext('2d');
    if (!ctx) {
      this.error.set('Votre navigateur ne permet pas de recadrer la photo.');
      return;
    }
    // Ce qu'on voit dans le cercle (CROP_SIZE px) est redessiné à l'échelle PHOTO_SIZE px.
    const ratio = PHOTO_SIZE / CROP_SIZE;
    const { width, height } = displayedSize(c);
    ctx.fillStyle = '#fff';
    ctx.fillRect(0, 0, PHOTO_SIZE, PHOTO_SIZE);
    ctx.drawImage(
      c.img,
      (CROP_SIZE / 2 + c.x - width / 2) * ratio,
      (CROP_SIZE / 2 + c.y - height / 2) * ratio,
      width * ratio,
      height * ratio,
    );
    this.image.set(canvas.toDataURL('image/jpeg', 0.85));
    this.cancelCrop();
  }

  protected cancelCrop(): void {
    this.releaseCrop();
    this.crop.set(null);
  }

  protected save(): void {
    this.saving.set(true);
    this.error.set(null);
    this.auth
      .updateAvatar({ color: this.color(), image: this.image() })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          // Les membres de la coloc (liste, accueil...) affichent aussi le nouvel avatar.
          this.houses.load(true).catch(() => undefined);
          this.closed.emit();
        },
        error: (err) => {
          this.error.set(apiErrorMessage(err));
          this.saving.set(false);
        },
      });
  }

  /** Libère la mémoire occupée par la photo chargée. */
  private releaseCrop(): void {
    const c = this.crop();
    if (c) URL.revokeObjectURL(c.url);
  }
}

/** Taille de l'image à l'écran : au zoom 1, son plus petit côté remplit exactement le cercle. */
function displayedSize(c: CropState): { width: number; height: number } {
  const scale = (CROP_SIZE / Math.min(c.img.naturalWidth, c.img.naturalHeight)) * c.zoom;
  return { width: c.img.naturalWidth * scale, height: c.img.naturalHeight * scale };
}

/** Empêche de déplacer l'image au point de laisser un bord vide dans le cercle. */
function clamp(c: CropState): CropState {
  const { width, height } = displayedSize(c);
  const maxX = (width - CROP_SIZE) / 2;
  const maxY = (height - CROP_SIZE) / 2;
  return { ...c, x: Math.max(-maxX, Math.min(maxX, c.x)), y: Math.max(-maxY, Math.min(maxY, c.y)) };
}