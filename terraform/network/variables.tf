variable "region" {
  description = "Region where the instance is created"
  default     = "eu-central-1"
}

variable "availability_zone" {
  description = "Availability zone where the instance is created"
  default     = "eu-central-1a"
}

variable "ssh_allowed_ips" {
  description = "IP addresses allowed to open SSH connections"
  default     = []
}

variable "name_prefix" {
  description = "A prefix for names of objects created by this module"
  default     = "pase-"
}
