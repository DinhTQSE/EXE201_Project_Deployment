alter table gamification_profiles
    add column if not exists last_activity_date date;

update gamification_profiles
set last_activity_date = current_date
where current_streak > 0
  and last_activity_date is null;
