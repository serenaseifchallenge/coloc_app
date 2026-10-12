#!/usr/bin/env bash
# Vérifie que le back respecte l'architecture 3-tiers.
# Usage (depuis la racine du projet) : bash .claude/skills/create-endpoint/scripts/check-layers.sh
# Code de sortie : 0 si tout est conforme, 1 sinon.

BACK="${1:-back/src/main/java/com/coloc/back}"
errors=0

fail() { echo "ERREUR : $1"; errors=$((errors + 1)); }

if [ ! -d "$BACK" ]; then
  echo "Dossier introuvable : $BACK (lancer le script depuis la racine du projet)"
  exit 2
fi

# 1. Les contrôleurs ne touchent ni aux repositories ni aux entités.
for f in $(grep -rlE 'import com\.coloc\.back\.(repository|entity)' "$BACK/controller" 2>/dev/null); do
  fail "$f importe un repository ou une entité (passer par un service et des DTO)"
done

# 2. Pas de @Transactional dans les contrôleurs.
for f in $(grep -rl '@Transactional' "$BACK/controller" 2>/dev/null); do
  fail "$f utilise @Transactional (à placer dans le service)"
done

# 3. Les services ne dépendent pas des contrôleurs.
for f in $(grep -rl 'import com\.coloc\.back\.controller' "$BACK/service" 2>/dev/null); do
  fail "$f (service) dépend d'un contrôleur"
done

# 4. Les repositories ne dépendent ni des services ni des contrôleurs.
for f in $(grep -rlE 'import com\.coloc\.back\.(service|controller)' "$BACK/repository" 2>/dev/null); do
  fail "$f (repository) dépend d'une couche supérieure"
done

# 5. Chaque contrôleur est exposé sous /api/.
for f in "$BACK"/controller/*.java; do
  if grep -q '@RestController' "$f" && ! grep -qE '@RequestMapping\("/api/' "$f"; then
    fail "$f n'est pas exposé sous /api/"
  fi
done

if [ "$errors" -eq 0 ]; then
  echo "OK : architecture 3-tiers respectée."
  exit 0
fi
echo "$errors problème(s) détecté(s)."
exit 1
