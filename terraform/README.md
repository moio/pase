# Terraform configuration for PaSe playground

Files in this directory allow to set up an AWS host with a playground for PaSe.

In order to deploy a playground:

- obtain an AWS account, specifically an Access Key ID and a Secret Access Key
- save your Access Key ID and a Secret Access Key in `~/.aws/credentials` in the format below
```
[default]
aws_access_key_id=AKIAIOSFODNN7EXAMPLE
aws_secret_access_key=wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY
```
- obtain [an SSH key pair](http://docs.aws.amazon.com/AWSEC2/latest/UserGuide/ec2-key-pairs.html#having-ec2-create-your-key-pair) valid for that account
- copy `variables.tf.example` to `variables.tf` and complete commented-out variables
- install [Terraform](https://www.terraform.io/downloads.html)
- run:
```
terraform init
terraform apply
```

Refer to the [official guides](https://www.terraform.io/docs/index.html) for a general understanding of Terraform and full commands.

When Terraform has finished a DNS name will be shown as output, you can SSH into the machine via:

```
ssh -i ~/.ssh/moio.pem ec2-user@$DOMAIN_NAME_HERE
```

## Re-apply configuration

```
terraform taint module.server.null_resource.host_salt_configuration
terraform applyterraform init
terraform apply
```
