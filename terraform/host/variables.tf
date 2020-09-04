variable "name" {
  description = "Name of this instance"
  type        = string
}

variable "instance_type" {
  description = "EC2 instance type, https://aws.amazon.com/ec2/instance-types/"
  default     = "t3a.medium"
}

variable "volume_size" {
  description = "Size of the main volume in GB"
  default     = 2000
}

variable "network_configuration" {
  description = "Use module.network.configuration"
  type        = map(any)
}

variable "key_name" {
  description = "the name of the SSH key to use to deploy the host. Must be created in AWS manually"
  type        = string
}

variable "key_file" {
  description = "Path to the private key file (pem) corresponding to key_name"
  type        = string
}

variable "region" {
  description = "Region where the instance is created"
  default     = "eu-central-1"
}

variable "availability_zone" {
  description = "Availability zone where the instance is created"
  default     = "eu-central-1a"
}

variable "name_prefix" {
  description = "A prefix for names of objects created by this module"
  default     = "pase-"
}

variable "timezone" {
  description = "Timezone setting for this VM"
  default     = "Europe/Berlin"
}