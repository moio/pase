resource "aws_vpc" "main" {
  cidr_block           = "172.16.0.0/16"
  enable_dns_support   = true
  enable_dns_hostnames = true

  tags = {
    Name = "${var.name_prefix}vpc"
  }
}

resource "aws_internet_gateway" "main" {
  vpc_id = aws_vpc.main.id

  tags = {
    Name = "${var.name_prefix}internet-gateway"
  }
}

resource "aws_eip" "nat_eip" {
  vpc = true
  tags = {
    Name = "${var.name_prefix}nat-eip"
  }
}

resource "aws_nat_gateway" "nat" {
  allocation_id = aws_eip.nat_eip.id
  subnet_id     = aws_subnet.public.id

  depends_on = [aws_internet_gateway.main]

  tags = {
    Name = "${var.name_prefix}nat"
  }
}

resource "aws_route_table" "public" {
  vpc_id = aws_vpc.main.id

  route {
    cidr_block = "0.0.0.0/0"
    gateway_id = aws_internet_gateway.main.id
  }

  tags = {
    Name = "${var.name_prefix}public-route-table"
  }
}

resource "aws_main_route_table_association" "vpc_internet" {
  vpc_id         = aws_vpc.main.id
  route_table_id = aws_route_table.public.id
}

resource "aws_subnet" "public" {
  availability_zone       = var.availability_zone
  vpc_id                  = aws_vpc.main.id
  cidr_block              = "172.16.0.0/24"
  map_public_ip_on_launch = true

  tags = {
    Name = "${var.name_prefix}public-subnet"
  }
}

resource "aws_route_table_association" "public" {
  subnet_id      = aws_subnet.public.id
  route_table_id = aws_route_table.public.id
}

resource "aws_vpc_dhcp_options" "dhcp_options" {
  domain_name         = var.region == "us-east-1" ? "ec2.internal" : "${var.region}.compute.internal"
  domain_name_servers = ["AmazonProvidedDNS"]

  tags = {
    Name = "${var.name_prefix}dhcp-option-set"
  }
}

resource "aws_vpc_dhcp_options_association" "vpc_dhcp_options" {
  vpc_id          = aws_vpc.main.id
  dhcp_options_id = aws_vpc_dhcp_options.dhcp_options.id
}

resource "aws_security_group" "public" {
  name        = "${var.name_prefix}public-security-group"
  description = "Allow SSH connections from whitelisted IPs; allow all outbound connections"
  vpc_id      = aws_vpc.main.id

  ingress {
    from_port   = 22
    to_port     = 22
    protocol    = "tcp"
    cidr_blocks = formatlist("%s/32", var.ssh_allowed_ips)
  }

  ingress {
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  lifecycle {
    create_before_destroy = true
  }

  tags = {
    Name = "${var.name_prefix}public-security-group"
  }
}

output "configuration" {
  depends_on = [aws_route_table_association.public]
  value = {
    subnet_id         = aws_subnet.public.id
    security_group_id = aws_security_group.public.id
  }
}
