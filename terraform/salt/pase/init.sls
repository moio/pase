java-11-openjdk-devel:
  pkg.installed

git-core:
  pkg.installed

bzip2:
  pkg.installed

pase_repo:
  cmd.run:
    - name: git clone https://github.com/moio/pase.git
    - cwd: /home/ec2-user
    - creates: /home/ec2-user/pase

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

build_pase:
  cmd.run:
    - name: mvn clean install
    - cwd: /home/ec2-user/pase
