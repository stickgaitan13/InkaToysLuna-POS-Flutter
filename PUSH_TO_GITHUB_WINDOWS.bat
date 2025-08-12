@echo off
setlocal ENABLEDELAYEDEXPANSION
echo === Preparando repositorio en GitHub ===

where gh >NUL 2>&1
if ERRORLEVEL 1 (
  echo.
  echo ** Necesitas GitHub CLI (gh). Se intentara instalar con winget...
  where winget >NUL 2>&1 || (echo ** winget no disponible. Instala gh desde https://cli.github.com/ y vuelve a ejecutar. & pause & exit /b 1)
  winget install --id GitHub.cli -e --source winget || (echo ** No se pudo instalar gh automaticamente. & pause & exit /b 1)
)

where git >NUL 2>&1 || (echo ** Necesitas Git. Instala desde https://git-scm.com/downloads y vuelve a ejecutar. & pause & exit /b 1)

echo.
echo Ingresa tu usuario de GitHub (sin @):
set /p GH_USER=

if "%GH_USER%"=="" (
  echo ** Usuario vacio. Cancelando.
  pause
  exit /b 1
)

set DEF_REPO=%~n0
if "%DEF_REPO%"=="" set DEF_REPO=inkatoys-pos

echo Nombre del repositorio (ENTER para usar %DEF_REPO%):
set /p GH_REPO=
if "%GH_REPO%"=="" set GH_REPO=%DEF_REPO%

echo Iniciando sesion en GitHub (se abrira el navegador si hace falta)...
gh auth login

echo Creando repo https://github.com/%GH_USER%/%GH_REPO% ...
gh repo create %GH_USER%/%GH_REPO% --public --source . --remote origin --push -y || (
  echo ** Si el repo ya existe, se usara como remoto.
  git init
  git add .
  git commit -m "chore: initial commit" 2>NUL
  git branch -M main
  git remote remove origin 2>NUL
  git remote add origin https://github.com/%GH_USER%/%GH_REPO%.git
  git push -u origin main
)

echo.
echo ** Listo. Ve a GitHub > Actions para ver la construccion.
pause
