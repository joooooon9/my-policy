-- The service regions a run collected (comma-separated Region names). A region a source collects for the
-- first time fills the list without new-notice events, like a source's first run.
alter table collection_run add column regions varchar(100);

-- Runs before this column existed collected nationwide notices only.
update collection_run set regions = 'NATIONWIDE' where regions is null;
