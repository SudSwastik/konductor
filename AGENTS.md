# Repository Guidelines

## Project Structure & Module Organization

This repository is a flat multi-project workspace. Keep each project at the repository root:

- `publisher/` for publishing source events.
- `projector/` for consuming source events and projecting them to downstream consumers.
- `consumer/` for consumer-side processing.
- `subscription-ui/` for the Next.js UI used to create subscriptions, model flows, and manage configuration.
- `docs/` for architecture notes, contracts, and operations.

Do not add shared library or common configuration folders. Each project owns its build file, dependencies, configuration, and tests. Java projects should use `src/main/java`, `src/test/java`, and `src/main/resources`.

## Build, Test, and Development Commands

No project build files are configured yet. Run commands from the relevant project directory:

- `cd publisher && ./mvnw clean verify` or `./gradlew clean build` to compile, test, and run checks.
- `cd projector && ./mvnw test` or `./gradlew test` to run tests.
- `cd subscription-ui && npm test` to run UI tests once configured.
- `cd subscription-ui && npm run dev` to run Next.js locally.

Commit wrapper scripts such as `mvnw` or `gradlew` inside each Java project.

## Coding Style & Naming Conventions

Use 4-space indentation for Java. Name classes in `PascalCase`, methods and fields in `camelCase`, constants in `UPPER_SNAKE_CASE`, and packages in lowercase.

For Next.js, use TypeScript, component names in `PascalCase`, hooks in `useCamelCase`, and route folders in lowercase kebab-case.

If a formatter or linter is introduced, include it in the default build.

## Testing Guidelines

Place Java tests under each project's `src/test/java/`, mirroring production packages. Use behavior-focused names, for example `createsTaskWhenInputIsValid`. Prefer JUnit 5.

Place UI tests inside `subscription-ui/`, using `__tests__/` or colocated `*.test.tsx` files.

New behavior should include focused tests. Bug fixes should include a regression test that fails without the fix.

## Commit & Pull Request Guidelines

Use concise, imperative commit messages scoped to the affected project, such as `Add publisher event model`, `Wire projector subscription handler`, or `Create subscription UI shell`.

Pull requests should include a summary, verification commands, linked issues when applicable, and screenshots or logs for user-visible behavior.

## Security & Configuration Tips

Do not commit secrets, local credentials, or machine-specific configuration. Use environment variables or ignored local files, and provide safe examples such as `.env.example`.

Keep configuration project-local. If multiple projects need the same value, document the contract in `docs/` rather than creating a shared config module.
