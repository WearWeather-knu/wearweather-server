# wearweather-server

This repository is organized as a backend monorepo.

## Structure

- `spring-boot/`: Spring Boot API server
- `supabase/`: Supabase local config, migrations, seed data, and edge functions

## Spring Boot

```sh
cd spring-boot
./gradlew bootRun
```

## Supabase

```sh
supabase init
```

After running `supabase init`, keep generated config and database migrations in this directory.
