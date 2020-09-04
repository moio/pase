include:
  - default.time
  - default.repos

update_packages:
  pkg.uptodate:
    - require:
      - cmd: refresh_repos
