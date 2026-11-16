-- What the policy gives, in the source's words (the detail page's 지원 내용); the summary stays the short explanation.
alter table policy add column support_content text;
-- The institution that runs the policy (shown beside the region: a 대구 policy open to residents nationwide).
alter table policy add column institution varchar(200);
-- Only for people not in work (미취업자): left out for someone who searches as employed.
alter table policy add column unemployed_only boolean not null default false;

-- Real notices now come from the official API: the screen-check samples go.
delete from policy where source = 'sample';
