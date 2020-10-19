java-11-openjdk-devel:
  pkg.installed

git-core:
  pkg.installed

bzip2:
  pkg.installed

nodejs-common:
  pkg.installed

maven:
  archive.extracted:
    - name: /opt/
    - source: https://downloads.apache.org/maven/maven-3/3.6.3/binaries/apache-maven-3.6.3-bin.tar.gz
    - source_hash: https://downloads.apache.org/maven/maven-3/3.6.3/binaries/apache-maven-3.6.3-bin.tar.gz.sha512
    - archive_format: tar
    - keep: True
    - overwrite: True
  file.symlink:
    - name: /usr/bin/mvn
    - target: /opt/apache-maven-3.6.3/bin/mvn

pase_repo_present:
  cmd.run:
    - name: git clone https://github.com/moio/pase.git
    - cwd: /opt
    - creates: /opt/pase

pase_repo_updated:
  cmd.run:
    - name: git pull --rebase
    - cwd: /opt/pase

pase_frontend_built:
  cmd.run:
    - name: npm install; npm run build; rm -rf ../src/main/resources/htdocs/*; mkdir -p ../src/main/resources/htdocs; cp -rf ./build/* ../src/main/resources/htdocs/
    - cwd: /opt/pase/frontend

pase_built:
  cmd.run:
    - name: mvn clean install
    - cwd: /opt/pase

pase_launcher:
  file.managed:
    - name: /usr/bin/pase
    - source: /opt/pase/utils/pase
    - user: root
    - group: root
    - mode: 755

/srv/sources:
  file.directory:
    - mode: 777
    - makedirs: True

/srv/index:
  file.directory:
    - mode: 777
    - makedirs: True

prime_index:
  cmd.run:
    - name: pase index /dev/null /srv/index

pase_index_service:
  file.managed:
    - name: /etc/systemd/system/pase-index.service
    - source: /opt/pase/utils/pase-index.service

pase_index_timer:
  file.managed:
    - name: /etc/systemd/system/pase-index.timer
    - source: /opt/pase/utils/pase-index.timer

pase_index_timer_started:
  cmd.run:
    - name: systemctl start pase-index.timer

pase_service:
  file.managed:
    - name: /etc/systemd/system/pase.service
    - source: /opt/pase/utils/pase.service
  service.running:
    - name: pase
    - enable: True

motd:
  file.managed:
    - name: /etc/motd
    - contents: |
       # Welcome to the PaSe Server

        - Indexed sources are placed in /srv/sources
        - Index lives in /srv/index and is updated daily
        - Use `pase index /srv/sources /srv/index` to force reindexing immediately
        - Use `pase search /srv/index patchfile.patch` to search for a patch
