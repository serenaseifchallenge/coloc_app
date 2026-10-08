/** Titres selon les points (paliers de 50). À réutiliser pour la page Points. */
export const TITLES: ReadonlyArray<{ min: number; label: string }> = [
  { min: 0, label: 'Fantôme de la coloc' },
  { min: 50, label: 'Ramasse-poussière' },
  { min: 100, label: 'Petite main' },
  { min: 150, label: 'Coloc de bonne volonté' },
  { min: 200, label: 'Coloc modèle' },
  { min: 250, label: 'Héros du quotidien' },
  { min: 300, label: 'Chasseur de taches' },
  { min: 350, label: 'Pilier de la coloc' },
  { min: 400, label: 'Roi du ménage' },
  { min: 450, label: 'Légende du foyer' },
];

export interface TitleProgress {
  currentTitle: string;
  nextTitle: string | null;
  missingPoints: number;
  percent: number;
}

export function titleFor(points: number | null | undefined): string {
  const value = points ?? 0;
  let label = TITLES[0].label;
  for (const title of TITLES) {
    if (value >= title.min) {
      label = title.label;
    }
  }
  return label;
}

export function titleProgress(points: number | null | undefined): TitleProgress {
  const value = Math.max(points ?? 0, 0);
  const reachedTitles = TITLES.filter((title) => value >= title.min);
  const currentTitle = reachedTitles[reachedTitles.length - 1] ?? TITLES[0];
  const nextTitle = TITLES[reachedTitles.length];

  if (!nextTitle) {
    return { currentTitle: currentTitle.label, nextTitle: null, missingPoints: 0, percent: 100 };
  }

  const percent = Math.round(((value - currentTitle.min) / (nextTitle.min - currentTitle.min)) * 100);
  return {
    currentTitle: currentTitle.label,
    nextTitle: nextTitle.label,
    missingPoints: nextTitle.min - value,
    percent,
  };
}