os_pool_repo:
  pkgrepo.managed:
    - baseurl: http://download.opensuse.org/distribution/leap/{{ grains['osrelease'] }}/repo/oss/

os_update_repo:
  pkgrepo.managed:
    - baseurl: http://download.opensuse.org/update/leap/{{ grains['osrelease'] }}/oss/

refresh_repos:
  cmd.run:
    - name: zypper --non-interactive --gpg-auto-import-keys refresh
