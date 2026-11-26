# Distribucion automatica de Android

Cada push a `main` (normalmente al integrar un PR) ejecuta pruebas, lint y una compilacion del APK. GitHub conserva el APK como artefacto por 14 dias. Si la configuracion de Firebase esta completa, tambien lo publica en App Distribution y avisa al grupo de testers.

La distribucion no instala actualizaciones silenciosamente: Firebase avisa a los testers y cada persona acepta/instala la nueva version desde el enlace o la experiencia de testers.

## 1. Preparar el firmado estable

Android solo permite actualizar una instalacion si el APK nuevo esta firmado con la misma clave. Configura una clave de debug estable para CI; no la subas al repositorio ni la pegues en chats o issues.

En Linux, en la computadora donde se genera el APK que ya instalaste, comprueba si existe `~/.android/debug.keystore`. Si no existe, crea una clave de debug compatible:

```bash
mkdir -p ~/.android
keytool -genkeypair -v \
  -keystore ~/.android/debug.keystore \
  -storepass android \
  -alias androiddebugkey \
  -keypass android \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -dname "CN=Android Debug,O=Android,C=US"
```

Codifica el archivo localmente y guarda el resultado directamente como secreto de GitHub. No lo publiques ni lo compartas:

```bash
base64 -w0 ~/.android/debug.keystore
```

En `Settings > Secrets and variables > Actions > New repository secret`, crea `ANDROID_DEBUG_KEYSTORE_BASE64` y pega ahi el resultado. El workflow valida que la clave tenga el alias y contrasena de debug esperados. Si la app instalada se firmo con otra clave, habra que desinstalarla una sola vez antes de instalar la primera version distribuida por Firebase.

## 2. Configurar la app en Firebase

En Firebase Console, crea o selecciona el proyecto, registra una app Android con el identificador exacto `org.bookie.app` y habilita **App Distribution**. Crea un grupo de testers (por ejemplo, `bookie-testers`) e invita las cuentas que deban recibir cada version.

## 3. Dar acceso a GitHub sin guardar una clave de Google

Se usa Workload Identity Federation: GitHub obtiene credenciales temporales y no se guarda una clave de cuenta de servicio de larga duracion en el repositorio. En Cloud Shell, selecciona el proyecto correcto y ejecuta el siguiente bloque, reemplazando `PROJECT_ID` por el ID real del proyecto de Google Cloud/Firebase:

```bash
export GCP_PROJECT_ID="PROJECT_ID"
export GITHUB_REPOSITORY="tomasbondUade/bookie-frontend"
export GCP_SERVICE_ACCOUNT_ID="bookie-app-distributor"
export GCP_WIF_POOL_ID="github-actions"
export GCP_WIF_PROVIDER_ID="bookie-frontend-main"

export GCP_PROJECT_NUMBER="$(gcloud projects describe "$GCP_PROJECT_ID" --format='value(projectNumber)')"
export GCP_SERVICE_ACCOUNT="${GCP_SERVICE_ACCOUNT_ID}@${GCP_PROJECT_ID}.iam.gserviceaccount.com"

gcloud iam service-accounts create "$GCP_SERVICE_ACCOUNT_ID" --project "$GCP_PROJECT_ID"
gcloud projects add-iam-policy-binding "$GCP_PROJECT_ID" \
  --member="serviceAccount:$GCP_SERVICE_ACCOUNT" \
  --role="roles/firebaseappdistro.admin"
gcloud projects add-iam-policy-binding "$GCP_PROJECT_ID" \
  --member="serviceAccount:$GCP_SERVICE_ACCOUNT" \
  --role="roles/serviceusage.apiKeysViewer"

gcloud iam workload-identity-pools create "$GCP_WIF_POOL_ID" \
  --project="$GCP_PROJECT_ID" --location=global \
  --display-name="GitHub Actions for Bookie"

gcloud iam workload-identity-pools providers create-oidc "$GCP_WIF_PROVIDER_ID" \
  --project="$GCP_PROJECT_ID" --location=global \
  --workload-identity-pool="$GCP_WIF_POOL_ID" \
  --issuer-uri="https://token.actions.githubusercontent.com" \
  --attribute-mapping="google.subject=assertion.sub,attribute.repository=assertion.repository,attribute.ref=assertion.ref" \
  --attribute-condition="assertion.repository == '$GITHUB_REPOSITORY' && assertion.ref == 'refs/heads/main'"

gcloud iam service-accounts add-iam-policy-binding "$GCP_SERVICE_ACCOUNT" \
  --project="$GCP_PROJECT_ID" \
  --role="roles/iam.workloadIdentityUser" \
  --member="principalSet://iam.googleapis.com/projects/$GCP_PROJECT_NUMBER/locations/global/workloadIdentityPools/$GCP_WIF_POOL_ID/attribute.repository/$GITHUB_REPOSITORY"

printf 'GCP_WORKLOAD_IDENTITY_PROVIDER=projects/%s/locations/global/workloadIdentityPools/%s/providers/%s\n' \
  "$GCP_PROJECT_NUMBER" "$GCP_WIF_POOL_ID" "$GCP_WIF_PROVIDER_ID"
printf 'GCP_SERVICE_ACCOUNT=%s\n' "$GCP_SERVICE_ACCOUNT"
```

El proveedor acepta tokens unicamente del repositorio `tomasbondUade/bookie-frontend` y de la rama `main`. Guarda los valores que imprime el bloque; no son claves privadas, pero limita su acceso a variables de Actions.

## 4. Configurar GitHub Actions

En `Settings > Secrets and variables > Actions > Variables`, crea estas variables del repositorio:

| Variable | Valor |
| --- | --- |
| `BOOKIE_API_BASE_URL` | URL HTTPS publica del backend Bookie, sin una ruta de endpoint al final. |
| `SUPABASE_URL` | URL publica del proyecto Supabase Auth usado por Bookie. |
| `SUPABASE_PUBLISHABLE_KEY` | Clave publishable/anon de Supabase; nunca una `service_role` ni una clave `sb_secret_`. |
| `GCP_PROJECT_ID` | ID del proyecto Firebase/Google Cloud. |
| `GCP_WORKLOAD_IDENTITY_PROVIDER` | El identificador completo impreso por el bloque de Cloud Shell. |
| `GCP_SERVICE_ACCOUNT` | El correo impreso por el bloque de Cloud Shell. |
| `FIREBASE_APP_ID` | App ID de Android que muestra Firebase en la configuracion del proyecto. |
| `FIREBASE_TESTER_GROUPS` | Alias del grupo de App Distribution, por ejemplo `bookie-testers`. |

La URL y clave publishable de Supabase quedan incorporadas en el APK, igual que en cualquier app cliente; no guardes claves de servidor en estas variables.

Despues de guardar las variables y el secreto de firmado, integra un PR a `main`. En la pestaña **Actions**, abre la ejecucion **Android distribution**. El APK se adjunta como artefacto y, con Firebase configurado, tambien se distribuye al grupo.

## Referencias

- [Firebase App Distribution: CI/CD y testers](https://firebase.google.com/docs/app-distribution/best-practices-distributing-android-apps-to-qa-testers-with-ci-cd)
- [Distribuir APKs con Firebase CLI](https://firebase.google.com/docs/app-distribution/android/distribute-cli)
- [Autenticacion de GitHub Actions a Google Cloud con Workload Identity Federation](https://github.com/google-github-actions/auth)
