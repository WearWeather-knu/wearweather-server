create table if not exists public.clothes (
    id           bigint generated always as identity not null,
    name         character varying(100) not null,
    category     character varying(30)  not null,
    image_url    character varying(255) null,
    original_url character varying(255) null,
    min_temp     real                   not null,
    max_temp     real                   not null,
    is_active    boolean                null default true,
    constraint clothes_pkey primary key (id),
    constraint clothes_category_check check (
        (category)::text = any (
            array[
                'OUTER'::character varying,
                'TOP'::character varying,
                'BOTTOM'::character varying,
                'ACC'::character varying,
                'SHOES'::character varying,
                'BAG'::character varying
            ]::text[]
        )
    ),
    constraint clothes_temperature_check check (
        (min_temp >= -30) and (min_temp <= 50)
        and (max_temp >= -30) and (max_temp <= 50)
        and (min_temp <= max_temp)
    )
);

create unique index if not exists uq_clothes_original_url
    on public.clothes using btree (original_url)
    where (original_url is not null);

create table if not exists public.clothes_tops (
    clothes_id    bigint                 not null,
    sleeve_length character varying(20)  null,
    thickness     character varying(20)  not null,
    fit           character varying(20)  null,
    material      character varying(50)  not null,
    color         character varying(30)  not null,
    constraint clothes_tops_pkey primary key (clothes_id),
    constraint clothes_tops_clothes_id_fkey foreign key (clothes_id) references public.clothes(id) on delete cascade
);

create table if not exists public.clothes_outers (
    clothes_id    bigint                 not null,
    thickness     character varying(20)  not null,
    fit           character varying(20)  null,
    is_windproof  boolean                null default false,
    is_waterproof boolean                null default false,
    material      character varying(50)  not null,
    color         character varying(30)  not null,
    constraint clothes_outers_pkey primary key (clothes_id),
    constraint clothes_outers_clothes_id_fkey foreign key (clothes_id) references public.clothes(id) on delete cascade
);

create table if not exists public.clothes_bottoms (
    clothes_id bigint                 not null,
    length     character varying(20)  null,
    fit        character varying(20)  null,
    material   character varying(50)  not null,
    color      character varying(30)  not null,
    constraint clothes_bottoms_pkey primary key (clothes_id),
    constraint clothes_bottoms_clothes_id_fkey foreign key (clothes_id) references public.clothes(id) on delete cascade
);

create table if not exists public.clothes_acc (
    clothes_id   bigint                not null,
    type         character varying(30) not null,
    warmth_bonus integer               null default 0,
    color        character varying(30) null,
    constraint clothes_acc_pkey primary key (clothes_id),
    constraint clothes_acc_clothes_id_fkey foreign key (clothes_id) references public.clothes(id) on delete cascade,
    constraint clothes_acc_warmth_bonus_check check (
        (warmth_bonus >= 0) and (warmth_bonus <= 3)
    )
);

create table if not exists public.clothes_shoes (
    clothes_id    bigint                not null,
    type          character varying(30) not null,
    is_waterproof boolean               null default false,
    material      character varying(50) null,
    color         character varying(30) not null,
    constraint clothes_shoes_pkey primary key (clothes_id),
    constraint clothes_shoes_clothes_id_fkey foreign key (clothes_id) references public.clothes(id) on delete cascade
);

create table if not exists public.clothes_bags (
    clothes_id    bigint                not null,
    type          character varying(30) not null,
    material      character varying(50) null,
    color         character varying(30) not null,
    is_waterproof boolean               not null default false,
    constraint clothes_bags_pkey primary key (clothes_id),
    constraint clothes_bags_clothes_id_fkey foreign key (clothes_id) references public.clothes(id)
);

create table if not exists public.users (
    id                 uuid                     not null,
    email              character varying(100)   not null,
    nickname           character varying(30)    not null,
    gender             character varying(20)    null,
    age                integer                  null,
    sensitivity_offset real                     null default 0.0,
    created_at         timestamp with time zone null default now(),
    style_preference   character varying(20)    null,
    constraint users_pkey primary key (id),
    constraint users_email_key unique (email),
    constraint users_id_fkey foreign key (id) references auth.users(id) on delete cascade,
    constraint users_age_check check ((age > 0) and (age < 150)),
    constraint users_gender_check check (
        (gender)::text = any (array['MALE'::character varying, 'FEMALE'::character varying]::text[])
    ),
    constraint users_style_preference_check check (
        (style_preference)::text = any (
            array[
                'CASUAL'::character varying, 'SPORTY'::character varying, 'FORMAL'::character varying,
                'STREET'::character varying, 'VINTAGE'::character varying, 'MINIMAL'::character varying,
                'OUTDOOR'::character varying, 'PREPPY'::character varying, 'BOHEMIAN'::character varying,
                'CLASSIC'::character varying, 'ROMANTIC'::character varying, 'WORKWEAR'::character varying
            ]::text[]
        )
    )
);

create table if not exists public.user_clothes (
    user_id    uuid   not null,
    clothes_id bigint not null,
    is_like    boolean null default false,
    constraint user_clothes_pkey primary key (user_id, clothes_id),
    constraint user_clothes_clothes_id_fkey foreign key (clothes_id) references public.clothes(id),
    constraint user_clothes_user_id_fkey foreign key (user_id) references public.users(id)
);
