create table if not exists public.clothes_bags (
    clothes_id bigint primary key references public.clothes(id) on delete cascade,
    type varchar(30) not null,
    material varchar(50),
    color varchar(30) not null,
    is_waterproof boolean not null default false
);

create unique index if not exists uq_clothes_original_url
    on public.clothes (original_url)
    where original_url is not null;

alter table public.clothes
    add constraint clothes_category_check
    check (category in ('OUTER', 'TOP', 'BOTTOM', 'ACC', 'SHOES', 'BAG'));

alter table public.clothes
    add constraint clothes_temperature_check
    check (min_temp between -30 and 50 and max_temp between -30 and 50 and min_temp <= max_temp);

alter table public.clothes_acc
    add constraint clothes_acc_warmth_bonus_check
    check (warmth_bonus between 0 and 3);

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
