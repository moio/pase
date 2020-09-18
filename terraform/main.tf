provider "aws" {
  version = "~> 2.51"
  region  = var.region
}

module "network" {
  source = "./network"

  ssh_allowed_ips = var.ssh_allowed_ips
}

module "server" {
  source                = "./host"
  network_configuration = module.network.configuration
  volume_size           = 2000
  name                  = "server"
  key_name              = var.key_name
  key_file              = var.key_file
  instance_type         = var.instance_type
}

output "host_public_name" {
  value = "${module.server.public_name}"
}
