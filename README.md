# Konductor

Konductor is a flat multi-project workspace for building an event subscription platform. Each project lives at the repository root and owns its own dependencies, build configuration, tests, and runtime setup.

## Projects

- `konductor-ui/` - Next.js UI for signup and future subscription management flows.
- `publisher/` - planned service for publishing source events.
- `projector/` - Spring Boot scaffold for consuming source events and projecting them downstream.
- `consumer/` - planned service for consumer-side processing.
- `docs/` - planned architecture notes, contracts, and operations documentation.

## Current UI

The active project today is `konductor-ui`, a TypeScript Next.js app.

```bash
cd konductor-ui
npm install
npm run dev
```

Open `http://localhost:3000/signup` to view the signup wizard.

Useful commands:

```bash
npm run lint
npm run build
```

## Repository Conventions

- Keep projects independent at the repository root.
- Do not add shared library or common configuration folders.
- Keep project-specific configuration inside each project.
- Do not commit secrets, local credentials, generated build output, or dependency folders.

## Git

Use feature branches with the `feat/` prefix.

Example:

```bash
git checkout -b feat/signup-wizard
```
