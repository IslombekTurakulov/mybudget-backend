ALTER TABLE projects
  ADD COLUMN category       VARCHAR(100);

ALTER TABLE projects
  ADD COLUMN category_icon  VARCHAR(10);

ALTER TABLE projects
  ADD CONSTRAINT fk_projects_owner
  FOREIGN KEY (owner_id)
  REFERENCES users(id)
  ON DELETE CASCADE;
