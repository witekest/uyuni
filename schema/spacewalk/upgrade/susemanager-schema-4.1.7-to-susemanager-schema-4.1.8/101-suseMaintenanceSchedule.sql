--
-- Copyright (c) 2020 SUSE LLC
--
-- This software is licensed to you under the GNU General Public License,
-- version 2 (GPLv2). There is NO WARRANTY for this software, express or
-- implied, including the implied warranties of MERCHANTABILITY or FITNESS
-- FOR A PARTICULAR PURPOSE. You should have received a copy of GPLv2
-- along with this software; if not, see
-- http://www.gnu.org/licenses/old-licenses/gpl-2.0.txt.
--

CREATE TABLE IF NOT EXISTS suseMaintenanceScheduleData
(
  id          NUMERIC NOT NULL
              CONSTRAINT suse_scheddat_id_pk PRIMARY KEY,
  -- org_id ???
  label       VARCHAR(128) NOT NULL,
  ical        TEXT NOT NULL,
  created     TIMESTAMPTZ
              DEFAULT (current_timestamp) NOT NULL,
  modified    TIMESTAMPTZ
              DEFAULT (current_timestamp) NOT NULL
);

CREATE SEQUENCE IF NOT EXISTS suse_scheddat_id_seq;

CREATE UNIQUE INDEX IF NOT EXISTS suse_scheddat_label_uq
  ON suseMaintenanceScheduleData(label);


CREATE TABLE IF NOT EXISTS suseMaintenanceSchedule
(
  id          NUMERIC NOT NULL
              CONSTRAINT suse_mtsched_id_pk PRIMARY KEY,
  org_id      NUMERIC NOT NULL
              CONSTRAINT suse_mtsched_oid_fk
              REFERENCES web_customer(id)
              ON DELETE CASCADE,
  name        VARCHAR(128) NOT NULL,
  sched_type  VARCHAR(10) NOT NULL,
  url         VARCHAR(1024),
  ical_id     NUMERIC
              CONSTRAINT suse_mtsched_icid_fk
              REFERENCES suseMaintenanceScheduleData(id)
              ON DELETE SET NULL,
  created     TIMESTAMPTZ
              DEFAULT (current_timestamp) NOT NULL,
  modified    TIMESTAMPTZ
              DEFAULT (current_timestamp) NOT NULL
);

CREATE SEQUENCE IF NOT EXISTS suse_mtsched_id_seq;

CREATE UNIQUE INDEX IF NOT EXISTS suse_mtsched_oid_name_uq
  ON suseMaintenanceSchedule(org_id, name);

-- trigger

create or replace function suse_scheddat_mod_trig_fun() returns trigger as
$$
begin
        new.modified := current_timestamp;
        return new;
end;
$$ language plpgsql;

drop trigger if exists suse_scheddat_mod_trig ON suseMaintenanceScheduleData;
create trigger
suse_scheddat_mod_trig
before insert or update on suseMaintenanceScheduleData
for each row
execute procedure suse_scheddat_mod_trig_fun();


create or replace function suse_mtsched_mod_trig_fun() returns trigger as
$$
begin
        new.modified := current_timestamp;
        return new;
end;
$$ language plpgsql;

drop trigger if exists suse_mtsched_mod_trig ON suseMaintenanceSchedule;
create trigger
suse_mtsched_mod_trig
before insert or update on suseMaintenanceSchedule
for each row
execute procedure suse_mtsched_mod_trig_fun();
