#!/usr/bin/env bash
set -euo pipefail
echo "=== Preparando repositorio en GitHub ==="

if ! command -v gh >/dev/null 2>&1; then
  echo "** Necesitas GitHub CLI (gh). Instalar con: brew install gh  (macOS) o https://cli.github.com/"
  exit 1
fi
if ! command -v git >/dev/null 2>&1; then
  echo "** Necesitas Git. Instalar con: brew install git  (macOS) o https://git-scm.com/"
  exit 1
fi

read -p "Usuario de GitHub: " GH_USER
if [[ -z "${GH_USER}" ]]; then
  echo "** Usuario vacio"
  exit 1
fi

DEF_REPO="$(basename "$(pwd)")"
read -p "Nombre del repositorio [${DEF_REPO}]: " GH_REPO
GH_REPO="${GH_REPO:-$DEF_REPO}"

echo "Iniciando sesion en GitHub..."
gh auth login

echo "Creando repo https://github.com/${GH_USER}/${GH_REPO} ..."
if ! gh repo create "${GH_USER}/${GH_REPO}" --public --source . --remote origin --push -y; then
  git init
  git add .
  git commit -m "chore: initial commit" || true
  git branch -M main
  git remote remove origin || true
  git remote add origin "https://github.com/${GH_USER}/${GH_REPO}.git"
  git push -u origin main
fi

echo
echo "** Listo. Abre GitHub > Actions para ver el build."
