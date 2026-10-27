/// <reference types="vite/client" />

// the env vars the frontend reads (only VITE_ ones get exposed to the browser)
interface ImportMetaEnv {
  readonly VITE_API_URL?: string;
}
