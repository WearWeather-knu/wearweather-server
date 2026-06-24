alter table public.recommendation
    add column if not exists image_url text;

alter table public.recommendation
    add column if not exists bag_id bigint references public.clothes(id) on delete set null;

alter table public.recommendation
    alter column image_url set not null;

create index if not exists idx_recommendation_user_created_at
    on public.recommendation (user_id, created_at desc);
