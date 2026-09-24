# Ledjr

<!-- one-line description of what this project actually does — fill this in -->

## Running the backend with Docker

Build the image (run from the repo root):
\`\`\`bash
docker build -t ledjr-backend ./backend
\`\`\`

Run it:
\`\`\`bash
docker run ledjr-backend
\`\`\`

> Note: if you get a "permission denied" error connecting to the Docker
> daemon, either prefix these commands with `sudo`, or run
> `sudo usermod -aG docker $USER` once and log out/back in to fix it
> permanently.