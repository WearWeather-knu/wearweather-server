alter table public.clothes enable row level security;
alter table public.clothes_tops enable row level security;
alter table public.clothes_outers enable row level security;
alter table public.clothes_bottoms enable row level security;
alter table public.clothes_acc enable row level security;
alter table public.clothes_shoes enable row level security;
alter table public.clothes_bags enable row level security;
alter table public.user_clothes enable row level security;

insert into storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
values (
    'clothes-images',
    'clothes-images',
    true,
    5242880,
    array['image/jpeg', 'image/png', 'image/webp']
)
on conflict (id) do update
set public = excluded.public,
    file_size_limit = excluded.file_size_limit,
    allowed_mime_types = excluded.allowed_mime_types;
