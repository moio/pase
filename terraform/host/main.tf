locals {
  instance_family = split(".", var.instance_type)[0]
  arm_instance = contains(["a1", "t4g", "m6g"], local.instance_family)
}

data "aws_ami" "opensuse152" {
  most_recent = true
  name_regex  = "^openSUSE-Leap-15-2-v"
  owners      = ["679593333241"]

  filter {
    name   = "architecture"
    values = [local.arm_instance ? "arm64" : "x86_64"]
  }

  filter {
    name   = "virtualization-type"
    values = ["hvm"]
  }

  filter {
    name   = "root-device-type"
    values = ["ebs"]
  }
}

data "template_file" "user_data" {
  template = file("${path.module}/user_data.yaml")
}

resource "aws_instance" "instance" {
  ami                    = data.aws_ami.opensuse152.image_id
  instance_type          = var.instance_type
  availability_zone      = var.availability_zone
  key_name               = var.key_name
  subnet_id              = var.network_configuration["subnet_id"]
  vpc_security_group_ids = [var.network_configuration["security_group_id"]]

  root_block_device {
    volume_size = var.volume_size
  }

  user_data = data.template_file.user_data.rendered

  tags = {
    Name = "${var.name_prefix}${var.name}"
  }

  # HACK
  # SUSE internal openbare AWS accounts add special tags to identify the instance owner ("PrincipalId", "Owner").
  # After the first `apply`, terraform removes those tags. The following block avoids this behavior.
  # The correct way to do it would be by ignoring those tags, which is not supported yet by the AWS terraform provider
  # https://github.com/terraform-providers/terraform-provider-aws/issues/10689
  lifecycle {
    ignore_changes = [tags]
  }
}

resource "null_resource" "host_salt_configuration" {
  depends_on = [aws_instance.instance]

  triggers = {
    domain_id = aws_instance.instance.id
  }

  connection {
    host        = aws_instance.instance.public_dns
    private_key = file(var.key_file)
    user        = "ec2-user"
    timeout     = "120s"
  }

  provisioner "file" {
    source      = "salt"
    destination = "/tmp"
  }

  provisioner "file" {
    content = yamlencode({
      hostname : replace(aws_instance.instance.private_dns, ".${var.region == "us-east-1" ? "ec2.internal" : "${var.region}.compute.internal"}", "")
      domain : var.region == "us-east-1" ? "ec2.internal" : "${var.region}.compute.internal"
      timezone = var.timezone
    })
    destination = "/tmp/grains"
  }

  provisioner "remote-exec" {
    inline = [
      "bash /tmp/salt/wait_for_salt.sh",
      "sudo cp -rf /tmp/grains /etc/salt/grains",
      "sudo cp -rf /tmp/salt /root",
      "sudo salt-call --local --file-root=/root/salt --log-level=info --retcode-passthrough --force-color state.highstate"
    ]
  }
}

resource "aws_eip" "eip" {
  instance = aws_instance.instance.id
  vpc      = true
  tags = {
    Name = "${var.name_prefix}${var.name}-eip"
  }
}

output "public_name" {
  depends_on = [aws_instance.instance, null_resource.host_salt_configuration]
  value      = aws_eip.eip.public_dns
}
